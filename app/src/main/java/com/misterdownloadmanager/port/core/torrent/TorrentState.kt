package com.misterdownloadmanager.port.core.torrent

import java.security.MessageDigest

enum class TorrentStatus {
    CHECKING,
    DOWNLOADING,
    FINISHED,
    PAUSED,
    ERROR
}

object TorrentConstants {
    const val MAX_TORRENT_FILE_BYTES: Long = 10L shl 20
    const val MIN_RATE_BYTES_PER_SECOND: Long = 16L shl 10
    const val CHECK_HALTED_AFTER_MILLIS: Long = 5_000L
    const val PORT_TRIES: Int = 20
    const val PORT_WAIT_MILLIS: Long = 2_000L
    const val PORT_FILE: String = "listen-port"
    const val SESSION_FILE: String = "session.json"
    const val STATUS_POLL_MILLIS: Long = 200L
    val DHT_BOOTSTRAP: List<String> = listOf(
        "dht.libtorrent.org:25401",
        "dht.transmissionbt.com:6881",
        "router.bittorrent.com:6881",
        "router.utorrent.com:6881"
    )
}

data class TorrentConfig(
    val requireAuthorization: Boolean = true,
    val listen: Boolean = true,
    val upnp: Boolean = true,
    val port: Int? = null,
    val listenIp: String? = null,
    val utp: Boolean = true,
    val dht: Boolean = true,
    val lsd: Boolean = true,
    val proxy: String? = null,
    val blocklist: String? = null,
    val offline: Boolean = false,
    val dhtBootstrap: List<String>? = null,
    val bindDevice: String? = null,
    val bindAddress: String? = null
)

