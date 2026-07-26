package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    currentDateRange: Pair<Long?, Long?>?,
    onApplyDateRange: (Long?, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDateRangePickerState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Media by Date Range") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                DateRangePicker(
                    state = datePickerState,
                    modifier = Modifier.weight(1f),
                    title = null,
                    headline = null,
                    showModeToggle = false
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val startMillis = datePickerState.selectedStartDateMillis
                    val endMillis = datePickerState.selectedEndDateMillis
                    val startSec = startMillis?.div(1000)
                    val endSec = endMillis?.div(1000)?.plus(86399) // include end of day
                    onApplyDateRange(startSec, endSec)
                    onDismiss()
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            Row {
                if (currentDateRange != null) {
                    TextButton(
                        onClick = {
                            onApplyDateRange(null, null)
                            onDismiss()
                        }
                    ) {
                        Text("Reset Filter")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
