package com.example.zzubvideoplayer


import android.net.Uri

data class MediaFile(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val duration: Long
)