object TrackerFilter {
    fun allowed(tracker: String, proxied: Boolean): Boolean {
        val lower = tracker.lowercase()
        val scheme = lower.substringBefore("://", "")
        if (scheme != "http" && scheme != "https" && scheme != "udp") return false
        if (scheme == "udp" && proxied) return false
        val host = lower.substringAfter("://", "").substringBefore('/').substringBefore(':')
            .trimEnd('.')
        if (host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local")) return false
        if (isIpLiteral(host)) return isPublicIp(host)
        return host.isNotBlank()
    }

    fun isIpLiteral(host: String): Boolean {
        return host.contains(':') || Regex("^\\d{1,3}(\\.\\d{1,3}){3}$").matches(host)
    }

    fun isPublicIp(host: String): Boolean {
        val octets = host.split('.').mapNotNull { it.toIntOrNull() }
        if (octets.size == 4 && octets.all { it in 0..255 }) {
            val first = octets[0]
            val second = octets[1]
            if (first == 0) return false
            if (first == 10) return false
            if (first == 127) return false
            if (first == 169 && second == 254) return false
            if (first == 172 && second in 16..31) return false
            if (first == 192 && second == 0 && octets[2] == 0) return false
            if (first == 192 && second == 168) return false
            if (first == 198 && second == 18) return false
            if (first == 198 && second == 19) return false
            if (first == 100 && second in 64..127) return false
            if (first == 224 || first == 240) return false
            if (first >= 240) return false
            if (first == 255) return false
            return true
        }
        val lower = host.lowercase()
        if (lower == "::1" || lower == "::") return false
        if (lower.startsWith("fe80:") || lower.startsWith("fc") || lower.startsWith("fd")) return false
        if (lower.startsWith("ff")) return false
        if (lower.startsWith("::ffff:")) return isPublicIp(lower.removePrefix("::ffff:"))
        return true
    }

    fun inMagnet(magnet: String): List<String> {
        return magnet.substringAfter("magnet:?", "").split('&')
            .filter { it.startsWith("tr=") }
            .map { java.net.URLDecoder.decode(it.substringAfter('='), "UTF-8") }
    }

    fun filterMagnet(magnet: String, proxied: Boolean): String {
        val parts = magnet.split('&')
        val kept = parts.filter { part ->
            if (!part.startsWith("tr=")) return@filter true
            val tracker = java.net.URLDecoder.decode(part.substringAfter('='), "UTF-8")
            allowed(tracker, proxied)
        }
        return kept.joinToString("&")
    }
}

enum class PieceState {
    MISSING,
    DOWNLOADING,
    HAVE
}

data class MagnetLink(
    val infoHashHex: String,
    val displayName: String?,
    val trackers: List<String>
) {
    companion object {
        fun parse(text: String): MagnetLink? {
            val trimmed = text.trim()
            if (trimmed.length < 8 || !trimmed.substring(0, 8).equals("magnet:?", ignoreCase = true)) return null
            val params = trimmed.substring(8).split('&')
            var hash: String? = null
            var name: String? = null
            val trackers = mutableListOf<String>()
            for (param in params) {
                val key = param.substringBefore('=')
                val value = param.substringAfter('=', "")
                when (key) {
                    "xt" -> {
                        val hex = value.substringAfter("btih:", "")
                        if (hex.length == 40 && hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) hash = hex.lowercase()
                        if (hex.length == 32) hash = base32ToHex(hex)
                    }
                    "dn" -> name = java.net.URLDecoder.decode(value, "UTF-8")
                    "tr" -> trackers.add(java.net.URLDecoder.decode(value, "UTF-8"))
                }
            }
            val infoHash = hash ?: return null
            return MagnetLink(infoHash, name, trackers)
        }

        fun base32ToHex(base32: String): String {
            val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
            var bits = 0
            var value = 0
            val bytes = mutableListOf<Byte>()
            for (character in base32.uppercase()) {
                val index = alphabet.indexOf(character)
                if (index < 0) return ""
                value = (value shl 5) or index
                bits += 5
                if (bits >= 8) {
                    bits -= 8
                    bytes.add(((value shr bits) and 0xFF).toByte())
                }
            }
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}

data class TorrentFile(
    val path: String,
    val size: Long,
    val selected: Boolean = true,
    val padding: Boolean = false
)

data class TorrentInfo(
    val infoHashHex: String,
    val name: String,
    val totalBytes: Long,
    val pieceLength: Long,
    val pieceCount: Int,
    val files: List<TorrentFile>,
    val trackers: List<String>,
    val isPrivate: Boolean = false
) {
    fun sha1InfoHash(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-1").digest(bytes).joinToString("") { "%02x".format(it) }
    }

    fun folderIn(directory: String): String {
        val multiFile = files.count { !it.padding } > 1
        if (!multiFile) return directory
        val sanitized = name.map { character ->
            if (character in "<>:\"/\\|?*" || character.isISOControl()) '_' else character
        }.joinToString("").trim().trimEnd('.')
        val folder = sanitized.ifBlank { infoHashHex }
        return "$directory/$folder"
    }
}

sealed class TorrentError {
    object NotATorrent : TorrentError()
    data class UnsafeName(val path: String, val reason: String) : TorrentError()
    object MetadataTimeout : TorrentError()
    object Unknown : TorrentError()
    data class Engine(val message: String) : TorrentError()
}

class PieceManager(pieceCount: Int) {
    private val states = Array(pieceCount) { PieceState.MISSING }
    private val counts = IntArray(pieceCount)

    fun haveCount(): Int = states.count { it == PieceState.HAVE }

    fun progress(): Float {
        if (states.isEmpty()) return 0f
        return haveCount().toFloat() / states.size.toFloat()
    }

    fun markHave(index: Int) {
        if (index in states.indices) states[index] = PieceState.HAVE
    }

    fun markDownloading(index: Int) {
        if (index in states.indices && states[index] == PieceState.MISSING) {
            states[index] = PieceState.DOWNLOADING
        }
    }

    fun rarestFirst(available: Set<Int>): Int? {
        return available
            .filter { states[it] == PieceState.MISSING }
            .minByOrNull { counts[it] }
    }

    fun recordAvailability(index: Int) {
        if (index in counts.indices) counts[index] += 1
    }
}

data class PeerInfo(
    val address: String,
    val port: Int,
    val clientName: String?,
    val downloadSpeedBytesPerSecond: Long = 0L,
    val uploadSpeedBytesPerSecond: Long = 0L,
    val progress: Float = 0f,
    val choked: Boolean = true,
    val interested: Boolean = false
)

data class TrackerInfo(
    val url: String,
    val seeders: Int = 0,
    val leechers: Int = 0,
    val lastError: String? = null,
    val lastAnnounceEpochMillis: Long = 0L
)
