package com.ramparshad.taskmanager.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramparshad.taskmanager.ui.components.*
import com.ramparshad.taskmanager.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    onOpenTask: (Long) -> Unit,
    onSeeAllTasks: () -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LazyColumn(
        contentPadding = PaddingValues(Dimens.lg),
        verticalArrangement = Arrangement.spacedBy(Dimens.md),
        modifier = Modifier.padding(top = 20.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        state.greeting,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { Toast.makeText(context, "coming soon", Toast.LENGTH_SHORT).show() }) {
                    BadgedBox(badge = { Badge() }) {
                        Icon(Icons.Filled.Notifications, "Notifications")
                    }
                }
                Spacer(Modifier.width(Dimens.sm))
                Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(
                        Icons.Filled.Person, "Profile",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(20.dp),
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.md)) {
                DashboardStatCard("Total", state.totalTasks.toString(), Icons.Filled.List, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                DashboardStatCard("Done", state.completed.toString(), Icons.Filled.CheckCircle, Success, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.md)) {
                DashboardStatCard("Pending", state.pending.toString(), Icons.Filled.Pending, Warning, Modifier.weight(1f))
                DashboardStatCard("Overdue", state.overdue.toString(), Icons.Filled.Warning, Error, Modifier.weight(1f))
            }
        }

        item {
            Surface(shape = Dimens.cardShape, color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(Dimens.lg)) {
                    Text("Daily Progress", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Dimens.md))
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Spacer(Modifier.height(Dimens.sm))
                    Row {
                        Text(
                            "${(state.progress * 100).toInt()}% completed",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            state.motivation,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimens.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Today's Tasks", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = onSeeAllTasks) { Text("See all") }
            }
        }
        if (state.todayTasks.isEmpty()) {
            item {
                EmptyState(
                    Icons.Filled.CheckCircle, "Nothing scheduled",
                    "Enjoy your free time, or add a task to get going.",
                )
            }
        } else {
            items(state.todayTasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onToggleComplete = { vm.toggleComplete(task.id) },
                    onClick = { onOpenTask(task.id) },
                )
            }
        }

        if (state.upcomingTasks.isNotEmpty()) {
            item {
                Text(
                    "Upcoming",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = Dimens.md),
                )
            }
            items(state.upcomingTasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onToggleComplete = { vm.toggleComplete(task.id) },
                    onClick = { onOpenTask(task.id) },
                )
            }
        }
    }
}