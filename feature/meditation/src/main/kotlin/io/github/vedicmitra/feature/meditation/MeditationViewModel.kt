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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vedicmitra.core.astronomy.AstronomyEngine
import io.github.vedicmitra.core.astronomy.Muhurta
import io.github.vedicmitra.core.astronomy.NAKSHATRA_NAMES
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.datastore.MeditationRepository
import io.github.vedicmitra.core.datastore.MeditationSession
import io.github.vedicmitra.core.datastore.PersistedReminder
import io.github.vedicmitra.core.datastore.ReminderRepository
import io.github.vedicmitra.core.domain.ResolveLocationUseCase
import io.github.vedicmitra.core.notifications.AppNotification
import io.github.vedicmitra.core.notifications.AppNotificationChannel
import io.github.vedicmitra.core.scheduler.ScheduledTask
import io.github.vedicmitra.core.scheduler.TaskScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.time.Instant

private const val REMINDER_ID = "meditation:brahma"
private const val HISTORY_LIMIT = 20
private const val MIN_LOG_SECONDS = 60
private const val DAY_MILLIS = 86_400_000L
private const val TICK_MILLIS = 1_000L
private val DURATION_PRESETS_SECONDS = listOf(300, 600, 900, 1200, 1800)
private val DEFAULT_SECONDS = DURATION_PRESETS_SECONDS[1]

/**
 * Presentation logic for the meditation timer. Runs an in-memory countdown (start/pause/stop), logs a
 * finished sit to the history stamped with the day's nakshatra/tithi, emits start/end bell signals for
 * the screen to sound, surfaces today's Brahma Muhurta window, and can schedule a pre-dawn Brahma
 * Muhurta reminder — persisted and rolled forward to the next occurrence on each load, mirroring how
 * the reminders feature emulates a daily alarm.
 */
