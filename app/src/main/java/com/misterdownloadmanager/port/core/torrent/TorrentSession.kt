package com.misterdownloadmanager.port.core.torrent

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TorrentSessionState(
    val info: TorrentInfo,
    val status: TorrentStatus,
    val downloadedBytes: Long,
    val uploadedBytes: Long,
    val downloadSpeedBytesPerSecond: Long,
    val uploadSpeedBytesPerSecond: Long,
    val peers: List<PeerInfo>,
    val trackers: List<TrackerInfo>
) {
    fun progress(): Float {
        if (info.totalBytes <= 0L) return 0f
        return (downloadedBytes.toFloat() / info.totalBytes.toFloat()).coerceIn(0f, 1f)
    }
}

class TorrentSession(private val info: TorrentInfo) {
    private val pieces = PieceManager(info.pieceCount)
    private val _state = MutableStateFlow(
        TorrentSessionState(
            info = info,
            status = TorrentStatus.CHECKING,
            downloadedBytes = 0L,
            uploadedBytes = 0L,
            downloadSpeedBytesPerSecond = 0L,
            uploadSpeedBytesPerSecond = 0L,
            peers = emptyList(),
            trackers = info.trackers.map { TrackerInfo(url = it) }
        )
    )
    val state: StateFlow<TorrentSessionState> = _state.asStateFlow()

    fun pieceManager(): PieceManager = pieces

    fun start() {
        _state.value = _state.value.copy(status = TorrentStatus.DOWNLOADING)
    }

    fun pause() {
        _state.value = _state.value.copy(status = TorrentStatus.PAUSED)
    }

    fun complete() {
        _state.value = _state.value.copy(status = TorrentStatus.FINISHED)
    }

    fun fail() {
        _state.value = _state.value.copy(status = TorrentStatus.ERROR)
    }

    fun updatePeers(peers: List<PeerInfo>) {
        _state.value = _state.value.copy(peers = peers)
    }

    fun updateProgress(downloadedBytes: Long, speedBytesPerSecond: Long) {
        _state.value = _state.value.copy(
            downloadedBytes = downloadedBytes,
            downloadSpeedBytesPerSecond = speedBytesPerSecond
        )
    }

    fun updateTracker(tracker: TrackerInfo) {
        _state.value = _state.value.copy(
            trackers = _state.value.trackers.map { if (it.url == tracker.url) tracker else it }
        )
    }
}

object Blocklist {
    const val MAX_UNPACKED_BYTES: Long = 256L shl 20

    data class Converted(val text: String, val ranges: Int, val skipped: Int)

    fun isBlocked(ip: String, blockedRanges: List<Pair<Long, Long>>): Boolean {
        val value = ipToLong(ip) ?: return false
        return blockedRanges.any { (start, end) -> value in start..end }
    }

    fun convert(bytes: ByteArray): Converted? {
        val unpacked = if (bytes.size >= 2 && bytes[0] == 0x1F.toByte() && bytes[1] == 0x8B.toByte()) {
            val buffer = java.io.ByteArrayOutputStream()
            java.util.zip.GZIPInputStream(bytes.inputStream()).use { input ->
                val chunk = ByteArray(65_536)
                var total = 0L
                while (total < MAX_UNPACKED_BYTES) {
                    val allowed = minOf(chunk.size.toLong(), MAX_UNPACKED_BYTES - total).toInt()
                    val read = input.read(chunk, 0, allowed)
                    if (read < 0) break
                    buffer.write(chunk, 0, read)
                    total += read
                }
            }
            buffer.toByteArray()
        } else {
            bytes
        }
        val text = String(unpacked, Charsets.UTF_8)
        val out = StringBuilder()
        var ranges = 0
        var skipped = 0
        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val range = parseLine(line)
            if (range == null) {
                skipped += 1
            } else {
                out.append("mdm:${range.first}-${range.second}\n")
                ranges += 1
            }
        }
        if (ranges == 0) return null
        return Converted(out.toString(), ranges, skipped)
    }

    private fun parseLine(line: String): Pair<Long, Long>? {
        val rangeText = if (line.contains(',')) {
            line.substringBefore(',').trim()
        } else if (line.count { it == ':' } > 2) {
            line.substringAfter(':').trim()
        } else {
            line.substringAfterLast(':').trim()
        }
        val parts = rangeText.split('-')
        if (parts.size != 2) return null
        val start = ipToLong(parts[0].trim()) ?: return null
        val end = ipToLong(parts[1].trim()) ?: return null
        if (start > end) return null
        if (line.contains(',')) {
            val level = line.substringAfter(',', "").substringBefore(',').trim().toIntOrNull()
            if (level != null && level > 127) return null
        }
        return start to end
    }

    fun ipToLong(ip: String): Long? {
        val parts = ip.split('.')
        if (parts.size != 4) return null
        var result = 0L
        for (part in parts) {
            val number = part.toIntOrNull() ?: return null
            if (number !in 0..255) return null
            result = (result shl 8) or number.toLong()
        }
        return result
    }
}
