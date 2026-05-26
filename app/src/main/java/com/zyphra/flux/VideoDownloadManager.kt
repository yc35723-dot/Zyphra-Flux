package com.zyphra.flux

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

class VideoDownloadManager(private val context: Context) {

    companion object {
        private const val TAG = "VideoDownloadManager"
        private const val PREFS_NAME = "ZyphraFluxPrefs"
        private const val KEY_VIDEO_PATH = "video_storage_path"
        private const val KEY_AUDIO_PATH = "audio_storage_path"
        private const val KEY_VIDEO_QUALITY = "video_quality"
        private const val KEY_AUDIO_QUALITY = "audio_quality"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setVideoPath(path: String?) = prefs.edit().putString(KEY_VIDEO_PATH, path).apply()
    fun getVideoPath(): String? = prefs.getString(KEY_VIDEO_PATH, null)

    fun setAudioPath(path: String?) = prefs.edit().putString(KEY_AUDIO_PATH, path).apply()
    fun getAudioPath(): String? = prefs.getString(KEY_AUDIO_PATH, null)

    fun setVideoQuality(q: String) = prefs.edit().putString(KEY_VIDEO_QUALITY, q).apply()
    fun getVideoQuality(): String = prefs.getString(KEY_VIDEO_QUALITY, "1080p") ?: "1080p"

    fun setAudioQuality(q: String) = prefs.edit().putString(KEY_AUDIO_QUALITY, q).apply()
    fun getAudioQuality(): String = prefs.getString(KEY_AUDIO_QUALITY, "128k") ?: "128k"

    private fun getTempDir(): File {
        val dir = File(context.cacheDir, "dl_temp")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun download(
        url: String,
        isAudioOnly: Boolean = false,
        onProgress: (Float, Long, String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!VideoFlowApp.isInitialized) {
            return@withContext Result.failure(Exception("組件初始化中，請稍後再試"))
        }

        // Try to get video info first to get the title
        var displayTitle = "Download_${System.currentTimeMillis()}"
        try {
            val info = YoutubeDL.getInstance().getInfo(url)
            if (!info.title.isNullOrBlank()) {
                displayTitle = info.title!!
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get video info", e)
        }

        val tempDir = getTempDir()
        val tempFileName = "temp_${System.currentTimeMillis()}"
        val ext = if (isAudioOnly) "mp3" else "mp4"
        val tempFile = File(tempDir, "$tempFileName.$ext")

        try {
            val request = YoutubeDLRequest(url).apply {
                addOption("--no-mtime")
                addOption("--no-playlist")
                addOption("--no-check-certificate")
                
                if (isAudioOnly) {
                    val bitrate = getAudioQuality().replace("k", "")
                    addOption("-f", "bestaudio/best")
                    addOption("--extract-audio")
                    addOption("--audio-format", "mp3")
                    addOption("--audio-quality", bitrate)
                } else {
                    val quality = getVideoQuality()
                    val format = when(quality) {
                        "2k" -> "bestvideo[height<=1440]+bestaudio/best[height<=1440]"
                        "4k" -> "bestvideo[height<=2160]+bestaudio/best[height<=2160]"
                        else -> "bestvideo[height<=1080][fps<=60]+bestaudio/best[height<=1080]"
                    }
                    addOption("-f", "$format/best")
                    addOption("--merge-output-format", "mp4")
                }

                addOption("--user-agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                
                addOption("-o", tempFile.absolutePath)
            }

            YoutubeDL.getInstance().execute(request) { progress, eta, line ->
                val safeProgress = if (progress < 0) 0f else progress
                onProgress(safeProgress, eta, line ?: "")
            }

            val actualFile = findActualFile(tempDir, tempFileName)
                ?: return@withContext Result.failure(Exception("找不到下載後的原始檔案"))

            val finalUri = saveFile(actualFile, isAudioOnly, displayTitle)
            actualFile.delete()

            if (finalUri != null) Result.success(finalUri.toString())
            else Result.failure(Exception("無法存入目標位置"))

        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            if (tempFile.exists()) tempFile.delete()
            Result.failure(e)
        }
    }

    private fun findActualFile(dir: File, prefix: String): File? {
        return dir.listFiles()?.find { it.name.startsWith(prefix) }
    }

    private fun saveFile(file: File, isAudioOnly: Boolean, title: String): Uri? {
        val safeTitle = title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val fileName = "${safeTitle}.${file.extension}"
        
        val customPath = if (isAudioOnly) getAudioPath() else getVideoPath()
        if (customPath != null) {
            val destDir = File(customPath)
            if (!destDir.exists()) destDir.mkdirs()
            val destFile = File(destDir, fileName)
            try {
                file.copyTo(destFile, overwrite = true)
                scanFile(destFile.absolutePath, if (isAudioOnly) "audio/mpeg" else "video/mp4")
                return Uri.fromFile(destFile)
            } catch (e: Exception) {
                Log.e(TAG, "Custom path save failed", e)
            }
        }
        return saveToMediaStore(file, isAudioOnly, fileName)
    }

    private fun saveToMediaStore(file: File, isAudioOnly: Boolean, fileName: String): Uri? {
        val mimeType = if (isAudioOnly) "audio/mpeg" else "video/mp4"
        
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val folder = if (isAudioOnly) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$folder/ZyphraFlux")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (isAudioOnly) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            val folder = if (isAudioOnly) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES
            val publicDir = File(Environment.getExternalStoragePublicDirectory(folder), "ZyphraFlux")
            if (!publicDir.exists()) publicDir.mkdirs()
            val destFile = File(publicDir, fileName)
            file.copyTo(destFile, overwrite = true)
            scanFile(destFile.absolutePath, mimeType)
            return Uri.fromFile(destFile)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values) ?: return null

        try {
            resolver.openOutputStream(uri)?.use { os ->
                FileInputStream(file).use { it.copyTo(os) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            return uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            return null
        }
    }

    private fun scanFile(path: String, mimeType: String) {
        MediaScannerConnection.scanFile(context, arrayOf(path), arrayOf(mimeType), null)
    }

    suspend fun updateYtDlp(): String = withContext(Dispatchers.IO) {
        try {
            val status = YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)
            when (status) {
                YoutubeDL.UpdateStatus.DONE           -> "yt-dlp 已更新至最新版本 ✓"
                YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE -> "yt-dlp 已是最新版本"
                else                                   -> "更新狀態: $status"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update failed", e)
            "更新失敗: ${e.message}"
        }
    }
}
