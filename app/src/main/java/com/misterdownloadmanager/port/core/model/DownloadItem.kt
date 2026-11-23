package com.misterdownloadmanager.port.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    STOPPED,
    COMPLETED,
    ERROR;

    fun isActive(): Boolean = this == DOWNLOADING

    fun isTerminal(): Boolean = this == COMPLETED || this == ERROR

    fun canPause(): Boolean = this == DOWNLOADING || this == QUEUED

    fun canResume(): Boolean = this == STOPPED || this == ERROR

    fun canStart(): Boolean = this == QUEUED || this == STOPPED || this == ERROR
}

enum class EngineState {
    PROBING,
    DOWNLOADING,
    VERIFYING,
    MERGING,
    COMPLETING,
    STOPPING,
    DONE,
    FAILED,
    CANCELLED;

    fun toAppStatus(): DownloadStatus {
        return when (this) {
            DONE -> DownloadStatus.COMPLETED
            STOPPING, CANCELLED -> DownloadStatus.STOPPED
            FAILED -> DownloadStatus.ERROR
            PROBING, DOWNLOADING, VERIFYING, MERGING, COMPLETING -> DownloadStatus.DOWNLOADING
        }
    }
}

enum class DownloadKind {
    HTTP,
    FTP,
    TORRENT,
    VIDEO,
    DEBRID
}

@Serializable
data class DownloadItem(
    val id: Long,
    val url: String,
    val fileName: String,
    val destinationFolder: String,
    val categoryId: String = Category.GENERAL.id,
    val kind: DownloadKind = DownloadKind.HTTP,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val totalBytes: Long = -1L,
    val downloadedBytes: Long = 0L,
    val speedBytesPerSecond: Long = 0L,
    val connections: Int = 0,
    val maxConnections: Int = 8,
    val error: String? = null,
    val checksum: String? = null,
    val checksumAlgorithm: ChecksumAlgorithm? = null,
    val username: String? = null,
    @Transient
    val password: String? = null,
    val description: String = "",
    val createdEpochMillis: Long = 0L,
    val finishedEpochMillis: Long? = null,
    val resumeSupported: Boolean? = null,
    val infoHash: String? = null,
    val ownSpeedLimitBytesPerSecond: Long? = null,
    val log: List<LogEntry> = emptyList(),
    val referer: String? = null
) {
    fun progress(): Float {
        if (totalBytes <= 0L) return 0f
        return (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
    }

    fun remainingBytes(): Long {
        if (totalBytes < 0L) return -1L
        return (totalBytes - downloadedBytes).coerceAtLeast(0L)
    }

    fun estimatedSecondsLeft(): Long? {
        if (speedBytesPerSecond <= 0L) return null
        val remaining = remainingBytes()
        if (remaining < 0L) return null
        return remaining / speedBytesPerSecond
    }

    fun withLog(message: String, nowEpochMillis: Long): DownloadItem {
        val entries = (log + LogEntry(nowEpochMillis, message)).takeLast(50)
        return copy(log = entries)
    }
}

@Serializable
data class LogEntry(val epochSeconds: Long, val message: String)
