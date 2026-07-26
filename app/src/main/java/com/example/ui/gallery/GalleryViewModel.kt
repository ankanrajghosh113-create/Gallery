package com.example.ui.gallery

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.GalleryDatabase
import com.example.data.model.Album
import com.example.data.model.MediaItem
import com.example.data.repository.FavoritesRepository
import com.example.data.repository.MediaStoreRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SortOrder
import com.example.data.repository.TrashRepository
import com.example.data.repository.TrashedMediaItem
import com.example.data.repository.UserPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class FilterType {
    ALL, PHOTOS_ONLY, VIDEOS_ONLY, SCREENSHOTS, DOCUMENTS
}

enum class ViewMode {
    GRID_3, GRID_4, LIST
}

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = GalleryDatabase.getDatabase(application)
    private val mediaRepo = MediaStoreRepository(application)
    private val favDao = db.favoriteDao()
    private val favRepo = FavoritesRepository(favDao)
    private val trashDao = db.trashDao()
    private val trashRepo = TrashRepository(trashDao)
    private val settingsRepo = SettingsRepository(application)

    val userPreferences: StateFlow<UserPreferences> = settingsRepo.userPreferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserPreferences()
    )

    private val _rawMediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val favoriteIds: StateFlow<List<Long>> = favRepo.favoriteIds.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val trashedMedia: StateFlow<List<TrashedMediaItem>> = userPreferences.flatMapLatest { prefs ->
        trashRepo.getTrashedItemsFlow(prefs.retentionDays)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow(FilterType.ALL)
    val filterType: StateFlow<FilterType> = _filterType.asStateFlow()

    private val _dateRange = MutableStateFlow<Pair<Long?, Long?>?>(null)
    val dateRange: StateFlow<Pair<Long?, Long?>?> = _dateRange.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.GRID_3)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _activeAlbum = MutableStateFlow<String?>(null)
    val activeAlbum: StateFlow<String?> = _activeAlbum.asStateFlow()

    private val _selectedMediaIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedMediaIds: StateFlow<Set<Long>> = _selectedMediaIds.asStateFlow()

    val isMultiSelectMode: StateFlow<Boolean> = _selectedMediaIds.map { it.isNotEmpty() }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private data class SearchAndFilterOptions(
        val query: String,
        val filter: FilterType,
        val dateRange: Pair<Long?, Long?>?,
        val album: String?,
        val prefs: UserPreferences
    )

    private val filterOptions: Flow<SearchAndFilterOptions> = combine(
        _searchQuery,
        _filterType,
        _dateRange,
        _activeAlbum,
        userPreferences
    ) { query, filter, date, album, prefs ->
        SearchAndFilterOptions(query, filter, date, album, prefs)
    }

    // Active media items excluding items in trash
    private val activeRawMedia: StateFlow<List<MediaItem>> = combine(
        _rawMediaItems,
        trashedMedia
    ) { items, trashed ->
        val trashedIds = trashed.map { it.mediaItem.id }.toSet()
        items.filterNot { trashedIds.contains(it.id) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Combined filtered media list
    val filteredMedia: StateFlow<List<MediaItem>> = combine(
        activeRawMedia,
        favoriteIds,
        filterOptions
    ) { items, favs, opts ->
        val query = opts.query
        val filter = opts.filter
        val date = opts.dateRange
        val album = opts.album
        val prefs = opts.prefs

        val list = items.map { item ->
            item.copy(isFavorite = favs.contains(item.id))
        }.filter { item ->
            // Album filter
            if (album != null && item.bucketName != album) return@filter false

            val isScreenshot = item.bucketName.contains("screenshot", ignoreCase = true) || item.displayName.contains("screenshot", ignoreCase = true)
            val isDoc = item.bucketName.contains("document", ignoreCase = true) || item.displayName.contains("doc", ignoreCase = true) || item.displayName.contains("pdf", ignoreCase = true) || item.displayName.contains("receipt", ignoreCase = true) || item.displayName.contains("scan", ignoreCase = true)

            // Hide screenshots or documents if toggled off in settings (when viewing "ALL")
            if (filter == FilterType.ALL) {
                if (!prefs.showScreenshots && isScreenshot) return@filter false
                if (!prefs.showDocuments && isDoc) return@filter false
            }

            // Media Type / Smart filter
            when (filter) {
                FilterType.PHOTOS_ONLY -> if (item.isVideo) return@filter false
                FilterType.VIDEOS_ONLY -> if (!item.isVideo) return@filter false
                FilterType.SCREENSHOTS -> if (!isScreenshot) return@filter false
                FilterType.DOCUMENTS -> if (!isDoc) return@filter false
                FilterType.ALL -> {}
            }

            // Search query filter
            if (query.isNotBlank()) {
                val matchesName = item.displayName.contains(query, ignoreCase = true)
                val matchesAlbum = item.bucketName.contains(query, ignoreCase = true)
                if (!matchesName && !matchesAlbum) return@filter false
            }

            // Date range filter
            if (date != null) {
                val (from, to) = date
                if (from != null && item.dateAdded < from) return@filter false
                if (to != null && item.dateAdded > to) return@filter false
            }

            true
        }

        if (prefs.sortOrder == SortOrder.OLDEST_FIRST) {
            list.sortedBy { it.dateAdded }
        } else {
            list.sortedByDescending { it.dateAdded }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Favorites only media list
    val favoriteMedia: StateFlow<List<MediaItem>> = combine(
        activeRawMedia,
        favoriteIds
    ) { items, favs ->
        items.filter { favs.contains(it.id) }.map { it.copy(isFavorite = true) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Albums list
    val albums: StateFlow<List<Album>> = activeRawMedia.map { items ->
        mediaRepo.buildAlbums(items)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            userPreferences.collect { prefs ->
                trashRepo.purgeExpiredItems(prefs.retentionDays)
                _viewMode.value = when (prefs.gridColumns) {
                    4 -> ViewMode.GRID_4
                    2 -> ViewMode.LIST
                    else -> ViewMode.GRID_3
                }
            }
        }
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            _isLoading.value = true
            _rawMediaItems.value = mediaRepo.loadAllMedia()
            _isLoading.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(type: FilterType) {
        _filterType.value = type
    }

    fun setDateRange(from: Long?, to: Long?) {
        if (from == null && to == null) {
            _dateRange.value = null
        } else {
            _dateRange.value = Pair(from, to)
        }
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
        val cols = when (mode) {
            ViewMode.GRID_3 -> 3
            ViewMode.GRID_4 -> 4
            ViewMode.LIST -> 2
        }
        viewModelScope.launch { settingsRepo.setGridColumns(cols) }
    }

    fun setActiveAlbum(albumName: String?) {
        _activeAlbum.value = albumName
    }

    fun toggleSelection(mediaId: Long) {
        val current = _selectedMediaIds.value.toMutableSet()
        if (current.contains(mediaId)) {
            current.remove(mediaId)
        } else {
            current.add(mediaId)
        }
        _selectedMediaIds.value = current
    }

    fun selectAll() {
        val allIds = filteredMedia.value.map { it.id }.toSet()
        _selectedMediaIds.value = allIds
    }

    fun clearSelection() {
        _selectedMediaIds.value = emptySet()
    }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            favRepo.toggleFavorite(item.id, item.uri.toString(), item.isFavorite)
        }
    }

    fun favoriteSelected() {
        val selected = selectedMediaIds.value
        val items = filteredMedia.value.filter { selected.contains(it.id) }
        viewModelScope.launch {
            items.forEach { item ->
                if (!item.isFavorite) {
                    favRepo.toggleFavorite(item.id, item.uri.toString(), false)
                }
            }
            clearSelection()
        }
    }

    fun shareSelected(context: Context) {
        val selectedIds = selectedMediaIds.value
        val items = filteredMedia.value.filter { selectedIds.contains(it.id) }
        if (items.isEmpty()) return

        val uris = ArrayList(items.map { it.uri })
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share ${uris.size} items via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
        clearSelection()
    }

    fun shareSingle(context: Context, item: MediaItem) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (item.isVideo) "video/*" else "image/*"
            putExtra(Intent.EXTRA_STREAM, item.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun deleteSelectedItems(context: Context, onPendingIntentSender: ((IntentSender) -> Unit)? = null) {
        val selectedIds = selectedMediaIds.value
        val items = filteredMedia.value.filter { selectedIds.contains(it.id) }
        if (items.isEmpty()) return

        moveToTrash(context, items)
        clearSelection()
    }

    fun deleteSingleItem(context: Context, item: MediaItem, onPendingIntentSender: ((IntentSender) -> Unit)? = null) {
        moveToTrash(context, listOf(item))
    }

    private fun moveToTrash(context: Context, items: List<MediaItem>) {
        viewModelScope.launch {
            trashRepo.moveToTrash(items)
            Toast.makeText(context, "Moved ${items.size} item(s) to Recycle Bin", Toast.LENGTH_SHORT).show()
        }
    }

    fun restoreFromTrash(context: Context, item: MediaItem) {
        viewModelScope.launch {
            trashRepo.restoreItem(item.id)
            Toast.makeText(context, "Restored '${item.displayName}' to gallery", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteForeverFromTrash(context: Context, item: MediaItem, onPendingIntentSender: ((IntentSender) -> Unit)? = null) {
        val uris = listOf(item.uri)
        val ids = listOf(item.id)

        viewModelScope.launch {
            trashRepo.deletePermanently(item.id)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
                    onPendingIntentSender?.invoke(pendingIntent.intentSender)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                try {
                    uris.forEach { context.contentResolver.delete(it, null, null) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            removeItemsLocally(ids)
            Toast.makeText(context, "Permanently deleted", Toast.LENGTH_SHORT).show()
        }
    }

    fun emptyRecycleBin(context: Context) {
        val currentTrashed = trashedMedia.value.map { it.mediaItem }
        if (currentTrashed.isEmpty()) return

        val ids = currentTrashed.map { it.id }
        viewModelScope.launch {
            trashRepo.emptyTrash()
            removeItemsLocally(ids)
            Toast.makeText(context, "Recycle Bin emptied", Toast.LENGTH_SHORT).show()
        }
    }

    fun removeItemsLocally(ids: List<Long>) {
        _rawMediaItems.value = _rawMediaItems.value.filterNot { ids.contains(it.id) }
        viewModelScope.launch {
            favRepo.removeFavorites(ids)
        }
    }
}
