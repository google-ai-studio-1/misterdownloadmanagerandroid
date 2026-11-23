package com.misterdownloadmanager.port.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.misterdownloadmanager.port.core.engine.DownloadEngine
import com.misterdownloadmanager.port.core.engine.FileNaming
import com.misterdownloadmanager.port.core.model.Category
import com.misterdownloadmanager.port.core.model.DownloadItem
import com.misterdownloadmanager.port.core.model.DownloadKind
import com.misterdownloadmanager.port.core.model.DownloadStatus
import com.misterdownloadmanager.port.core.model.Settings
import com.misterdownloadmanager.port.data.DownloadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class DownloadViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DownloadRepository(File(application.filesDir, "data"))
    private val engine = DownloadEngine(Settings())

    val items: StateFlow<List<DownloadItem>> = repository.items
    val settings: StateFlow<Settings> = repository.settings

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow(Category.ALL.id)
    val selectedCategoryId: StateFlow<String> = _selectedCategoryId.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            repository.load()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(categoryId: String) {
        _selectedCategoryId.value = categoryId
    }

    fun clearError() {
        _error.value = null
    }

    fun addDownload(url: String, fileName: String?, categoryId: String?): DownloadItem? {
        val trimmed = url.trim()
        val valid = trimmed.startsWith("http://") || trimmed.startsWith("https://") ||
            trimmed.startsWith("ftp://") || trimmed.startsWith("magnet:")
        if (!valid) {
            _error.value = "Enter a valid URL or magnet link."
            return null
        }
        val settings = repository.settings.value
        val name = fileName?.takeIf { it.isNotBlank() } ?: FileNaming.fromUrl(trimmed)
        val kind = when {
            trimmed.startsWith("magnet:") -> DownloadKind.TORRENT
            trimmed.startsWith("ftp://") -> DownloadKind.FTP
            else -> DownloadKind.HTTP
        }
        val category = categoryId ?: Category.of(name).id
        val folder = getApplication<Application>().getExternalFilesDir(null)?.absolutePath
            ?: getApplication<Application>().filesDir.absolutePath
        val downloadFolder = File(folder, "Download").absolutePath
        val item = DownloadItem(
            id = repository.nextId(),
            url = trimmed,
            fileName = FileNaming.sanitize(name),
            destinationFolder = downloadFolder,
            categoryId = category,
            kind = kind,
            status = DownloadStatus.QUEUED,
            maxConnections = settings.maxConnectionsPerDownload,
            createdEpochMillis = System.currentTimeMillis()
        )
        repository.add(item)
        viewModelScope.launch { repository.save() }
        return item
    }

    fun pauseDownload(item: DownloadItem) {
        if (!item.status.canPause()) return
        repository.update(item.copy(status = DownloadStatus.STOPPED))
        viewModelScope.launch { repository.save() }
    }

    fun resumeDownload(item: DownloadItem) {
        if (!item.status.canResume()) return
        repository.update(item.copy(status = DownloadStatus.QUEUED, error = null))
        viewModelScope.launch { repository.save() }
    }

    fun deleteDownload(item: DownloadItem, deleteFile: Boolean) {
        repository.remove(item.id, deleteFile)
        viewModelScope.launch { repository.save() }
    }

    fun retryDownload(item: DownloadItem) {
        repository.update(item.copy(status = DownloadStatus.QUEUED, error = null, downloadedBytes = item.downloadedBytes))
        viewModelScope.launch { repository.save() }
    }

    fun clearCompleted() {
        val completedIds = repository.items.value.filter { it.status == DownloadStatus.COMPLETED }.map { it.id }
        completedIds.forEach { repository.remove(it, deleteFile = false) }
        viewModelScope.launch { repository.save() }
    }

    fun updateSettings(settings: Settings) {
        repository.updateSettings(settings)
        engine.setSpeedLimit(settings.speedLimitBytesPerSecond())
        viewModelScope.launch { repository.save() }
    }

    fun filteredItems(): List<DownloadItem> {
        val query = _searchQuery.value.trim().lowercase()
        val categoryId = _selectedCategoryId.value
        return repository.items.value.filter { item ->
            val matchesCategory = categoryId == Category.ALL.id || item.categoryId == categoryId
            val matchesQuery = query.isEmpty() ||
                item.fileName.lowercase().contains(query) ||
                item.url.lowercase().contains(query)
            matchesCategory && matchesQuery
        }
    }
}
