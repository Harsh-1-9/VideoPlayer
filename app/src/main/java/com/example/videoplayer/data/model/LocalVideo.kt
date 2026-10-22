package com.example.videoplayer.data.model

import android.net.Uri

data class LocalVideo(
    val id:Long,
    val title: String,
    val uri:Uri,
    val duration:Long,
    val size: Long,
    val dateAdded: Long,
    val mimeType: String?
)