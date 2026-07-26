package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.data.model.Album
import com.example.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreRepository(private val context: Context) {

    suspend fun loadAllMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Video.VideoColumns.DURATION,
            MediaStore.Files.FileColumns.SIZE
        )

        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR " +
                "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"

        val args = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collection, projection, selection, args, sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                val bucketCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
                val typeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val durationCol = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)
                val sizeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val isVideo = cursor.getInt(typeCol) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                    val baseUri = if (isVideo) {
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else {
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }

                    val uri = ContentUris.withAppendedId(baseUri, id)
                    val displayName = cursor.getString(nameCol) ?: "Media_$id"
                    val dateAdded = cursor.getLong(dateCol)
                    val bucketName = if (bucketCol >= 0) cursor.getString(bucketCol) ?: "Camera" else "Camera"
                    val durationMs = if (durationCol >= 0 && !cursor.isNull(durationCol)) cursor.getLong(durationCol) else null
                    val sizeBytes = if (sizeCol >= 0 && !cursor.isNull(sizeCol)) cursor.getLong(sizeCol) else 0L

                    items.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            displayName = displayName,
                            dateAdded = dateAdded,
                            bucketName = bucketName,
                            isVideo = isVideo,
                            durationMs = durationMs,
                            sizeBytes = sizeBytes
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Return real media if found, otherwise return sample media for initial demo
        if (items.isEmpty()) {
            getSampleMedia()
        } else {
            items
        }
    }

    fun buildAlbums(items: List<MediaItem>): List<Album> {
        return items.groupBy { it.bucketName }
            .map { (name, group) ->
                Album(
                    name = name,
                    coverUri = group.first().uri,
                    itemCount = group.size,
                    isVideoAlbum = group.all { it.isVideo }
                )
            }
            .sortedByDescending { it.itemCount }
    }

    private fun getSampleMedia(): List<MediaItem> {
        val now = System.currentTimeMillis() / 1000
        val dayInSec = 86400L

        val sampleImages = listOf(
            Triple("https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800", "Mountain Sunrise.jpg", "Travel"),
            Triple("https://images.unsplash.com/photo-1511765224389-37f0e77cf0eb?w=800", "Architectural Modernism.jpg", "Architecture"),
            Triple("https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=800", "Foggy Forest Trail.jpg", "Nature"),
            Triple("https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?w=800", "Autumn River Valley.jpg", "Nature"),
            Triple("https://images.unsplash.com/photo-1426604966848-d7adac402bff?w=800", "Alpine Lake Reflection.jpg", "Travel"),
            Triple("https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=800", "Coding Workspace setup.jpg", "Work"),
            Triple("https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800", "Cosmic Night Sky.jpg", "Astronomy"),
            Triple("https://images.unsplash.com/photo-1517841905240-472988babdf9?w=800", "Portrait In Golden Hour.jpg", "Portraits"),
            Triple("https://images.unsplash.com/photo-1533450718592-29d45635f0a9?w=800", "African Safari Lion.jpg", "Wildlife"),
            Triple("https://images.unsplash.com/photo-1519681393784-d120267933ba?w=800", "Starry Snowy Mountains.jpg", "Travel"),
            Triple("https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800", "Tropical Beach Sunset.jpg", "Vacation"),
            Triple("https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=800", "Mountain Ridge Trail.jpg", "Travel")
        )

        val list = mutableListOf<MediaItem>()

        sampleImages.forEachIndexed { index, triple ->
            list.add(
                MediaItem(
                    id = 1000L + index,
                    uri = Uri.parse(triple.first),
                    displayName = triple.second,
                    dateAdded = now - (index * dayInSec / 2),
                    bucketName = triple.third,
                    isVideo = index % 4 == 3, // mark some as video
                    durationMs = if (index % 4 == 3) (15000L + index * 4200L) else null,
                    sizeBytes = (2_500_000L + index * 340_000L)
                )
            )
        }
        return list
    }
}
