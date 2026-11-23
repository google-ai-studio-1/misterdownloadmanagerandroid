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
import com.misterdownloadmanager.port.core.model.DownloadStatus
import com.misterdownloadmanager.port.data.DownloadContainer
import com.misterdownloadmanager.port.data.DownloadRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class DownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var engine: DownloadEngine? = null

    @Volatile
    private var repository: DownloadRepository? = null

    @Volatile
    private var job: Job? = null

    @Volatile
    private var runningId: Long? = null

    private val progressChannel = Channel<DownloadItem>(capacity = Channel.CONFLATED)
    private val lastForwarded = mutableMapOf<Long, DownloadItem>()

    private val queueMutex = Mutex()
    private lateinit var setup: Deferred<Unit>

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val app = applicationContext
        setup = scope.async {
            val repo = DownloadContainer.repository(app)
            repository = repo
            repo.loadOnce()
            engine = DownloadContainer.engine(app, repo.settings.value)
            scope.launch { collectEngineStates() }
            scope.launch { collectRepositoryItems() }
            scope.launch {
                for (item in progressChannel) {
                    repository?.update(item)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val itemId = intent?.getLongExtra(EXTRA_DOWNLOAD_ID, -1L) ?: -1L
        startForeground(NOTIFICATION_ID, buildNotification())
        scope.launch {
            setup.await()
            if (itemId >= 0L) {
                processQueue()
            } else if (repository?.settings?.value?.resumeOnStart == true) {
                processQueue()
            } else {
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private suspend fun collectEngineStates() {
        val eng = engine ?: return
        eng.states.collect { states ->
            states.values.forEach { item ->
                if (lastForwarded[item.id] != item) {
                    lastForwarded[item.id] = item
                    progressChannel.trySend(item)
                }
            }
        }
    }

    private suspend fun collectRepositoryItems() {
        val repo = repository ?: return
        repo.items.collect { items ->
            val running = runningId ?: return@collect
            val current = items.firstOrNull { it.id == running }
            if (current == null || current.status == DownloadStatus.STOPPED) {
                job?.cancel()
            }
        }
    }

    private suspend fun processQueue() {
        queueMutex.withLock {
            if (job?.isActive == true) return
            val repo = repository ?: return
            val next = repo.items.value
                .filter { it.status == DownloadStatus.QUEUED }
                .minByOrNull { it.id }
            if (next == null) {
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
                return
            }
            startDownload(next)
        }
    }

    private fun startDownload(item: DownloadItem) {
        val repo = repository ?: return
        val eng = engine ?: return
        runningId = item.id
        job = scope.launch {
            try {
                val target = File(item.destinationFolder, item.fileName)
                val result = eng.run(item, target)
                repo.update(result)
            } finally {
                withContext(NonCancellable) {
                    repo.save()
                    runningId = null
                    job = null
                    processQueue()
                }
            }
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
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.status_downloading))
            .setSmallIcon(R.drawable.ic_download)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        job?.cancel()
        progressChannel.close()
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
