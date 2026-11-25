package com.ramparshad.taskmanager.ui.screens.settings


import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramparshad.taskmanager.domain.model.Priority
import com.ramparshad.taskmanager.domain.model.SortOrder
import com.ramparshad.taskmanager.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var editProfile by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmClearAll by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(Dimens.lg, Dimens.sm, Dimens.lg, Dimens.xxxl),
        verticalArrangement = Arrangement.spacedBy(Dimens.md),
        modifier = Modifier.padding(top = 20.dp, bottom = 20.dp)
    ) {
        item { Text("Settings", style = MaterialTheme.typography.headlineMedium) }

        // Profile
        item {
            SettingsSection("Profile") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(56.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                (state.name.firstOrNull()?.uppercase() ?: "U"),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Spacer(Modifier.width(Dimens.md))
                    Column(Modifier.weight(1f)) {
                        Text(state.name.ifBlank { "Set your name" },
                            style = MaterialTheme.typography.titleMedium)
                        Text(state.email.ifBlank { "Add an email" },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { editProfile = true }) { Text("Edit") }
                }
            }
        }

        // Appearance
        item {
            SettingsSection("Appearance") {
                Text("Theme", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(Dimens.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                    ThemeMode.values().forEach { m ->
                        FilterChip(
                            selected = state.themeMode == m,
                            onClick = { vm.setTheme(m) },
                            label = { Text(m.name) },
                        )
                    }
                }
                Spacer(Modifier.height(Dimens.lg))
                Text("Accent color", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(Dimens.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.md)) {
                    Accents.all.forEach { c ->
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (state.accent == c) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape,
                                )
                                .clickable { vm.setAccent(c) },
                        )
                    }
                }
            }
        }

        // Notifications
        item {
            SettingsSection("Notifications") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable notifications", modifier = Modifier.weight(1f))
                    Switch(
                        checked = state.notifications,
                        onCheckedChange = vm::setNotifications,
                    )
                }
            }
        }

        // Task preferences
        item {
            SettingsSection("Task Preferences") {
                Text("Default priority", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(Dimens.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                    Priority.values().forEach { p ->
                        FilterChip(
                            selected = state.defaultPriority == p,
                            onClick = { vm.setDefaultPriority(p) },
                            label = { Text(p.name) },
                        )
                    }
                }
                Spacer(Modifier.height(Dimens.lg))
                Text("Default sort order", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(Dimens.sm))
                val labels = mapOf(
                    SortOrder.RecentlyCreated to "Recent",
                    SortOrder.DueDateEarliest to "Due ↑",
                    SortOrder.PriorityHighLow to "Priority",
                    SortOrder.Alphabetical to "A–Z",
                )
                labels.forEach { (order, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { vm.setDefaultSort(order) }
                            .padding(vertical = Dimens.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, modifier = Modifier.weight(1f))
                        RadioButton(
                            selected = state.defaultSort == order,
                            onClick = { vm.setDefaultSort(order) },
                        )
                    }
                }
            }
        }

        // Data
        item {
            SettingsSection("Data Management") {
                SettingsAction(Icons.Filled.FileUpload, "Export tasks") {
                    scope.launch {
                        val file = withContext(kotlinx.coroutines.Dispatchers.IO) { vm.exportData() }
                        if (file != null) {
                            val uri = FileProvider.getUriForFile(
                                ctx, "${ctx.packageName}.fileprovider", file,
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            ctx.startActivity(Intent.createChooser(intent, "Export tasks"))
                        }
                    }
                }
                SettingsAction(Icons.Filled.CleaningServices, "Clear completed tasks") {
                    confirmClear = true
                }
                SettingsAction(Icons.Filled.DeleteForever, "Clear all data",
                    tint = MaterialTheme.colorScheme.error) {
                    confirmClearAll = true
                }
            }
        }

        // About
        item {
            SettingsSection("About") {
                SettingsAction(Icons.Filled.Info, "App version") {}
                SettingsAction(Icons.Filled.Lock, "Privacy policy") {}
                SettingsAction(Icons.Filled.Description, "Terms & conditions") {}
                SettingsAction(Icons.Filled.Support, "Contact support") {}
            }
        }
    }

    if (editProfile) {
        ProfileEditDialog(
            initialName = state.name, initialEmail = state.email,
            onSave = { n, e -> vm.saveProfile(n, e); editProfile = false },
            onDismiss = { editProfile = false },
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear completed tasks?") },
            text = { Text("All completed tasks will be removed. Pending tasks will remain.") },
            confirmButton = {
                TextButton(onClick = { vm.clearCompleted(); confirmClear = false }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            title = { Text("Delete everything?") },
            text = { Text("All tasks will be permanently deleted. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.clearAll(); confirmClearAll = false }) {
                    Text("Delete all", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmClearAll = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = Dimens.sm))
        Surface(shape = Dimens.cardShape, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(Dimens.lg), content = content)
        }
    }
}

@Composable
private fun SettingsAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = tint)
        Spacer(Modifier.width(Dimens.md))
        Text(label, modifier = Modifier.weight(1f), color = tint)
        Icon(Icons.Filled.ChevronRight, null, Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProfileEditDialog(
    initialName: String, initialEmail: String,
    onSave: (String, String) -> Unit, onDismiss: () -> Unit,
) {
    var n by remember { mutableStateOf(initialName) }
    var e by remember { mutableStateOf(initialEmail) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                OutlinedTextField(n, { n = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(e, { e = it }, label = { Text("Email") }, singleLine = true)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(n.trim(), e.trim()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}