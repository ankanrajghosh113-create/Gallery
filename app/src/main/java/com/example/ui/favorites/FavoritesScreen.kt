package com.example.ui.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.MediaItem
import com.example.ui.components.MediaGrid
import com.example.ui.gallery.ViewMode

@Composable
fun FavoritesScreen(
    favoriteItems: List<MediaItem>,
    viewMode: ViewMode,
    selectedIds: Set<Long>,
    isMultiSelectMode: Boolean,
    onItemClick: (MediaItem, Int) -> Unit,
    onItemLongClick: (MediaItem) -> Unit,
    onFavoriteToggle: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (favoriteItems.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFEF4444).copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Favorites Yet",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap the heart icon on any photo or video to add it to your favorites.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        MediaGrid(
            items = favoriteItems,
            viewMode = viewMode,
            selectedIds = selectedIds,
            isMultiSelectMode = isMultiSelectMode,
            onItemClick = onItemClick,
            onItemLongClick = onItemLongClick,
            onFavoriteToggle = onFavoriteToggle,
            modifier = modifier
        )
    }
}
