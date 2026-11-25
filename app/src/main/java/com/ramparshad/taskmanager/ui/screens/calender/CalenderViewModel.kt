package com.ramparshad.taskmanager.ui.screens.calender


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramparshad.taskmanager.data.repository.TaskRepository
import com.ramparshad.taskmanager.domain.model.Task
import com.ramparshad.taskmanager.domain.model.TaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class CalendarView { Month, Week, Day }

data class CalendarUiState(
    val view: CalendarView = CalendarView.Month,
    val focusedMonth: Calendar = Calendar.getInstance(),
    val selectedDate: Calendar = Calendar.getInstance(),
    val tasksForSelectedDate: List<Task> = emptyList(),
    val tasksByDay: Map<Long, List<Task>> = emptyMap(),
    val weekTasks: Map<Long, List<Task>> = emptyMap(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repo: TaskRepository,
) : ViewModel() {

    private val _local = MutableStateFlow(Local())
    private data class Local(
        val view: CalendarView = CalendarView.Month,
        val focusedMonthMillis: Long = startOfMonth(System.currentTimeMillis()),
        val selectedDateMillis: Long = startOfDay(System.currentTimeMillis()),
    )

    val uiState: StateFlow<CalendarUiState> = combine(repo.observeAll(), _local) { tasks, local ->
        val focused = Calendar.getInstance().apply { timeInMillis = local.focusedMonthMillis }
        val selected = Calendar.getInstance().apply { timeInMillis = local.selectedDateMillis }

        val monthStart = startOfMonth(focused.timeInMillis)
        val monthEnd = monthStart + 42L * 86_400_000L

        val byDay = tasks
            .filter { it.dueDate != null }
            .groupBy { startOfDay(it.dueDate!!) }

        val weekStart = startOfWeek(selected.timeInMillis)
        val weekEnd = weekStart + 7L * 86_400_000L
        val weekTasks = byDay.filterKeys { it in weekStart until weekEnd }

        val selectedDayTasks = byDay[startOfDay(selected.timeInMillis)].orEmpty()
            .sortedBy { it.dueTime ?: 0L }

        CalendarUiState(
            view = local.view,
            focusedMonth = focused,
            selectedDate = selected,
            tasksForSelectedDate = selectedDayTasks,
            tasksByDay = byDay.filterKeys { it in monthStart until monthEnd },
            weekTasks = weekTasks,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    fun setView(v: CalendarView) = _local.update { it.copy(view = v) }

    fun selectDate(millis: Long) = _local.update {
        it.copy(selectedDateMillis = startOfDay(millis))
    }

    fun previousMonth() = _local.update {
        val c = Calendar.getInstance().apply { timeInMillis = it.focusedMonthMillis }
        c.add(Calendar.MONTH, -1)
        it.copy(focusedMonthMillis = c.timeInMillis)
    }

    fun nextMonth() = _local.update {
        val c = Calendar.getInstance().apply { timeInMillis = it.focusedMonthMillis }
        c.add(Calendar.MONTH, 1)
        it.copy(focusedMonthMillis = c.timeInMillis)
    }

    fun goToToday() = _local.update {
        val today = System.currentTimeMillis()
        it.copy(
            focusedMonthMillis = startOfMonth(today),
            selectedDateMillis = startOfDay(today),
        )
    }

    fun toggleComplete(task: Task) = viewModelScope.launch {
        repo.updateStatus(
            task.id,
            if (task.status == TaskStatus.Completed) TaskStatus.Pending else TaskStatus.Completed,
        )
    }

    // helpers
    private companion object {
        fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        fun startOfMonth(millis: Long): Long = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        fun startOfWeek(millis: Long): Long = Calendar.getInstance().apply {
            timeInMillis = millis
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}