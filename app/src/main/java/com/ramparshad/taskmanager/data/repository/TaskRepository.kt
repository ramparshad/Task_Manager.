package com.ramparshad.taskmanager.data.repository


import com.ramparshad.taskmanager.data.local.*
import com.ramparshad.taskmanager.domain.model.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao,
) {
    fun observeAll(): Flow<List<Task>> =
        taskDao.observeAll().map { list ->
            list.map { it.toDomain() }
        }

    fun observeById(id: Long): Flow<Task?> =
        taskDao.observeById(id).map { it?.toDomain() }

    suspend fun save(task: Task): Long {
        val id = taskDao.upsert(task.toEntity())
        subtaskDao.deleteForTask(if (task.id == 0L) id else task.id)
        task.subtasks.forEach { st ->
            subtaskDao.upsert(st.copy(taskId = id).toEntity())
        }
        return id
    }

    suspend fun toggleComplete(id: Long) {
        val current = taskDao.observeById(id).first() ?: return
        val next = if (current.status == TaskStatus.Completed) TaskStatus.Pending else TaskStatus.Completed
        taskDao.updateStatus(id, next)
    }

    suspend fun updateStatus(id: Long, status: TaskStatus) = taskDao.updateStatus(id, status)
    suspend fun updatePriority(id: Long, p: Priority) = taskDao.updatePriority(id, p)
    suspend fun delete(id: Long) = taskDao.softDelete(id)
    suspend fun restore(id: Long) = taskDao.restore(id)
    suspend fun clearCompleted() = taskDao.getAll()
        .filter { it.status == TaskStatus.Completed }
        .forEach { taskDao.softDelete(it.id) }
    suspend fun clearAll() = taskDao.clearAll()
}

fun TaskEntity.toDomain() = Task(
    id = id, title = title, description = description, category = category,
    tags = tags, priority = priority, status = status, startDate = startDate,
    dueDate = dueDate, dueTime = dueTime, reminderEnabled = reminderEnabled,
    reminderAt = reminderAt, recurrence = recurrence,
    recurrenceInterval = recurrenceInterval, notes = notes,
    attachments = attachments, createdAt = createdAt, updatedAt = updatedAt,
)

fun Task.toEntity() = TaskEntity(
    id = id, title = title, description = description, category = category,
    tags = tags, priority = priority, status = status, startDate = startDate,
    dueDate = dueDate, dueTime = dueTime, reminderEnabled = reminderEnabled,
    reminderAt = reminderAt, recurrence = recurrence,
    recurrenceInterval = recurrenceInterval, notes = notes,
    attachments = attachments, createdAt = createdAt, updatedAt = updatedAt,
)

fun SubtaskEntity.toDomain() = Subtask(id = id, taskId = taskId, title = title, isDone = isDone)
fun Subtask.toEntity() = SubtaskEntity(id = id, taskId = taskId, title = title, isDone = isDone)