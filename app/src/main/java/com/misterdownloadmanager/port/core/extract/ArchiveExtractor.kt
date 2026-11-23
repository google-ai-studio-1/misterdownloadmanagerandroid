package com.misterdownloadmanager.port.core.extract

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

enum class ArchiveFormat {
    ZIP,
    SEVEN_ZIP,
    RAR,
    TAR,
    TAR_GZ,
    GZ,
    TAR_XZ,
    XZ,
    UNKNOWN
}

enum class ExtractSubfolder {
    SMART,
    ALWAYS,
    NEVER
}

sealed class ExtractError {
    object NeedsPassword : ExtractError()
    object WrongPassword : ExtractError()
    data class MissingVolume(val number: Int) : ExtractError()
    data class Damaged(val detail: String) : ExtractError()
    data class TooBig(val wouldBe: Long, val limit: Long) : ExtractError()
    object TooMany : ExtractError()
    object NoTool : ExtractError()
    data class NoRoom(val detail: String) : ExtractError()
    object Unsupported : ExtractError()
    object Cancelled : ExtractError()
    data class Io(val detail: String) : ExtractError()
}

data class ExtractReport(
    val folder: File,
    val files: List<File>,
    val bytes: Long,
    val skipped: List<Pair<String, String>>,
    val renamed: List<Pair<String, String>>
)

object ArchiveLimits {
    const val MAX_ENTRIES: Int = 100_000
    const val MAX_DEPTH: Int = 64
    const val MIN_TOTAL_BYTES: Long = 20L shl 30

    fun totalLimitFor(archiveSize: Long): Long {
        return maxOf(archiveSize * 200L, MIN_TOTAL_BYTES)
    }
}

object SafePath {
    private val ILLEGAL = charArrayOf(':', '<', '>', '"', '|', '?', '*')

    fun isSafe(entryName: String): Boolean {
        val normalized = entryName.replace('\\', '/')
        if (normalized.startsWith("/") || normalized.contains("://")) return false
        val parts = normalized.split('/')
        for (part in parts) {
            if (part == "..") return false
            if (part.any { it in ILLEGAL || it.isISOControl() }) return false
        }
        return true
    }

    fun cleanComponents(entryName: String): List<String> {
        return entryName.replace('\\', '/').split('/')
            .filter { it.isNotEmpty() && it != "." }
            .map { part ->
                part.map { character ->
                    if (character in ILLEGAL || character.isISOControl()) '_' else character
                }.joinToString("")
            }
    }

    fun depth(entryName: String): Int {
        return entryName.replace('\\', '/').split('/').count { it.isNotEmpty() && it != "." }
    }

    fun resolve(destination: File, entryName: String): File? {
        if (!isSafe(entryName)) return null
        val target = File(destination, cleanComponents(entryName).joinToString(File.separator))
        val base = destination.canonicalFile
        val canonical = target.canonicalFile
        if (canonical != base && !canonical.path.startsWith(base.path + File.separator)) return null
        return target
    }
}

