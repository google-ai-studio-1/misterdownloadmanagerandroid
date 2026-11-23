package com.misterdownloadmanager.port.core.geo

object GeoDatabaseFormat {
    const val MAGIC: String = "MDMGEO"
    const val VERSION: Int = 1
    const val SPARSE_EVERY: Int = 256
    const val FLAG_COUNT: Int = 250
    const val FLAG_WIDTH: Int = 32
    const val FLAG_HEIGHT: Int = 24
    const val FLAG_COLUMNS: Int = 16
    const val ATTRIBUTION: String = "IP Geolocation by DB-IP"
    const val ATTRIBUTION_URL: String = "https://db-ip.com"

    fun flagCell(countryIndex: Int): Pair<Int, Int>? {
        if (countryIndex < 1) return null
        val cell = countryIndex - 1
        return Pair((cell % FLAG_COLUMNS) * FLAG_WIDTH, (cell / FLAG_COLUMNS) * FLAG_HEIGHT)
    }
}

class GeoDatabase private constructor(
    private val countryCodes: List<String>,
    private val ipv4Ranges: List<Pair<Long, Int>>,
    private val ipv6Ranges: List<Pair<java.math.BigInteger, Int>>
) {
    fun country(ip: String): String? {
        if (isReservedAddress(ip)) return null
        val ipv4 = parseIpv4(ip)
        if (ipv4 != null) {
            return lookupIpv4(ipv4)
        }
        val ipv6 = parseIpv6(ip) ?: return null
        if (ipv6.startsWith("::ffff:")) {
            val mapped = parseIpv4(ipv6.removePrefix("::ffff:")) ?: return null
            return lookupIpv4(mapped)
        }
        return lookupIpv6(ipv6)
    }

    private fun lookupIpv4(address: Long): String? {
        var best: Int? = null
        var low = 0
        var high = ipv4Ranges.size - 1
        while (low <= high) {
            val middle = (low + high) / 2
            if (ipv4Ranges[middle].first <= address) {
                best = middle
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        val index = best?.let { ipv4Ranges[it].second } ?: return null
        return countryCodes.getOrNull(index - 1)
    }

    private fun lookupIpv6(address: String): String? {
        val value = ipv6ToBigInteger(address) ?: return null
        var best: Int? = null
        var low = 0
        var high = ipv6Ranges.size - 1
        while (low <= high) {
            val middle = (low + high) / 2
            if (ipv6Ranges[middle].first <= value) {
                best = middle
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        val index = best?.let { ipv6Ranges[it].second } ?: return null
        return countryCodes.getOrNull(index - 1)
    }

    companion object {
        fun load(bytes: ByteArray): GeoDatabase? {
            return try {
                parse(bytes)
            } catch (e: Exception) {
                null
            }
        }

        private fun parse(bytes: ByteArray): GeoDatabase? {
            if (bytes.size < 7) return null
            val magic = String(bytes, 0, 6, Charsets.US_ASCII)
            if (magic != GeoDatabaseFormat.MAGIC || bytes[6] != GeoDatabaseFormat.VERSION.toByte()) return null
            var offset = 7
            val countryCount = bytes[offset].toInt() and 0xFF
            offset += 1
            val codes = mutableListOf<String>()
            repeat(countryCount) {
                codes.add(String(bytes, offset, 2, Charsets.US_ASCII))
                offset += 2
            }
            val ipv4Result = readIpv4Section(bytes, offset)
            offset = ipv4Result.second
            val ipv6Result = readIpv6Section(bytes, offset)
            return GeoDatabase(codes, ipv4Result.first, ipv6Result.first)
        }

        private fun readIpv4Section(bytes: ByteArray, start: Int): Pair<List<Pair<Long, Int>>, Int> {
            var offset = start
            val count = readInt32(bytes, offset)
            offset += 4
            val ranges = mutableListOf<Pair<Long, Int>>()
            var address = 0L
            repeat(count) {
                val delta = readLeb128(bytes, offset)
                address += delta.first
                offset = delta.second
                val index = bytes[offset].toInt() and 0xFF
                offset += 1
                ranges.add(address to index)
            }
            return ranges to offset
        }

        private fun readIpv6Section(bytes: ByteArray, start: Int): Pair<List<Pair<java.math.BigInteger, Int>>, Int> {
            var offset = start
            val count = readInt32(bytes, offset)
            offset += 4
            val ranges = mutableListOf<Pair<java.math.BigInteger, Int>>()
            var address = java.math.BigInteger.ZERO
            repeat(count) {
                val delta = readLeb128(bytes, offset)
                address = address.add(java.math.BigInteger.valueOf(delta.first))
                offset = delta.second
                val index = bytes[offset].toInt() and 0xFF
                offset += 1
                ranges.add(address to index)
            }
            return ranges to offset
        }

        private fun readInt32(bytes: ByteArray, offset: Int): Int {
            return (bytes[offset].toInt() and 0xFF) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 3].toInt() and 0xFF) shl 24)
        }

        private fun readLeb128(bytes: ByteArray, offset: Int): Pair<Long, Int> {
            var result = 0L
            var shift = 0
            var position = offset
            while (position < bytes.size) {
                val byte = bytes[position].toInt() and 0xFF
                position += 1
                result = result or ((byte and 0x7F).toLong() shl shift)
                if (byte and 0x80 == 0) break
                shift += 7
            }
            return result to position
        }

        fun parseIpv4(ip: String): Long? {
            val octets = ip.split('.').mapNotNull { it.toIntOrNull() }
            if (octets.size != 4 || octets.any { it !in 0..255 }) return null
            var value = 0L
            for (octet in octets) value = (value shl 8) or octet.toLong()
            return value
        }

        fun parseIpv6(ip: String): String? {
            val normalized = ip.lowercase()
            if (!normalized.contains(':')) return null
            return normalized
        }

        fun ipv6ToBigInteger(ip: String): java.math.BigInteger? {
            return try {
                val address = java.net.InetAddress.getByName(ip) ?: return null
                java.math.BigInteger(1, address.address)
            } catch (e: Exception) {
                null
            }
        }

        fun isReservedAddress(ip: String): Boolean {
            val ipv4 = parseIpv4(ip)
            if (ipv4 != null) {
                val first = (ipv4 shr 24) and 0xFF
                val second = (ipv4 shr 16) and 0xFF
                if (first == 0L || first == 10L || first == 127L) return true
                if (first == 169L && second == 254L) return true
                if (first == 172L && second in 16L..31L) return true
                if (first == 192L && second == 168L) return true
                if (first == 224L || first >= 240L || first == 255L) return true
                if (first == 100L && second in 64L..127L) return true
                return false
            }
            val lower = ip.lowercase()
            if (lower == "::1" || lower == "::") return true
            if (lower.startsWith("fe80:") || lower.startsWith("fc") || lower.startsWith("fd")) return true
            if (lower.startsWith("ff")) return true
            if (lower.startsWith("::ffff:")) return isReservedAddress(lower.removePrefix("::ffff:"))
            return false
        }
    }
}
