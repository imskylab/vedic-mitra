/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

@file:Suppress("MagicNumber")

package io.github.vedicmitra.feature.meditation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vedicmitra.core.designsystem.component.VedicSelectField
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme

/**
 * Meditation screen. Runs a countdown sit with a breath-pacing circle and a generated start/end bell
 * via [MeditationViewModel], shows today's total/streak and history, and offers the Brahma Muhurta
 * window with a daily-reminder toggle. The stateless [MeditationContent] is previewable.
 */
@Composable
fun MeditationScreen(
    modifier: Modifier = Modifier,
    viewModel: MeditationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ToneGenerator needs no Context; guard against devices that refuse to allocate it.
    val toneGenerator = remember { runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { toneGenerator?.release() } }

    // The Brahma Muhurta reminder posts a notification, so ask for POST_NOTIFICATIONS (Android 13+).
    val notificationPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (needsNotificationPermission(context)) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(Unit) {
        viewModel.signals.collect { signal ->
            val tone =
                when (signal) {
                    MeditationSignal.START_BELL -> ToneGenerator.TONE_PROP_BEEP
                    MeditationSignal.END_BELL -> ToneGenerator.TONE_PROP_BEEP2
                }
            runCatching { toneGenerator?.startTone(tone, 300) }
        }
    }

    MeditationContent(
        uiState = uiState,
        onSelectDuration = viewModel::selectDuration,
        onStart = viewModel::start,
        onPause = viewModel::pause,
        onStop = viewModel::stop,
        onToggleReminder = viewModel::setReminder,
        modifier = modifier,
    )
}

/** Whether the app still needs the runtime notification permission (Android 13+). */
private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

@Composable
private fun MeditationContent(
    uiState: MeditationUiState,
    onSelectDuration: (Int) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onToggleReminder: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        MeditationUiState.Loading ->
            Column(
                modifier = modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }

        is MeditationUiState.Ready ->
            ReadyView(uiState, onSelectDuration, onStart, onPause, onStop, onToggleReminder, modifier)
    }
}

@Composable
private fun ReadyView(
    state: MeditationUiState.Ready,
    onSelectDuration: (Int) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onToggleReminder: (Boolean) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Meditate", style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth())

        BreathCircle(remainingSeconds = state.remainingSeconds, running = state.phase == TimerPhase.RUNNING)

        if (state.phase == TimerPhase.IDLE || state.phase == TimerPhase.DONE) {
            DurationPicker(state.presetsSeconds, state.selectedSeconds, onSelectDuration)
        }
        Controls(state.phase, onStart, onPause, onStop)

        Text(
            text =
                "Today: ${MeditationLogic.formatDuration(state.todaySeconds)} · " +
                    "Streak: ${state.streak} ${dayWord(state.streak)}",
            style = MaterialTheme.typography.bodyMedium,
        )

        state.brahmaMuhurta?.let { window ->
            BrahmaCard(window, state.reminderEnabled, state.canScheduleExactAlarms, onStart, onToggleReminder)
        }

        if (state.history.isNotEmpty()) HistorySection(state.history)
    }
}

@Composable
private fun BreathCircle(
    remainingSeconds: Int,
    running: Boolean,
) {
    val transition = rememberInfiniteTransition(label = "breath")
    val breath by transition.animateFloat(
        initialValue = 0.62f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathScale",
    )
    // Ease back to a still circle when not running.
    val scale by animateFloatAsState(targetValue = if (running) breath else 1f, label = "scale")
    Box(modifier = Modifier.size(260.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier =
                Modifier
                    .size(220.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }.clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = MeditationLogic.formatClock(remainingSeconds),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                text = if (running) "breathe" else "ready",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun DurationPicker(
    presets: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    VedicSelectField(
        label = "Duration",
        options = presets,
        selected = selected,
        optionLabel = { MeditationLogic.formatDuration(it) },
        onSelect = onSelect,
    )
}

@Composable
private fun Controls(
    phase: TimerPhase,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        when (phase) {
            TimerPhase.RUNNING -> {
                OutlinedButton(onClick = onPause) { Text("Pause") }
                OutlinedButton(onClick = onStop) { Text("Stop") }
            }

            TimerPhase.PAUSED -> {
                Button(onClick = onStart) { Text("Resume") }
                OutlinedButton(onClick = onStop) { Text("Stop") }
            }

            else -> Button(onClick = onStart) { Text("Start") }
        }
    }
}

@Composable
private fun BrahmaCard(
    window: BrahmaWindowView,
    reminderEnabled: Boolean,
    canScheduleExact: Boolean,
    onStart: () -> Unit,
    onToggleReminder: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Brahma Muhurta" + if (window.isNow) " · now" else "",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "${window.label} — an auspicious time to sit.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (window.isNow) {
                Button(onClick = onStart) { Text("Sit now") }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Remind me daily",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = reminderEnabled, onCheckedChange = onToggleReminder)
            }
            if (reminderEnabled && !canScheduleExact) {
                Text(
                    text = "Allow exact alarms in system settings for an on-time reminder.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun HistorySection(history: List<MeditationSessionView>) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "History", style = MaterialTheme.typography.titleMedium)
        history.forEach { sit ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = sit.durationLabel, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = sit.dateLabel + (sit.nakshatraLabel?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider()
        }
    }
}

private fun dayWord(n: Int): String = if (n == 1) "day" else "days"

@Preview
@Composable
private fun MeditationContentPreview() {
    VedicMitraTheme {
        MeditationContent(
            uiState =
                MeditationUiState.Ready(
                    presetsSeconds = listOf(300, 600, 900, 1200, 1800),
                    selectedSeconds = 600,
                    phase = TimerPhase.IDLE,
                    remainingSeconds = 600,
                    todaySeconds = 900,
                    streak = 3,
                    history =
                        listOf(
                            MeditationSessionView("Mon, 18 Aug", "15 min", "Rohini"),
                            MeditationSessionView("Sun, 17 Aug", "10 min", null),
                        ),
                    brahmaMuhurta = BrahmaWindowView("04:52 – 05:40", isNow = true),
                    reminderEnabled = true,
                    canScheduleExactAlarms = true,
                ),
            onSelectDuration = {},
            onStart = {},
            onPause = {},
            onStop = {},
            onToggleReminder = {},
        )
    }
}
