/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.alarm

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vedicmitra.core.astronomy.MuhurtaQuality
import io.github.vedicmitra.core.common.model.AlertStyle
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Instant

/**
 * Reminders screen entry point. Resolves location + notification permissions, drives
 * [AlarmViewModel.load], and renders the list of **added** reminders plus an "add reminder" picker.
 * The stateless [AlarmContent] takes state and callbacks so it is trivially previewable.
 */
@Composable
fun AlarmScreen(
    modifier: Modifier = Modifier,
    viewModel: AlarmViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            // Reload regardless of the result — the ViewModel falls back to a default location and
            // the notifier degrades gracefully if notifications remain disabled.
            viewModel.load()
        }

    LaunchedEffect(Unit) {
        val required = requiredPermissions()
        if (required.all { context.isGranted(it) }) {
            viewModel.load()
        } else {
            permissionLauncher.launch(required.toTypedArray())
        }
    }

    AlarmContent(
        uiState = uiState,
        onAdd = viewModel::addReminder,
        onAddTithi = viewModel::addTithiReminder,
        onRemove = viewModel::removeReminder,
        onRename = viewModel::renameReminder,
        onOffsetChange = viewModel::setOffsetMinutes,
        onAlertChange = viewModel::setAlertType,
        onRequestExactAlarm = context::openExactAlarmSettings,
        modifier = modifier,
    )
}

@Composable
private fun AlarmContent(
    uiState: AlarmUiState,
    onAdd: (String) -> Unit,
    onAddTithi: (TithiTarget) -> Unit,
    onRemove: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onOffsetChange: (String, Int) -> Unit,
    onAlertChange: (String, AlertStyle) -> Unit,
    onRequestExactAlarm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        AlarmUiState.Loading -> CenteredBox(modifier) { CircularProgressIndicator() }
        is AlarmUiState.Error ->
            CenteredBox(modifier) {
                Text(text = uiState.message, style = MaterialTheme.typography.bodyLarge)
            }

        is AlarmUiState.Ready -> {
            val fullScreenContext = LocalContext.current
            val needsFullScreenIntent = !fullScreenContext.canUseFullScreenIntent()
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!uiState.canScheduleExactAlarms) {
                    item { ExactAlarmBanner(onRequestExactAlarm = onRequestExactAlarm) }
                }
                if (needsFullScreenIntent) {
                    item {
                        FullScreenIntentBanner(
                            onRequestFullScreenIntent = fullScreenContext::openFullScreenIntentSettings,
                        )
                    }
                }
                if (uiState.usingDefaultLocation) {
                    item {
                        Text(
                            text = "Showing New Delhi — grant location access for your area.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AddReminderButton(available = uiState.available, onAdd = onAdd)
                        AddEventButton(onAddTithi = onAddTithi)
                    }
                }
                if (uiState.reminders.isEmpty()) {
                    item {
                        Text(
                            text = "No reminders yet — add one above.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                items(items = uiState.reminders, key = { it.id }) { item ->
                    ReminderCard(item, onRemove, onRename, onOffsetChange, onAlertChange)
                }
            }
        }
    }
}

@Composable
private fun AddReminderButton(
    available: List<SourceOption>,
    onAdd: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { open = true }, enabled = available.isNotEmpty()) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = if (available.isEmpty()) "All periods added" else "Add reminder")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            val onSelect: (String) -> Unit = { key ->
                onAdd(key)
                open = false
            }
            SourceSection("Auspicious", available.filter { it.quality == MuhurtaQuality.AUSPICIOUS }, onSelect)
            SourceSection("Inauspicious", available.filterNot { it.quality == MuhurtaQuality.AUSPICIOUS }, onSelect)
        }
    }
}

@Composable
private fun ColumnScope.SourceSection(
    title: String,
    options: List<SourceOption>,
    onSelect: (String) -> Unit,
) {
    if (options.isEmpty()) return
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
    )
    options.forEach { option ->
        DropdownMenuItem(text = { Text(option.label) }, onClick = { onSelect(option.key) })
    }
}

@Composable
private fun AddEventButton(onAddTithi: (TithiTarget) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Tithi event")
    }
    if (open) {
        TithiPickerDialog(
            onDismiss = { open = false },
            onConfirm = { target ->
                onAddTithi(target)
                open = false
            },
        )
    }
}

private val MAASA_LABELS = listOf("Every month") + MAASA_OPTIONS
private val PAKSHA_LABELS = listOf("Shukla (waxing)", "Krishna (waning)", "Either fortnight")

