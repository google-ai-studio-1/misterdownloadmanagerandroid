package com.misterdownloadmanager.port.core.engine

import com.misterdownloadmanager.port.core.model.Checksum
import com.misterdownloadmanager.port.core.model.DownloadError
import com.misterdownloadmanager.port.core.model.DownloadItem
import com.misterdownloadmanager.port.core.model.DownloadStatus
import com.misterdownloadmanager.port.core.model.Settings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class DownloadEngine(private val settings: Settings) {
    private val limiter = SpeedLimiter(settings.downloadSpeedLimitBytesPerSecond)
    private val http = HttpDownloader(settings, limiter)
    private val _states = MutableStateFlow<Map<Long, DownloadItem>>(emptyMap())
    val states: StateFlow<Map<Long, DownloadItem>> = _states.asStateFlow()

    fun setSpeedLimit(bytesPerSecond: Long?) {
        limiter.set(bytesPerSecond)
    }

    fun speedLimit(): Long? = limiter.get()

    fun register(item: DownloadItem) {
        _states.value = _states.value + (item.id to item)
    }

    fun update(item: DownloadItem) {
        _states.value = _states.value + (item.id to item)
    }

    fun remove(id: Long) {
        _states.value = _states.value - id
    }

    suspend fun run(item: DownloadItem, target: File): DownloadItem {
        var current = item.copy(status = DownloadStatus.DOWNLOADING)
        update(current)
        var attempt = 0
        var rangesFailed = false
        while (true) {
            try {
                val head = http.head(current)
                val total = head.totalBytes
                val fileName = head.fileName ?: current.fileName
                var downloaded = current.downloadedBytes
                if (total <= 0L || rangesFailed) {
                    val written = http.downloadUnknown(current.copy(fileName = fileName), target) { bytes ->
                        downloaded += bytes
                        current = current.copy(downloadedBytes = downloaded, totalBytes = -1L, resumeSupported = false)
                        update(current)
                    }
                    current = current.copy(downloadedBytes = written, totalBytes = written)
                } else {
                    val planner = if (head.resumeSupported && target.exists() && target.length() > 0L && target.length() < total) {
                        SegmentPlanner.resume(
                            size = total,
                            minSplit = settings.minSplitBytes,
                            saved = listOf(SavedSegment(0L, total, target.length()))
                        )
                    } else {
                        SegmentPlanner(total, settings.minSplitBytes)
                    }
                    DiskWriter(target).ensureSize(total)
                    http.download(current.copy(fileName = fileName), target, planner) { bytes ->
                        downloaded += bytes
                        current = current.copy(downloadedBytes = downloaded, totalBytes = total, resumeSupported = head.resumeSupported)
                        update(current)
                    }
                    current = current.copy(downloadedBytes = total.coerceAtLeast(downloaded), totalBytes = total)
                }
                val expected = current.checksum?.let { Checksum.parse(it) } ?: head.checksum
                if (settings.verifyChecksumAfterDownload && expected != null) {
                    current = current.copy(status = DownloadStatus.DOWNLOADING)
                    update(current)
                    val ok = ChecksumVerifier.verify(target, expected)
                    if (!ok) {
                        val actual = ChecksumVerifier.compute(target, expected.algorithm)
                        throw ChecksumFailure(expected, actual)
                    }
                }
                current = current.copy(status = DownloadStatus.COMPLETED, finishedEpochMillis = System.currentTimeMillis())
                update(current)
                return current
            } catch (e: CancellationException) {
                current = current.copy(status = DownloadStatus.STOPPED)
                update(current)
                throw e
            } catch (e: HttpDownloader.RangeNotSupported) {
                rangesFailed = true
                continue
            } catch (e: HttpDownloader.HttpFailure) {
                val retryable = e.statusCode == 429 || e.statusCode in 500..599
                attempt += 1
                if (!retryable || attempt > settings.maxRetries) {
                    current = fail(current, e.toDownloadError())
                    return current
                }
                kotlinx.coroutines.delay(retryWaitMillis(attempt, e.retryAfterMillis))
            } catch (e: ChecksumFailure) {
                current = fail(current, DownloadError.ChecksumMismatch(e.expected.hex, e.actual))
                return current
            } catch (e: Exception) {
                attempt += 1
                if (attempt > settings.maxRetries) {
                    current = fail(current, DownloadError.Network(e.message ?: "Network error."))
                    return current
                }
                kotlinx.coroutines.delay(retryWaitMillis(attempt, null))
            }
        }
    }

    fun retryWaitMillis(failures: Int, retryAfterMillis: Long?): Long {
        if (retryAfterMillis != null) return minOf(retryAfterMillis, 300_000L)
        if (failures <= 1) return 0L
        val exponent = minOf(failures - 2, 10)
        val waited = settings.retryDelayMillis * (1L shl exponent)
        return minOf(waited, 30_000L)
    }

    private fun fail(item: DownloadItem, error: DownloadError): DownloadItem {
        val failed = item.copy(status = DownloadStatus.ERROR, error = error.message())
        update(failed)
        return failed
    }

    class ChecksumFailure(val expected: Checksum, val actual: String) : Exception("Checksum mismatch.")
}
