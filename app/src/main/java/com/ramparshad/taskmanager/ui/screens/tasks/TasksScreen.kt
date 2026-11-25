package com.ramparshad.taskmanager.ui.screens.tasks


import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramparshad.taskmanager.domain.model.Priority
import com.ramparshad.taskmanager.domain.model.SortOrder
import com.ramparshad.taskmanager.domain.model.TaskStatus
import com.ramparshad.taskmanager.ui.components.EmptyState
import com.ramparshad.taskmanager.ui.components.TaskCard
import com.ramparshad.taskmanager.ui.theme.Dimens
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onOpenTask: (Long) -> Unit,
    onCreateTask: () -> Unit,
    vm: TasksViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showFilter by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var searchActive by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.lastDeleted) {
        val deleted = state.lastDeleted ?: return@LaunchedEffect
        val result = snackbar.showSnackbar(
            message = "Task deleted",
            actionLabel = "Undo",
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) {
            vm.undoDelete()
        } else {
            vm.consumeUndo()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // Header
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Dimens.lg, vertical = Dimens.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (searchActive) {
                    TextField(
                        value = state.searchQuery,
                        onValueChange = vm::setQuery,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search tasks…") },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        trailingIcon = {
                            IconButton(onClick = { vm.setQuery(""); searchActive = false }) {
                                Icon(Icons.Filled.Close, "Close search")
                            }
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                        ),
                    )
                } else {
                    Column(Modifier.weight(1f)) {
                        Text("My Tasks", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "${state.filteredTasks.size} of ${state.allTasks.size} tasks",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { searchActive = true }) {
                        Icon(Icons.Filled.Search, "Search")
                    }
                    IconButton(onClick = { showFilter = true }) {
                        BadgedBox(badge = {
                            if (!state.filters.isEmpty) {
                                Badge { Text((state.filters.priorities.size + state.filters.statuses.size +
                                        state.filters.categories.size + state.filters.tags.size +
                                        if (state.filters.dueDate != DueDateFilter.Any) 1 else 0).toString()) }
                            }
                        }) { Icon(Icons.Filled.FilterList, "Filter") }
                    }
                    IconButton(onClick = { showSort = true }) {
                        Icon(Icons.Filled.Sort, "Sort")
                    }
                }
            }

            // Tabs
            ScrollableTabRow(
                selectedTabIndex = state.selectedTab.ordinal,
                edgePadding = Dimens.lg,
                containerColor = MaterialTheme.colorScheme.background,
                divider = {},
            ) {
                TaskTab.values().forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { vm.selectTab(tab) },
                        text = { Text(tab.label) },
                    )
                }
            }

            // List
            when {
                state.isLoading -> {
                    LazyColumn(
                        contentPadding = PaddingValues(Dimens.lg),
                        verticalArrangement = Arrangement.spacedBy(Dimens.md),
                    ) { items(4) { SkeletonTaskCard() } }
                }
                state.filteredTasks.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = Icons.Filled.Inbox,
                            title = if (state.searchQuery.isNotBlank()) "No matches"
                            else "No tasks here",
                            message = if (state.searchQuery.isNotBlank())
                                "Try a different search term or clear filters."
                            else "Create your first task to get started.",
                            actionLabel = if (state.searchQuery.isBlank()) "Add Task" else null,
                            onAction = if (state.searchQuery.isBlank()) onCreateTask else null,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = Dimens.lg, end = Dimens.lg,
                            top = Dimens.sm, bottom = Dimens.xxxl + 80.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(Dimens.md),
                    ) {
                        items(state.filteredTasks, key = { it.id }) { task ->
                            SwipeToDismissTask(
                                task = task,
                                onToggle = { vm.toggleComplete(task) },
                                onDelete = { vm.deleteTask(task) },
                                onClick = { onOpenTask(task.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilter) {
        FilterBottomSheet(
            current = state.filters,
            categories = state.availableCategories,
            tags = state.availableTags,
            onApply = { vm.setFilters(it); showFilter = false },
            onClear = { vm.setFilters(TaskFilters()); showFilter = false },
            onDismiss = { showFilter = false },
        )
    }
    if (showSort) {
        SortBottomSheet(
            current = state.sortOrder,
            onSelect = { vm.setSort(it); showSort = false },
            onDismiss = { showSort = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissTask(
    task: com.ramparshad.taskmanager.domain.model.Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { onToggle(); false }
                SwipeToDismissBoxValue.EndToStart -> { onDelete(); true }
                else -> false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val isDelete = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
            val color = if (isDelete) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary
            val alignment = if (isDelete) Alignment.CenterEnd else Alignment.CenterStart
            Box(
                Modifier
                    .fillMaxSize()
                    .background(color.copy(alpha = 0.18f), Dimens.cardShape)
                    .padding(horizontal = Dimens.xl),
                contentAlignment = alignment,
            ) {
                Icon(
                    if (isDelete) Icons.Filled.Delete else Icons.Filled.Check,
                    contentDescription = null,
                    tint = color,
                )
            }
        },
    ) {
        TaskCard(task = task, onToggleComplete = onToggle, onClick = onClick)
    }
}

@Composable
private fun SkeletonTaskCard() {
    Surface(
        shape = Dimens.cardShape,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Dimens.lg)) {
            Box(Modifier.fillMaxWidth(0.6f).height(18.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, Dimens.chipShape))
            Spacer(Modifier.height(Dimens.sm))
            Box(Modifier.fillMaxWidth(0.9f).height(12.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, Dimens.chipShape))
            Spacer(Modifier.height(Dimens.sm))
            Box(Modifier.fillMaxWidth(0.3f).height(12.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, Dimens.chipShape))
        }
    }
}