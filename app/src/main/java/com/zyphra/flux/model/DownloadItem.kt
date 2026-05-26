package com.zyphra.flux.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class DownloadStatus { PENDING, DOWNLOADING, COMPLETED, FAILED, CANCELLED }

@Parcelize
data class DownloadItem(
    val id: String = System.currentTimeMillis().toString(),
    val url: String,
    val platform: String,
    val title: String = "",
    val filePath: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: DownloadStatus = DownloadStatus.PENDING
) : Parcelable
