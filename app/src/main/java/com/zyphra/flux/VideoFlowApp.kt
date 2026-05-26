package com.zyphra.flux

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.ffmpeg.FFmpeg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VideoFlowApp : Application() {

    companion object {
        const val CHANNEL_ID        = "videoflow_download"
        const val NOTIFICATION_ID   = 1001
        private const val TAG       = "VideoFlowApp"

        var isInitialized = false
            private set
    }

    override fun onCreate() {
        super.onCreate()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Initialise basic binaries
                YoutubeDL.getInstance().init(this@VideoFlowApp)
                FFmpeg.init(this@VideoFlowApp)
                isInitialized = true
                Log.d(TAG, "YoutubeDL and FFmpeg initialised successfully")

                // Auto-update check on first launch or periodically
                val prefs = getSharedPreferences("ZyphraFluxPrefs", Context.MODE_PRIVATE)
                val lastUpdate = prefs.getLong("last_auto_update", 0)
                val now = System.currentTimeMillis()
                
                // If never updated or > 24 hours ago
                if (now - lastUpdate > 24 * 60 * 60 * 1000) {
                    Log.d(TAG, "Triggering background yt-dlp update...")
                    try {
                        YoutubeDL.getInstance().updateYoutubeDL(this@VideoFlowApp)
                        prefs.edit().putLong("last_auto_update", now).apply()
                        Log.d(TAG, "Background update finished")
                    } catch (e: Exception) {
                        Log.e(TAG, "Background update failed", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Initialisation failed", e)
            }
        }

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "影片下載",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Zyphra Flux 下載進度通知"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}
