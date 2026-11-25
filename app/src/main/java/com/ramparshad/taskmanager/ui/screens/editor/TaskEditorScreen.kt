package com.ramparshad.taskmanager.ui.screens.editor

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramparshad.taskmanager.domain.model.Priority
import com.ramparshad.taskmanager.domain.model.RecurrenceType
import com.ramparshad.taskmanager.domain.model.TaskStatus
import com.ramparshad.taskmanager.ui.components.PriorityChip
import com.ramparshad.taskmanager.ui.theme.Dimens
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorScreen(
    taskId: Long,
    prefillDate: Long?,
    onSaved: (Long) -> Unit,
    onCancel: () -> Unit,
) {
    // THIS IS THE FIX: By wrapping the UI in a key() based on taskId,
    // Compose throws away the old state and recreates the ViewModel from scratch.
    key(taskId) {
        val vm: TaskEditorViewModel = hiltViewModel()

        // Tell the ViewModel to load the task if editing, or prefill the date if creating
        LaunchedEffect(taskId, prefillDate) {
            vm.loadTask(taskId, prefillDate)
        }

        val state by vm.state.collectAsStateWithLifecycle()
        val ctx = LocalContext.current
        val dateFmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
        val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

        val attachLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri: Uri? ->
            uri?.let {
                runCatching {
                    ctx.contentResolver.takePersistableUriPermission(
                        it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
                vm.addAttachment(it.toString())
            }
        }

        LaunchedEffect(state.saved) {
            if (state.saved) onSaved(state.taskId)
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(if (state.isEditing) "Edit Task" else "New Task") },
                    navigationIcon = {
                        IconButton(onClick = onCancel) { Icon(Icons.Filled.Close, "Cancel") }
                    },
                    actions = {
                        TextButton(
                            onClick = { vm.save { } },
                            enabled = !state.isSaving,
                        ) { Text("Save") }
                    },
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(Dimens.lg),
                verticalArrangement = Arrangement.spacedBy(Dimens.md),
            ) {
                item {
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = vm::updateTitle,
                        label = { Text("Title *") },
                        isError = state.titleError != null,
                        supportingText = { state.titleError?.let { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                item {
                    OutlinedTextField(
                        value = state.description,
                        onValueChange = vm::updateDescription,
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                    )
                }
                item {
                    OutlinedTextField(
                        value = state.category,
                        onValueChange = vm::updateCategory,
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                item {
                    Text("Priority", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Dimens.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                        Priority.values().forEach { p ->
                            val selected = state.priority == p
                            Surface(
                                onClick = { vm.updatePriority(p) },
                                shape = Dimens.chipShape,
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface,
                                border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                else null,
                            ) {
                                Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    PriorityChip(p)
                                }
                            }
                        }
                    }
                }
                item {
                    Text("Status", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Dimens.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                        TaskStatus.values().forEach { s ->
                            FilterChip(
                                selected = state.status == s,
                                onClick = { vm.updateStatus(s) },
                                label = { Text(if (s == TaskStatus.InProgress) "In Progress" else s.name) },
                            )
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.md)) {
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply {
                                    state.startDate?.let { timeInMillis = it }
                                }
                                DatePickerDialog(
                                    ctx,
                                    { _, y, m, d ->
                                        cal.set(y, m, d, 0, 0, 0); cal.set(Calendar.MILLISECOND, 0)
                                        vm.updateStartDate(cal.timeInMillis)
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH),
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(state.startDate?.let { dateFmt.format(Date(it)) } ?: "Start date")
                        }
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply {
                                    state.dueDate?.let { timeInMillis = it }
                                }
                                DatePickerDialog(
                                    ctx,
                                    { _, y, m, d ->
                                        cal.set(y, m, d, 0, 0, 0); cal.set(Calendar.MILLISECOND, 0)
                                        vm.updateDueDate(cal.timeInMillis)
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH),
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.Event, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(state.dueDate?.let { dateFmt.format(Date(it)) } ?: "Due date")
                        }
                    }
                    state.dateError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium)
                    }
                }
                item {
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply {
                                state.dueTime?.let { timeInMillis = it }
                            }
                            TimePickerDialog(
                                ctx,
                                { _, h, min ->
                                    cal.set(Calendar.HOUR_OF_DAY, h)
                                    cal.set(Calendar.MINUTE, min)
                                    vm.updateDueTime(cal.timeInMillis)
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                false,
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Schedule, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(state.dueTime?.let { timeFmt.format(Date(it)) } ?: "Due time")
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Reminder", modifier = Modifier.weight(1f))
                        Switch(
                            checked = state.reminderEnabled,
                            onCheckedChange = vm::updateReminderEnabled,
                        )
                    }
                }

                item {
                    Text("Repeat", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Dimens.sm))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                        RecurrenceType.values().forEach { r ->
                            FilterChip(
                                selected = state.recurrence == r,
                                onClick = { vm.updateRecurrence(r) },
                                label = { Text(r.name) },
                            )
                        }
                    }
                }
                if (state.recurrence != RecurrenceType.None) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Every", modifier = Modifier.weight(1f))
                            OutlinedTextField(
                                value = state.recurrenceInterval.toString(),
                                onValueChange = { vm.updateRecurrenceInterval(it.toIntOrNull() ?: 1) },
                                modifier = Modifier.width(80.dp),
                                singleLine = true,
                            )
                            Spacer(Modifier.width(Dimens.sm))
                            Text(state.recurrence.name.lowercase())
                        }
                    }
                }

                item {
                    Text("Subtasks (${state.subtasks.count { it.isDone }}/${state.subtasks.size})",
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Dimens.sm))
                    Row {
                        OutlinedTextField(
                            value = state.newSubtask,
                            onValueChange = vm::updateNewSubtask,
                            placeholder = { Text("Add subtask") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        Spacer(Modifier.width(Dimens.sm))
                        IconButton(onClick = vm::addSubtask) {
                            Icon(Icons.Filled.Add, "Add subtask")
                        }
                    }
                }
                items(state.subtasks, key = { it.id }) { sub ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = sub.isDone,
                            onCheckedChange = { if (sub.id != 0L) vm.toggleSubtask(sub.id) },
                        )
                        Text(sub.title, modifier = Modifier.weight(1f))
                        IconButton(onClick = { if (sub.id != 0L) vm.removeSubtask(sub.id) }) {
                            Icon(Icons.Filled.Delete, "Remove")
                        }
                    }
                }

                item {
                    Text("Tags", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Dimens.sm))
                    Row {
                        OutlinedTextField(
                            value = state.newTag,
                            onValueChange = vm::updateNewTag,
                            placeholder = { Text("Add tag") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        Spacer(Modifier.width(Dimens.sm))
                        IconButton(onClick = vm::addTag) { Icon(Icons.Filled.Add, "Add tag") }
                    }
                    if (state.tags.isNotEmpty()) {
                        Spacer(Modifier.height(Dimens.sm))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                            state.tags.forEach { tag ->
                                InputChip(
                                    selected = false,
                                    onClick = { vm.removeTag(tag) },
                                    label = { Text(tag) },
                                    trailingIcon = { Icon(Icons.Filled.Close, null, Modifier.size(16.dp)) },
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = vm::updateNotes,
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                    )
                }

                item {
                    OutlinedButton(
                        onClick = { attachLauncher.launch(arrayOf("image/*", "application/pdf", "text/*")) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.AttachFile, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Attach file (${state.attachments.size})")
                    }
                    state.attachments.forEach { uri ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.InsertDriveFile, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(Dimens.sm))
                            Text(uri.substringAfterLast('/'), Modifier.weight(1f),
                                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            IconButton(onClick = { vm.removeAttachment(uri) }) {
                                Icon(Icons.Filled.Close, "Remove", Modifier.size(18.dp))
                            }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.md),
                        modifier = Modifier.padding(top = Dimens.lg)) {
                        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = { vm.save { } },
                            modifier = Modifier.weight(1f),
                            enabled = !state.isSaving,
                        ) { Text(if (state.isSaving) "Saving…" else "Save") }
                    }
                    Spacer(Modifier.height(Dimens.xxl))
                }
            }
        }
    }
}