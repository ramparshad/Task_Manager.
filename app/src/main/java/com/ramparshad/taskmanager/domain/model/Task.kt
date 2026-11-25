package com.ramparshad.taskmanager.domain.model

enum class Priority { Low, Medium, High, Urgent }
enum class TaskStatus { Pending, InProgress, Completed }
enum class RecurrenceType { None, Daily, Weekly, Monthly, Yearly }
enum class SortOrder {
    DueDateEarliest, DueDateLatest, PriorityHighLow, PriorityLowHigh,
    RecentlyCreated, OldestCreated, Alphabetical, RecentlyUpdated,
}

data class Task(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "General",
    val tags: List<String> = emptyList(),
    val priority: Priority = Priority.Medium,
    val status: TaskStatus = TaskStatus.Pending,
    val startDate: Long? = null,
    val dueDate: Long? = null,
    val dueTime: Long? = null,
    val reminderEnabled: Boolean = false,
    val reminderAt: Long? = null,
    val recurrence: RecurrenceType = RecurrenceType.None,
    val recurrenceInterval: Int = 1,
    val notes: String = "",
    val attachments: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val subtasks: List<Subtask> = emptyList(),
) {
    val isCompleted get() = status == TaskStatus.Completed
    val isOverdue get() =
        !isCompleted && dueDate != null && dueDate < System.currentTimeMillis()
    val subtaskProgress: Float
        get() = if (subtasks.isEmpty()) 0f
        else subtasks.count { it.isDone }.toFloat() / subtasks.size
}

data class Subtask(
    val id: Long = 0,
    val taskId: Long,
    val title: String,
    val isDone: Boolean = false,
)