@Composable
private fun TithiPickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (TithiTarget) -> Unit,
) {
    var maasaIndex by remember { mutableStateOf(0) }
    var pakshaIndex by remember { mutableStateOf(0) }
    var tithiIndex by remember { mutableStateOf(0) }
    val paksha = PickPaksha.entries[pakshaIndex]

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add a tithi reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Common events", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TITHI_PRESETS.forEach { preset ->
                        AssistChip(onClick = { onConfirm(preset) }, label = { Text(preset.eventName) })
                    }
                }
                HorizontalDivider()
                Text(text = "Or build your own", style = MaterialTheme.typography.labelMedium)
                LabeledDropdown("Month", MAASA_LABELS, maasaIndex) { maasaIndex = it }
                LabeledDropdown("Fortnight", PAKSHA_LABELS, pakshaIndex) { pakshaIndex = it }
                LabeledDropdown("Tithi", pickerTithiLabels(shukla = paksha == PickPaksha.SHUKLA), tithiIndex) {
                    tithiIndex = it
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val maasa = MAASA_OPTIONS.getOrNull(maasaIndex - 1)
                    onConfirm(TithiTarget.custom(maasa, paksha, tithiIndex + 1))
                },
            ) { Text(text = "Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(text = "Cancel") } },
    )
}

@Composable
private fun LabeledDropdown(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box {
            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
                Text(text = options.getOrElse(selectedIndex) { "" }, modifier = Modifier.weight(1f))
                Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(index)
                            open = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderCard(
    item: ReminderItem,
    onRemove: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onOffsetChange: (String, Int) -> Unit,
    onAlertChange: (String, AlertStyle) -> Unit,
) {
    var renaming by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.displayName, style = MaterialTheme.typography.titleMedium)
                    val subtitle =
                        item.dateLabel ?: run {
                            val whenLabel = if (item.isTomorrow) "Tomorrow · " else ""
                            "$whenLabel${formatTime(item.start)}–${formatTime(item.end)} · ${item.quality.label}"
                        }
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { renaming = true }) {
                    Icon(imageVector = Icons.Filled.Edit, contentDescription = "Rename reminder")
                }
                IconButton(onClick = { onRemove(item.id) }) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Remove reminder")
                }
            }
            OffsetEditor(
                itemId = item.id,
                offsetMinutes = item.offsetMinutes,
                onOffsetChange = onOffsetChange,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = item.alertType == AlertStyle.NOTIFICATION,
                    onClick = { onAlertChange(item.id, AlertStyle.NOTIFICATION) },
                    label = { Text("Notify") },
                )
                FilterChip(
                    selected = item.alertType == AlertStyle.ALARM,
                    onClick = { onAlertChange(item.id, AlertStyle.ALARM) },
                    label = { Text("Alarm") },
                )
            }
        }
    }

    if (renaming) {
        RenameReminderDialog(
            initial = item.nickname ?: item.name,
            onDismiss = { renaming = false },
            onSave = {
                onRename(item.id, it)
                renaming = false
            },
        )
    }
}

@Composable
private fun RenameReminderDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename reminder") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Name") },
                supportingText = { Text("Leave blank to use the default name.") },
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text.trim()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * Editor for a reminder's lead time: a number field plus a minutes/hours/days unit dropdown, capped
 * at [MAX_OFFSET_MINUTES]. The stored value stays as total minutes; on entry it is decomposed to the
 * largest whole unit for display. Keyed by [itemId] so it doesn't reset while the user types.
 */
@Composable
private fun OffsetEditor(
    itemId: String,
    offsetMinutes: Int,
    onOffsetChange: (String, Int) -> Unit,
) {
    var valueText by remember(itemId) { mutableStateOf(decomposeOffset(offsetMinutes).first.toString()) }
    var unit by remember(itemId) { mutableStateOf(decomposeOffset(offsetMinutes).second) }
    var unitMenuOpen by remember { mutableStateOf(false) }

    fun commit() {
        val entered = valueText.toIntOrNull() ?: 0
        onOffsetChange(itemId, (entered * unit.minutes).coerceIn(0, MAX_OFFSET_MINUTES))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "Remind", style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = valueText,
            onValueChange = {
                valueText = it.filter(Char::isDigit).take(OFFSET_MAX_DIGITS)
                commit()
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(96.dp),
        )
        Box {
            OutlinedButton(onClick = { unitMenuOpen = true }) {
                Text(unit.label)
                Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = unitMenuOpen, onDismissRequest = { unitMenuOpen = false }) {
                OffsetUnit.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            unit = option
                            unitMenuOpen = false
                            commit()
                        },
                    )
                }
            }
        }
        Text(
            text = if ((valueText.toIntOrNull() ?: 0) == 0) "(at start)" else "before",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Decomposes a lead time in minutes to its largest whole unit for display in the [OffsetEditor]. */
private fun decomposeOffset(minutes: Int): Pair<Int, OffsetUnit> =
    when {
        minutes <= 0 -> 0 to OffsetUnit.MINUTES
        minutes % OffsetUnit.DAYS.minutes == 0 -> (minutes / OffsetUnit.DAYS.minutes) to OffsetUnit.DAYS
        minutes % OffsetUnit.HOURS.minutes == 0 -> (minutes / OffsetUnit.HOURS.minutes) to OffsetUnit.HOURS
        else -> minutes to OffsetUnit.MINUTES
    }

/** A lead-time unit for the [OffsetEditor]. */
private enum class OffsetUnit(
    val label: String,
) {
    MINUTES("Minutes"),
    HOURS("Hours"),
    DAYS("Days"),
    ;

    /** How many minutes one of this unit is. */
    val minutes: Int
        get() =
            when (this) {
                MINUTES -> 1
                HOURS -> MINUTES_PER_HOUR
                DAYS -> MINUTES_PER_DAY
            }
}

@Composable
private fun ExactAlarmBanner(onRequestExactAlarm: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Reminders may be delayed. Allow exact alarms for on-time notifications.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onRequestExactAlarm) { Text("Allow exact alarms") }
        }
    }
}

