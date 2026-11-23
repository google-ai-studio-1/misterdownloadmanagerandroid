package com.misterdownloadmanager.port.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ProxyConfig(
    val enabled: Boolean = false,
    val host: String = "",
    val port: Int = 8080,
    val username: String? = null,
    val password: String? = null,
    val scheme: ProxyScheme = ProxyScheme.HTTP
) {
    fun isValid(): Boolean {
        if (!enabled) return true
        return host.isNotBlank() && port in 1..65535
    }
}

enum class ProxyScheme {
    HTTP,
    SOCKS5,
    SOCKS4
}

enum class ProxyMode {
    NONE,
    SYSTEM,
    PAC,
    MANUAL
}

enum class DnsMode {
    SYSTEM,
    PLAIN,
    DOT,
    DOH
}

@Serializable
data class Settings(
    val maxSimultaneousDownloads: Int = 4,
    val maxConnectionsPerDownload: Int = 8,
    val manyConnections: Boolean = false,
    val downloadFolder: String = "",
    val autoStartDownloads: Boolean = true,
    val resumeOnStart: Boolean = true,
    val showNotifications: Boolean = true,
    val speedLimitOn: Boolean = false,
    val speedLimitKbPerSecond: Long = 10L,
    val globalLimitOn: Boolean = false,
    val globalLimitKbPerSecond: Long = 500L,
    val globalLimitOnlyWhenActive: Boolean = false,
    val downloadSpeedLimitBytesPerSecond: Long? = null,
    val uploadSpeedLimitBytesPerSecond: Long? = null,
    val proxy: ProxyConfig = ProxyConfig(),
    val proxyMode: ProxyMode = ProxyMode.NONE,
    val proxyFromBrowser: Boolean = true,
    val customDnsServers: List<String> = emptyList(),
    val dnsMode: DnsMode = DnsMode.SYSTEM,
    val dnsProvider: String = "cloudflare",
    val verifyChecksumAfterDownload: Boolean = true,
    val keepFinishedDownloads: Boolean = true,
    val extractArchivesAfterDownload: Boolean = false,
    val deleteArchiveAfterExtract: Boolean = false,
    val extractInPlace: Boolean = false,
    val extractSubfolder: Int = 0,
    val followSystemTheme: Boolean = true,
    val language: String = "en",
    val minSplitBytes: Long = 12_288L,
    val segmentAlignmentBytes: Long = 4096L,
    val connectionTimeoutMillis: Long = 30_000L,
    val maxRetries: Int = 5,
    val retryDelayMillis: Long = 1_000L,
    val queueRetryDelayMillis: Long = 30_000L,
    val limitsOn: Boolean = false,
    val limitsMb: Long = 200L,
    val limitsHours: Long = 5L,
    val limitsWarn: Boolean = true,
    val confirmDelete: Boolean = true,
    val minimizeToTray: Boolean = true,
    val clipboardAutoStart: Boolean = true,
    val showCompleteDialog: Boolean = true,
    val doubleClickOpensFile: Boolean = false,
    val markDownloads: Boolean = true,
    val duplicateAction: Int = 0,
    val ftpPassive: Boolean = true,
    val torrentKeepSharing: Boolean = false,
    val torrentIncoming: Boolean = true,
    val torrentUpnp: Boolean = true,
    val torrentPort: Int = 0,
    val torrentDht: Boolean = true,
    val torrentLsd: Boolean = true,
    val torrentUtp: Boolean = true,
    val torrentUploadOn: Boolean = false,
    val torrentUploadKbPerSecond: Long = 100L,
    val torrentStopRatioOn: Boolean = false,
    val torrentStopRatioHundredths: Long = 200L,
    val torrentStopMinutesOn: Boolean = false,
    val torrentStopMinutes: Long = 1440L,
    val debridOn: Boolean = true,
    val debridTorrents: Boolean = false,
    val remoteOn: Boolean = false,
    val remotePort: Int = 8463,
    val cnlOn: Boolean = false,
    val updateCheck: Boolean = true,
    val interfaceSizePercent: Int = 100,
    val themeName: String = "classic"
) {
    fun connectionLimit(): Int {
        return if (manyConnections) 128 else 32
    }

    fun connectionsFor(url: String): Int {
        return maxConnectionsPerDownload.coerceIn(1, connectionLimit())
    }

    fun speedLimitBytesPerSecond(): Long? {
        return if (speedLimitOn) speedLimitKbPerSecond.coerceAtLeast(1L) * 1024L else null
    }

    fun globalLimitBytesPerSecond(): Long? {
        return if (globalLimitOn) globalLimitKbPerSecond.coerceAtLeast(1L) * 1024L else null
    }

    fun validated(): Settings {
        return copy(
            maxSimultaneousDownloads = maxSimultaneousDownloads.coerceIn(1, 32),
            maxConnectionsPerDownload = maxConnectionsPerDownload.coerceIn(1, 128),
            minSplitBytes = minSplitBytes.coerceAtLeast(1L),
            segmentAlignmentBytes = segmentAlignmentBytes.coerceAtLeast(1L),
            connectionTimeoutMillis = connectionTimeoutMillis.coerceAtLeast(1_000L),
            maxRetries = maxRetries.coerceIn(0, 20),
            retryDelayMillis = retryDelayMillis.coerceAtLeast(0L),
            interfaceSizePercent = interfaceSizePercent.coerceIn(100, 200)
        )
    }
}
