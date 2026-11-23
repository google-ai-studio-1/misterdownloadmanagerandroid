package com.misterdownloadmanager.port.core.model

import kotlinx.serialization.Serializable

enum class ChecksumAlgorithm(val displayName: String, val hexLength: Int, val byteLength: Int) {
    CRC32("CRC32", 8, 4),
    MD5("MD5", 32, 16),
    SHA1("SHA-1", 40, 20),
    SHA256("SHA-256", 64, 32),
    SHA512("SHA-512", 128, 64);

    companion object {
        fun parse(text: String): Pair<ChecksumAlgorithm, String>? {
            val trimmed = text.trim()
            val named = trimmed.split(":", "=", limit = 2)
            if (named.size == 2) {
                val algorithm = when (named[0].lowercase().replace("-", "")) {
                    "crc32" -> CRC32
                    "md5" -> MD5
                    "sha1" -> SHA1
                    "sha256" -> SHA256
                    "sha512" -> SHA512
                    else -> null
                }
                if (algorithm != null) {
                    val hex = named[1].trim().lowercase().substringBefore(' ').trim()
                    if (hex.length == algorithm.hexLength && hex.all { it in '0'..'9' || it in 'a'..'f' }) {
                        return algorithm to hex
                    }
                    return null
                }
            }
            val hex = trimmed.lowercase().substringBefore(' ').trim()
            if (!hex.all { it in '0'..'9' || it in 'a'..'f' }) return null
            val byLength = when (hex.length) {
                CRC32.hexLength -> CRC32
                MD5.hexLength -> MD5
                SHA1.hexLength -> SHA1
                SHA256.hexLength -> SHA256
                SHA512.hexLength -> SHA512
                else -> null
            }
            if (byLength != null) return byLength to hex
            return null
        }

        fun fromHeaderName(name: String): ChecksumAlgorithm? {
            return when (name.lowercase()) {
                "sha-512", "sha512" -> SHA512
                "sha-256", "sha256" -> SHA256
                "sha", "sha-1", "sha1" -> SHA1
                "md5" -> MD5
                else -> null
            }
        }
    }
}

@Serializable
data class Checksum(val algorithm: ChecksumAlgorithm, val hex: String) {
    fun display(): String = "${algorithm.displayName.lowercase()}:$hex"

    companion object {
        fun parse(text: String): Checksum? {
            val parsed = ChecksumAlgorithm.parse(text) ?: return null
            return Checksum(parsed.first, parsed.second)
        }

        fun fromHeaders(headers: Map<String, String>): Checksum? {
            val found = mutableListOf<Checksum>()
            headers["Repr-Digest"]?.let { value ->
                Regex("([A-Za-z0-9-]+)=:([A-Za-z0-9+/=]+):").findAll(value).forEach { match ->
                    ChecksumAlgorithm.fromHeaderName(match.groupValues[1])?.let { algorithm ->
                        decodeBase64Hex(match.groupValues[2], algorithm)?.let { found.add(Checksum(algorithm, it)) }
                    }
                }
            }
            headers["Digest"]?.let { value ->
                value.split(',').forEach { part ->
                    val name = part.substringBefore('=').trim()
                    val encoded = part.substringAfter('=', "").trim()
                    ChecksumAlgorithm.fromHeaderName(name)?.let { algorithm ->
                        decodeBase64Hex(encoded, algorithm)?.let { found.add(Checksum(algorithm, it)) }
                    }
                }
            }
            headers["Content-MD5"]?.let { value ->
                decodeBase64Hex(value.trim(), ChecksumAlgorithm.MD5)?.let { found.add(Checksum(ChecksumAlgorithm.MD5, it)) }
            }
            headers["x-goog-hash"]?.let { value ->
                value.split(',').forEach { part ->
                    val trimmed = part.trim()
                    if (trimmed.startsWith("md5=")) {
                        decodeBase64Hex(trimmed.removePrefix("md5="), ChecksumAlgorithm.MD5)?.let { found.add(Checksum(ChecksumAlgorithm.MD5, it)) }
                    }
                }
            }
            return found.maxByOrNull { it.algorithm.ordinal }
        }

        private fun decodeBase64Hex(encoded: String, algorithm: ChecksumAlgorithm): String? {
            return try {
                val bytes = android.util.Base64.decode(encoded, android.util.Base64.DEFAULT)
                if (bytes.size != algorithm.byteLength) return null
                bytes.joinToString("") { "%02x".format(it) }
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
