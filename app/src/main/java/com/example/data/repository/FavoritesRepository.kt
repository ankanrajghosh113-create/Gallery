package com.example.data.repository

import com.example.data.local.FavoriteDao
import com.example.data.local.FavoriteEntity
import kotlinx.coroutines.flow.Flow

class FavoritesRepository(private val favoriteDao: FavoriteDao) {

    val favoriteIds: Flow<List<Long>> = favoriteDao.getFavoriteIdsFlow()

    suspend fun toggleFavorite(mediaId: Long, uriString: String, currentIsFavorite: Boolean) {
        if (currentIsFavorite) {
            favoriteDao.removeFavorite(mediaId)
        } else {
            favoriteDao.addFavorite(
                FavoriteEntity(
                    mediaId = mediaId,
                    uriString = uriString
                )
            )
        }
    }

    suspend fun isFavorite(mediaId: Long): Boolean {
        return favoriteDao.isFavorite(mediaId)
    }

    suspend fun removeFavorites(mediaIds: List<Long>) {
        favoriteDao.removeFavorites(mediaIds)
    }
}
