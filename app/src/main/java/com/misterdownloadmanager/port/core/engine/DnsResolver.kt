package com.misterdownloadmanager.port.core.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

data class DnsPreset(
    val id: String,
    val plainServers: List<String>,
    val tlsName: String,
    val dohUrl: String,
    val offersPlain: Boolean = true
)

object DnsPresets {
    const val DNS_TIMEOUT_MILLIS: Long = 4_000L
    const val STAGGER_MILLIS: Long = 250L
    const val RACE_BUDGET_MILLIS: Long = 3_000L
    const val REMEMBER_SECONDS: Long = 300L

    val CLOUDFLARE = DnsPreset(
        id = "cloudflare",
        plainServers = listOf("1.1.1.1", "1.0.0.1", "2606:4700:4700::1111", "2606:4700:4700::1001"),
        tlsName = "one.one.one.one",
        dohUrl = "https://cloudflare-dns.com/dns-query"
    )
    val GOOGLE = DnsPreset(
        id = "google",
        plainServers = listOf("8.8.8.8", "8.8.4.4", "2001:4860:4860::8888", "2001:4860:4860::8844"),
        tlsName = "dns.google",
        dohUrl = "https://dns.google/dns-query"
    )
    val QUAD9 = DnsPreset(
        id = "quad9",
        plainServers = listOf("9.9.9.9", "149.112.112.112", "2620:fe::fe", "2620:fe::9"),
        tlsName = "dns.quad9.net",
        dohUrl = "https://dns.quad9.net/dns-query"
    )
    val ADGUARD = DnsPreset(
        id = "adguard",
        plainServers = listOf("94.140.14.14", "94.140.15.15", "2a10:50c0::ad1:ff", "2a10:50c0::ad2:ff"),
        tlsName = "dns.adguard-dns.com",
        dohUrl = "https://dns.adguard-dns.com/dns-query"
    )
    val NEXTDNS = DnsPreset(
        id = "nextdns",
        plainServers = listOf("45.90.28.0", "45.90.30.0", "2a07:a8c0::", "2a07:a8c1::"),
        tlsName = "dns.nextdns.io",
        dohUrl = "https://dns.nextdns.io",
        offersPlain = false
    )

    val ALL: List<DnsPreset> = listOf(CLOUDFLARE, GOOGLE, QUAD9, ADGUARD, NEXTDNS)

    fun byId(id: String): DnsPreset? = ALL.firstOrNull { it.id == id.lowercase() }

    fun isValidNextDnsProfile(profile: String): Boolean {
        if (profile.isEmpty()) return true
        return profile.length == 6 && profile.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
    }

    fun nextDnsTlsName(profile: String): String {
        return if (profile.isEmpty()) "dns.nextdns.io" else "${profile.lowercase()}.dns.nextdns.io"
    }

    fun nextDnsDohUrl(profile: String): String {
        return if (profile.isEmpty()) "https://dns.nextdns.io" else "https://dns.nextdns.io/${profile.lowercase()}"
    }
}

class DnsResolver(private val customServers: List<String>) {
    fun isCustom(): Boolean = customServers.isNotEmpty()

    suspend fun resolve(host: String): List<String> {
        return withContext(Dispatchers.IO) {
            InetAddress.getAllByName(host).map { it.hostAddress ?: "" }.filter { it.isNotBlank() }
        }
    }

    fun isLocalName(host: String): Boolean {
        val lower = host.lowercase()
        return lower == "localhost" || !lower.contains('.') ||
            lower.endsWith(".local") || lower.endsWith(".lan") ||
            lower.endsWith(".home.arpa") || lower.endsWith(".internal")
    }

    fun isIpLiteral(host: String): Boolean {
        val ipv4 = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")
        return ipv4.matches(host) || host.contains(':')
    }

    companion object {
        fun parseServers(text: String): List<String> {
            return text.split(',', ';', ' ', '\n', '\t')
                .map { it.trim() }
                .filter { it.isNotBlank() }
        }
    }
}

object BrowserLike {
    const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

    fun headers(): Map<String, String> {
        return mapOf(
            "User-Agent" to USER_AGENT,
            "Accept" to "*/*",
            "Accept-Language" to "en-US,en;q=0.9",
            "Accept-Encoding" to "identity"
        )
    }
}
