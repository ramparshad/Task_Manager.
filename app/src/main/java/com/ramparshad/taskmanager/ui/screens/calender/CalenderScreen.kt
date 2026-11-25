package com.ramparshad.taskmanager.ui.screens.calender


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramparshad.taskmanager.domain.model.Priority
import com.ramparshad.taskmanager.domain.model.Task
import com.ramparshad.taskmanager.ui.components.EmptyState
import com.ramparshad.taskmanager.ui.components.TaskCard
import com.ramparshad.taskmanager.ui.navigation.TaskEditorRoute
import com.ramparshad.taskmanager.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onOpenTask: (Long) -> Unit,
    onCreateTaskForDate: (Long) -> Unit,
    vm: CalendarViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val monthFmt = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(

                onClick = {
                    onCreateTaskForDate(state.selectedDate.timeInMillis)
                },

                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("New Task") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Dimens.lg, vertical = Dimens.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Calendar", style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.weight(1f))
                    FilledTonalButton(onClick = vm::goToToday) { Text("Today") }
                }
            }

            item {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.lg),
                ) {
                    CalendarView.values().forEachIndexed { i, v ->
                        SegmentedButton(
                            selected = state.view == v,
                            onClick = { vm.setView(v) },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = 3),
                        ) { Text(v.name) }
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Dimens.lg, vertical = Dimens.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = vm::previousMonth) {
                        Icon(Icons.Filled.ChevronLeft, "Previous")
                    }
                    Text(
                        monthFmt.format(state.focusedMonth.time),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = vm::nextMonth) {
                        Icon(Icons.Filled.ChevronRight, "Next")
                    }
                }
            }

            when (state.view) {
                CalendarView.Month -> {
                    item { MonthGrid(state, onSelect = vm::selectDate) }
                    item { SelectedDayHeader(state) }
                    val tasks = state.tasksForSelectedDate
                    if (tasks.isEmpty()) {
                        item {
                            EmptyState(
                                Icons.Filled.EventAvailable, "No tasks",
                                "Nothing scheduled for this day.",
                            )
                        }
                    } else {
                        items(tasks, key = { it.id }) { t ->
                            TaskCard(
                                task = t,
                                onToggleComplete = { vm.toggleComplete(t) },
                                onClick = { onOpenTask(t.id) },
                                modifier = Modifier.padding(horizontal = Dimens.lg, vertical = 4.dp),
                            )
                        }
                    }
                }
                CalendarView.Week -> {
                    item { WeekStrip(state, onSelect = vm::selectDate) }
                    item { SelectedDayHeader(state) }
                    val tasks = state.tasksForSelectedDate
                    if (tasks.isEmpty()) {
                        item {
                            EmptyState(Icons.Filled.EventAvailable, "No tasks",
                                "Nothing scheduled for this day.")
                        }
                    } else {
                        items(tasks, key = { it.id }) { t ->
                            TaskCard(
                                task = t,
                                onToggleComplete = { vm.toggleComplete(t) },
                                onClick = { onOpenTask(t.id) },
                                modifier = Modifier.padding(horizontal = Dimens.lg, vertical = 4.dp),
                            )
                        }
                    }
                }
                CalendarView.Day -> {
                    item { SelectedDayHeader(state) }
                    val tasks = state.tasksForSelectedDate
                    if (tasks.isEmpty()) {
                        item {
                            EmptyState(Icons.Filled.EventAvailable, "Day is clear",
                                "No tasks scheduled on this day.")
                        }
                    } else {
                        items(tasks, key = { it.id }) { t ->
                            DayTimelineRow(t, onToggle = { vm.toggleComplete(t) },
                                onClick = { onOpenTask(t.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(state: CalendarUiState, onSelect: (Long) -> Unit) {
    val monthCal = state.focusedMonth
    val firstDay = Calendar.getInstance().apply {
        timeInMillis = monthCal.timeInMillis
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val startOffset = (firstDay.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday first
    val daysInMonth = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH)
    val todayStart = startOfDayMillis(System.currentTimeMillis())
    val selectedStart = startOfDayMillis(state.selectedDate.timeInMillis)

    Column(Modifier.padding(horizontal = Dimens.lg)) {
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(Dimens.sm))

        var day = 1
        val rows = ((startOffset + daysInMonth + 6) / 7)
        repeat(rows) { r ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { c ->
                    val cellIndex = r * 7 + c
                    if (cellIndex < startOffset || day > daysInMonth) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val thisDay = day
                        val cellCal = Calendar.getInstance().apply {
                            timeInMillis = firstDay.timeInMillis
                            set(Calendar.DAY_OF_MONTH, thisDay)
                        }
                        val cellMillis = cellCal.timeInMillis
                        val dayTasks = state.tasksByDay[startOfDayMillis(cellMillis)].orEmpty()
                        val isSelected = cellMillis == selectedStart
                        val isToday = cellMillis == todayStart
                        DayCell(
                            day = thisDay,
                            tasks = dayTasks,
                            isSelected = isSelected,
                            isToday = isToday,
                            onClick = { onSelect(cellMillis) },
                            modifier = Modifier.weight(1f),
                        )
                        day++
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    tasks: List<Task>,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    val fg = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(day.toString(), style = MaterialTheme.typography.bodyMedium, color = fg)
            if (tasks.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    tasks.take(3).forEach { t ->
                        Box(
                            Modifier.size(4.dp).clip(CircleShape).background(
                                if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else priorityColor(t.priority),
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekStrip(state: CalendarUiState, onSelect: (Long) -> Unit) {
    val selCal = state.selectedDate
    val weekStart = Calendar.getInstance().apply {
        timeInMillis = selCal.timeInMillis
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    }
    val days = (0..6).map {
        Calendar.getInstance().apply {
            timeInMillis = weekStart.timeInMillis
            add(Calendar.DAY_OF_MONTH, it)
        }
    }
    val dayFmt = SimpleDateFormat("EEE", Locale.getDefault())
    val todayStart = startOfDayMillis(System.currentTimeMillis())

    LazyRow(
        contentPadding = PaddingValues(horizontal = Dimens.lg),
        horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
    ) {
        items(days) { d ->
            val millis = startOfDayMillis(d.timeInMillis)
            val isSelected = millis == startOfDayMillis(state.selectedDate.timeInMillis)
            val isToday = millis == todayStart
            val dayTasks = state.weekTasks[millis].orEmpty()
            Surface(
                onClick = { onSelect(millis) },
                shape = Dimens.cardShape,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(60.dp),
            ) {
                Column(
                    Modifier.padding(vertical = Dimens.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        dayFmt.format(d.time),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        d.get(Calendar.DAY_OF_MONTH).toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    if (dayTasks.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            dayTasks.take(3).forEach {
                                Box(
                                    Modifier.size(4.dp).clip(CircleShape).background(
                                        if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else priorityColor(it.priority),
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDayHeader(state: CalendarUiState) {
    val fmt = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Dimens.lg, vertical = Dimens.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(fmt.format(state.selectedDate.time), style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f))
        Text(
            "${state.tasksForSelectedDate.size} task${if (state.tasksForSelectedDate.size == 1) "" else "s"}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DayTimelineRow(task: Task, onToggle: () -> Unit, onClick: () -> Unit) {
    val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Dimens.lg, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(60.dp), horizontalAlignment = Alignment.End) {
            Text(
                task.dueTime?.let { timeFmt.format(Date(it)) } ?: "—",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }



       // Spacer(Modifier.width(Dimens.md))
       // Column(Modifier.width(3.dp).height(44.dp).background(priorityColor(task.priority), CircleShape))
        Spacer(Modifier.width(Dimens.md))
        TaskCard(task = task, onToggleComplete = onToggle, onClick = onClick,
            modifier = Modifier.weight(1f))
    }
}

private fun priorityColor(p: Priority): Color = when (p) {
    Priority.Low -> PriorityLow
    Priority.Medium -> PriorityMedium
    Priority.High -> PriorityHigh
    Priority.Urgent -> PriorityUrgent
}

private fun startOfDayMillis(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis