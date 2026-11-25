package com.ramparshad.taskmanager.ui.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramparshad.taskmanager.data.repository.TaskRepository
import com.ramparshad.taskmanager.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskDetailsViewModel @Inject constructor(
    private val repo: TaskRepository,
) : ViewModel() {

    private val _task = MutableStateFlow<Task?>(null)
    val task: StateFlow<Task?> = _task.asStateFlow()

    // This method is called directly by the Screen
    fun loadTask(id: Long) {
        if (_task.value?.id == id) return
        viewModelScope.launch {
            repo.observeById(id).collect { loadedTask ->
                _task.value = loadedTask
            }
        }
    }

    fun toggleComplete(t: Task) = viewModelScope.launch {
        repo.updateStatus(
            t.id,
            if (t.status == TaskStatus.Completed) TaskStatus.Pending else TaskStatus.Completed,
        )
    }

    fun changeStatus(t: Task, s: TaskStatus) = viewModelScope.launch { repo.updateStatus(t.id, s) }
    fun changePriority(t: Task, p: Priority) = viewModelScope.launch { repo.updatePriority(t.id, p) }
    fun delete(taskId: Long) = viewModelScope.launch { repo.delete(taskId) }

    fun toggleSubtask(task: Task, sub: Subtask) = viewModelScope.launch {
        val updated = task.copy(
            subtasks = task.subtasks.map { if (it.id == sub.id) it.copy(isDone = !it.isDone) else it },
            updatedAt = System.currentTimeMillis(),
        )
        repo.save(updated)
    }
}