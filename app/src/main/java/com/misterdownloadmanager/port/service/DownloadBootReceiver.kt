package com.misterdownloadmanager.port.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DownloadBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            DownloadService.start(context, -1L)
        }
    }
}
