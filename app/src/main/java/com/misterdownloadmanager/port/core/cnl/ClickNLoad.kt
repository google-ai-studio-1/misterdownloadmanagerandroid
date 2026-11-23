package com.misterdownloadmanager.port.core.cnl

import java.net.URLDecoder
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

data class ClickNLoadPackage(
    val urls: List<String>,
    val packageName: String?,
    val passwords: List<String>,
    val source: String?
)

object ClickNLoad {
    const val PORT: Int = 9666
    const val MAX_BODY_BYTES: Int = 256 * 1024
    const val MAX_LINKS: Int = 500
    const val MAX_PASSWORDS: Int = 10
    const val MAX_TEXT_LENGTH: Int = 200
    const val POSTS_PER_MINUTE: Int = 10
    const val SITES_COUNTED: Int = 100

    fun parseForm(body: String): ClickNLoadPackage {
        val params = body.split('&').associate { part ->
            val key = part.substringBefore('=')
            val value = part.substringAfter('=', "")
            key to URLDecoder.decode(value, "UTF-8")
        }
        val urls = (params["urls"] ?: "")
            .split('\n', '\r')
            .map { it.trim() }
            .filter { it.isNotBlank() }
        val crypted = params["crypted"]
        val jk = params["jk"]
        val decrypted = if (!crypted.isNullOrBlank() && !jk.isNullOrBlank()) {
            runCatching { decrypt(crypted, jk) }.getOrNull() ?: ""
        } else {
            ""
        }
        val allUrls = (urls + lines(decrypted)).distinct().take(MAX_LINKS)
        val passwords = (params["passwords"] ?: "").split('\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(MAX_PASSWORDS)
        return ClickNLoadPackage(
            urls = allUrls,
            packageName = params["package"]?.takeIf { it.isNotBlank() }?.take(MAX_TEXT_LENGTH),
            passwords = passwords,
            source = params["source"]?.takeIf { it.isNotBlank() }
        )
    }

    fun keyOf(jk: String): ByteArray? {
        val quoted = Regex("return\\s+['\"]([^'\"]+)['\"]").find(jk)?.groupValues?.getOrNull(1)
            ?: return null
        if (quoted.length != 32 || !quoted.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
        return quoted.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    fun decrypt(crypted: String, jk: String): String {
        val key = keyOf(jk) ?: throw IllegalArgumentException("Click'n'Load key function did not return a 32-digit hex key.")
        val cleaned = crypted.filter { !it.isWhitespace() }
        val cipherBytes = Base64.getDecoder().decode(cleaned)
        if (cipherBytes.size % 16 != 0) throw IllegalArgumentException("Click'n'Load data length is not a multiple of 16.")
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(key))
        val plain = cipher.doFinal(cipherBytes)
        val text = String(plain, Charsets.UTF_8)
        return text.trimEnd('\u0000')
    }

    fun lines(text: String): List<String> {
        return text.split('\n', '\r')
            .map { it.trim().trimEnd('\u0000') }
            .filter { it.isNotBlank() }
            .filter { url ->
                val scheme = url.substringBefore("://", "").lowercase()
                (scheme == "http" || scheme == "https" || scheme == "ftp") && !isInternalLink(url)
            }
            .distinct()
            .take(MAX_LINKS)
    }

    fun isInternalLink(url: String): Boolean {
        val host = url.substringAfter("://", "").substringBefore('/').substringBefore(':')
            .trimEnd('.').lowercase()
        if (host.isEmpty()) return true
        if (host == "localhost" || !host.contains('.')) return true
        val internalSuffixes = listOf(".localhost", ".local", ".internal", ".lan", ".home.arpa", ".intranet")
        if (internalSuffixes.any { host.endsWith(it) }) return true
        val octets = host.split('.').mapNotNull { it.toIntOrNull() }
        if (octets.size == 4 && octets.all { it in 0..255 }) {
            val first = octets[0]
            val second = octets[1]
            if (first == 0 || first == 10 || first == 127) return true
            if (first == 169 && second == 254) return true
            if (first == 172 && second in 16..31) return true
            if (first == 192 && second == 168) return true
            if (first == 100 && second in 64..127) return true
            if (first == 224 || first >= 240 || first == 255) return true
        }
        if (host.startsWith("::ffff:")) return isInternalLink("http://" + host.removePrefix("::ffff:"))
        if (host == "::1" || host == "::" || host.startsWith("fe80:") || host.startsWith("fc") || host.startsWith("fd")) return true
        return false
    }
}
