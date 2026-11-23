package com.misterdownloadmanager.port.core.remote

import java.security.MessageDigest
import java.security.SecureRandom

data class RemoteDevice(
    val id: String,
    val name: String,
    val address: String,
    val port: Int,
    val paired: Boolean = false,
    val lastSeenEpochMillis: Long = 0L
) {
    companion object {
        const val MAX_NAME_LENGTH: Int = 40
    }
}

data class PairingCode(val code: String, val expiresEpochMillis: Long) {
    fun isExpired(nowEpochMillis: Long): Boolean = nowEpochMillis > expiresEpochMillis
}

object RemotePairing {
    const val PORT: Int = 8463
    const val CODE_VALID_MILLIS: Long = 600_000L
    const val MAX_BODY_BYTES: Int = 64 * 1024
    const val MAX_URL_LENGTH: Int = 8192
    const val MAX_TORRENT_BYTES: Long = 10L shl 20
    const val ANSWER_WAIT_MILLIS: Long = 5_000L

    fun generateCode(nowEpochMillis: Long, validityMillis: Long = CODE_VALID_MILLIS): PairingCode {
        val random = SecureRandom()
        val number = random.nextInt(100_000_000)
        val code = "%08d".format(number)
        return PairingCode(code = code, expiresEpochMillis = nowEpochMillis + validityMillis)
    }

    fun verify(expected: PairingCode, supplied: String, nowEpochMillis: Long): Boolean {
        if (expected.isExpired(nowEpochMillis)) return false
        return constantTimeEquals(expected.code, supplied.trim())
    }

    fun constantTimeEquals(left: String, right: String): Boolean {
        if (left.length != right.length) return false
        var difference = 0
        for (index in left.indices) {
            difference = difference or (left[index].code xor right[index].code)
        }
        return difference == 0
    }
}

enum class PairingCheck {
    RIGHT,
    WRONG,
    NO_CODE,
    TOO_SOON
}

class PairingGuard {
    companion object {
        const val TRIES_PER_ADDRESS: Int = 5
        const val TOTAL_TRIES: Int = 20
        const val PACE_MILLIS: Long = 1_000L
        const val PEERS_REMEMBERED: Int = 256
    }

    private val wrongByAddress = LinkedHashMap<String, Int>()
    private val lastAttemptByAddress = mutableMapOf<String, Long>()
    private var totalWrong = 0

    fun check(address: String, code: PairingCode?, supplied: String, nowEpochMillis: Long): PairingCheck {
        if (code == null || code.isExpired(nowEpochMillis)) return PairingCheck.NO_CODE
        val last = lastAttemptByAddress[address]
        if (last != null && nowEpochMillis - last < PACE_MILLIS) return PairingCheck.TOO_SOON
        lastAttemptByAddress[address] = nowEpochMillis
        if (RemotePairing.constantTimeEquals(code.code, supplied.trim())) {
            wrongByAddress.remove(address)
            return PairingCheck.RIGHT
        }
        val wrong = (wrongByAddress[address] ?: 0) + 1
        wrongByAddress[address] = wrong
        if (wrongByAddress.size > PEERS_REMEMBERED) {
            wrongByAddress.entries.firstOrNull()?.let { wrongByAddress.remove(it.key) }
        }
        totalWrong += 1
        return PairingCheck.WRONG
    }

    fun wrongLeft(address: String): Int {
        val perAddress = TRIES_PER_ADDRESS - (wrongByAddress[address] ?: 0)
        val total = TOTAL_TRIES - totalWrong
        return minOf(perAddress, total).coerceAtLeast(0)
    }
}

object RemoteDeviceTokens {
    fun generate(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hash(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun fingerprint(certificateSha256Hex: String): String {
        return certificateSha256Hex.lowercase()
    }
}
