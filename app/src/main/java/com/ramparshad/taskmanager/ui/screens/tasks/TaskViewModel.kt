package com.ramparshad.taskmanager.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramparshad.taskmanager.data.repository.SettingsRepository
import com.ramparshad.taskmanager.data.repository.TaskRepository
import com.ramparshad.taskmanager.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TaskTab(val label: String) {
    All("All"), Pending("Pending"), InProgress("In Progress"),
    Completed("Completed"), Overdue("Overdue"),
}

data class TaskFilters(
    val priorities: Set<Priority> = emptySet(),
    val statuses: Set<TaskStatus> = emptySet(),
    val categories: Set<String> = emptySet(),
    val dueDate: DueDateFilter = DueDateFilter.Any,
    val tags: Set<String> = emptySet(),
) {
    val isEmpty get() = priorities.isEmpty() && statuses.isEmpty() &&
            categories.isEmpty() && dueDate == DueDateFilter.Any && tags.isEmpty()
}

enum class DueDateFilter(val label: String) {
    Any("Any"), Today("Today"), Tomorrow("Tomorrow"),
    ThisWeek("This Week"), Overdue("Overdue"), Custom("Custom Range"),
}

data class TasksUiState(
    val allTasks: List<Task> = emptyList(),
    val filteredTasks: List<Task> = emptyList(),
    val searchQuery: String = "",
    val selectedTab: TaskTab = TaskTab.All,
    val filters: TaskFilters = TaskFilters(),
    val sortOrder: SortOrder = SortOrder.RecentlyCreated,
    val availableCategories: List<String> = emptyList(),
    val availableTags: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val lastDeleted: Task? = null,
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repo: TaskRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _localState = MutableStateFlow(LocalState())
    private data class LocalState(
        val query: String = "",
        val tab: TaskTab = TaskTab.All,
        val filters: TaskFilters = TaskFilters(),
        val sort: SortOrder = SortOrder.RecentlyCreated,
        val lastDeleted: Task? = null,
    )

    init {
        viewModelScope.launch {
            settings.defaultSort.collect { sort ->
                _localState.update { if (it.sort == SortOrder.RecentlyCreated) it.copy(sort = sort) else it }
            }
        }
    }

    val uiState: StateFlow<TasksUiState> = combine(
        repo.observeAll(),
        _localState,
    ) { tasks, local ->
        val searched = tasks.filter { t ->
            local.query.isBlank() ||
                    t.title.contains(local.query, true) ||
                    t.description.contains(local.query, true) ||
                    t.category.contains(local.query, true) ||
                    t.tags.any { it.contains(local.query, true) }
        }
        val tabbed = when (local.tab) {
            TaskTab.All -> searched
            TaskTab.Pending -> searched.filter { it.status == TaskStatus.Pending }
            TaskTab.InProgress -> searched.filter { it.status == TaskStatus.InProgress }
            TaskTab.Completed -> searched.filter { it.status == TaskStatus.Completed }
            TaskTab.Overdue -> searched.filter { it.isOverdue }
        }
        val filtered = applyFilters(tabbed, local.filters)
        val sorted = sort(filtered, local.sort)

        TasksUiState(
            allTasks = tasks,
            filteredTasks = sorted,
            searchQuery = local.query,
            selectedTab = local.tab,
            filters = local.filters,
            sortOrder = local.sort,
            availableCategories = tasks.map { it.category }.distinct().sorted(),
            availableTags = tasks.flatMap { it.tags }.distinct().sorted(),
            isLoading = false,
            lastDeleted = local.lastDeleted,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksUiState())

    fun setQuery(q: String) = _localState.update { it.copy(query = q) }
    fun selectTab(tab: TaskTab) = _localState.update { it.copy(tab = tab) }
    fun setFilters(f: TaskFilters) = _localState.update { it.copy(filters = f) }
    fun setSort(s: SortOrder) = _localState.update { it.copy(sort = s) }

    fun toggleComplete(task: Task) = viewModelScope.launch {
        repo.updateStatus(
            task.id,
            if (task.status == TaskStatus.Completed) TaskStatus.Pending else TaskStatus.Completed,
        )
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        repo.delete(task.id)
        _localState.update { it.copy(lastDeleted = task) }
    }

    fun undoDelete() = viewModelScope.launch {
        _localState.value.lastDeleted?.let { repo.restore(it.id) }
        _localState.update { it.copy(lastDeleted = null) }
    }

    fun consumeUndo() = _localState.update { it.copy(lastDeleted = null) }

    private fun applyFilters(tasks: List<Task>, f: TaskFilters): List<Task> {
        if (f.isEmpty) return tasks
        val now = System.currentTimeMillis()
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val endOfDay = startOfDay + 86_400_000
        val endOfTomorrow = endOfDay + 86_400_000
        val endOfWeek = startOfDay + 7L * 86_400_000

        return tasks.filter { t ->
            (f.priorities.isEmpty() || t.priority in f.priorities) &&
                    (f.statuses.isEmpty() || t.status in f.statuses) &&
                    (f.categories.isEmpty() || t.category in f.categories) &&
                    (f.tags.isEmpty() || t.tags.any { it in f.tags }) &&
                    when (f.dueDate) {
                        DueDateFilter.Any -> true
                        DueDateFilter.Today -> t.dueDate in startOfDay until endOfDay
                        DueDateFilter.Tomorrow -> t.dueDate in endOfDay until endOfTomorrow
                        DueDateFilter.ThisWeek -> t.dueDate in startOfDay until endOfWeek
                        DueDateFilter.Overdue -> t.isOverdue
                        DueDateFilter.Custom -> true
                    }
        }
    }

    private fun sort(tasks: List<Task>, order: SortOrder): List<Task> = when (order) {
        SortOrder.DueDateEarliest -> tasks.sortedWith(compareBy(nullsLast()) { it.dueDate })
        SortOrder.DueDateLatest -> tasks.sortedWith(compareByDescending(nullsFirst()) { it.dueDate })
        SortOrder.PriorityHighLow -> tasks.sortedByDescending { it.priority.ordinal }
        SortOrder.PriorityLowHigh -> tasks.sortedBy { it.priority.ordinal }
        SortOrder.RecentlyCreated -> tasks.sortedByDescending { it.createdAt }
        SortOrder.OldestCreated -> tasks.sortedBy { it.createdAt }
        SortOrder.Alphabetical -> tasks.sortedBy { it.title.lowercase() }
        SortOrder.RecentlyUpdated -> tasks.sortedByDescending { it.updatedAt }
    }
}

private fun <T : Comparable<T>> nullsLast(): Comparator<T?> =
    compareBy(nullsLast()) { it }
private fun <T : Comparable<T>> nullsFirst(): Comparator<T?> =
    compareBy(nullsFirst()) { it }