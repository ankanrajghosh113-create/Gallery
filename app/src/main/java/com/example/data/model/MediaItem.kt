package com.example.data.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateAdded: Long, // timestamp in seconds or ms
    val bucketName: String, // Album / folder name
    val isVideo: Boolean,
    val durationMs: Long? = null,
    val isFavorite: Boolean = false,
    val sizeBytes: Long = 0
)
