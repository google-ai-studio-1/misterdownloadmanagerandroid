package com.misterdownloadmanager.port.core.tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class VideoFormat(
    val formatId: String,
    val extension: String,
    val resolution: String?,
    val hasVideo: Boolean,
    val hasAudio: Boolean
)

data class PlaylistEntry(
    val url: String,
    val title: String?,
    val index: Int
)

object VideoTools {
    const val MAX_WHILE_RUNNING_PERCENT: Float = 99.9f

    fun isVideoUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("youtube.com") || lower.contains("youtu.be") ||
            lower.contains("vimeo.com") || lower.contains("dailymotion.com") ||
            lower.contains("twitch.tv")
    }

    fun isPlaylistUrl(url: String): Boolean {
        return url.contains("list=") || url.contains("/playlist")
    }

    suspend fun probeFormats(url: String): List<VideoFormat> {
        return withContext(Dispatchers.IO) {
            emptyList()
        }
    }
}

object PlaylistResolver {
    fun entries(urls: List<String>): List<PlaylistEntry> {
        return urls.mapIndexed { index, url -> PlaylistEntry(url = url, title = null, index = index) }
    }
}

object GalleryResolver {
    fun isGalleryUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("imgur.com") || lower.contains("instagram.com") ||
            lower.contains("flickr.com") || lower.contains("deviantart.com")
    }
}

class AutoUpdate(private val currentVersion: String) {
    fun isNewer(latestVersion: String): Boolean {
        val current = currentVersion.split('.').mapNotNull { it.toIntOrNull() }
        val latest = latestVersion.split('.').mapNotNull { it.toIntOrNull() }
        for (index in 0 until maxOf(current.size, latest.size)) {
            val a = current.getOrNull(index) ?: 0
            val b = latest.getOrNull(index) ?: 0
            if (b > a) return true
            if (b < a) return false
        }
        return false
    }

    fun downloadDirectory(filesDir: File): File {
        return File(filesDir, "updates").apply { mkdirs() }
    }
}
