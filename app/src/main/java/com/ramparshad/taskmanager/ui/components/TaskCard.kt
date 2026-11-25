package com.ramparshad.taskmanager.ui.components


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ramparshad.taskmanager.domain.model.*
import com.ramparshad.taskmanager.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TaskCard(
    task: Task,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val done = task.isCompleted
    Surface(
        modifier = modifier.fillMaxWidth().clickable{onClick()},
        shape = Dimens.cardShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,

    ) {
        Row(
            Modifier.padding(Dimens.lg),
            verticalAlignment = Alignment.Top,
        ) {
            Checkbox(
                checked = done,
                onCheckedChange = { onToggleComplete() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline,
                ),
            )
            Spacer(Modifier.width(Dimens.md))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        task.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (done)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(Dimens.sm))
                    PriorityChip(task.priority)
                }
                if (task.description.isNotBlank()) {
                    Spacer(Modifier.height(Dimens.xs))
                    Text(
                        task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(Dimens.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(task.status)
                    Spacer(Modifier.width(Dimens.sm))
                    if (task.dueDate != null) {
                        Icon(
                            Icons.Filled.Schedule,
                            null,
                            tint = if (task.isOverdue) Error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        val fmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                        Text(
                            fmt.format(Date(task.dueDate)),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (task.isOverdue) Error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (task.subtasks.isNotEmpty()) {
                        val doneCount = task.subtasks.count { it.isDone }
                        Text(
                            "$doneCount/${task.subtasks.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (task.subtasks.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { task.subtaskProgress },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }
    }
}