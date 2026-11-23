package com.misterdownloadmanager.port.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String,
    val name: String,
    val folderName: String,
    val extensions: List<String>
) {
    companion object {
        val ALL = Category("all", "All downloads", "", emptyList())
        val GENERAL = Category("general", "General", "", emptyList())
        val COMPRESSED = Category(
            "compressed", "Compressed", "Compressed",
            listOf("zip", "rar", "r0*", "r1*", "arj", "gz", "sit", "sitx", "sea", "ace", "bz2", "7z")
        )
        val DOCUMENTS = Category(
            "documents", "Documents", "Documents",
            listOf("doc", "pdf", "ppt", "pps", "docx", "pptx")
        )
        val MUSIC = Category(
            "music", "Music", "Music",
            listOf("mp3", "wav", "wma", "mpa", "ram", "ra", "aac", "aif", "m4a", "tsa")
        )
        val PROGRAMS = Category(
            "programs", "Programs", "Programs",
            listOf("exe", "msi")
        )
        val VIDEO = Category(
            "video", "Video", "Video",
            listOf("avi", "mpg", "mpe", "mpeg", "asf", "wmv", "mov", "qt", "rm", "mp4", "flv", "m4v", "webm", "ogv", "ogg", "mkv", "ts", "tsv")
        )
        val TORRENTS = Category("torrents", "Torrents", "Torrents", emptyList())

        val BUILT_IN = listOf(COMPRESSED, DOCUMENTS, MUSIC, PROGRAMS, VIDEO, TORRENTS)
        val DEFAULTS = listOf(GENERAL) + BUILT_IN

        fun of(fileName: String, custom: List<CustomCategory> = emptyList(), deletedBuiltIn: Set<String> = emptySet()): Category {
            val extension = fileName.substringAfterLast('.', "").lowercase()
            if (extension.isEmpty()) return GENERAL
            for (item in custom) {
                if (item.matches(extension)) return Category("custom:${item.id}", item.name, item.folder ?: item.name, item.types())
            }
            for (item in BUILT_IN) {
                if (deletedBuiltIn.contains(item.id)) continue
                if (item.id == TORRENTS.id) continue
                if (item.extensions.any { matchesPattern(it, extension) }) return item
            }
            return GENERAL
        }

        fun matchesPattern(pattern: String, extension: String): Boolean {
            return if (pattern.endsWith("*")) {
                extension.startsWith(pattern.dropLast(1))
            } else {
                pattern == extension
            }
        }

        fun normalizeTypes(text: String): String {
            return text.split(' ', ',', ';')
                .map { it.trim().trimStart('.', '*').lowercase() }
                .filter { it.isNotBlank() }
                .joinToString(" ")
        }
    }
}

@Serializable
data class CustomCategory(
    val id: Int,
    val name: String,
    val types: String,
    val folder: String? = null
) {
    fun types(): List<String> = Category.normalizeTypes(types).split(' ').filter { it.isNotBlank() }

    fun matches(extension: String): Boolean {
        return types().any { Category.matchesPattern(it, extension) }
    }
}

@Serializable
data class BuiltinEdit(
    val categoryId: String,
    val name: String? = null,
    val types: String? = null,
    val deleted: Boolean = false
)
