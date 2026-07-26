package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.gallery.FilterType
import com.example.ui.gallery.ViewMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GallerySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    filterType: FilterType,
    onFilterTypeChange: (FilterType) -> Unit,
    viewMode: ViewMode,
    onViewModeChange: (ViewMode) -> Unit,
    hasDateFilter: Boolean,
    onOpenFilterDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Search Input Field
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search photos, videos, albums...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedBorderColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Pills & View Mode Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Filter Pills
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterType.values().forEach { type ->
                    val selected = filterType == type
                    FilterChip(
                        selected = selected,
                        onClick = { onFilterTypeChange(type) },
                        label = {
                            Text(
                                text = when (type) {
                                    FilterType.ALL -> "All"
                                    FilterType.PHOTOS_ONLY -> "Photos"
                                    FilterType.VIDEOS_ONLY -> "Videos"
                                },
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Date Filter & View Mode Actions
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                // Date Range Button
                IconButton(onClick = onOpenFilterDialog) {
                    BadgedBox(
                        badge = {
                            if (hasDateFilter) {
                                Badge(containerColor = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Date Filter",
                            tint = if (hasDateFilter) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // View Mode Switcher Button
                IconButton(onClick = {
                    val nextMode = when (viewMode) {
                        ViewMode.GRID_3 -> ViewMode.GRID_4
                        ViewMode.GRID_4 -> ViewMode.LIST
                        ViewMode.LIST -> ViewMode.GRID_3
                    }
                    onViewModeChange(nextMode)
                }) {
                    Icon(
                        imageVector = when (viewMode) {
                            ViewMode.GRID_3 -> Icons.Default.GridView
                            ViewMode.GRID_4 -> Icons.Default.ViewColumn
                            ViewMode.LIST -> Icons.Default.List
                        },
                        contentDescription = "Change View Mode",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
