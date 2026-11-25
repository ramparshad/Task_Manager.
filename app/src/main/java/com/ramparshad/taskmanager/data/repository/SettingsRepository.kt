package com.ramparshad.taskmanager.data.repository


import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.ramparshad.taskmanager.domain.model.*
import com.ramparshad.taskmanager.ui.theme.Accents
import com.ramparshad.taskmanager.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext
    private val context: Context) {
    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val ACCENT = intPreferencesKey("accent")
        val DEFAULT_PRIORITY = stringPreferencesKey("default_priority")
        val DEFAULT_SORT = stringPreferencesKey("default_sort")
        val NOTIFICATIONS = booleanPreferencesKey("notifications")
        val NAME = stringPreferencesKey("name")
        val EMAIL = stringPreferencesKey("email")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map {
        runCatching { ThemeMode.valueOf(it[Keys.THEME] ?: "System") }.getOrDefault(ThemeMode.System)
    }
    val accentColor: Flow<Color> = context.dataStore.data.map {
        Color(it[Keys.ACCENT] ?: Accents.Indigo.toArgb())
    }
    val defaultPriority: Flow<Priority> = context.dataStore.data.map {
        runCatching { Priority.valueOf(it[Keys.DEFAULT_PRIORITY] ?: "Medium") }.getOrDefault(Priority.Medium)
    }
    val defaultSort: Flow<SortOrder> = context.dataStore.data.map {
        runCatching { SortOrder.valueOf(it[Keys.DEFAULT_SORT] ?: "RecentlyCreated") }.getOrDefault(SortOrder.RecentlyCreated)
    }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.NOTIFICATIONS] ?: true }
    val name: Flow<String> = context.dataStore.data.map { it[Keys.NAME] ?: "" }
    val email: Flow<String> = context.dataStore.data.map { it[Keys.EMAIL] ?: "" }

    suspend fun setTheme(m: ThemeMode) = context.dataStore.edit { it[Keys.THEME] = m.name }
    suspend fun setAccent(c: Color) = context.dataStore.edit { it[Keys.ACCENT] = c.toArgb() }
    suspend fun setDefaultPriority(p: Priority) = context.dataStore.edit { it[Keys.DEFAULT_PRIORITY] = p.name }
    suspend fun setDefaultSort(s: SortOrder) = context.dataStore.edit { it[Keys.DEFAULT_SORT] = s.name }
    suspend fun setNotifications(b: Boolean) = context.dataStore.edit { it[Keys.NOTIFICATIONS] = b }
    suspend fun setProfile(name: String, email: String) = context.dataStore.edit {
        it[Keys.NAME] = name; it[Keys.EMAIL] = email
    }
}