@HiltViewModel
class MeditationViewModel
    @Inject
    constructor(
        private val meditationRepository: MeditationRepository,
        private val reminderRepository: ReminderRepository,
        private val taskScheduler: TaskScheduler,
        private val astronomyEngine: AstronomyEngine,
        private val resolveLocation: ResolveLocationUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<MeditationUiState>(MeditationUiState.Loading)

        /** Observable UI state consumed by the meditation screen. */
        val uiState: StateFlow<MeditationUiState> = _uiState.asStateFlow()

        private val _signals = MutableSharedFlow<MeditationSignal>(extraBufferCapacity = 4)

        /** One-shot bell signals (start/end) for the screen to sound. */
        val signals: SharedFlow<MeditationSignal> = _signals.asSharedFlow()

        private var selectedSeconds = DEFAULT_SECONDS
        private var remainingSeconds = DEFAULT_SECONDS
        private var phase = TimerPhase.IDLE
        private var sessions: List<MeditationSession> = emptyList()
        private var coordinates: GeoCoordinates? = null
        private var todayNakshatra: Int? = null
        private var todayTithi: Int? = null
        private var brahmaWindow: BrahmaWindowView? = null
        private var reminderEnabled = false
        private var canScheduleExact = true
        private var tickJob: Job? = null

        /** Loads the panchanga hooks and history, and rolls the reminder forward if enabled. */
        fun load() {
            viewModelScope.launch {
                val resolved = resolveLocation()
                coordinates = resolved.coordinates
                val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
                val snapshot = (astronomyEngine.snapshotAt(now, resolved.coordinates) as? AppResult.Success)?.data
                todayNakshatra = snapshot?.nakshatra?.number
                todayTithi = snapshot?.tithi?.number
                brahmaWindow =
                    snapshot
                        ?.muhurtas
                        ?.firstOrNull { it.name.contains("Brahma", ignoreCase = true) }
                        ?.let(::brahmaViewOf)
                sessions = meditationRepository.sessions.first()
                reminderEnabled = reminderRepository.reminders.first().any { it.id == REMINDER_ID }
                canScheduleExact = taskScheduler.canScheduleExactAlarms()
                if (reminderEnabled) enableReminder()
                remainingSeconds = selectedSeconds
                phase = TimerPhase.IDLE
                emit()
            }
        }

        /** Chooses a sit length (seconds); ignored while a sit is running. */
        fun selectDuration(seconds: Int) {
            if (phase == TimerPhase.RUNNING) return
            selectedSeconds = seconds
            remainingSeconds = seconds
            phase = TimerPhase.IDLE
            emit()
        }

        /** Starts (or resumes) the countdown. */
        fun start() {
            if (phase == TimerPhase.RUNNING) return
            val fresh = phase != TimerPhase.PAUSED
            if (fresh) {
                remainingSeconds = selectedSeconds
                _signals.tryEmit(MeditationSignal.START_BELL)
            }
            phase = TimerPhase.RUNNING
            emit()
            tickJob?.cancel()
            tickJob = viewModelScope.launch { runTimer() }
        }

        /** Pauses a running countdown, keeping the remaining time. */
        fun pause() {
            if (phase != TimerPhase.RUNNING) return
            phase = TimerPhase.PAUSED
            tickJob?.cancel()
            emit()
        }

        /** Ends the sit early, logging the elapsed time (if long enough) and resetting. */
        fun stop() {
            tickJob?.cancel()
            if (phase == TimerPhase.RUNNING || phase == TimerPhase.PAUSED) logSit(selectedSeconds - remainingSeconds)
            remainingSeconds = selectedSeconds
            phase = TimerPhase.IDLE
            emit()
        }

        /** Turns the daily Brahma Muhurta reminder on or off. */
        fun setReminder(enabled: Boolean) {
            viewModelScope.launch {
                if (enabled) enableReminder() else disableReminder()
                reminderEnabled = enabled
                emit()
            }
        }

        private suspend fun runTimer() {
            while (remainingSeconds > 0 && phase == TimerPhase.RUNNING) {
                delay(TICK_MILLIS)
                if (phase != TimerPhase.RUNNING) return
                remainingSeconds--
                emit()
            }
            if (remainingSeconds == 0 && phase == TimerPhase.RUNNING) {
                phase = TimerPhase.DONE
                _signals.tryEmit(MeditationSignal.END_BELL)
                logSit(selectedSeconds)
                emit()
            }
        }

        private fun logSit(seconds: Int) {
            if (seconds < MIN_LOG_SECONDS) return
            val session =
                MeditationSession(
                    completedAtEpochMillis = System.currentTimeMillis(),
                    dateEpochDay = todayEpochDay(),
                    durationSeconds = seconds,
                    nakshatraNumber = todayNakshatra,
                    tithiNumber = todayTithi,
                )
            viewModelScope.launch {
                meditationRepository.add(session)
                sessions = meditationRepository.sessions.first()
                emit()
            }
        }

        private suspend fun enableReminder() {
            val location = coordinates ?: return
            val start = nextBrahmaStart(location) ?: return
            val title = "Time to meditate"
            val body = "Brahma Muhurta has begun — an auspicious time to sit."
            val scheduled =
                taskScheduler.schedule(
                    ScheduledTask(
                        id = REMINDER_ID,
                        triggerAt = start,
                        notification =
                            AppNotification(
                                id = REMINDER_ID.hashCode(),
                                channel = AppNotificationChannel.MEDITATION_REMINDERS,
                                title = title,
                                body = body,
                            ),
                    ),
                )
            if (scheduled is AppResult.Success) {
                reminderRepository.upsert(
                    PersistedReminder(
                        id = REMINDER_ID,
                        triggerAtEpochMillis = start.toEpochMilliseconds(),
                        title = title,
                        body = body,
                    ),
                )
            }
        }

        private suspend fun disableReminder() {
            taskScheduler.cancel(REMINDER_ID)
            reminderRepository.remove(REMINDER_ID)
        }

        // The next Brahma Muhurta start: today's if it's still ahead, otherwise tomorrow's.
        private suspend fun nextBrahmaStart(location: GeoCoordinates): Instant? {
            val nowMillis = System.currentTimeMillis()
            val today = brahmaStartAt(nowMillis, location)
            if (today != null && today.toEpochMilliseconds() > nowMillis) return today
            return brahmaStartAt(nowMillis + DAY_MILLIS, location)
        }

        private suspend fun brahmaStartAt(
            millis: Long,
            location: GeoCoordinates,
        ): Instant? {
            val instant = Instant.fromEpochMilliseconds(millis)
            val snapshot = (astronomyEngine.snapshotAt(instant, location) as? AppResult.Success)?.data
            return snapshot?.muhurtas?.firstOrNull { it.name.contains("Brahma", ignoreCase = true) }?.start
        }

        private fun brahmaViewOf(muhurta: Muhurta): BrahmaWindowView {
            val nowMillis = System.currentTimeMillis()
            val isNow =
                nowMillis >= muhurta.start.toEpochMilliseconds() && nowMillis < muhurta.end.toEpochMilliseconds()
            return BrahmaWindowView(label = "${formatTime(muhurta.start)} – ${formatTime(muhurta.end)}", isNow = isNow)
        }

        private fun emit() {
            val today = todayEpochDay()
            _uiState.value =
                MeditationUiState.Ready(
                    presetsSeconds = DURATION_PRESETS_SECONDS,
                    selectedSeconds = selectedSeconds,
                    phase = phase,
                    remainingSeconds = remainingSeconds,
                    todaySeconds = MeditationLogic.secondsOn(sessions, today),
                    streak = MeditationLogic.currentStreak(sessions.map { it.dateEpochDay }.toSet(), today),
                    history = sessions.take(HISTORY_LIMIT).map { it.toView() },
                    brahmaMuhurta = brahmaWindow,
                    reminderEnabled = reminderEnabled,
                    canScheduleExactAlarms = canScheduleExact,
                )
        }

        private fun MeditationSession.toView(): MeditationSessionView =
            MeditationSessionView(
                dateLabel = LocalDate.ofEpochDay(dateEpochDay).format(historyDateFormatter),
                durationLabel = MeditationLogic.formatDuration(durationSeconds),
                nakshatraLabel = nakshatraNumber?.let { NAKSHATRA_NAMES.getOrNull(it - 1) },
            )

        private fun todayEpochDay(): Long = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
    }

