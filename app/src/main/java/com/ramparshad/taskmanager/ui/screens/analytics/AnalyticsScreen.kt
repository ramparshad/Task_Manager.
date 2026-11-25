package com.taskmanager.ui.screens.analytics


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.*
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart

import com.ramparshad.taskmanager.domain.model.Priority
import com.ramparshad.taskmanager.ui.components.DashboardStatCard
import com.ramparshad.taskmanager.ui.screens.analytics.AnalyticsRange
import com.ramparshad.taskmanager.ui.screens.analytics.AnalyticsViewModel
import com.ramparshad.taskmanager.ui.screens.analytics.DailyPoint
import com.ramparshad.taskmanager.ui.theme.*
import kotlin.math.max

@Composable
fun AnalyticsScreen(vm: AnalyticsViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = PaddingValues(Dimens.lg, Dimens.sm, Dimens.lg, Dimens.xxxl),
        verticalArrangement = Arrangement.spacedBy(Dimens.md),
        modifier = Modifier.padding(top = 20.dp)
    ) {
        item {
            Text("Analytics", style = MaterialTheme.typography.headlineMedium)
            Text("Your productivity at a glance",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            SegmentedRangeSelector(current = state.range, onSelect = vm::setRange)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.md)) {
                DashboardStatCard("Created", state.totalCreated.toString(),
                    Icons.Filled.AddTask, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                DashboardStatCard("Done", state.completed.toString(),
                    Icons.Filled.CheckCircle, Success, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.md)) {
                DashboardStatCard("Pending", state.pending.toString(),
                    Icons.Filled.Pending, Warning, Modifier.weight(1f))
                DashboardStatCard("Overdue", state.overdue.toString(),
                    Icons.Filled.Warning, Error, Modifier.weight(1f))
            }
        }

        item {
            Surface(shape = Dimens.cardShape, color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(Dimens.lg)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Completion Rate",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f))
                        Text("${(state.completionRate * 100).toInt()}%",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(Dimens.sm))
                    LinearProgressIndicator(
                        progress = { state.completionRate },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }

        item {
            SectionCard("Daily Completions") {
                if (state.dailyPoints.isEmpty() || state.dailyPoints.all { it.completed == 0 }) {
                    Text("No completions in this range.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium)
                } else {
                    DailyCompletionChart(state.dailyPoints)
                }
            }
        }

        item {
            SectionCard("Task Distribution") {
                DonutChart(
                    segments = listOf(
                        "Pending" to state.pending to MaterialTheme.colorScheme.onSurfaceVariant,
                        "In Progress" to state.inProgress to MaterialTheme.colorScheme.primary,
                        "Completed" to state.completed to Success,
                        "Overdue" to state.overdue to Error,
                    ).filter { it.first.second > 0 },
                )
            }
        }

        item {
            SectionCard("Priority Distribution") {
                val total = max(1, state.priorityStats.sumOf { it.count })
                state.priorityStats.forEach { s ->
                    val color = when (s.priority) {
                        Priority.Low -> PriorityLow; Priority.Medium -> PriorityMedium
                        Priority.High -> PriorityHigh; Priority.Urgent -> PriorityUrgent
                    }
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(s.priority.name, modifier = Modifier.width(70.dp),
                            style = MaterialTheme.typography.labelMedium)
                        LinearProgressIndicator(
                            progress = { s.count.toFloat() / total },
                            modifier = Modifier.weight(1f).height(6.dp),
                            color = color,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Spacer(Modifier.width(Dimens.sm))
                        Text(s.count.toString(), style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.width(28.dp))
                    }
                }
            }
        }

        if (state.categoryStats.isNotEmpty()) {
            item {
                SectionCard("Category Performance") {
                    state.categoryStats.forEach { c ->
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 6.dp)) {
                            Text(c.category, modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium)
                            Text("${c.completed}/${c.total}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(Dimens.sm))
                            Text("${(c.rate * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        item {
            SectionCard("Insights") {
                InsightRow(Icons.Filled.Today, "Completed this week",
                    state.completedThisWeek.toString())
                InsightRow(Icons.Filled.TrendingUp, "Avg. daily completions",
                    String.format("%.1f", state.avgDailyCompletion))
                InsightRow(Icons.Filled.Star, "Most productive day",
                    state.mostProductiveDay)
                InsightRow(Icons.Filled.Warning, "Overdue tasks",
                    state.overdue.toString())
            }
        }
    }
}

@Composable
private fun SegmentedRangeSelector(current: AnalyticsRange, onSelect: (AnalyticsRange) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        AnalyticsRange.values().forEachIndexed { i, r ->
            SegmentedButton(
                selected = current == r,
                onClick = { onSelect(r) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = 4),
            ) { Text(r.label, style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = Dimens.cardShape, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(Dimens.lg)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Dimens.md))
            content()
        }
    }
}

@Composable
private fun DailyCompletionChart(points: List<DailyPoint>) {
    val producer = remember { CartesianChartModelProducer() }

    // Using LaunchedEffect to ensure data updates trigger chart refresh
    LaunchedEffect(points) {
        producer.runTransaction {
            // Key fix 1: Use columnModel, and pass named parameter y = for List
            columnModel {
                series(y = points.map { it.completed })
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            // Key fix 2: Use the default rememberColumnCartesianLayer() directly
            // Remove the complex ColumnProvider chain to avoid referencing non-existent internal classes
            rememberColumnCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(),
        ),
        modelProducer = producer,
        modifier = Modifier.fillMaxWidth().height(180.dp),
    )
}

@Composable
private fun DonutChart(segments: List<Pair<Pair<String, Int>, Color>>) {
    // Simplified stacked bar (donut charts typically need Canvas)
    val total = max(1, segments.sumOf { it.first.second })
    Surface(shape = Dimens.cardShape, color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(Dimens.md)) {
            Row(Modifier.fillMaxWidth().height(12.dp)) {
                segments.forEach { (pair, color) ->
                    Box(
                        Modifier.weight(pair.second.toFloat().coerceAtLeast(0.001f))
                            .fillMaxHeight()
                            .background(color),
                    )
                }
            }
            Spacer(Modifier.height(Dimens.md))
            segments.forEach { (pair, color) ->
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)) {
                    Box(Modifier.size(10.dp).background(color, MaterialTheme.shapes.extraSmall))
                    Spacer(Modifier.width(Dimens.sm))
                    Text(pair.first, style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f))
                    Text("${pair.second} (${(pair.second.toFloat() / total * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun InsightRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
        Icon(icon, null, modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(Dimens.sm))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface)
    }
}