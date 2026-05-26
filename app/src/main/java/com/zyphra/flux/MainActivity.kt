package com.zyphra.flux

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.zyphra.flux.databinding.ActivityMainBinding
import com.zyphra.flux.databinding.LayoutAdvancedBinding
import com.zyphra.flux.databinding.LayoutStandardBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val dlManager by lazy { VideoDownloadManager(this) }
    private var downloadJob: Job? = null

    private var standardBinding: LayoutStandardBinding? = null
    private var advancedBinding: LayoutAdvancedBinding? = null

    private var selectingVideoPath = true

    private val folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            val path = it.path?.substringAfter(":") ?: ""
            val fullPath = "/storage/emulated/0/$path"
            
            if (selectingVideoPath) {
                dlManager.setVideoPath(fullPath)
            } else {
                dlManager.setAudioPath(fullPath)
            }
            
            advancedBinding?.let { b -> updateAdvancedUI(b) }
            toast("儲存位置已更改")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        showPage(true)
        requestNeededPermissions()
    }

    private fun setupNavigation() {
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                showPage(checkedId == R.id.btnStandard)
            }
        }
    }

    private fun showPage(isStandard: Boolean) {
        binding.pageContainer.removeAllViews()
        if (isStandard) {
            if (standardBinding == null) {
                standardBinding = LayoutStandardBinding.inflate(layoutInflater, binding.pageContainer, false)
                setupStandardPage(standardBinding!!)
            }
            binding.pageContainer.addView(standardBinding!!.root)
            handleShareIntent(intent)
        } else {
            if (advancedBinding == null) {
                advancedBinding = LayoutAdvancedBinding.inflate(layoutInflater, binding.pageContainer, false)
                setupAdvancedPage(advancedBinding!!)
            }
            binding.pageContainer.addView(advancedBinding!!.root)
        }
    }

    private fun setupStandardPage(b: LayoutStandardBinding) {
        b.etUrl.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val url = s.toString().trim()
                if (url.isNotEmpty()) {
                    val platform = PlatformDetector.detect(url)
                    if (platform != Platform.UNKNOWN) {
                        b.llPlatform.text = "${platform.emoji} ${platform.displayName}"
                        b.llPlatform.visibility = View.VISIBLE
                    } else {
                        b.llPlatform.visibility = View.GONE
                    }
                } else {
                    b.llPlatform.visibility = View.GONE
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        b.tilUrl.setEndIconOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
            if (!text.isNullOrBlank()) b.etUrl.setText(text)
            else toast("剪貼簿沒有內容")
        }

        b.btnDownload.setOnClickListener {
            val url = b.etUrl.text.toString().trim()
            if (url.isEmpty()) toast("請輸入影片網址")
            else startDownload(url, false, b)
        }

        b.btnDownloadAudio.setOnClickListener {
            val url = b.etUrl.text.toString().trim()
            if (url.isEmpty()) toast("請輸入音檔網址")
            else startDownload(url, true, b)
        }

        b.btnCancel.setOnClickListener {
            downloadJob?.cancel()
            stopDownloadService()
            setDownloading(false, b)
            toast("已取消下載")
        }

        b.btnUpdate.setOnClickListener {
            b.btnUpdate.isEnabled = false
            b.btnUpdate.text = "更新中…"
            lifecycleScope.launch {
                val msg = dlManager.updateYtDlp()
                toast(msg)
                b.btnUpdate.isEnabled = true
                b.btnUpdate.text = "更新 yt-dlp 核心"
            }
        }
    }

    private fun startDownload(url: String, isAudioOnly: Boolean, b: LayoutStandardBinding) {
        setDownloading(true, b)
        b.tvStatus.text = "正在解析連結…"
        b.progressBar.progress = 0
        b.tvProgress.text = "0%"
        startDownloadService()

        downloadJob = lifecycleScope.launch {
            val result = dlManager.download(url, isAudioOnly) { progress, eta, line ->
                runOnUiThread {
                    b.progressBar.progress = progress.toInt()
                    b.tvProgress.text = "${progress.toInt()}%"
                    b.tvStatus.text = if (eta > 0) "剩餘約 ${eta}s" else line.take(72)
                }
            }
            stopDownloadService()
            setDownloading(false, b)
            result.fold(
                onSuccess = {
                    toast("✓ 下載完成！")
                    b.etUrl.text?.clear()
                    b.llPlatform.visibility = View.GONE
                },
                onFailure = { e -> toast("下載失敗：${e.message?.take(120)}") }
            )
        }
    }

    private fun setDownloading(active: Boolean, b: LayoutStandardBinding) {
        b.blurProgressCard.visibility = if (active) View.VISIBLE else View.GONE
        b.btnDownload.isEnabled = !active
        b.btnDownloadAudio.isEnabled = !active
    }

    private fun setupAdvancedPage(b: LayoutAdvancedBinding) {
        updateAdvancedUI(b)

        b.btnSelectVideoPath.setOnClickListener {
            selectingVideoPath = true
            folderPicker.launch(null)
        }

        b.btnSelectAudioPath.setOnClickListener {
            selectingVideoPath = false
            folderPicker.launch(null)
        }

        // Video Quality
        when(dlManager.getVideoQuality()) {
            "1080p" -> b.chip1080p.isChecked = true
            "2k" -> b.chip2k.isChecked = true
            "4k" -> b.chip4k.isChecked = true
        }
        b.cgVideoQuality.setOnCheckedStateChangeListener { _, checkedIds ->
            val q = when (checkedIds.firstOrNull()) {
                R.id.chip2k -> "2k"
                R.id.chip4k -> "4k"
                else -> "1080p"
            }
            dlManager.setVideoQuality(q)
        }

        // Audio Quality
        when(dlManager.getAudioQuality()) {
            "128k" -> b.chip128k.isChecked = true
            "192k" -> b.chip192k.isChecked = true
            "320k" -> b.chip320k.isChecked = true
        }
        b.cgAudioQuality.setOnCheckedStateChangeListener { _, checkedIds ->
            val q = when (checkedIds.firstOrNull()) {
                R.id.chip192k -> "192k"
                R.id.chip320k -> "320k"
                else -> "128k"
            }
            dlManager.setAudioQuality(q)
        }
    }

    private fun updateAdvancedUI(b: LayoutAdvancedBinding) {
        val vPath = dlManager.getVideoPath() ?: "預設 (Movies/ZyphraFlux)"
        val aPath = dlManager.getAudioPath() ?: "預設 (Music/ZyphraFlux)"
        b.tvVideoPath.text = "目前位置: $vPath"
        b.tvAudioPath.text = "目前位置: $aPath"
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val shared = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
            if (shared.isNotBlank()) {
                if (binding.toggleGroup.checkedButtonId != R.id.btnStandard) {
                    binding.toggleGroup.check(R.id.btnStandard)
                }
                standardBinding?.etUrl?.setText(shared.trim())
            }
        }
    }

    private fun startDownloadService() {
        val svc = Intent(this, DownloadForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(svc)
        else startService(svc)
    }

    private fun stopDownloadService() = stopService(Intent(this, DownloadForegroundService::class.java))

    private fun requestNeededPermissions() {
        val needed = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
                add(Manifest.permission.READ_MEDIA_VIDEO)
                add(Manifest.permission.READ_MEDIA_AUDIO)
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        if (needed.isNotEmpty()) ActivityCompat.requestPermissions(this, needed.toTypedArray(), 100)
    }

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
