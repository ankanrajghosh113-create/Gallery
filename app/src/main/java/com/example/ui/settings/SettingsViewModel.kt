package com.example.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.Coil
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SortOrder
import com.example.data.repository.ThemeMode
import com.example.data.repository.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)

    val userPreferences: StateFlow<UserPreferences> = settingsRepo.userPreferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserPreferences()
    )

    fun setGridColumns(columns: Int) {
        viewModelScope.launch { settingsRepo.setGridColumns(columns) }
    }

    fun setRetentionDays(days: Int) {
        viewModelScope.launch { settingsRepo.setRetentionDays(days) }
    }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch { settingsRepo.setSortOrder(order) }
    }

    fun setShowScreenshots(show: Boolean) {
        viewModelScope.launch { settingsRepo.setShowScreenshots(show) }
    }

    fun setShowDocuments(show: Boolean) {
        viewModelScope.launch { settingsRepo.setShowDocuments(show) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepo.setThemeMode(mode) }
    }

    fun setConfirmBeforeDelete(confirm: Boolean) {
        viewModelScope.launch { settingsRepo.setConfirmBeforeDelete(confirm) }
    }

    fun clearThumbnailCache(context: Context, onCleared: () -> Unit) {
        viewModelScope.launch {
            try {
                val imageLoader = Coil.imageLoader(context)
                imageLoader.diskCache?.clear()
                imageLoader.memoryCache?.clear()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            onCleared()
        }
    }
}
