package com.misterdownloadmanager.port.core.ipc

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable

object IpcLimits {
    const val MAX_MESSAGE_BYTES: Int = 1 * 1024 * 1024
    const val MAX_REQUEST_BYTES: Int = 2 * MAX_MESSAGE_BYTES + 4096
    const val MAX_IN_FLIGHT: Int = 8
    const val ANSWER_WAIT_MILLIS: Long = 2_000L
    const val REPLY_WAIT_MILLIS: Long = 2_000L
    const val REQUEST_DEADLINE_MILLIS: Long = 3_000L
    const val MAX_LINK_PARTS: Int = 256
    const val RECENT_MAX: Int = 200
    const val RECENT_BUDGET_BYTES: Int = 512 * 1024
}

object IpcToken {
    fun random(): String {
        val bytes = ByteArray(32)
        java.security.SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

@Serializable
data class IpcCommand(
    val url: String? = null,
    val dir: String? = null,
    val name: String? = null,
    val checksum: String? = null,
    val later: Boolean = false,
    val silent: Boolean = false,
    val startQueue: Boolean = false,
    val referer: String? = null,
    val pageUrl: String? = null,
    val origin: String? = null,
    val cookies: String? = null,
    val userAgent: String? = null,
    val finalUrl: String? = null,
    val size: Long? = null,
    val mime: String? = null,
    val privateWindow: Boolean = false,
    val proxy: String? = null,
    val proxyPac: String? = null,
    val query: String? = null,
    val api: Boolean? = null,
    val action: String? = null,
    val ids: List<Long> = emptyList(),
    val queue: String? = null,
    val category: String? = null,
    val pair: Boolean = false,
    val exit: Boolean = false,
    val startup: Boolean = false,
    val hidden: Boolean = false,
    val show: String? = null,
    val links: List<String> = emptyList(),
    val remote: Boolean = false
) {
    companion object {
        const val STARTUP_FLAG: String = "/startup"
        const val FROM_HOST_FLAG: String = "/fromhost"
        val STATUSES: List<String> = listOf("Queued", "Downloading", "Stopped", "Complete", "Error")
        val SHOW_WINDOWS: List<String> = listOf(
            "addurl", "batch", "find", "scheduler", "about", "language", "shortcuts",
            "speedlimit", "properties", "delete", "progress", "existing-files",
            "video-page", "video-helpers", "video-subs", "torrent-settings",
            "add-trackers", "menu", "options"
        )

        fun validateAdd(command: IpcCommand): String? {
            val url = command.url
            if (url.isNullOrBlank()) return "URL must not be empty."
            val valid = url.startsWith("http://") || url.startsWith("https://") ||
                url.startsWith("ftp://") || url.startsWith("ftps://") ||
                url.startsWith("magnet:")
            if (!valid) return "URL must start with http, https, ftp, ftps, or magnet."
            return null
        }
    }
}

@Serializable
data class IpcDownloadInfo(
    val id: Long,
    val url: String,
    val fileName: String,
    val status: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val category: String,
    val queue: String
)

@Serializable
data class IpcListAnswer(
    val downloads: List<IpcDownloadInfo> = emptyList(),
    val speed: Long = 0L,
    val queues: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val free: Long? = null
)

@Serializable
data class IpcAddAnswer(
    val id: Long,
    val pending: Boolean = false
)

@Serializable
data class IpcActAnswer(
    val done: List<Long> = emptyList(),
    val skipped: List<IpcSkipped> = emptyList()
)

@Serializable
data class IpcSkipped(val id: Long, val why: String)

object IpcActReasons {
    const val GONE: String = "gone"
    const val MISSING: String = "missing"
    const val RUNNING: String = "running"
    const val CANT: String = "cant"
    const val UNSUPPORTED: String = "unsupported"
    const val FAILED: String = "failed"
    const val UNKNOWN: String = "unknown"
}

class IpcCommandBus {
    private val _commands = MutableSharedFlow<IpcCommand>(extraBufferCapacity = IpcLimits.MAX_IN_FLIGHT)
    val commands: SharedFlow<IpcCommand> = _commands.asSharedFlow()

    fun send(command: IpcCommand): Boolean {
        return _commands.tryEmit(command)
    }
}

@Serializable
data class IpcAddRequest(
    val url: String,
    val fileName: String? = null,
    val category: String? = null,
    val referer: String? = null,
    val cookies: String? = null,
    val userAgent: String? = null
)

@Serializable
data class IpcAddResponse(
    val accepted: Boolean,
    val downloadId: String? = null,
    val error: String? = null
)

@Serializable
data class IpcStatusResponse(
    val downloadId: String,
    val status: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSecond: Long
)

object IpcApi {
    fun validateAdd(request: IpcAddRequest): String? {
        if (request.url.isBlank()) return "URL must not be empty."
        val valid = request.url.startsWith("http://") ||
            request.url.startsWith("https://") ||
            request.url.startsWith("ftp://") ||
            request.url.startsWith("magnet:")
        if (!valid) return "URL must start with http, https, ftp, or magnet."
        return null
    }
}

data class BrowserCapture(
    val url: String,
    val fileName: String?,
    val contentType: String?,
    val contentLength: Long,
    val referer: String?,
    val cookies: String?
) {
    fun isDownloadable(): Boolean {
        if (contentType == null) return false
        val type = contentType.lowercase()
        return !type.startsWith("text/html") && !type.startsWith("image/") || contentLength > 0L
    }
}
