package com.example.data.model

import android.net.Uri

data class Album(
    val name: String,
    val coverUri: Uri,
    val itemCount: Int,
    val isVideoAlbum: Boolean = false
)
