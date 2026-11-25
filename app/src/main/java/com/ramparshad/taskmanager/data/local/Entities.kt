package com.ramparshad.taskmanager.data.local


import androidx.room.*
import com.ramparshad.taskmanager.domain.model.*

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val category: String,
    val tags: List<String>,             // TypeConverter: List<String> <-> JSON
    val priority: Priority,
    val status: TaskStatus,
    val startDate: Long?,
    val dueDate: Long?,
    val dueTime: Long?,
    val reminderEnabled: Boolean,
    val reminderAt: Long?,
    val recurrence: RecurrenceType,
    val recurrenceInterval: Int,
    val notes: String,
    val attachments: List<String>,      // TypeConverter
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,        // soft-delete for undo
)

@Entity(
    tableName = "subtasks",
    foreignKeys = [ForeignKey(
        entity = TaskEntity::class,
        parentColumns = ["id"],
        childColumns = ["taskId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("taskId")],
)
data class SubtaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val title: String,
    val isDone: Boolean,
)