package com.misterdownloadmanager.port.core.nmhost

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class NativeMessage(val type: String, val url: String? = null, val tabId: Long? = null)

object NativeMessagingHost {
    const val MAX_MESSAGE_BYTES: Int = 1_048_576
    const val POLL_WAIT_MILLIS: Long = 1500L
    const val POLL_EVERY_MILLIS: Long = 100L
    const val TITLE_MAX: Int = 300
    const val TOKEN_MAX: Int = 128
    const val SEARCH_MAX: Int = 200
    const val HAVE_MAX: Int = 512
    const val KEY_MAX: Int = 40
    const val PAGE_MAX: Int = 8192
    const val PAC_MAX_BYTES: Int = 256 * 1024
    const val START_WAIT_SECONDS: Long = 20L
    const val PACKAGED_HOST: String = "/usr/bin/misterdownloadmanager-nmhost"

    private val json = Json { ignoreUnknownKeys = true }

    fun readMessage(input: InputStream): NativeMessage? {
        val lengthBytes = ByteArray(4)
        if (input.read(lengthBytes) != 4) return null
        val length = (lengthBytes[0].toInt() and 0xFF) or
            ((lengthBytes[1].toInt() and 0xFF) shl 8) or
            ((lengthBytes[2].toInt() and 0xFF) shl 16) or
            ((lengthBytes[3].toInt() and 0xFF) shl 24)
        if (length <= 0 || length > 1_048_576) return null
        val payload = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val read = input.read(payload, offset, length - offset)
            if (read < 0) return null
            offset += read
        }
        return json.decodeFromString<NativeMessage>(String(payload, Charsets.UTF_8))
    }

    fun writeMessage(output: OutputStream, message: NativeMessage) {
        val payload = json.encodeToString(message).toByteArray(Charsets.UTF_8)
        val length = payload.size
        output.write(byteArrayOf(
            (length and 0xFF).toByte(),
            ((length shr 8) and 0xFF).toByte(),
            ((length shr 16) and 0xFF).toByte(),
            ((length shr 24) and 0xFF).toByte()
        ))
        output.write(payload)
        output.flush()
    }
}
