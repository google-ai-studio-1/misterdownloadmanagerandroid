package com.misterdownloadmanager.port.data

import com.misterdownloadmanager.port.core.model.Category
import com.misterdownloadmanager.port.core.model.CustomCategory
import com.misterdownloadmanager.port.core.model.DownloadItem
import com.misterdownloadmanager.port.core.model.DownloadQueue
import com.misterdownloadmanager.port.core.model.DownloadStatus
import com.misterdownloadmanager.port.core.model.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class StoreData(
    val version: Int = Store.VERSION,
    val nextId: Long = 1L,
    val items: List<DownloadItem> = emptyList(),
    val settings: Settings = Settings(),
    val queues: List<DownloadQueue> = emptyList(),
    val customCategories: List<CustomCategory> = emptyList()
)

object Store {
    const val VERSION: Int = 2
    const val FILE_NAME: String = "downloads.json"
    const val GENERATION_EVERY_MILLIS: Long = 600_000L

    private val blocked = mutableSetOf<String>()
    private var lastRotationMillis: Long = 0L

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun file(directory: File): File = File(directory, FILE_NAME)

    fun load(directory: File): StoreData {
        val candidates = listOf(
            File(directory, FILE_NAME),
            File(directory, "$FILE_NAME.bak"),
            File(directory, "$FILE_NAME.1.bak"),
            File(directory, "$FILE_NAME.2.bak")
        )
        for (candidate in candidates) {
            if (!candidate.exists()) continue
            val text = try {
                candidate.readText().removePrefix("\uFEFF")
            } catch (e: Exception) {
                blocked.add(candidate.absolutePath)
                continue
            }
            try {
                val data = json.decodeFromString<StoreData>(text)
                return migrate(data)
            } catch (e: Exception) {
                if (candidate.name == FILE_NAME) {
                    val backup = File(directory, "$FILE_NAME.corrupt-${System.currentTimeMillis() / 1000L}.bak")
                    if (!backup.exists()) candidate.copyTo(backup)
                }
                continue
            }
        }
        return StoreData()
    }

    fun migrate(data: StoreData): StoreData {
        val items = data.items.map { item ->
            if (item.status == DownloadStatus.DOWNLOADING) item.copy(status = DownloadStatus.STOPPED) else item
        }
        val queues = if (data.version < 2) {
            data.queues.map { queue ->
                if (queue.filesAtOnce == 1) queue.copy(filesAtOnce = DownloadQueue.DEFAULT_FILES_AT_ONCE) else queue
            }
        } else {
            data.queues
        }
        val nextId = maxOf(data.nextId, 1L, (items.maxOfOrNull { it.id } ?: 0L) + 1L)
        return data.copy(version = VERSION, items = items, queues = queues, nextId = nextId)
    }

    fun save(directory: File, data: StoreData): Boolean {
        directory.mkdirs()
        val target = file(directory)
        if (blocked.contains(target.absolutePath)) return false
        if (target.exists()) {
            val previous = target.readText()
            rotateGenerations(directory, previous)
        }
        val temp = File(directory, "$FILE_NAME.tmp")
        temp.writeText(json.encodeToString(data))
        temp.outputStream().flush()
        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
        return true
    }

    private fun rotateGenerations(directory: File, previousContent: String) {
        val now = System.currentTimeMillis()
        if (now - lastRotationMillis < GENERATION_EVERY_MILLIS) return
        val main = File(directory, "$FILE_NAME.bak")
        if (main.exists() && main.readText() == previousContent) return
        lastRotationMillis = now
        val second = File(directory, "$FILE_NAME.2.bak")
        val first = File(directory, "$FILE_NAME.1.bak")
        if (first.exists()) first.copyTo(second, overwrite = true)
        if (main.exists()) main.copyTo(first, overwrite = true)
        main.writeText(previousContent)
    }

    fun categories(data: StoreData): List<Category> {
        return Category.DEFAULTS + data.customCategories.map {
            Category("custom:${it.id}", it.name, it.folder ?: it.name, it.types())
        }
    }
}
