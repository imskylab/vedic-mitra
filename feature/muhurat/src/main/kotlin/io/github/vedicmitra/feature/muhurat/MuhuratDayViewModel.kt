/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.muhurat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vedicmitra.core.astronomy.AstronomyEngine
import io.github.vedicmitra.core.astronomy.AstronomySnapshot
import io.github.vedicmitra.core.astronomy.MuhurtaActivity
import io.github.vedicmitra.core.astronomy.MuhurtaQuality
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.datastore.PersistedReminder
import io.github.vedicmitra.core.datastore.ReminderRepository
import io.github.vedicmitra.core.domain.ResolveLocationUseCase
import io.github.vedicmitra.core.notifications.AppNotification
import io.github.vedicmitra.core.notifications.AppNotificationChannel
import io.github.vedicmitra.core.scheduler.ScheduledTask
import io.github.vedicmitra.core.scheduler.TaskScheduler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.time.Instant

/** The `day` nav argument — the chosen day's sunrise instant, as epoch milliseconds. */
internal const val MUHURAT_DAY_ARG = "day"

/** One named time window on a day, e.g. "Abhijit Muhurta" or "Amrit (Choghadiya)". */
data class DayWindow(
    val label: String,
    val start: Instant,
    val end: Instant,
)

/**
 * Presentation logic for the muhurta day-detail screen: computes the full panchanga for the chosen
 * day (from the `day` and `activity` nav arguments) at the resolved location, splits its time windows
 * — the muhurtas and Choghadiya — into the auspicious ones to prefer and the inauspicious ones to
 * avoid, and can set a one-shot reminder for the day.
 */
@HiltViewModel
class MuhuratDayViewModel
    @Inject
    constructor(
        private val astronomyEngine: AstronomyEngine,
        private val resolveLocation: ResolveLocationUseCase,
        private val taskScheduler: TaskScheduler,
        private val reminderRepository: ReminderRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val activity: MuhurtaActivity =
            savedStateHandle
                .get<String>(MUHURAT_ACTIVITY_ARG)
                ?.let { name -> runCatching { MuhurtaActivity.valueOf(name) }.getOrNull() }
                ?: MuhurtaActivity.GRIHA_PRAVESH

        private val dayMillis: Long = savedStateHandle[MUHURAT_DAY_ARG] ?: System.currentTimeMillis()

        private val _uiState = MutableStateFlow<MuhuratDayUiState>(MuhuratDayUiState.Loading)

        /** Observable UI state consumed by the day-detail screen. */
        val uiState: StateFlow<MuhuratDayUiState> = _uiState.asStateFlow()

        private val _messages = MutableSharedFlow<String>()

        /** One-shot messages (reminder confirmations) for the screen to surface. */
        val messages: SharedFlow<String> = _messages.asSharedFlow()

        /** (Re)loads the chosen day's panchanga and its auspicious/inauspicious windows. */
        fun load() {
            viewModelScope.launch {
                _uiState.value = MuhuratDayUiState.Loading
                val resolved = resolveLocation()
                val instant = Instant.fromEpochMilliseconds(dayMillis)
                _uiState.value =
                    when (val result = astronomyEngine.snapshotAt(instant, resolved.coordinates)) {
                        is AppResult.Success -> readyState(result.data)
                        is AppResult.Failure ->
                            MuhuratDayUiState.Ready(dayMillis, activity.displayName, "—", emptyList(), emptyList())
                    }
            }
        }

        /**
         * Schedules a one-shot notification on the morning (sunrise) of the chosen day, persisted so
         * it survives a reboot, and confirms via [messages]. No-op with a message if the day has begun.
         */
        fun setReminder() {
            viewModelScope.launch {
                if (dayMillis <= System.currentTimeMillis()) {
                    _messages.emit("That day has already begun.")
                    return@launch
                }
                val key = "$REMINDER_KEY_PREFIX$dayMillis"
                val title = "Auspicious day: ${activity.displayName}"
                val body = "${activity.displayName} is well-favoured today — open Muhurat for the day's windows."
                val scheduled =
                    taskScheduler.schedule(
                        ScheduledTask(
                            id = key,
                            triggerAt = Instant.fromEpochMilliseconds(dayMillis),
                            notification =
                                AppNotification(
                                    id = key.hashCode(),
                                    channel = AppNotificationChannel.MUHURTA_REMINDERS,
                                    title = title,
                                    body = body,
                                ),
                        ),
                    )
                if (scheduled is AppResult.Success) {
                    reminderRepository.upsert(
                        PersistedReminder(id = key, triggerAtEpochMillis = dayMillis, title = title, body = body),
                    )
                    _messages.emit("Reminder set for ${formatReminderDate(dayMillis)}")
                } else {
                    _messages.emit("Couldn't set the reminder.")
                }
            }
        }

        private fun readyState(snapshot: AstronomySnapshot): MuhuratDayUiState.Ready =
            MuhuratDayUiState.Ready(
                dateMillis = dayMillis,
                activityLabel = activity.displayName,
                summary = "${snapshot.vara.displayName} · ${snapshot.tithi.name} · ${snapshot.nakshatra.name}",
                auspicious = windowsOf(snapshot, MuhurtaQuality.AUSPICIOUS),
                inauspicious = windowsOf(snapshot, MuhurtaQuality.INAUSPICIOUS),
            )

        /** The muhurtas and Choghadiya of the given [quality] on [snapshot]'s day, in time order. */
        private fun windowsOf(
            snapshot: AstronomySnapshot,
            quality: MuhurtaQuality,
        ): List<DayWindow> {
            val muhurtas =
                snapshot.muhurtas
                    .filter { it.quality == quality }
                    .map { DayWindow(it.name, it.start, it.end) }
            val choghadiya =
                snapshot.choghadiya
                    .filter { it.quality == quality }
                    .map { DayWindow("${it.name.label} (Choghadiya)", it.start, it.end) }
            return (muhurtas + choghadiya).sortedBy { it.start }
        }

        private companion object {
            // Kept distinct from the alarm feature's "muhurta:"/"choghadiya:"/"tithi:" keys so the
            // Reminders screen never renews or rolls this one-shot forward.
            const val REMINDER_KEY_PREFIX = "muhurat:"
        }
    }

private val reminderDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")

/** Formats an epoch-millis day as a local date, e.g. "Wed, 8 Nov 2026". */
private fun formatReminderDate(epochMillis: Long): String =
    java.time.Instant
        .ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(reminderDateFormatter)

/** UI state for the muhurta day-detail screen. */
sealed interface MuhuratDayUiState {
    /** The day's detail is being computed. */
    data object Loading : MuhuratDayUiState

    /**
     * The day's detail is ready.
     *
     * @property dateMillis the day's sunrise instant as epoch milliseconds, for the date header.
     * @property activityLabel the activity this day was chosen for.
     * @property summary a short "weekday · tithi · nakshatra" line.
     * @property auspicious the auspicious windows to prefer, in time order.
     * @property inauspicious the inauspicious windows to avoid, in time order.
     */
    data class Ready(
        val dateMillis: Long,
        val activityLabel: String,
        val summary: String,
        val auspicious: List<DayWindow>,
        val inauspicious: List<DayWindow>,
    ) : MuhuratDayUiState
}
