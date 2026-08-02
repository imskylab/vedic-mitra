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
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vedicmitra.core.astronomy.MuhurtaQuality
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Instant

/**
 * Reminders screen entry point. Resolves the location (to compute muhurtas) and notification
 * permissions, drives [AlarmViewModel.load], and renders the togglable list. The stateless
 * [AlarmContent] takes state and callbacks as parameters so it is trivially previewable.
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
        onToggleReminder = viewModel::setReminder,
        onSelectLeadTime = viewModel::setLeadTime,
        onRequestExactAlarm = context::openExactAlarmSettings,
        modifier = modifier,
    )
}

@Composable
private fun AlarmContent(
    uiState: AlarmUiState,
    onToggleReminder: (ReminderItem, Boolean) -> Unit,
    onSelectLeadTime: (Int) -> Unit,
    onRequestExactAlarm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        AlarmUiState.Loading -> CenteredBox(modifier) { CircularProgressIndicator() }
        is AlarmUiState.Error ->
            CenteredBox(modifier) {
                Text(text = uiState.message, style = MaterialTheme.typography.bodyLarge)
            }

        is AlarmUiState.Ready ->
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    LeadTimeSelector(
                        selectedMinutes = uiState.leadTimeMinutes,
                        onSelect = onSelectLeadTime,
                    )
                }
                if (!uiState.canScheduleExactAlarms) {
                    item { ExactAlarmBanner(onRequestExactAlarm = onRequestExactAlarm) }
                }
                if (uiState.usingDefaultLocation) {
                    item {
                        Text(
                            text = "Showing New Delhi — grant location access for your area.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                if (uiState.reminders.isEmpty()) {
                    item {
                        Text(
                            text = "No muhurta windows for today.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                items(items = uiState.reminders, key = { it.id }) { item ->
                    ReminderRow(item = item, onToggle = onToggleReminder)
                }
            }
    }
}

@Composable
private fun ReminderRow(
    item: ReminderItem,
    onToggle: (ReminderItem, Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${formatTime(item.start)}–${formatTime(item.end)} · ${item.quality.label}",
                style = MaterialTheme.typography.bodySmall,
            )
            if (item.isPast) {
                Text(
                    text = "Already passed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Switch(
            checked = item.isEnabled,
            enabled = !item.isPast,
            onCheckedChange = { checked -> onToggle(item, checked) },
        )
    }
}

@Composable
private fun LeadTimeSelector(
    selectedMinutes: Int,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Remind me before the window", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LEAD_TIME_OPTIONS.forEach { minutes ->
                FilterChip(
                    selected = minutes == selectedMinutes,
                    onClick = { onSelect(minutes) },
                    label = { Text(leadTimeLabel(minutes)) },
                )
            }
        }
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
private fun CenteredBox(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) { content() }
}

private val LEAD_TIME_OPTIONS = listOf(0, 5, 10, 15, 30)

private fun leadTimeLabel(minutes: Int): String = if (minutes == 0) "At start" else "$minutes min"

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
                id = "muhurta:abhijit",
                name = "Abhijit Muhurta",
                start = Instant.fromEpochMilliseconds(1_705_300_140_000L),
                end = Instant.fromEpochMilliseconds(1_705_302_960_000L),
                quality = MuhurtaQuality.AUSPICIOUS,
                isEnabled = true,
                isPast = false,
            ),
            ReminderItem(
                id = "muhurta:rahu",
                name = "Rahu Kalam",
                start = Instant.fromEpochMilliseconds(1_705_287_540_000L),
                end = Instant.fromEpochMilliseconds(1_705_292_265_000L),
                quality = MuhurtaQuality.INAUSPICIOUS,
                isEnabled = false,
                isPast = true,
            ),
        )
    VedicMitraTheme {
        AlarmContent(
            uiState =
                AlarmUiState.Ready(
                    reminders = reminders,
                    leadTimeMinutes = 10,
                    canScheduleExactAlarms = false,
                    usingDefaultLocation = true,
                ),
            onToggleReminder = { _, _ -> },
            onSelectLeadTime = {},
            onRequestExactAlarm = {},
        )
    }
}
