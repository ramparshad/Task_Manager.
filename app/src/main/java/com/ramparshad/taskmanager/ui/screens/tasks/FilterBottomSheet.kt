package com.ramparshad.taskmanager.ui.screens.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ramparshad.taskmanager.domain.model.Priority
import com.ramparshad.taskmanager.domain.model.TaskStatus
import com.ramparshad.taskmanager.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    current: TaskFilters,
    categories: List<String>,
    tags: List<String>,
    onApply: (TaskFilters) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(current) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(Dimens.lg)) {
            Text("Filters", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(Dimens.lg))

            FilterSection("Priority") {
                Priority.values().forEach { p ->
                    FilterChip(
                        selected = p in draft.priorities,
                        onClick = {
                            draft = draft.copy(
                                priorities = if (p in draft.priorities) draft.priorities - p
                                else draft.priorities + p,
                            )
                        },
                        label = { Text(p.name) },
                        leadingIcon = if (p in draft.priorities) {
                            { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }
                        } else null,
                    )
                }
            }

            FilterSection("Status") {
                TaskStatus.values().forEach { s ->
                    FilterChip(
                        selected = s in draft.statuses,
                        onClick = {
                            draft = draft.copy(
                                statuses = if (s in draft.statuses) draft.statuses - s
                                else draft.statuses + s,
                            )
                        },
                        label = { Text(if (s == TaskStatus.InProgress) "In Progress" else s.name) },
                    )
                }
            }

            FilterSection("Due Date") {
                DueDateFilter.values().forEach { d ->
                    FilterChip(
                        selected = draft.dueDate == d,
                        onClick = { draft = draft.copy(dueDate = d) },
                        label = { Text(d.label) },
                    )
                }
            }

            if (categories.isNotEmpty()) {
                FilterSection("Category") {
                    categories.forEach { c ->
                        FilterChip(
                            selected = c in draft.categories,
                            onClick = {
                                draft = draft.copy(
                                    categories = if (c in draft.categories) draft.categories - c
                                    else draft.categories + c,
                                )
                            },
                            label = { Text(c) },
                        )
                    }
                }
            }

            if (tags.isNotEmpty()) {
                FilterSection("Tags") {
                    tags.forEach { t ->
                        FilterChip(
                            selected = t in draft.tags,
                            onClick = {
                                draft = draft.copy(
                                    tags = if (t in draft.tags) draft.tags - t
                                    else draft.tags + t,
                                )
                            },
                            label = { Text(t) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(Dimens.xl))
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.md)) {
                OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) {
                    Text("Clear All")
                }
                Button(onClick = { onApply(draft) }, modifier = Modifier.weight(1f)) {
                    Text("Apply")
                }
            }
            Spacer(Modifier.height(Dimens.xl))
        }
    }
}

@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(bottom = Dimens.lg)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Dimens.sm))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
            verticalArrangement = Arrangement.spacedBy(Dimens.xs),
        ) { content() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    horizontalArrangement: Arrangement.Horizontal,
    verticalArrangement: Arrangement.Vertical,
    content: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() },
    )
}