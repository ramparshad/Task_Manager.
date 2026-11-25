package com.ramparshad.taskmanager.ui.screens.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramparshad.taskmanager.domain.model.*
import com.ramparshad.taskmanager.ui.components.PriorityChip
import com.ramparshad.taskmanager.ui.components.StatusChip
import com.ramparshad.taskmanager.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailsScreen(
    taskId: Long,
    onEdit: (Long) -> Unit,
    onBack: () -> Unit,
    vm: TaskDetailsViewModel = hiltViewModel(),
) {
    // THIS IS THE FIX: Tell the ViewModel to load the task when the screen opens
    LaunchedEffect(taskId) {
        vm.loadTask(taskId)
    }

    val task by vm.task.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }
    var showPriorityMenu by remember { mutableStateOf(false) }
    val dateFmt = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Task Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
                },
                actions = {
                    if (task != null) {
                        IconButton(onClick = { onEdit(task!!.id) }) {
                            Icon(Icons.Filled.Edit, "Edit")
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, "Delete")
                        }
                    }
                },
            )
        },
    ) { padding ->
        task?.let { t ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(Dimens.lg),
                verticalArrangement = Arrangement.spacedBy(Dimens.md),
            ) {
                item {
                    Row(verticalAlignment = Alignment.Top) {
                        Checkbox(
                            checked = t.isCompleted,
                            onCheckedChange = { vm.toggleComplete(t) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                        Spacer(Modifier.width(Dimens.sm))
                        Text(
                            t.title,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (t.description.isNotBlank()) {
                    item {
                        Text(
                            t.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                        Box {
                            Surface(onClick = { showPriorityMenu = true }, shape = Dimens.chipShape) {
                                Box(Modifier.padding(4.dp)) { PriorityChip(t.priority) }
                            }
                            DropdownMenu(expanded = showPriorityMenu,
                                onDismissRequest = { showPriorityMenu = false }) {
                                Priority.values().forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p.name) },
                                        onClick = { vm.changePriority(t, p); showPriorityMenu = false },
                                    )
                                }
                            }
                        }
                        Box {
                            Surface(onClick = { showStatusMenu = true }, shape = Dimens.chipShape) {
                                Box(Modifier.padding(4.dp)) { StatusChip(t.status) }
                            }
                            DropdownMenu(expanded = showStatusMenu,
                                onDismissRequest = { showStatusMenu = false }) {
                                TaskStatus.values().forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s.name) },
                                        onClick = { vm.changeStatus(t, s); showStatusMenu = false },
                                    )
                                }
                            }
                        }
                        Surface(shape = Dimens.chipShape,
                            color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text(t.category, Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                item {
                    Surface(shape = Dimens.cardShape, color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.padding(Dimens.lg),
                            verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                            DetailRow(Icons.Filled.CalendarToday, "Created",
                                dateFmt.format(Date(t.createdAt)))
                            t.startDate?.let {
                                DetailRow(Icons.Filled.PlayArrow, "Start", dateFmt.format(Date(it)))
                            }
                            t.dueDate?.let {
                                DetailRow(
                                    Icons.Filled.Event, "Due",
                                    dateFmt.format(Date(it)) +
                                            (t.dueTime?.let { tm -> " at " +
                                                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(tm)) } ?: ""),
                                    tint = if (t.isOverdue) Error else null,
                                )
                            }
                            if (t.reminderEnabled && t.reminderAt != null) {
                                DetailRow(Icons.Filled.Notifications, "Reminder",
                                    dateFmt.format(Date(t.reminderAt!!)))
                            }
                            if (t.recurrence != RecurrenceType.None) {
                                DetailRow(Icons.Filled.Repeat, "Repeats",
                                    "Every ${t.recurrenceInterval} ${t.recurrence.name.lowercase()}")
                            }
                        }
                    }
                }

                if (t.tags.isNotEmpty()) {
                    item {
                        Text("Tags", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(Dimens.sm))
                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                            t.tags.forEach { tag ->
                                Surface(shape = Dimens.chipShape,
                                    color = MaterialTheme.colorScheme.primaryContainer) {
                                    Text(tag, Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                    }
                }

                if (t.subtasks.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Subtasks (${t.subtasks.count { it.isDone }}/${t.subtasks.size})",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f))
                            Text("${(t.subtaskProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(Dimens.sm))
                        LinearProgressIndicator(
                            progress = { t.subtaskProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    items(t.subtasks, key = { it.id }) { sub ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = sub.isDone,
                                onCheckedChange = { vm.toggleSubtask(t, sub) },
                            )
                            Text(sub.title, modifier = Modifier.weight(1f))
                        }
                    }
                }

                if (t.notes.isNotBlank()) {
                    item {
                        Text("Notes", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(Dimens.sm))
                        Text(t.notes, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (t.attachments.isNotEmpty()) {
                    item {
                        Text("Attachments", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(Dimens.sm))
                        t.attachments.forEach { uri ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.InsertDriveFile, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(Dimens.sm))
                                Text(uri.substringAfterLast('/'), Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(Dimens.lg))
                    if (!t.isCompleted) {
                        Button(
                            onClick = { vm.toggleComplete(t) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.CheckCircle, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Mark as Completed")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { vm.toggleComplete(t) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Reopen Task") }
                    }
                    Spacer(Modifier.height(Dimens.xxl))
                }
            }
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete task?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; vm.delete(taskId); onBack() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: Color? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(16.dp),
            tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(Dimens.sm))
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium,
            color = tint ?: MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium)
    }
}