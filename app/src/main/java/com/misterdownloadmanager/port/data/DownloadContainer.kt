package com.misterdownloadmanager.port.data

import android.content.Context
import com.misterdownloadmanager.port.core.engine.DownloadEngine
import com.misterdownloadmanager.port.core.model.Settings
import java.io.File

object DownloadContainer {
    @Volatile
    private var repository: DownloadRepository? = null

    @Volatile
    private var engine: DownloadEngine? = null

    fun repository(context: Context): DownloadRepository {
        repository?.let { return it }
        return synchronized(this) {
            repository ?: DownloadRepository(File(context.applicationContext.filesDir, "data")).also { repository = it }
        }
    }

    fun engine(context: Context, settings: Settings): DownloadEngine {
        engine?.let { return it }
        return synchronized(this) {
            engine ?: DownloadEngine(settings).also { engine = it }
        }
    }
}
