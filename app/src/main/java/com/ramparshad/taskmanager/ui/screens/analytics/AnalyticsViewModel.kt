package com.ramparshad.taskmanager.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramparshad.taskmanager.data.repository.TaskRepository
import com.ramparshad.taskmanager.domain.model.Priority
import com.ramparshad.taskmanager.domain.model.Task
import com.ramparshad.taskmanager.domain.model.TaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import java.util.Calendar

enum class AnalyticsRange(val label: String) {
    Week("This Week"), Month("This Month"),
    Last30("Last 30 Days"), Custom("Custom"),
}

data class DailyPoint(val dayMillis: Long, val label: String, val completed: Int)
data class PriorityStat(val priority: Priority, val count: Int)
data class CategoryStat(val category: String, val total: Int, val completed: Int) {
    val rate: Float get() = if (total == 0) 0f else completed.toFloat() / total
}

data class AnalyticsUiState(
    val range: AnalyticsRange = AnalyticsRange.Week,
    val rangeStart: Long = 0,
    val rangeEnd: Long = 0,
    val totalCreated: Int = 0,
    val completed: Int = 0,
    val pending: Int = 0,
    val inProgress: Int = 0,
    val overdue: Int = 0,
    val completionRate: Float = 0f,
    val dailyPoints: List<DailyPoint> = emptyList(),
    val priorityStats: List<PriorityStat> = emptyList(),
    val categoryStats: List<CategoryStat> = emptyList(),
    val mostProductiveDay: String = "—",
    val avgDailyCompletion: Float = 0f,
    val completedThisWeek: Int = 0,
    val isLoading: Boolean = true,
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    repo: TaskRepository,
) : ViewModel() {

    private val _range = MutableStateFlow(AnalyticsRange.Week)
    private val _customStart = MutableStateFlow(System.currentTimeMillis() - 7L * 86_400_000)
    private val _customEnd = MutableStateFlow(System.currentTimeMillis())

    val uiState: StateFlow<AnalyticsUiState> = combine(
        repo.observeAll(), _range, _customStart, _customEnd,
    ) { tasks, range, cs, ce ->
        compute(tasks, range, cs, ce)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())

    fun setRange(r: AnalyticsRange) { _range.value = r }
    fun setCustomRange(start: Long, end: Long) {
        _customStart.value = start; _customEnd.value = end
        _range.value = AnalyticsRange.Custom
    }

    private fun compute(tasks: List<Task>, range: AnalyticsRange, cs: Long, ce: Long): AnalyticsUiState {
        val (start, end) = when (range) {
            AnalyticsRange.Week -> startOfWeekMillis() to System.currentTimeMillis()
            AnalyticsRange.Month -> startOfMonthMillis() to System.currentTimeMillis()
            AnalyticsRange.Last30 -> (System.currentTimeMillis() - 30L * 86_400_000) to System.currentTimeMillis()
            AnalyticsRange.Custom -> startOfDay(cs) to (startOfDay(ce) + 86_400_000)
        }
        val inRange = tasks.filter {
            it.createdAt in start..end || (it.dueDate ?: 0) in start..end
        }

        val completed = inRange.count { it.status == TaskStatus.Completed }
        val pending = inRange.count { it.status == TaskStatus.Pending }
        val inProgress = inRange.count { it.status == TaskStatus.InProgress }
        val overdue = inRange.count { it.isOverdue }
        val total = inRange.size
        val rate = if (total == 0) 0f else completed.toFloat() / total

        // Daily buckets
        val days = ((end - start) / 86_400_000L + 1).toInt().coerceAtLeast(1)
        val cal = Calendar.getInstance().apply { timeInMillis = start }
        val dayFmt = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
        val buckets = mutableListOf<DailyPoint>()
        repeat(days) { i ->
            val dayStart = startOfDay(cal.timeInMillis)
            val dayEnd = dayStart + 86_400_000L
            val count = inRange.count {
                it.status == TaskStatus.Completed &&
                        it.updatedAt in dayStart until dayEnd
            }
            buckets += DailyPoint(dayStart, dayFmt.format(java.util.Date(dayStart)), count)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Priority distribution
        val priorityStats = Priority.values().map { p ->
            PriorityStat(p, inRange.count { it.priority == p })
        }

        // Category performance
        val categoryStats = inRange.groupBy { it.category }.map { (cat, list) ->
            CategoryStat(cat, list.size, list.count { it.status == TaskStatus.Completed })
        }.sortedByDescending { it.total }.take(6)

        val mostProductiveDay = buckets.maxByOrNull { it.completed }?.label ?: "—"
        val avg = if (buckets.isEmpty()) 0f else buckets.sumOf { it.completed }.toFloat() / buckets.size
        val weekStart = startOfWeekMillis()
        val completedThisWeek = tasks.count {
            it.status == TaskStatus.Completed && it.updatedAt >= weekStart
        }

        return AnalyticsUiState(
            range = range, rangeStart = start, rangeEnd = end,
            totalCreated = total, completed = completed, pending = pending,
            inProgress = inProgress, overdue = overdue, completionRate = rate,
            dailyPoints = buckets, priorityStats = priorityStats,
            categoryStats = categoryStats, mostProductiveDay = mostProductiveDay,
            avgDailyCompletion = avg, completedThisWeek = completedThisWeek,
            isLoading = false,
        )
    }

    private fun startOfDay(millis: Long) = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun startOfWeekMillis(): Long = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun startOfMonthMillis(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}