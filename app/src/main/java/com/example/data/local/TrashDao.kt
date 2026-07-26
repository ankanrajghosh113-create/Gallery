package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashDao {
    @Query("SELECT * FROM trashed_media ORDER BY trashedTimestampMs DESC")
    fun getAllTrashed(): Flow<List<TrashEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrash(trashEntity: TrashEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(trashEntities: List<TrashEntity>)

    @Query("DELETE FROM trashed_media WHERE mediaId = :mediaId")
    suspend fun deleteTrash(mediaId: Long)

    @Query("DELETE FROM trashed_media WHERE mediaId IN (:mediaIds)")
    suspend fun deleteTrashBatch(mediaIds: List<Long>)

    @Query("DELETE FROM trashed_media")
    suspend fun clearAll()

    @Query("DELETE FROM trashed_media WHERE trashedTimestampMs < :cutoffMs")
    suspend fun purgeExpired(cutoffMs: Long)
}
