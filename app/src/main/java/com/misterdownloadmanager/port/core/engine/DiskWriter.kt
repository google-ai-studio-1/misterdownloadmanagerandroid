package com.misterdownloadmanager.port.core.engine

import java.io.File
import java.io.RandomAccessFile

class DiskWriter(private val file: File) {
    fun ensureSize(size: Long) {
        RandomAccessFile(file, "rw").use { access ->
            if (access.length() < size) {
                access.setLength(size)
            }
        }
    }

    fun writeAt(position: Long, bytes: ByteArray, length: Int) {
        RandomAccessFile(file, "rw").use { access ->
            access.seek(position)
            access.write(bytes, 0, length)
        }
    }

    fun exists(): Boolean = file.exists()

    fun length(): Long = if (file.exists()) file.length() else 0L

    fun delete(): Boolean = file.delete()
}

object SparseFile {
    const val MIN_SIZE_BYTES: Long = 5L shl 20

    fun create(file: File, size: Long): DiskWriter {
        val writer = DiskWriter(file)
        writer.ensureSize(size)
        return writer
    }
}

sealed class DiskCheck {
    object Ok : DiskCheck()
    data class TooBigForFileSystem(val size: Long, val fileSystem: String) : DiskCheck()
    data class NoSpace(val needed: Long, val free: Long) : DiskCheck()
}

object DiskSpace {
    const val MARGIN_BYTES: Long = 128L shl 20
    const val FAT32_MAX_BYTES: Long = (4L shl 30) - 1L

    fun check(directory: File, size: Long): DiskCheck {
        val free = directory.usableSpace
        if (size > FAT32_MAX_BYTES) {
            return DiskCheck.TooBigForFileSystem(size, "FAT32")
        }
        if (size + MARGIN_BYTES > free) {
            return DiskCheck.NoSpace(size, free - minOf(MARGIN_BYTES, free))
        }
        return DiskCheck.Ok
    }

    fun human(bytes: Long): String {
        if (bytes < 1024L) return "$bytes bytes"
        val units = arrayOf("KB", "MB", "GB", "TB", "PB")
        var value = bytes.toDouble() / 1024.0
        var unit = 0
        while (value >= 1024.0 && unit < units.size - 1) {
            value /= 1024.0
            unit += 1
        }
        return when {
            value < 10.0 -> "%.2f %s".format(value, units[unit])
            value < 100.0 -> "%.1f %s".format(value, units[unit])
            else -> "%.0f %s".format(value, units[unit])
        }
    }
}
