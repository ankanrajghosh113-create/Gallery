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
        val groups = items.groupBy { item ->
            val lowerBucket = item.bucketName.lowercase()
            val lowerName = item.displayName.lowercase()
            when {
                lowerBucket.contains("screenshot") || lowerName.contains("screenshot") -> "Screenshots"
                lowerBucket.contains("document") || lowerName.contains("doc") || lowerName.contains("pdf") || lowerName.contains("receipt") || lowerName.contains("scan") -> "Documents"
                else -> item.bucketName
            }
        }

        return groups.map { (name, group) ->
            Album(
                name = name,
                coverUri = group.first().uri,
                itemCount = group.size,
                isVideoAlbum = group.all { it.isVideo }
            )
        }.sortedByDescending { it.itemCount }
    }

    private fun getSampleMedia(): List<MediaItem> {
        val now = System.currentTimeMillis() / 1000
        val dayInSec = 86400L

        // Group 1: Today (9 items -> 3x3 uniform grid)
        val todayItems = listOf(
            Triple("https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800", "Mountain Sunrise.jpg", "Travel"),
            Triple("https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800", "Screenshot_2026-07-25_Chat.png", "Screenshots"),
            Triple("https://images.unsplash.com/photo-1511765224389-37f0e77cf0eb?w=800", "Architectural Modernism.jpg", "Architecture"),
            Triple("https://images.unsplash.com/photo-1554415707-6e8cfc93fe23?w=800", "Tax_Receipt_Doc_Scan.jpg", "Documents"),
            Triple("https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=800", "Foggy Forest Trail.jpg", "Nature"),
            Triple("https://images.unsplash.com/photo-1568602471122-7832951cc4c5?w=800", "Screenshot_2026-07-25_Dashboard.png", "Screenshots"),
            Triple("https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?w=800", "Autumn River Valley.jpg", "Nature"),
            Triple("https://images.unsplash.com/photo-1426604966848-d7adac402bff?w=800", "Alpine Lake Video.mp4", "Travel"),
            Triple("https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=800", "Coding Workspace setup.jpg", "Work")
        )

        // Group 2: Yesterday (6 items -> 3x2 uniform grid)
        val yesterdayItems = listOf(
            Triple("https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800", "Cosmic Night Sky.jpg", "Astronomy"),
            Triple("https://images.unsplash.com/photo-1517841905240-472988babdf9?w=800", "Portrait In Golden Hour.jpg", "Portraits"),
            Triple("https://images.unsplash.com/photo-1533450718592-29d45635f0a9?w=800", "African Safari Lion.jpg", "Wildlife"),
            Triple("https://images.unsplash.com/photo-1519681393784-d120267933ba?w=800", "Starry Snowy Mountains.jpg", "Travel"),
            Triple("https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800", "Tropical Beach Sunset.jpg", "Vacation"),
            Triple("https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=800", "Screenshot_2026-07-24_Order.png", "Screenshots")
        )

        // Group 3: Earlier Date 1 (6 items -> 3x2 uniform grid)
        val earlierDate1 = listOf(
            Triple("https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=800", "Lake View Sunset.jpg", "Travel"),
            Triple("https://images.unsplash.com/photo-1472214103451-9374bd1c798e?w=800", "Green Valley Hills.jpg", "Nature"),
            Triple("https://images.unsplash.com/photo-1518791841217-8f162f1e1131?w=800", "Cute Cat Portrait.jpg", "Pets"),
            Triple("https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=800", "City Lights Video.mp4", "City"),
            Triple("https://images.unsplash.com/photo-1493246507139-91e8fad9978e?w=800", "Mountain Pass Drive.jpg", "Travel"),
            Triple("https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800", "Invoice_Document.pdf", "Documents")
        )

        // Group 4: Earlier Date 2 (3 items -> 3x1 uniform grid)
        val earlierDate2 = listOf(
            Triple("https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800", "Ocean Waves Shore.jpg", "Vacation"),
            Triple("https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800", "Sunset Peak Hike.jpg", "Nature"),
            Triple("https://images.unsplash.com/photo-1511765224389-37f0e77cf0eb?w=800", "Downtown Skyline.jpg", "Architecture")
        )

        val list = mutableListOf<MediaItem>()
        var globalId = 1000L

        todayItems.forEachIndexed { index, triple ->
            list.add(
                MediaItem(
                    id = globalId++,
                    uri = Uri.parse(triple.first),
                    displayName = triple.second,
                    dateAdded = now - (index * 300L), // All today
                    bucketName = triple.third,
                    isVideo = triple.second.endsWith(".mp4"),
                    durationMs = if (triple.second.endsWith(".mp4")) 24000L else null,
                    sizeBytes = 2_800_000L
                )
            )
        }

        yesterdayItems.forEachIndexed { index, triple ->
            list.add(
                MediaItem(
                    id = globalId++,
                    uri = Uri.parse(triple.first),
                    displayName = triple.second,
                    dateAdded = (now - dayInSec) - (index * 300L), // All yesterday
                    bucketName = triple.third,
                    isVideo = triple.second.endsWith(".mp4"),
                    durationMs = if (triple.second.endsWith(".mp4")) 18000L else null,
                    sizeBytes = 3_100_000L
                )
            )
        }

        earlierDate1.forEachIndexed { index, triple ->
            list.add(
                MediaItem(
                    id = globalId++,
                    uri = Uri.parse(triple.first),
                    displayName = triple.second,
                    dateAdded = (now - 3 * dayInSec) - (index * 300L), // 3 days ago
                    bucketName = triple.third,
                    isVideo = triple.second.endsWith(".mp4"),
                    durationMs = if (triple.second.endsWith(".mp4")) 42000L else null,
                    sizeBytes = 4_200_000L
                )
            )
        }

        earlierDate2.forEachIndexed { index, triple ->
            list.add(
                MediaItem(
                    id = globalId++,
                    uri = Uri.parse(triple.first),
                    displayName = triple.second,
                    dateAdded = (now - 7 * dayInSec) - (index * 300L), // 7 days ago
                    bucketName = triple.third,
                    isVideo = triple.second.endsWith(".mp4"),
                    durationMs = if (triple.second.endsWith(".mp4")) 12000L else null,
                    sizeBytes = 2_100_000L
                )
            )
        }

        return list
    }
}
