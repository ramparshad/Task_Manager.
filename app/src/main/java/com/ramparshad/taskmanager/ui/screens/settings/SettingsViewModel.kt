package com.ramparshad.taskmanager.ui.screens.settings


import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramparshad.taskmanager.data.repository.SettingsRepository
import com.ramparshad.taskmanager.data.repository.TaskRepository
import com.ramparshad.taskmanager.domain.model.Priority
import com.ramparshad.taskmanager.domain.model.SortOrder
import com.ramparshad.taskmanager.ui.theme.Accents
import com.ramparshad.taskmanager.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.System,
    val accent: androidx.compose.ui.graphics.Color = Accents.Indigo,
    val notifications: Boolean = true,
    val defaultPriority: Priority = Priority.Medium,
    val defaultSort: SortOrder = SortOrder.RecentlyCreated,
    val name: String = "",
    val email: String = "",
    val appVersion: String = "1.0.0",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val tasks: TaskRepository,
    @ApplicationContext private val ctx: Context,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.System)
    val accentColor: StateFlow<androidx.compose.ui.graphics.Color> = settings.accentColor
        .stateIn(viewModelScope, SharingStarted.Eagerly, Accents.Indigo)

    val uiState: StateFlow<SettingsUiState> = combine(
        settings.themeMode, settings.accentColor, settings.notificationsEnabled,
        settings.defaultPriority, settings.defaultSort, settings.name, settings.email,
    ) { values ->
        SettingsUiState(
            themeMode = values[0] as ThemeMode,
            accent = values[1] as androidx.compose.ui.graphics.Color,
            notifications = values[2] as Boolean,
            defaultPriority = values[3] as Priority,
            defaultSort = values[4] as SortOrder,
            name = values[5] as String,
            email = values[6] as String,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTheme(m: ThemeMode) = viewModelScope.launch { settings.setTheme(m) }
    fun setAccent(c: androidx.compose.ui.graphics.Color) = viewModelScope.launch { settings.setAccent(c) }
    fun setNotifications(b: Boolean) = viewModelScope.launch { settings.setNotifications(b) }
    fun setDefaultPriority(p: Priority) = viewModelScope.launch { settings.setDefaultPriority(p) }
    fun setDefaultSort(s: SortOrder) = viewModelScope.launch { settings.setDefaultSort(s) }
    fun saveProfile(name: String, email: String) = viewModelScope.launch { settings.setProfile(name, email) }

    fun clearCompleted() = viewModelScope.launch { tasks.clearCompleted() }
    fun clearAll() = viewModelScope.launch { tasks.clearAll() }

    // Simple local JSON export — writes to app-private cache dir, returns File for sharing
    fun exportData(): File? = runCatching {
        val all = kotlinx.coroutines.runBlocking { tasks.observeAll().first() }
        val json = kotlinx.serialization.json.Json { prettyPrint = true }
        val file = File(ctx.cacheDir, "tasks_export_${System.currentTimeMillis()}.json")
        file.writeText(json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(ExportTask.serializer()),
            all.map { ExportTask.from(it) },
        ))
        file
    }.getOrNull()

    @kotlinx.serialization.Serializable
    data class ExportTask(
        val id: Long, val title: String, val description: String,
        val category: String, val priority: String, val status: String,
        val dueDate: Long?, val createdAt: Long,
    ) {
        companion object {
            fun from(t: com.ramparshad.taskmanager.domain.model.Task) = ExportTask(
                t.id, t.title, t.description, t.category, t.priority.name,
                t.status.name, t.dueDate, t.createdAt,
            )
        }
    }
}