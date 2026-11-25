package com.ramparshad.taskmanager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.ramparshad.taskmanager.ui.navigation.AnalyticsRoute
import com.ramparshad.taskmanager.ui.navigation.AppShell
import com.ramparshad.taskmanager.ui.navigation.CalendarRoute
import com.ramparshad.taskmanager.ui.navigation.HomeRoute
import com.ramparshad.taskmanager.ui.navigation.NavigationState
import com.ramparshad.taskmanager.ui.navigation.SettingsRoute
import com.ramparshad.taskmanager.ui.navigation.TaskDetailsRoute
import com.ramparshad.taskmanager.ui.navigation.TaskEditorRoute
import com.ramparshad.taskmanager.ui.navigation.TasksRoute
import com.ramparshad.taskmanager.ui.navigation.rememberNavigationState
import com.ramparshad.taskmanager.ui.screens.calender.CalendarScreen
import com.ramparshad.taskmanager.ui.screens.details.TaskDetailsScreen
import com.ramparshad.taskmanager.ui.screens.editor.TaskEditorScreen
import com.ramparshad.taskmanager.ui.screens.home.HomeScreen
import com.ramparshad.taskmanager.ui.screens.settings.SettingsScreen
import com.ramparshad.taskmanager.ui.screens.settings.SettingsViewModel
import com.ramparshad.taskmanager.ui.screens.tasks.TasksScreen
import com.ramparshad.taskmanager.ui.theme.AppTheme
import com.taskmanager.ui.screens.analytics.AnalyticsScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best-effort */ }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val settingsVm: SettingsViewModel = hiltViewModel()
            val themeMode by settingsVm.themeMode.collectAsState()
            val accent by settingsVm.accentColor.collectAsState()

            AppTheme(themeMode = themeMode, accentColor = accent) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppRoot()
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun AppRoot() {
    val navState: NavigationState = rememberNavigationState(HomeRoute)

    AppShell(
        navState = navState,
        onAddTask = { navState.push(TaskEditorRoute()) },
    ) { _ ->
        NavDisplay(
            backStack = navState.backStack,
            onBack = { navState.pop() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator()
            ),
            entryProvider = entryProvider {

                // ----- Home -----
                entry<HomeRoute> {
                    HomeScreen(
                        onOpenTask = { id -> navState.push(TaskDetailsRoute(id)) },
                        onSeeAllTasks = { navState.resetToTop(TasksRoute) },
                    )
                }

                // ----- Tasks -----
                entry<TasksRoute> {
                    TasksScreen(
                        onOpenTask = { id -> navState.push(TaskDetailsRoute(id)) },
                        onCreateTask = { navState.push(TaskEditorRoute()) },
                    )
                }

                // ----- Calendar -----
                entry<CalendarRoute> {
                    CalendarScreen(
                        onOpenTask = { id -> navState.push(TaskDetailsRoute(id)) },
                        onCreateTaskForDate = { millis ->
                            navState.push(TaskEditorRoute(prefillDate = millis))
                        },
                    )
                }

                // ----- Analytics -----
                entry<AnalyticsRoute> {
                    AnalyticsScreen()
                }

                // ----- Settings -----
                entry<SettingsRoute> {
                    SettingsScreen()
                }

                // ----- Task Editor (create or edit) -----
                entry<TaskEditorRoute> { key ->
                    TaskEditorScreen(
                        taskId = key.taskId,
                        prefillDate = key.prefillDate,
                        onSaved = { id ->
                            navState.pop()
                            navState.push(TaskDetailsRoute(id))
                        },
                        onCancel = { navState.pop() },
                    )
                }

                // ----- Task Details -----
                entry<TaskDetailsRoute> { key ->
                    TaskDetailsScreen(
                        taskId = key.taskId,
                        onEdit = { id -> navState.push(TaskEditorRoute(taskId = id)) },
                        onBack = { navState.pop() },
                    )
                }
            },
        )
    }
}


