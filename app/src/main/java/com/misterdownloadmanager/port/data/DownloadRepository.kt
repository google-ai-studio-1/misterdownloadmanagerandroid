package com.misterdownloadmanager.port.data

import com.misterdownloadmanager.port.core.model.DownloadItem
import com.misterdownloadmanager.port.core.model.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class DownloadRepository(private val directory: File) {
    private val mutex = Mutex()
    private var nextId: Long = 1L
    private var loaded: Boolean = false
    private val _items = MutableStateFlow<List<DownloadItem>>(emptyList())
    val items: StateFlow<List<DownloadItem>> = _items.asStateFlow()
    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    suspend fun load() {
        mutex.withLock {
            loadLocked()
        }
    }

    suspend fun loadOnce() {
        mutex.withLock {
            if (!loaded) {
                loadLocked()
                loaded = true
            }
        }
    }

    private fun loadLocked() {
        val data = Store.load(directory)
        nextId = data.nextId
        _items.value = data.items
        _settings.value = data.settings.validated()
    }

    suspend fun save() {
        mutex.withLock {
            val data = StoreData(
                nextId = nextId,
                items = _items.value,
                settings = _settings.value,
                queues = emptyList()
            )
            Store.save(directory, data)
        }
    }

    fun nextId(): Long {
        val id = nextId
        nextId += 1L
        return id
    }

    fun add(item: DownloadItem) {
        _items.value = _items.value + item
    }

    fun update(item: DownloadItem) {
        _items.value = _items.value.map { if (it.id == item.id) item else it }
    }

    fun remove(id: Long, deleteFile: Boolean = false) {
        val item = _items.value.firstOrNull { it.id == id }
        if (deleteFile && item != null) {
            File(item.destinationFolder, item.fileName).delete()
        }
        _items.value = _items.value.filterNot { it.id == id }
    }

    fun updateSettings(settings: Settings) {
        _settings.value = settings.validated()
    }
}
