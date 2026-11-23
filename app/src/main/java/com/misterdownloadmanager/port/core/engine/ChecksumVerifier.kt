package com.misterdownloadmanager.port.core.engine

import com.misterdownloadmanager.port.core.model.Checksum
import com.misterdownloadmanager.port.core.model.ChecksumAlgorithm
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.zip.CRC32

object ChecksumVerifier {
    fun digestLength(algorithm: ChecksumAlgorithm): Int = algorithm.byteLength

    fun strength(algorithm: ChecksumAlgorithm): Int {
        return when (algorithm) {
            ChecksumAlgorithm.CRC32 -> 0
            ChecksumAlgorithm.MD5 -> 1
            ChecksumAlgorithm.SHA1 -> 2
            ChecksumAlgorithm.SHA256 -> 3
            ChecksumAlgorithm.SHA512 -> 4
        }
    }

    fun strongest(checksums: List<Checksum>): Checksum? {
        return checksums.maxByOrNull { strength(it.algorithm) }
    }

    fun compute(file: File, algorithm: ChecksumAlgorithm): String {
        if (algorithm == ChecksumAlgorithm.CRC32) {
            val crc = CRC32()
            FileInputStream(file).use { input ->
                val buffer = ByteArray(1048576)
                var read = input.read(buffer)
                while (read >= 0) {
                    crc.update(buffer, 0, read)
                    read = input.read(buffer)
                }
            }
            return "%08x".format(crc.value)
        }
        val digestName = when (algorithm) {
            ChecksumAlgorithm.MD5 -> "MD5"
            ChecksumAlgorithm.SHA1 -> "SHA-1"
            ChecksumAlgorithm.SHA256 -> "SHA-256"
            ChecksumAlgorithm.SHA512 -> "SHA-512"
            ChecksumAlgorithm.CRC32 -> "MD5"
        }
        val digest = MessageDigest.getInstance(digestName)
        FileInputStream(file).use { input ->
            val buffer = ByteArray(1048576)
            var read = input.read(buffer)
            while (read >= 0) {
                digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun verify(file: File, expected: Checksum): Boolean {
        if (!file.exists()) return false
        val actual = compute(file, expected.algorithm)
        return actual.equals(expected.hex, ignoreCase = true)
    }
}
