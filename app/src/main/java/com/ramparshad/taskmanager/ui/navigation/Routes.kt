package com.ramparshad.taskmanager.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute : NavKey
@Serializable data object TasksRoute : NavKey
@Serializable data object CalendarRoute : NavKey
@Serializable data object AnalyticsRoute : NavKey
@Serializable data object SettingsRoute : NavKey

@Serializable data class TaskEditorRoute(
    val taskId: Long = 0L,
    val prefillDate: Long? = null,
) : NavKey

// Kept as Long. No changes needed anywhere else.
@Serializable data class TaskDetailsRoute(val taskId: Long) : NavKey

data class NavigationState(val backStack: SnapshotStateList<NavKey>) {
    fun push(route: NavKey) = backStack.add(route)
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun pop() { if (backStack.size > 1) backStack.removeLast() }
    fun resetToTop(route: NavKey) {
        backStack.clear()
        backStack.add(route)
    }
}

fun rememberNavigationState(start: NavKey): NavigationState =
    NavigationState(mutableStateListOf(start))

val primaryDestinations = listOf(
    HomeRoute, TasksRoute, CalendarRoute, AnalyticsRoute, SettingsRoute,
)