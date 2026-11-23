package com.misterdownloadmanager.port.core.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket

data class FtpEntry(val name: String, val size: Long, val isDirectory: Boolean)

object FtpDefaults {
    const val PORT: Int = 21
    const val TLS_PORT: Int = 990
    const val IDLE_MILLIS: Long = 30_000L
    const val READ_BYTES: Int = 65_536
    const val ANONYMOUS_USER: String = "anonymous"
    const val ANONYMOUS_PASSWORD: String = "mdm@"
}

fun defaultFtpPort(scheme: String): Int {
    return when (scheme.lowercase()) {
        "ftps" -> FtpDefaults.TLS_PORT
        else -> FtpDefaults.PORT
    }
}

fun isFtpScheme(scheme: String): Boolean {
    return scheme.lowercase() in setOf("ftp", "ftps", "ftpes")
}

class FtpClient(
    private val host: String,
    private val port: Int = FtpDefaults.PORT,
    private val username: String = FtpDefaults.ANONYMOUS_USER,
    private val password: String = FtpDefaults.ANONYMOUS_PASSWORD,
    private val useTls: Boolean = false,
    private val timeoutMillis: Int = 30_000
) {
    fun parseUrl(url: String): Triple<String, Int, String> {
        val scheme = url.substringBefore("://", "").lowercase()
        val withoutScheme = url.substringAfter("://", url)
        val hostPort = withoutScheme.substringBefore('/')
        val path = "/" + withoutScheme.substringAfter('/', "")
        val hostPart = hostPort.substringAfter('@').substringBefore(':')
        val portPart = hostPort.substringAfter('@').substringAfter(':', "").toIntOrNull()
            ?: defaultFtpPort(scheme)
        return Triple(hostPart, portPart, path)
    }

    suspend fun fileSize(path: String): Long {
        return withContext(Dispatchers.IO) {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), timeoutMillis)
            socket.use {
                it.soTimeout = timeoutMillis
                val input = it.getInputStream()
                val output = it.getOutputStream()
                readReply(input)
                send(output, "USER $username")
                readReply(input)
                send(output, "PASS $password")
                readReply(input)
                send(output, "TYPE I")
                readReply(input)
                send(output, "SIZE $path")
                val reply = readReply(input)
                val code = reply.substringBefore(' ').trim().toIntOrNull() ?: 0
                if (code == 550) return@withContext -1L
                reply.substringAfter(' ').trim().toLongOrNull() ?: -1L
            }
        }
    }

    private fun send(output: java.io.OutputStream, command: String) {
        output.write("$command\r\n".toByteArray(Charsets.US_ASCII))
        output.flush()
    }

    private fun readReply(input: InputStream): String {
        val buffer = ByteArray(4096)
        val read = input.read(buffer)
        if (read <= 0) return ""
        return String(buffer, 0, read, Charsets.US_ASCII)
    }
}
