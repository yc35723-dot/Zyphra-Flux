package com.zyphra.flux

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Keeps the download alive when the app goes to background.
 * MainActivity starts/stops this service around each download.
 */
class DownloadForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        // We MUST call startForeground in onCreate for modern Android
        // to avoid "ForegroundServiceDidNotStartInTimeException"
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, VideoFlowApp.CHANNEL_ID)
            .setContentTitle("Zyphra Flux")
            .setContentText("正在下載影片…")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()

        startForeground(VideoFlowApp.NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
