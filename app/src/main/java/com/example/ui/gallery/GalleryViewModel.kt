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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class FilterType {
    ALL, PHOTOS_ONLY, VIDEOS_ONLY
}

enum class ViewMode {
    GRID_3, GRID_4, LIST
}

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val mediaRepo = MediaStoreRepository(application)
    private val favDao = GalleryDatabase.getDatabase(application).favoriteDao()
    private val favRepo = FavoritesRepository(favDao)

    private val _rawMediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val favoriteIds: StateFlow<List<Long>> = favRepo.favoriteIds.stateIn(
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

    private val _extraFilters = combine(_dateRange, _activeAlbum) { date, album -> Pair(date, album) }

    // Combined filtered media list
    val filteredMedia: StateFlow<List<MediaItem>> = combine(
        _rawMediaItems,
        favoriteIds,
        _searchQuery,
        _filterType,
        _extraFilters
    ) { items, favs, query, filter, extra ->
        val date = extra.first
        val album = extra.second
        items.map { item ->
            item.copy(isFavorite = favs.contains(item.id))
        }.filter { item ->
            // Album filter
            if (album != null && item.bucketName != album) return@filter false

            // Media Type filter
            when (filter) {
                FilterType.PHOTOS_ONLY -> if (item.isVideo) return@filter false
                FilterType.VIDEOS_ONLY -> if (!item.isVideo) return@filter false
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
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Favorites only media list
    val favoriteMedia: StateFlow<List<MediaItem>> = combine(
        _rawMediaItems,
        favoriteIds
    ) { items, favs ->
        items.filter { favs.contains(it.id) }.map { it.copy(isFavorite = true) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Albums list
    val albums: StateFlow<List<Album>> = _rawMediaItems.map { items ->
        mediaRepo.buildAlbums(items)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
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

        deleteItems(context, items, onPendingIntentSender)
        clearSelection()
    }

    fun deleteSingleItem(context: Context, item: MediaItem, onPendingIntentSender: ((IntentSender) -> Unit)? = null) {
        deleteItems(context, listOf(item), onPendingIntentSender)
    }

    private fun deleteItems(context: Context, items: List<MediaItem>, onPendingIntentSender: ((IntentSender) -> Unit)? = null) {
        val uris = items.map { it.uri }
        val ids = items.map { it.id }

        viewModelScope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
                    onPendingIntentSender?.invoke(pendingIntent.intentSender)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Fallback local remove for sample media
                    removeItemsLocally(ids)
                }
            } else {
                try {
                    uris.forEach { context.contentResolver.delete(it, null, null) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                removeItemsLocally(ids)
            }
        }
    }

    fun removeItemsLocally(ids: List<Long>) {
        _rawMediaItems.value = _rawMediaItems.value.filterNot { ids.contains(it.id) }
        viewModelScope.launch {
            favRepo.removeFavorites(ids)
        }
    }
}