private val historyDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun formatTime(instant: Instant): String =
    java.time.Instant
        .ofEpochMilli(instant.toEpochMilliseconds())
        .atZone(ZoneId.systemDefault())
        .format(timeFormatter)

/** The countdown's state. */
enum class TimerPhase { IDLE, RUNNING, PAUSED, DONE }

/** A one-shot audible signal for the screen to sound. */
enum class MeditationSignal { START_BELL, END_BELL }

/** Today's Brahma Muhurta window as a label, plus whether it's happening right now. */
data class BrahmaWindowView(
    val label: String,
    val isNow: Boolean,
)

/** A logged sit, formatted for the history list. */
data class MeditationSessionView(
    val dateLabel: String,
    val durationLabel: String,
    val nakshatraLabel: String?,
)

/** UI state for the meditation screen. */
sealed interface MeditationUiState {
    /** The hooks and history are loading. */
    data object Loading : MeditationUiState

    /**
     * The timer is ready.
     *
     * @property presetsSeconds the offered sit lengths, in seconds.
     * @property selectedSeconds the chosen sit length.
     * @property phase the countdown state.
     * @property remainingSeconds seconds left in the current sit.
     * @property todaySeconds total seconds meditated today.
     * @property streak consecutive days with a logged sit.
     * @property history recent logged sits, newest first.
     * @property brahmaMuhurta today's Brahma Muhurta window, or `null` if unavailable.
     * @property reminderEnabled whether the daily Brahma Muhurta reminder is on.
     * @property canScheduleExactAlarms whether exact alarms are permitted (for a settings nudge).
     */
    data class Ready(
        val presetsSeconds: List<Int>,
        val selectedSeconds: Int,
        val phase: TimerPhase,
        val remainingSeconds: Int,
        val todaySeconds: Int,
        val streak: Int,
        val history: List<MeditationSessionView>,
        val brahmaMuhurta: BrahmaWindowView?,
        val reminderEnabled: Boolean,
        val canScheduleExactAlarms: Boolean,
    ) : MeditationUiState
}
