package com.misterdownloadmanager.port

import android.app.Application
import com.misterdownloadmanager.port.core.crash.CrashHandler
import java.io.File

class MisterDownloadManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(File(filesDir, "crashes"), BuildConfig.VERSION_NAME)
    }
}
