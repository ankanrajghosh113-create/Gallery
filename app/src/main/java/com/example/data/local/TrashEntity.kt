package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trashed_media")
data class TrashEntity(
    @PrimaryKey val mediaId: Long,
    val uriString: String,
    val displayName: String,
    val dateAdded: Long,
    val bucketName: String,
    val isVideo: Boolean,
    val durationMs: Long?,
    val sizeBytes: Long,
    val trashedTimestampMs: Long
)
