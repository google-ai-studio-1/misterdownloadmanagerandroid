package com.misterdownloadmanager.port.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.misterdownloadmanager.port.MainActivity
import com.misterdownloadmanager.port.R
import com.misterdownloadmanager.port.core.engine.DownloadEngine
import com.misterdownloadmanager.port.core.model.DownloadItem
import com.misterdownloadmanager.port.core.model.Settings
import com.misterdownloadmanager.port.data.DownloadRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

class DownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var engine: DownloadEngine? = null
    private var repository: DownloadRepository? = null
    private var job: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val settings = Settings()
        engine = DownloadEngine(settings)
        repository = DownloadRepository(filesDir)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val itemId = intent?.getLongExtra(EXTRA_DOWNLOAD_ID, -1L) ?: -1L
        startForeground(NOTIFICATION_ID, buildNotification())
        if (itemId >= 0L) {
            val item = repository?.items?.value?.firstOrNull { it.id == itemId }
            if (item != null) startDownload(item)
        }
        return START_STICKY
    }

    fun startDownload(item: DownloadItem) {
        job?.cancel()
        job = scope.launch {
            val target = File(item.destinationFolder, item.fileName)
            engine?.run(item, target)
            repository?.save()
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_downloads),
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = getString(R.string.notification_channel_downloads_desc)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.status_downloading))
            .setSmallIcon(R.drawable.ic_download)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        job?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID: String = "downloads"
        const val NOTIFICATION_ID: Int = 1
        const val EXTRA_DOWNLOAD_ID: String = "download_id"

        fun start(context: Context, downloadId: Long) {
            val intent = Intent(context, DownloadService::class.java)
            intent.putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
