package com.ramparshad.taskmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ramparshad.taskmanager.domain.model.Priority
import com.ramparshad.taskmanager.domain.model.TaskStatus
import com.ramparshad.taskmanager.ui.theme.*

@Composable
fun PriorityChip(priority: Priority, modifier: Modifier = Modifier) {
    val color = when (priority) {
        Priority.Low -> PriorityLow
        Priority.Medium -> PriorityMedium
        Priority.High -> PriorityHigh
        Priority.Urgent -> PriorityUrgent
    }
    Surface(
        modifier = modifier,
        shape = Dimens.chipShape,
        color = color.copy(alpha = 0.14f),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // This now correctly uses the import from androidx.compose.foundation.background
            Box(Modifier.size(6.dp).background(color, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(
                priority.name,
                style = MaterialTheme.typography.labelMedium,
                color = color,
            )
        }
    }
}

@Composable
fun StatusChip(status: TaskStatus, modifier: Modifier = Modifier) {
    val (label, color) = when (status) {
        TaskStatus.Pending -> "Pending" to MaterialTheme.colorScheme.onSurfaceVariant
        TaskStatus.InProgress -> "In Progress" to MaterialTheme.colorScheme.primary
        TaskStatus.Completed -> "Completed" to Success
    }
    Surface(
        modifier = modifier,
        shape = Dimens.chipShape,
        color = color.copy(alpha = 0.14f),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}