@Composable
private fun FullScreenIntentBanner(onRequestFullScreenIntent: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text =
                    "Alarms may not appear on the lock screen. Allow full-screen alarms so they " +
                        "ring over a locked device.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onRequestFullScreenIntent) { Text("Allow full-screen alarms") }
        }
    }
}

@Composable
private fun CenteredBox(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) { content() }
}

private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = 1440
private const val MAX_OFFSET_DAYS = 30
private const val MAX_OFFSET_MINUTES = MINUTES_PER_DAY * MAX_OFFSET_DAYS
private const val OFFSET_MAX_DIGITS = 5

private val MuhurtaQuality.label: String
    get() =
        when (this) {
            MuhurtaQuality.AUSPICIOUS -> "Auspicious"
            MuhurtaQuality.INAUSPICIOUS -> "Inauspicious"
        }

private fun requiredPermissions(): List<String> =
    buildList {
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

private fun Context.isGranted(permission: String): Boolean =
    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

/** Opens the system screen where the user can allow this app to schedule exact alarms (API 31+). */
private fun Context.openExactAlarmSettings() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/**
 * Whether this app may post full-screen-intent notifications. Always true before Android 14, where
 * the permission is granted on install; from Android 14 it must be granted explicitly, so alarms are
 * demoted to plain notifications until the user allows it.
 */
private fun Context.canUseFullScreenIntent(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        getSystemService(NotificationManager::class.java)?.canUseFullScreenIntent() == true
    } else {
        true
    }

/** Opens the system screen where the user can allow this app to use full-screen intents (API 34+). */
private fun Context.openFullScreenIntentSettings() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        startActivity(
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, "package:$packageName".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Formats an instant as local wall-clock time in the device's zone. */
private fun formatTime(instant: Instant): String {
    val local =
        java.time.Instant
            .ofEpochMilli(instant.toEpochMilliseconds())
            .atZone(ZoneId.systemDefault())
    return local.format(timeFormatter)
}

@Preview
@Composable
private fun AlarmContentPreview() {
    val reminders =
        listOf(
            ReminderItem(
                id = "muhurta:Abhijit Muhurta",
                name = "Abhijit Muhurta",
                start = Instant.fromEpochMilliseconds(1_705_300_140_000L),
                end = Instant.fromEpochMilliseconds(1_705_302_960_000L),
                quality = MuhurtaQuality.AUSPICIOUS,
                isTomorrow = false,
                offsetMinutes = 30,
                alertType = AlertStyle.ALARM,
            ),
            ReminderItem(
                id = "choghadiya:AMRIT",
                name = "Amrit",
                start = Instant.fromEpochMilliseconds(1_705_287_540_000L),
                end = Instant.fromEpochMilliseconds(1_705_292_265_000L),
                quality = MuhurtaQuality.AUSPICIOUS,
                isTomorrow = true,
                offsetMinutes = 10,
                alertType = AlertStyle.NOTIFICATION,
            ),
            ReminderItem(
                id = "tithi:*:30",
                name = "Amavasya",
                start = Instant.fromEpochMilliseconds(1_762_560_000_000L),
                end = Instant.fromEpochMilliseconds(1_762_560_000_000L),
                quality = MuhurtaQuality.AUSPICIOUS,
                isTomorrow = false,
                offsetMinutes = 15,
                alertType = AlertStyle.NOTIFICATION,
                dateLabel = "Every month · Fri, 7 Nov",
            ),
        )
    val available =
        listOf(
            SourceOption("muhurta:Brahma Muhurta", "Brahma Muhurta", MuhurtaQuality.AUSPICIOUS),
            SourceOption("muhurta:Rahu Kalam", "Rahu Kalam", MuhurtaQuality.INAUSPICIOUS),
        )
    VedicMitraTheme {
        AlarmContent(
            uiState =
                AlarmUiState.Ready(
                    reminders = reminders,
                    available = available,
                    canScheduleExactAlarms = false,
                    usingDefaultLocation = true,
                ),
            onAdd = {},
            onAddTithi = {},
            onRemove = {},
            onRename = { _, _ -> },
            onOffsetChange = { _, _ -> },
            onAlertChange = { _, _ -> },
            onRequestExactAlarm = {},
        )
    }
}
