package com.misterdownloadmanager.port.core.engine

import com.misterdownloadmanager.port.core.model.Checksum
import com.misterdownloadmanager.port.core.model.DownloadError
import com.misterdownloadmanager.port.core.model.DownloadItem
import com.misterdownloadmanager.port.core.model.ProxyConfig
import com.misterdownloadmanager.port.core.model.Settings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

data class HeadInfo(
    val totalBytes: Long,
    val resumeSupported: Boolean,
    val fileName: String?,
    val checksum: Checksum?,
    val contentType: String?
)

class HttpDownloader(
    private val settings: Settings,
    private val limiter: SpeedLimiter
) {
    private fun buildClient(proxy: ProxyConfig): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(settings.connectionTimeoutMillis, TimeUnit.MILLISECONDS)
            .readTimeout(settings.connectionTimeoutMillis, TimeUnit.MILLISECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
        if (proxy.enabled && proxy.isValid()) {
            val type = when (proxy.scheme) {
                com.misterdownloadmanager.port.core.model.ProxyScheme.HTTP -> Proxy.Type.HTTP
                com.misterdownloadmanager.port.core.model.ProxyScheme.SOCKS5 -> Proxy.Type.SOCKS
                com.misterdownloadmanager.port.core.model.ProxyScheme.SOCKS4 -> Proxy.Type.SOCKS
            }
            builder.proxy(Proxy(type, InetSocketAddress(proxy.host, proxy.port)))
            if (!proxy.username.isNullOrBlank()) {
                builder.proxyAuthenticator { _, response ->
                    response.request.newBuilder()
                        .header("Proxy-Authorization", Credentials.basic(proxy.username, proxy.password ?: ""))
                        .build()
                }
            }
        }
        return builder.build()
    }

    suspend fun head(item: DownloadItem): HeadInfo {
        return withContext(Dispatchers.IO) {
            val client = buildClient(settings.proxy)
            val requestBuilder = Request.Builder().url(item.url).head()
            if (!item.username.isNullOrBlank()) {
                requestBuilder.header("Authorization", Credentials.basic(item.username, item.password ?: ""))
            }
            val response = client.newCall(requestBuilder.build()).execute()
            response.use {
                if (!it.isSuccessful && it.code != 404) {
                    throw HttpFailure(it.code)
                }
                val length = it.header("Content-Length")?.toLongOrNull() ?: -1L
                val acceptRanges = it.header("Accept-Ranges")?.equals("bytes", ignoreCase = true) == true
                val checksum = Checksum.fromHeaders(
                    mapOf(
                        "Repr-Digest" to (it.header("Repr-Digest") ?: ""),
                        "Digest" to (it.header("Digest") ?: ""),
                        "Content-MD5" to (it.header("Content-MD5") ?: ""),
                        "x-goog-hash" to (it.header("x-goog-hash") ?: "")
                    )
                )
                HeadInfo(
                    totalBytes = length,
                    resumeSupported = acceptRanges,
                    fileName = FileNaming.fromContentDisposition(it.header("Content-Disposition")),
                    checksum = checksum,
                    contentType = it.header("Content-Type")
                )
            }
        }
    }

    suspend fun downloadUnknown(
        item: DownloadItem,
        target: File,
        onProgress: (Long) -> Unit
    ): Long {
        return withContext(Dispatchers.IO) {
            val client = buildClient(settings.proxy)
            val requestBuilder = Request.Builder().url(item.url)
            if (!item.username.isNullOrBlank()) {
                requestBuilder.header("Authorization", Credentials.basic(item.username, item.password ?: ""))
            }
            val response = client.newCall(requestBuilder.build()).execute()
            response.use {
                if (it.code == 401 || it.code == 403 || it.code == 404) {
                    throw HttpFailure(it.code)
                }
                if (!it.isSuccessful) throw HttpFailure(it.code)
                val body = it.body ?: throw IllegalStateException("Empty response body.")
                val buffer = ByteArray(65536)
                var total = 0L
                val stream = body.byteStream()
                File(target.parentFile, target.name).outputStream().use { output ->
                    var read = stream.read(buffer)
                    while (read >= 0) {
                        if (!coroutineContext.isActive) throw CancellationException()
                        limiter.acquire(read.toLong())
                        output.write(buffer, 0, read)
                        total += read
                        onProgress(read.toLong())
                        read = stream.read(buffer)
                    }
                }
                total
            }
        }
    }

    suspend fun download(
        item: DownloadItem,
        target: File,
        planner: SegmentPlanner,
        onProgress: (Long) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val client = buildClient(settings.proxy)
            coroutineScope {
                val workers = (0 until item.maxConnections.coerceIn(1, settings.connectionLimit())).map { worker ->
                    async {
                        var after: Long? = null
                        while (isActive) {
                            val claim = planner.claim(worker, after)
                            if (claim is Claim.Done) break
                            if (claim is Claim.Lost) {
                                after = null
                                continue
                            }
                            val bytes = (claim as Claim.Write).bytes
                            val segment = planner.segments().firstOrNull { it.worker == worker }
                                ?: break
                            after = fetchSegment(client, item, segment.from, segment.from + bytes, target, planner, worker, onProgress)
                        }
                    }
                }
                workers.awaitAll()
            }
            if (!planner.isComplete()) {
                throw IllegalStateException("Download did not complete.")
            }
        }
    }

    private suspend fun fetchSegment(
        client: OkHttpClient,
        item: DownloadItem,
        from: Long,
        toExclusive: Long,
        target: File,
        planner: SegmentPlanner,
        worker: Int,
        onProgress: (Long) -> Unit
    ): Long {
        return withContext(Dispatchers.IO) {
            val requestBuilder = Request.Builder()
                .url(item.url)
                .header("Range", "bytes=$from-${toExclusive - 1}")
            if (!item.username.isNullOrBlank()) {
                requestBuilder.header("Authorization", Credentials.basic(item.username, item.password ?: ""))
            }
            val response = client.newCall(requestBuilder.build()).execute()
            response.use {
                if (it.code == 401 || it.code == 403 || it.code == 404) {
                    throw HttpFailure(it.code, parseRetryAfter(it.header("Retry-After"), it.header("Date")))
                }
                if (!it.isSuccessful) throw HttpFailure(it.code, parseRetryAfter(it.header("Retry-After"), it.header("Date")))
                if (it.code == 200) throw RangeNotSupported()
                val body = it.body ?: throw IllegalStateException("Empty response body.")
                val buffer = ByteArray(65536)
                var position = from
                val stream = body.byteStream()
                RandomAccessFile(target, "rw").use { access ->
                    access.seek(position)
                    var read = stream.read(buffer)
                    while (read >= 0) {
                        if (!coroutineContext.isActive) throw CancellationException()
                        limiter.acquire(read.toLong())
                        access.write(buffer, 0, read)
                        position += read
                        planner.markReceived(worker, position)
                        onProgress(read.toLong())
                        read = stream.read(buffer)
                    }
                }
                planner.release(worker)
                position
            }
        }
    }

    class RangeNotSupported : Exception("Server ignored the Range request.")

    class HttpFailure(val statusCode: Int, val retryAfterMillis: Long? = null) : Exception("HTTP $statusCode") {
        fun toDownloadError(): DownloadError = DownloadError.fromHttpStatus(statusCode)
    }

    companion object {
        fun parseRetryAfter(retryAfter: String?, dateHeader: String?): Long? {
            if (retryAfter.isNullOrBlank()) return null
            retryAfter.trim().toLongOrNull()?.let { seconds ->
                return (seconds.coerceAtLeast(0L) * 1000L)
            }
            if (dateHeader.isNullOrBlank()) return null
            return try {
                val format = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.US)
                val target = format.parse(retryAfter.trim()) ?: return null
                val now = format.parse(dateHeader.trim()) ?: return null
                (target.time - now.time).coerceAtLeast(0L)
            } catch (e: Exception) {
                null
            }
        }
    }
}
