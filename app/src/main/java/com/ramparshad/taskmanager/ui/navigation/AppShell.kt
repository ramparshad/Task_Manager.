package com.ramparshad.taskmanager.ui.navigation


import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.window.core.layout.WindowWidthSizeClass
import com.ramparshad.taskmanager.ui.theme.Dimens

data class NavItem(
    val route: NavKey,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val navItems = listOf(
    NavItem(HomeRoute, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    NavItem(TasksRoute, "Tasks", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle),
    NavItem(CalendarRoute, "Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    NavItem(AnalyticsRoute, "Analytics", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    NavItem(SettingsRoute, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
fun AppShell(
    navState: NavigationState,
    onAddTask: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val windowClass = currentWindowAdaptiveInfo().windowSizeClass
    val showRail = windowClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT
    val showFab = navState.backStack.lastOrNull() in primaryDestinations

    Row(Modifier.fillMaxSize()) {
        if (showRail) {
            NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Spacer(Modifier.height(Dimens.md))
                navItems.forEach { item ->
                    val selected = navState.backStack.lastOrNull() == item.route
                    NavigationRailItem(
                        selected = selected,
                        onClick = { navState.resetToTop(item.route) },
                        icon = {
                            Icon(
                                if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                            )
                        },
                        label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
                Spacer(Modifier.weight(1f))
                if (showFab) {
                    SmallFloatingActionButton(
                        onClick = onAddTask,
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = Dimens.lg),
                    ) { Icon(Icons.Filled.Add, "Add task") }
                }
            }
        }

        Scaffold(
            modifier = Modifier.weight(1f),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!showRail && navState.backStack.lastOrNull() in primaryDestinations) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        navItems.forEach { item ->
                            val selected = navState.backStack.lastOrNull() == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navState.resetToTop(item.route) },
                                icon = {
                                    Icon(
                                        if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label,
                                    )
                                },
                                label = { Text(item.label) },
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (showFab && !showRail) {
                    FloatingActionButton(
                        onClick = onAddTask,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) { Icon(Icons.Filled.Add, "Add task") }
                }
            },
            content = content,
        )
    }
}