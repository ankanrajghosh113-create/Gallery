package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.MediaItem
import com.example.ui.gallery.ViewMode
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MediaGrid(
    items: List<MediaItem>,
    viewMode: ViewMode,
    selectedIds: Set<Long>,
    isMultiSelectMode: Boolean,
    onItemClick: (MediaItem, Int) -> Unit,
    onItemLongClick: (MediaItem) -> Unit,
    onFavoriteToggle: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ImageSearch,
                    contentDescription = "No items",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Photos or Videos Found",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Try adjusting your search query or filter settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val groupedItems = remember(items) {
        items.groupBy { formatDateHeader(it.dateAdded) }
    }

    when (viewMode) {
        ViewMode.GRID_3, ViewMode.GRID_4 -> {
            val columns = if (viewMode == ViewMode.GRID_3) 3 else 4
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(bottom = 16.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = modifier.fillMaxSize()
            ) {
                groupedItems.forEach { (dateHeader, itemList) ->
                    item(
                        span = { GridItemSpan(columns) },
                        key = "header_$dateHeader"
                    ) {
                        DateHeader(title = dateHeader, count = itemList.size)
                    }

                    items(
                        items = itemList,
                        key = { it.id }
                    ) { item ->
                        val index = items.indexOf(item)
                        MediaListItem(
                            item = item,
                            isSelected = selectedIds.contains(item.id),
                            isMultiSelectMode = isMultiSelectMode,
                            onClick = { onItemClick(item, index) },
                            onLongClick = { onItemLongClick(item) },
                            onFavoriteToggle = { onFavoriteToggle(item) }
                        )
                    }
                }
            }
        }
        ViewMode.LIST -> {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = modifier.fillMaxSize()
            ) {
                groupedItems.forEach { (dateHeader, itemList) ->
                    item(key = "header_$dateHeader") {
                        DateHeader(title = dateHeader, count = itemList.size)
                    }
                    items(
                        items = itemList,
                        key = { it.id }
                    ) { item ->
                        val index = items.indexOf(item)
                        MediaListRowItem(
                            item = item,
                            isSelected = selectedIds.contains(item.id),
                            isMultiSelectMode = isMultiSelectMode,
                            onClick = { onItemClick(item, index) },
                            onLongClick = { onItemLongClick(item) },
                            onFavoriteToggle = { onFavoriteToggle(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (count == 1) "1 item" else "$count items",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDateHeader(epochSec: Long): String {
    val dateMs = if (epochSec < 10_000_000_000L) epochSec * 1000 else epochSec
    val itemCal = Calendar.getInstance().apply { timeInMillis = dateMs }
    val nowCal = Calendar.getInstance()

    val itemYear = itemCal.get(Calendar.YEAR)
    val nowYear = nowCal.get(Calendar.YEAR)
    val itemDay = itemCal.get(Calendar.DAY_OF_YEAR)
    val nowDay = nowCal.get(Calendar.DAY_OF_YEAR)

    return when {
        itemYear == nowYear && itemDay == nowDay -> "Today"
        itemYear == nowYear && itemDay == nowDay - 1 -> "Yesterday"
        itemYear == nowYear -> {
            val fmt = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
            fmt.format(itemCal.time)
        }
        else -> {
            val fmt = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
            fmt.format(itemCal.time)
        }
    }
}
