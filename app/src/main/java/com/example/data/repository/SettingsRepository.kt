package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

enum class SortOrder {
    NEWEST_FIRST, OLDEST_FIRST
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

data class UserPreferences(
    val gridColumns: Int = 3,
    val retentionDays: Int = 30,
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    val showScreenshots: Boolean = true,
    val showDocuments: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val confirmBeforeDelete: Boolean = true
)

class SettingsRepository(private val context: Context) {

    companion object {
        val KEY_GRID_COLUMNS = intPreferencesKey("grid_columns")
        val KEY_RETENTION_DAYS = intPreferencesKey("retention_days")
        val KEY_SORT_ORDER = stringPreferencesKey("sort_order")
        val KEY_SHOW_SCREENSHOTS = booleanPreferencesKey("show_screenshots")
        val KEY_SHOW_DOCUMENTS = booleanPreferencesKey("show_documents")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_CONFIRM_BEFORE_DELETE = booleanPreferencesKey("confirm_before_delete")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            gridColumns = preferences[KEY_GRID_COLUMNS] ?: 3,
            retentionDays = preferences[KEY_RETENTION_DAYS] ?: 30,
            sortOrder = when (preferences[KEY_SORT_ORDER]) {
                SortOrder.OLDEST_FIRST.name -> SortOrder.OLDEST_FIRST
                else -> SortOrder.NEWEST_FIRST
            },
            showScreenshots = preferences[KEY_SHOW_SCREENSHOTS] ?: true,
            showDocuments = preferences[KEY_SHOW_DOCUMENTS] ?: true,
            themeMode = when (preferences[KEY_THEME_MODE]) {
                ThemeMode.LIGHT.name -> ThemeMode.LIGHT
                ThemeMode.DARK.name -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            },
            confirmBeforeDelete = preferences[KEY_CONFIRM_BEFORE_DELETE] ?: true
        )
    }

    suspend fun setGridColumns(columns: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_GRID_COLUMNS] = columns
        }
    }

    suspend fun setRetentionDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_RETENTION_DAYS] = days
        }
    }

    suspend fun setSortOrder(order: SortOrder) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SORT_ORDER] = order.name
        }
    }

    suspend fun setShowScreenshots(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SHOW_SCREENSHOTS] = show
        }
    }

    suspend fun setShowDocuments(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SHOW_DOCUMENTS] = show
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun setConfirmBeforeDelete(confirm: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CONFIRM_BEFORE_DELETE] = confirm
        }
    }
}
