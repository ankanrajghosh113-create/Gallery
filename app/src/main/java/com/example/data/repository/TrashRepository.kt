package com.example.data.repository

import android.net.Uri
import com.example.data.local.TrashDao
import com.example.data.local.TrashEntity
import com.example.data.model.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class TrashedMediaItem(
    val mediaItem: MediaItem,
    val trashedTimestampMs: Long,
    val maxRetentionDays: Int = 30
) {
    val daysRemaining: Int
        get() {
            val elapsedMs = System.currentTimeMillis() - trashedTimestampMs
            val elapsedDays = (elapsedMs / (1000 * 60 * 60 * 24)).toInt()
            return (maxRetentionDays - elapsedDays).coerceAtLeast(0)
        }
}

class TrashRepository(private val trashDao: TrashDao) {

    fun getTrashedItemsFlow(retentionDays: Int = 30): Flow<List<TrashedMediaItem>> = trashDao.getAllTrashed().map { entities ->
        entities.map { entity ->
            TrashedMediaItem(
                mediaItem = MediaItem(
                    id = entity.mediaId,
                    uri = Uri.parse(entity.uriString),
                    displayName = entity.displayName,
                    dateAdded = entity.dateAdded,
                    bucketName = entity.bucketName,
                    isVideo = entity.isVideo,
                    durationMs = entity.durationMs,
                    sizeBytes = entity.sizeBytes
                ),
                trashedTimestampMs = entity.trashedTimestampMs,
                maxRetentionDays = retentionDays
            )
        }
    }

    val trashedItems: Flow<List<TrashedMediaItem>> = getTrashedItemsFlow(30)

    suspend fun moveToTrash(items: List<MediaItem>) {
        val now = System.currentTimeMillis()
        val entities = items.map { item ->
            TrashEntity(
                mediaId = item.id,
                uriString = item.uri.toString(),
                displayName = item.displayName,
                dateAdded = item.dateAdded,
                bucketName = item.bucketName,
                isVideo = item.isVideo,
                durationMs = item.durationMs,
                sizeBytes = item.sizeBytes,
                trashedTimestampMs = now
            )
        }
        trashDao.insertAll(entities)
    }

    suspend fun restoreItem(mediaId: Long) {
        trashDao.deleteTrash(mediaId)
    }

    suspend fun restoreBatch(mediaIds: List<Long>) {
        trashDao.deleteTrashBatch(mediaIds)
    }

    suspend fun deletePermanently(mediaId: Long) {
        trashDao.deleteTrash(mediaId)
    }

    suspend fun deletePermanentlyBatch(mediaIds: List<Long>) {
        trashDao.deleteTrashBatch(mediaIds)
    }

    suspend fun emptyTrash() {
        trashDao.clearAll()
    }

    suspend fun purgeExpiredItems(retentionDays: Int = 30) {
        val cutoffMs = System.currentTimeMillis() - (retentionDays.toLong() * 24 * 60 * 60 * 1000L)
        trashDao.purgeExpired(cutoffMs)
    }
}
