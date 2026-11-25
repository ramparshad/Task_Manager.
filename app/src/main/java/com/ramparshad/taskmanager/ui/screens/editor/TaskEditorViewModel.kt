package com.ramparshad.taskmanager.ui.screens.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramparshad.taskmanager.data.reminder.ReminderWorker
import com.ramparshad.taskmanager.data.repository.TaskRepository
import com.ramparshad.taskmanager.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context

data class EditorUiState(
    val taskId: Long = 0L,
    val title: String = "",
    val description: String = "",
    val category: String = "General",
    val tags: List<String> = emptyList(),
    val newTag: String = "",
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
    val subtasks: List<Subtask> = emptyList(),
    val newSubtask: String = "",
    val titleError: String? = null,
    val dateError: String? = null,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
)

@HiltViewModel
class TaskEditorViewModel @Inject constructor(
    private val repo: TaskRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(EditorUiState())
    val state = _state.asStateFlow()

    // Call this from the UI when the screen opens
    fun loadTask(id: Long, prefillDate: Long?) {
        // 1. Clear any old data immediately so it's fresh
        _state.value = EditorUiState()

        if (id != 0L) {
            viewModelScope.launch {
                repo.observeById(id).filterNotNull().first().let { t ->
                    _state.value = EditorUiState(
                        taskId = t.id, title = t.title, description = t.description,
                        category = t.category, tags = t.tags, priority = t.priority,
                        status = t.status, startDate = t.startDate, dueDate = t.dueDate,
                        dueTime = t.dueTime, reminderEnabled = t.reminderEnabled,
                        reminderAt = t.reminderAt, recurrence = t.recurrence,
                        recurrenceInterval = t.recurrenceInterval, notes = t.notes,
                        attachments = t.attachments, subtasks = t.subtasks,
                        isEditing = true,
                    )
                }
            }
        } else if (prefillDate != null) {
            _state.update { it.copy(dueDate = prefillDate) }
        }
    }

    fun updateTitle(v: String) = _state.update { it.copy(title = v, titleError = null) }
    fun updateDescription(v: String) = _state.update { it.copy(description = v) }
    fun updateCategory(v: String) = _state.update { it.copy(category = v) }
    fun updatePriority(v: Priority) = _state.update { it.copy(priority = v) }
    fun updateStatus(v: TaskStatus) = _state.update { it.copy(status = v) }
    fun updateStartDate(v: Long?) = _state.update { it.copy(startDate = v) }
    fun updateDueDate(v: Long?) = _state.update { it.copy(dueDate = v, dateError = null) }
    fun updateDueTime(v: Long?) = _state.update { it.copy(dueTime = v) }
    fun updateReminderEnabled(v: Boolean) = _state.update {
        it.copy(reminderEnabled = v, reminderAt = if (!v) null else it.reminderAt)
    }
    fun updateReminderAt(v: Long?) = _state.update { it.copy(reminderAt = v) }
    fun updateRecurrence(v: RecurrenceType) = _state.update { it.copy(recurrence = v) }
    fun updateRecurrenceInterval(v: Int) = _state.update { it.copy(recurrenceInterval = v.coerceAtLeast(1)) }
    fun updateNotes(v: String) = _state.update { it.copy(notes = v) }

    fun updateNewTag(v: String) = _state.update { it.copy(newTag = v) }
    fun addTag() = _state.update {
        val tag = it.newTag.trim()
        if (tag.isBlank() || tag in it.tags) it
        else it.copy(tags = it.tags + tag, newTag = "")
    }
    fun removeTag(tag: String) = _state.update { it.copy(tags = it.tags - tag) }

    fun updateNewSubtask(v: String) = _state.update { it.copy(newSubtask = v) }
    fun addSubtask() = _state.update {
        val title = it.newSubtask.trim()
        if (title.isBlank()) it
        else it.copy(
            subtasks = it.subtasks + Subtask(id = 0, taskId = it.taskId, title = title),
            newSubtask = "",
        )
    }
    fun toggleSubtask(id: Long) = _state.update {
        it.copy(subtasks = it.subtasks.map { s ->
            if (s.id == id) s.copy(isDone = !s.isDone) else s
        })
    }
    fun removeSubtask(id: Long) = _state.update {
        it.copy(subtasks = it.subtasks.filterNot { s -> s.id == id })
    }
    fun addAttachment(uri: String) = _state.update { it.copy(attachments = it.attachments + uri) }
    fun removeAttachment(uri: String) = _state.update { it.copy(attachments = it.attachments - uri) }

    fun save(onDone: (Long) -> Unit) = viewModelScope.launch {
        val s = _state.value
        if (s.title.isBlank()) {
            _state.update { it.copy(titleError = "Title is required") }
            return@launch
        }
        if (s.startDate != null && s.dueDate != null && s.startDate > s.dueDate) {
            _state.update { it.copy(dateError = "Due date must be after start date") }
            return@launch
        }
        _state.update { it.copy(isSaving = true) }
        val task = Task(
            id = s.taskId, title = s.title.trim(), description = s.description,
            category = s.category.ifBlank { "General" }, tags = s.tags,
            priority = s.priority, status = s.status, startDate = s.startDate,
            dueDate = s.dueDate, dueTime = s.dueTime, reminderEnabled = s.reminderEnabled,
            reminderAt = s.reminderAt, recurrence = s.recurrence,
            recurrenceInterval = s.recurrenceInterval, notes = s.notes,
            attachments = s.attachments,
            createdAt = if (s.taskId == 0L) System.currentTimeMillis() else s.startDate ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(), subtasks = s.subtasks,
        )
        val id = repo.save(task)

        if (s.reminderEnabled && s.reminderAt != null && s.reminderAt > System.currentTimeMillis()) {
            ReminderWorker.schedule(
                ctx = appContext,
                taskId = id,
                title = "Task reminder: ${s.title}",
                body = s.description.ifBlank { "Due soon" },
                atMillis = s.reminderAt,
            )
        }

        _state.update { it.copy(isSaving = false, saved = true, taskId = id) }
        onDone(id)
    }
}