class ArchiveExtractor {
    fun detectFormat(file: File): ArchiveFormat {
        val header = ByteArray(512)
        val read = FileInputStream(file).use { it.read(header) }
        if (read < 4) return ArchiveFormat.UNKNOWN
        if (header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() &&
            ((header[2] == 0x03.toByte() && header[3] == 0x04.toByte()) ||
                (header[2] == 0x05.toByte() && header[3] == 0x06.toByte()))
        ) {
            return ArchiveFormat.ZIP
        }
        if (read >= 6 && header[0] == 0x37.toByte() && header[1] == 0x7A.toByte() &&
            header[2] == 0xBC.toByte() && header[3] == 0xAF.toByte() &&
            header[4] == 0x27.toByte() && header[5] == 0x1C.toByte()
        ) {
            return ArchiveFormat.SEVEN_ZIP
        }
        if (read >= 7 && header[0] == 0x52.toByte() && header[1] == 0x61.toByte() &&
            header[2] == 0x72.toByte() && header[3] == 0x21.toByte() &&
            header[4] == 0x1A.toByte() && header[5] == 0x07.toByte()
        ) {
            return ArchiveFormat.RAR
        }
        if (read >= 262 && header[257] == 0x75.toByte() && header[258] == 0x73.toByte() &&
            header[259] == 0x74.toByte() && header[260] == 0x61.toByte() && header[261] == 0x72.toByte()
        ) {
            return ArchiveFormat.TAR
        }
        if (header[0] == 0x1F.toByte() && header[1] == 0x8B.toByte()) {
            val name = file.name.lowercase()
            return if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) ArchiveFormat.TAR_GZ else ArchiveFormat.GZ
        }
        if (read >= 6 && header[0] == 0xFD.toByte() && header[1] == 0x37.toByte() &&
            header[2] == 0x7A.toByte() && header[3] == 0x58.toByte() &&
            header[4] == 0x5A.toByte() && header[5] == 0x00.toByte()
        ) {
            val name = file.name.lowercase()
            return if (name.endsWith(".tar.xz") || name.endsWith(".txz")) ArchiveFormat.TAR_XZ else ArchiveFormat.XZ
        }
        return ArchiveFormat.UNKNOWN
    }

    fun extract(
        archive: File,
        destination: File,
        subfolder: ExtractSubfolder = ExtractSubfolder.SMART
    ): Result<ExtractReport> {
        val format = detectFormat(archive)
        if (format == ArchiveFormat.SEVEN_ZIP || format == ArchiveFormat.RAR ||
            format == ArchiveFormat.XZ || format == ArchiveFormat.TAR_XZ
        ) {
            return Result.failure(IllegalStateException("NoTool: this format needs an external 7-Zip tool, which is not available on Android."))
        }
        if (format == ArchiveFormat.UNKNOWN || format == ArchiveFormat.GZ) {
            return Result.failure(IllegalStateException("Unsupported archive format."))
        }
        val staging = File(destination.parentFile, destination.name + ".staging-" + System.currentTimeMillis())
        staging.mkdirs()
        return try {
            val entries = when (format) {
                ArchiveFormat.ZIP -> extractZip(archive, staging)
                ArchiveFormat.TAR -> extractTar(FileInputStream(archive), staging)
                ArchiveFormat.TAR_GZ -> extractTar(GZIPInputStream(FileInputStream(archive)), staging)
                else -> throw IllegalStateException("Unsupported archive format.")
            }
            val target = resolveDestination(destination, staging, subfolder, archive, entries)
            target.mkdirs()
            val moved = mutableListOf<File>()
            var bytes = 0L
            for (entry in entries) {
                val relative = entry.relativeTo(staging)
                val finalFile = numbered(File(target, relative.path))
                finalFile.parentFile?.mkdirs()
                if (!entry.renameTo(finalFile)) {
                    entry.copyTo(finalFile, overwrite = false)
                    entry.delete()
                }
                bytes += finalFile.length()
                moved.add(finalFile)
            }
            staging.deleteRecursively()
            Result.success(ExtractReport(target, moved, bytes, emptyList(), emptyList()))
        } catch (e: Exception) {
            staging.deleteRecursively()
            Result.failure(e)
        }
    }

    private fun resolveDestination(destination: File, staging: File, subfolder: ExtractSubfolder, archive: File, entries: List<File>): File {
        val topLevel = entries.map { it.relativeTo(staging).path.split(File.separator).firstOrNull() ?: "" }.distinct()
        val archiveStem = archive.nameWithoutExtension
        return when (subfolder) {
            ExtractSubfolder.ALWAYS -> File(destination, archiveStem)
            ExtractSubfolder.NEVER -> destination
            ExtractSubfolder.SMART -> if (entries.size > 1 || topLevel.size > 1) File(destination, archiveStem) else destination
        }
    }

    private fun numbered(file: File): File {
        if (!file.exists()) return file
        val stem = file.nameWithoutExtension
        val extension = file.extension
        var index = 1
        while (true) {
            val candidate = if (extension.isEmpty()) {
                File(file.parentFile, "$stem ($index)")
            } else {
                File(file.parentFile, "$stem ($index).$extension")
            }
            if (!candidate.exists()) return candidate
            index += 1
        }
    }

    private fun extractZip(archive: File, staging: File): List<File> {
        val extracted = mutableListOf<File>()
        val limit = ArchiveLimits.totalLimitFor(archive.length())
        var totalRead = 0L
        ZipInputStream(FileInputStream(archive)).use { input ->
            var entry = input.nextEntry
            while (entry != null) {
                if (extracted.size >= ArchiveLimits.MAX_ENTRIES) {
                    throw IllegalStateException("TooMany: archive has more than ${ArchiveLimits.MAX_ENTRIES} entries.")
                }
                if (SafePath.depth(entry.name) > ArchiveLimits.MAX_DEPTH) {
                    input.closeEntry()
                    entry = input.nextEntry
                    continue
                }
                val target = SafePath.resolve(staging, entry.name)
                if (target != null && !entry.isDirectory) {
                    target.parentFile?.mkdirs()
                    val buffer = ByteArray(65_536)
                    FileOutputStream(target).use { output ->
                        var read = input.read(buffer)
                        while (read >= 0) {
                            totalRead += read
                            if (totalRead > limit) throw IllegalStateException("TooBig: archive expands past its limit.")
                            output.write(buffer, 0, read)
                            read = input.read(buffer)
                        }
                    }
                    extracted.add(target)
                } else {
                    val buffer = ByteArray(65_536)
                    var read = input.read(buffer)
                    while (read >= 0) {
                        totalRead += read
                        if (totalRead > limit) throw IllegalStateException("TooBig: archive expands past its limit.")
                        read = input.read(buffer)
                    }
                }
                input.closeEntry()
                entry = input.nextEntry
            }
        }
        return extracted
    }

    private fun extractTar(inputStream: java.io.InputStream, staging: File): List<File> {
        val extracted = mutableListOf<File>()
        val header = ByteArray(512)
        inputStream.use { input ->
            while (true) {
                var filled = 0
                while (filled < 512) {
                    val read = input.read(header, filled, 512 - filled)
                    if (read < 0) return extracted
                    filled += read
                }
                if (header.all { it == 0.toByte() }) return extracted
                val name = String(header, 0, 100, Charsets.UTF_8).trimEnd('\u0000').trim()
                val sizeText = String(header, 124, 12, Charsets.UTF_8).trimEnd('\u0000').trim()
                val size = sizeText.toLongOrNull(8) ?: 0L
                val typeFlag = header[156]
                val prefix = String(header, 345, 155, Charsets.UTF_8).trimEnd('\u0000').trim()
                val fullName = if (prefix.isNotEmpty()) "$prefix/$name" else name
                if (extracted.size >= ArchiveLimits.MAX_ENTRIES) {
                    throw IllegalStateException("TooMany: archive has more than ${ArchiveLimits.MAX_ENTRIES} entries.")
                }
                if (typeFlag == '0'.code.toByte() || typeFlag == 0.toByte()) {
                    if (SafePath.depth(fullName) <= ArchiveLimits.MAX_DEPTH) {
                        val target = SafePath.resolve(staging, fullName)
                        if (target != null) {
                            target.parentFile?.mkdirs()
                            var remaining = size
                            val buffer = ByteArray(65_536)
                            FileOutputStream(target).use { output ->
                                while (remaining > 0L) {
                                    val wanted = minOf(buffer.size.toLong(), remaining).toInt()
                                    val read = input.read(buffer, 0, wanted)
                                    if (read < 0) break
                                    output.write(buffer, 0, read)
                                    remaining -= read
                                }
                            }
                            extracted.add(target)
                        } else {
                            skipBytes(input, size)
                        }
                    } else {
                        skipBytes(input, size)
                    }
                } else {
                    skipBytes(input, size)
                }
                val padding = (512L - (size % 512L)) % 512L
                skipBytes(input, padding)
            }
        }
    }

    private fun skipBytes(input: java.io.InputStream, count: Long) {
        var remaining = count
        val buffer = ByteArray(65_536)
        while (remaining > 0L) {
            val wanted = minOf(buffer.size.toLong(), remaining).toInt()
            val read = input.read(buffer, 0, wanted)
            if (read < 0) return
            remaining -= read
        }
    }
}
