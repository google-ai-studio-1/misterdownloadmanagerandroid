package com.misterdownloadmanager.port.core.engine

import java.net.URLDecoder

object FileNaming {
    private val ILLEGAL_CHARACTERS = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
    private val RESERVED_NAMES = setOf(
        "CON", "PRN", "AUX", "NUL", "CONIN\$", "CONOUT\$",
        "COM0", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "LPT0", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    )
    const val MAX_NAME_LENGTH: Int = 230

    fun fromUrl(url: String): String {
        val withoutQuery = url.substringBefore('?').substringBefore('#')
        val queryName = url.substringAfter('?', "")
            .substringBefore('#')
            .split('&')
            .firstOrNull { part ->
                val key = part.substringBefore('=').lowercase()
                key == "file" || key == "filename"
            }
            ?.substringAfter('=', "")
            ?.takeIf { it.isNotBlank() }
        val lastSegment = withoutQuery.substringAfterLast('/').ifBlank { "download" }
        val named = if (lastSegment == "download" || lastSegment.contains('.').not()) {
            queryName ?: lastSegment
        } else {
            lastSegment
        }
        return sanitize(decode(named))
    }

    fun fromContentDisposition(header: String?): String? {
        if (header.isNullOrBlank()) return null
        val filenameStar = Regex("filename\\*=UTF-8''([^;]+)", RegexOption.IGNORE_CASE)
            .find(header)?.groupValues?.getOrNull(1)
        if (filenameStar != null) return sanitize(decode(filenameStar))
        val filename = Regex("filename=\"?([^\";]+)\"?", RegexOption.IGNORE_CASE)
            .find(header)?.groupValues?.getOrNull(1)
        if (filename != null) return sanitize(filename)
        return null
    }

    fun sanitize(name: String): String {
        val withoutFormat = name.filter { Character.getType(it) != Character.FORMAT.toInt() }
        var result = withoutFormat.map { if (it.isISOControl()) '_' else it }.joinToString("").trim()
        for (character in ILLEGAL_CHARACTERS) {
            result = result.replace(character, '_')
        }
        result = result.trimEnd('.', ' ')
        if (result.isEmpty()) result = "download"
        val base = result.substringBefore('.').uppercase()
        if (RESERVED_NAMES.contains(base)) result = "_$result"
        if (result.length > MAX_NAME_LENGTH) {
            val extension = result.substringAfterLast('.', "")
            val stem = result.substringBeforeLast('.', result)
            result = if (extension.isNotEmpty() && extension != result) {
                stem.take(MAX_NAME_LENGTH - extension.length - 1) + "." + extension
            } else {
                result.take(MAX_NAME_LENGTH)
            }
        }
        return result
    }

    fun unique(baseName: String, exists: (String) -> Boolean): String {
        if (!exists(baseName)) return baseName
        val stem = baseName.substringBeforeLast('.', baseName)
        val extension = baseName.substringAfterLast('.', "")
        val hasExtension = extension.isNotEmpty() && extension != baseName
        var index = 1
        while (true) {
            val candidate = if (hasExtension) {
                "$stem ($index).$extension"
            } else {
                "$stem ($index)"
            }
            if (!exists(candidate)) return candidate
            index += 1
        }
    }

    private fun decode(raw: String): String {
        return try {
            URLDecoder.decode(raw.replace('+', ' '), "UTF-8")
        } catch (e: IllegalArgumentException) {
            raw
        }
    }
}
