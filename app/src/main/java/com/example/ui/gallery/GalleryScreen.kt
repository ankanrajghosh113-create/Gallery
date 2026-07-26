package com.example.ui.gallery

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MediaItem
import com.example.ui.albums.AlbumsScreen
import com.example.ui.components.FilterDialog
import com.example.ui.components.GallerySearchBar
import com.example.ui.components.MediaGrid
import com.example.ui.components.PermissionBanner
import com.example.ui.favorites.FavoritesScreen
import com.example.ui.viewer.PhotoViewerScreen

enum class GalleryTab {
    PHOTOS, ALBUMS, FAVORITES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val mediaList by viewModel.filteredMedia.collectAsStateWithLifecycle()
    val favoriteList by viewModel.favoriteMedia.collectAsStateWithLifecycle()
    val albumsList by viewModel.albums.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    val dateRange by viewModel.dateRange.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val activeAlbum by viewModel.activeAlbum.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedMediaIds.collectAsStateWithLifecycle()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(GalleryTab.PHOTOS) }
    var viewerStartIndex by remember { mutableStateOf<Int?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }

    var isSearchBarVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -12f) {
                    isSearchBarVisible = false
                } else if (available.y > 12f) {
                    isSearchBarVisible = true
                }
                return Offset.Zero
            }
        }
    }

    // Intent launcher for System Delete confirmation dialog (Android 11+)
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.loadMedia()
        }
    }

    if (viewerStartIndex != null) {
        val viewerItems = if (selectedTab == GalleryTab.FAVORITES) favoriteList else mediaList
        PhotoViewerScreen(
            items = viewerItems,
            startIndex = viewerStartIndex!!,
            onBack = { viewerStartIndex = null },
            onFavoriteToggle = { viewModel.toggleFavorite(it) },
            onShare = { ctx, item -> viewModel.shareSingle(ctx, item) },
            onDelete = { ctx, item ->
                viewModel.deleteSingleItem(ctx, item) { intentSender ->
                    deleteLauncher.launch(
                        IntentSenderRequest.Builder(intentSender).build()
                    )
                }
            }
        )
        return
    }

    Scaffold(
        topBar = {
            Column {
                if (isMultiSelectMode) {
                    MultiSelectTopAppBar(
                        selectedCount = selectedIds.size,
                        onClearSelection = { viewModel.clearSelection() },
                        onSelectAll = { viewModel.selectAll() },
                        onShareSelected = { viewModel.shareSelected(context) },
                        onFavoriteSelected = { viewModel.favoriteSelected() },
                        onDeleteSelected = {
                            viewModel.deleteSelectedItems(context) { intentSender ->
                                deleteLauncher.launch(
                                    IntentSenderRequest.Builder(intentSender).build()
                                )
                            }
                        }
                    )
                } else {
                    AnimatedVisibility(
                        visible = isSearchBarVisible,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            GallerySearchBar(
                                query = searchQuery,
                                onQueryChange = { viewModel.setSearchQuery(it) },
                                filterType = filterType,
                                onFilterTypeChange = { viewModel.setFilterType(it) },
                                viewMode = viewMode,
                                onViewModeChange = { viewModel.setViewMode(it) },
                                hasDateFilter = dateRange != null,
                                onOpenFilterDialog = { showFilterDialog = true }
                            )

                            // Active Album filter / Navigation Back banner
                            if (activeAlbum != null || selectedTab != GalleryTab.PHOTOS) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    selectedTab = GalleryTab.PHOTOS
                                                    viewModel.setActiveAlbum(null)
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowBack,
                                                    contentDescription = "Back to All Photos",
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                            Text(
                                                text = when {
                                                    activeAlbum != null -> "Album: $activeAlbum"
                                                    selectedTab == GalleryTab.ALBUMS -> "All Albums"
                                                    selectedTab == GalleryTab.FAVORITES -> "Favorites"
                                                    else -> "All Photos"
                                                },
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                        TextButton(
                                            onClick = {
                                                selectedTab = GalleryTab.PHOTOS
                                                viewModel.setActiveAlbum(null)
                                                viewModel.setFilterType(FilterType.ALL)
                                            }
                                        ) {
                                            Text("Back to All Photos")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = selectedTab == GalleryTab.PHOTOS,
                    onClick = {
                        selectedTab = GalleryTab.PHOTOS
                        viewModel.setActiveAlbum(null)
                        viewModel.clearSelection()
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == GalleryTab.PHOTOS) Icons.Filled.Photo else Icons.Outlined.Photo,
                            contentDescription = "Photos"
                        )
                    },
                    label = { Text("Photos") }
                )

                NavigationBarItem(
                    selected = selectedTab == GalleryTab.ALBUMS,
                    onClick = {
                        selectedTab = GalleryTab.ALBUMS
                        viewModel.clearSelection()
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == GalleryTab.ALBUMS) Icons.Filled.Folder else Icons.Outlined.Folder,
                            contentDescription = "Albums"
                        )
                    },
                    label = { Text("Albums") }
                )

                NavigationBarItem(
                    selected = selectedTab == GalleryTab.FAVORITES,
                    onClick = {
                        selectedTab = GalleryTab.FAVORITES
                        viewModel.clearSelection()
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == GalleryTab.FAVORITES) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorites"
                        )
                    },
                    label = { Text("Favorites") }
                )
            }
        },
        modifier = modifier.nestedScroll(nestedScrollConnection)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                PermissionBanner(
                    onPermissionGranted = { viewModel.loadMedia() }
                )

                AnimatedContent(
                    targetState = selectedTab,
                    label = "TabTransition",
                    modifier = Modifier.weight(1f)
                ) { tab ->
                    when (tab) {
                        GalleryTab.PHOTOS -> {
                            MediaGrid(
                                items = mediaList,
                                viewMode = viewMode,
                                selectedIds = selectedIds,
                                isMultiSelectMode = isMultiSelectMode,
                                onItemClick = { item, index ->
                                    if (isMultiSelectMode) {
                                        viewModel.toggleSelection(item.id)
                                    } else {
                                        viewerStartIndex = index
                                    }
                                },
                                onItemLongClick = { item ->
                                    viewModel.toggleSelection(item.id)
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(it) }
                            )
                        }
                        GalleryTab.ALBUMS -> {
                            AlbumsScreen(
                                albums = albumsList,
                                onAlbumClick = { album ->
                                    viewModel.setActiveAlbum(album.name)
                                    selectedTab = GalleryTab.PHOTOS
                                }
                            )
                        }
                        GalleryTab.FAVORITES -> {
                            FavoritesScreen(
                                favoriteItems = favoriteList,
                                viewMode = viewMode,
                                selectedIds = selectedIds,
                                isMultiSelectMode = isMultiSelectMode,
                                onItemClick = { item, index ->
                                    if (isMultiSelectMode) {
                                        viewModel.toggleSelection(item.id)
                                    } else {
                                        viewerStartIndex = index
                                    }
                                },
                                onItemLongClick = { item ->
                                    viewModel.toggleSelection(item.id)
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(it) }
                            )
                        }
                    }
                }
            }
        }

        if (showFilterDialog) {
            FilterDialog(
                currentDateRange = dateRange,
                onApplyDateRange = { from, to -> viewModel.setDateRange(from, to) },
                onDismiss = { showFilterDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectTopAppBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onShareSelected: () -> Unit,
    onFavoriteSelected: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "$selectedCount Selected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close Selection")
            }
        },
        actions = {
            TextButton(onClick = onSelectAll) {
                Text("Select All")
            }
            IconButton(onClick = onShareSelected) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Share Selected")
            }
            IconButton(onClick = onFavoriteSelected) {
                Icon(imageVector = Icons.Default.Favorite, contentDescription = "Favorite Selected")
            }
            IconButton(onClick = onDeleteSelected) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Selected")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}
