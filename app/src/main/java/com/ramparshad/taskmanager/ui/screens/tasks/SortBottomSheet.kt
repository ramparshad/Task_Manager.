package com.ramparshad.taskmanager.ui.screens.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ramparshad.taskmanager.domain.model.SortOrder
import com.ramparshad.taskmanager.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    current: SortOrder,
    onSelect: (SortOrder) -> Unit,
    onDismiss: () -> Unit,
) {
    val labels = mapOf(
        SortOrder.DueDateEarliest to "Due Date — Earliest First",
        SortOrder.DueDateLatest to "Due Date — Latest First",
        SortOrder.PriorityHighLow to "Priority — Highest First",
        SortOrder.PriorityLowHigh to "Priority — Lowest First",
        SortOrder.RecentlyCreated to "Recently Created",
        SortOrder.OldestCreated to "Oldest Created",
        SortOrder.Alphabetical to "Alphabetical (A–Z)",
        SortOrder.RecentlyUpdated to "Recently Updated",
    )
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(Dimens.lg)) {
            Text("Sort by", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(Dimens.lg))
            SortOrder.values().forEach { order ->
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { onSelect(order) }
                        .padding(vertical = Dimens.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        labels.getValue(order),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (order == current) {
                        Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(Modifier.height(Dimens.lg))
        }
    }
}