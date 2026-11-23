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
        var rangeRestarts = 0
        var effectiveTarget = target
        var nameResolved = false
        while (true) {
            try {
                val head = http.head(current)
                val total = head.totalBytes
                if (!nameResolved) {
                    val resolvedName = FileNaming.sanitize(head.fileName ?: current.fileName)
                    if (resolvedName != effectiveTarget.name) {
                        val candidate = File(effectiveTarget.parentFile, resolvedName)
                        val finalTarget = if (candidate.exists() && candidate.absolutePath != effectiveTarget.absolutePath) {
                            File(effectiveTarget.parentFile, FileNaming.unique(resolvedName) { File(effectiveTarget.parentFile, it).exists() })
                        } else {
                            candidate
                        }
                        if (effectiveTarget.exists() && effectiveTarget.absolutePath != finalTarget.absolutePath) {
                            effectiveTarget.renameTo(finalTarget)
                        }
                        effectiveTarget = finalTarget
                        current = current.copy(fileName = finalTarget.name)
                        update(current)
                    }
                    nameResolved = true
                }
                val folder = effectiveTarget.parentFile
                if (folder != null) {
                    folder.mkdirs()
                    if (!folder.isDirectory) {
                        throw DownloadFolderFailure(folder.absolutePath)
                    }
                }
                val fileName = current.fileName
                var downloaded = current.downloadedBytes
                if (total <= 0L || rangesFailed) {
                    val written = http.downloadUnknown(current.copy(fileName = fileName), effectiveTarget) { bytes ->
                        downloaded += bytes
                        current = current.copy(downloadedBytes = downloaded, totalBytes = -1L, resumeSupported = false)
                        update(current)
                    }
                    current = current.copy(downloadedBytes = written, totalBytes = written)
                } else {
                    current = current.copy(totalBytes = total, resumeSupported = head.resumeSupported)
                    update(current)
                    val planner = if (head.resumeSupported && effectiveTarget.exists() && effectiveTarget.length() > 0L && effectiveTarget.length() < total) {
                        SegmentPlanner.resume(
                            size = total,
                            minSplit = settings.minSplitBytes,
                            saved = listOf(SavedSegment(0L, total, effectiveTarget.length()))
                        )
                    } else {
                        SegmentPlanner(total, settings.minSplitBytes)
                    }
                    DiskWriter(effectiveTarget).ensureSize(total)
                    http.download(current.copy(fileName = fileName), effectiveTarget, planner) { bytes ->
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
                    val ok = ChecksumVerifier.verify(effectiveTarget, expected)
                    if (!ok) {
                        val actual = ChecksumVerifier.compute(effectiveTarget, expected.algorithm)
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
                if (e.statusCode == 416) {
                    if (rangeRestarts < 2) {
                        rangeRestarts += 1
                        effectiveTarget.delete()
                        rangesFailed = false
                        current = current.copy(downloadedBytes = 0L)
                        update(current)
                        continue
                    }
                    current = fail(current, DownloadError.Network("The server rejected the requested part of the file (HTTP 416) even after restarting the download from the beginning."))
                    return current
                }
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
            } catch (e: DownloadFolderFailure) {
                current = fail(current, DownloadError.Network("Cannot create download folder: ${e.folder}."))
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

    class DownloadFolderFailure(val folder: String) : Exception("Cannot create download folder.")
}
