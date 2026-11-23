package com.misterdownloadmanager.port.core.debrid

enum class DebridProvider(val displayName: String, val baseUrl: String, val rateLimitPerMinute: Int) {
    REAL_DEBRID("Real-Debrid", "https://api.real-debrid.com", 120),
    ALL_DEBRID("AllDebrid", "https://api.alldebrid.com", 300),
    PREMIUMIZE("Premiumize", "https://www.premiumize.me", 60),
    TORBOX("TorBox", "https://api.torbox.app", 120),
    MEGA_DEBRID("MegaDebrid", "https://www.mega-debrid.eu", 60)
}

object DebridHosts {
    fun hostMatches(host: String, domains: List<String>): Boolean {
        val normalized = host.lowercase().trimEnd('.').removePrefix("www.")
        return domains.any { domain ->
            val candidate = domain.lowercase().trimEnd('.').removePrefix("www.")
            normalized == candidate || normalized.endsWith(".$candidate")
        }
    }
}

sealed class DebridResult {
    data class Unrestricted(val url: String, val fileName: String?, val sizeBytes: Long, val maxConnections: Int? = null) : DebridResult()
    data class NotSupported(val reason: String) : DebridResult()
    data class Error(val message: String) : DebridResult()
}

data class DebridAccount(
    val provider: DebridProvider,
    val apiKey: String,
    val username: String? = null
) {
    fun isConfigured(): Boolean = apiKey.isNotBlank()
}

interface DebridService {
    val provider: DebridProvider
    suspend fun unrestrict(account: DebridAccount, link: String): DebridResult
    suspend fun isSupported(account: DebridAccount, link: String): Boolean
}

object DebridServices {
    fun all(): List<DebridService> {
        return listOf(RealDebridService(), AllDebridService(), PremiumizeService(), TorBoxService(), MegaDebridService())
    }

    fun forProvider(provider: DebridProvider): DebridService {
        return when (provider) {
            DebridProvider.REAL_DEBRID -> RealDebridService()
            DebridProvider.ALL_DEBRID -> AllDebridService()
            DebridProvider.PREMIUMIZE -> PremiumizeService()
            DebridProvider.TORBOX -> TorBoxService()
            DebridProvider.MEGA_DEBRID -> MegaDebridService()
        }
    }
}
