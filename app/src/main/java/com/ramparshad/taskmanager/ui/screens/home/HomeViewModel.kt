package com.ramparshad.taskmanager.ui.screens.home


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramparshad.taskmanager.data.repository.TaskRepository
import com.ramparshad.taskmanager.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeUiState(
    val totalTasks: Int = 0,
    val completed: Int = 0,
    val pending: Int = 0,
    val overdue: Int = 0,
    val todayTasks: List<Task> = emptyList(),
    val upcomingTasks: List<Task> = emptyList(),
    val greeting: String = "Good Morning",
    val progress: Float = 0f,
    val motivation: String = "",
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: TaskRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = repo.observeAll().map { tasks ->
        val now = System.currentTimeMillis()
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000

        val today = tasks.filter { it.dueDate in startOfDay until endOfDay }
        val upcoming = tasks.filter { it.dueDate != null && it.dueDate >= endOfDay }
            .sortedBy { it.dueDate }

        val completed = tasks.count { it.isCompleted }
        val pending = tasks.count { it.status != TaskStatus.Completed }
        val overdue = tasks.count { it.isOverdue }
        val progress = if (tasks.isEmpty()) 0f else completed.toFloat() / tasks.size

        HomeUiState(
            totalTasks = tasks.size,
            completed = completed,
            pending = pending,
            overdue = overdue,
            todayTasks = today,
            upcomingTasks = upcoming.take(5),
            greeting = greetingForHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)),
            progress = progress,
            motivation = when {
                progress >= 0.8f -> "Excellent! You're crushing it today."
                progress >= 0.5f -> "Great progress, keep going!"
                progress > 0f -> "Good start — one task at a time."
                else -> "Let's make today productive."
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun toggleComplete(id: Long) = viewModelScope.launch { repo.toggleComplete(id) }

    private fun greetingForHour(h: Int) = when {
        h < 12 -> "Good Morning"
        h < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }
}