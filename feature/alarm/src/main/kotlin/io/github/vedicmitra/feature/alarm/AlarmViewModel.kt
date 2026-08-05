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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vedicmitra.core.astronomy.AstronomyEngine
import io.github.vedicmitra.core.astronomy.Muhurta
import io.github.vedicmitra.core.astronomy.MuhurtaQuality
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.datastore.PersistedReminder
import io.github.vedicmitra.core.datastore.ReminderRepository
import io.github.vedicmitra.core.location.LocationProvider
import io.github.vedicmitra.core.notifications.AppNotification
import io.github.vedicmitra.core.notifications.AppNotificationChannel
import io.github.vedicmitra.core.scheduler.ScheduledTask
import io.github.vedicmitra.core.scheduler.TaskScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Presentation logic for the reminders screen (MVVM). Loads today's muhurta windows for the device
 * location and, combined with the user's persisted reminders and per-muhurta lead-time overrides,
 * exposes a togglable list. Toggling schedules (or cancels) an exact alarm — lead-time adjusted —
 * and persists the full reminder so it can be re-armed after a reboot.
 *
 * [load] is driven by the screen once it has resolved the location permission.
 */
@HiltViewModel
class AlarmViewModel
    @Inject
    constructor(
        private val astronomyEngine: AstronomyEngine,
        private val locationProvider: LocationProvider,
        private val taskScheduler: TaskScheduler,
        private val reminderRepository: ReminderRepository,
    ) : ViewModel() {
        private val loadState = MutableStateFlow<AlarmLoad>(AlarmLoad.Loading)

        /** Observable UI state consumed by the reminders screen. */
        val uiState: StateFlow<AlarmUiState> =
            combine(
                loadState,
                reminderRepository.reminders,
                reminderRepository.offsetMinutesByName,
            ) { load, reminders, offsets ->
                when (load) {
                    AlarmLoad.Loading -> AlarmUiState.Loading
                    is AlarmLoad.Error -> AlarmUiState.Error(load.message)
                    is AlarmLoad.Ready -> {
                        val enabledIds = reminders.mapTo(mutableSetOf()) { it.id }
                        AlarmUiState.Ready(
                            reminders = load.muhurtas.map { it.toReminderItem(enabledIds, load.now, offsets) },
                            canScheduleExactAlarms = load.canScheduleExactAlarms,
                            usingDefaultLocation = load.usingDefaultLocation,
                        )
                    }
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = AlarmUiState.Loading,
            )

        /** (Re)loads today's muhurtas, using the device location when available. */
        fun load() {
            viewModelScope.launch {
                loadState.value = AlarmLoad.Loading
                reminderRepository.removePast(System.currentTimeMillis())

                val locationResult = locationProvider.currentLocation()
                val usingDefault = locationResult !is AppResult.Success
                val location = (locationResult as? AppResult.Success)?.data ?: DEFAULT_LOCATION
                val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())

                loadState.value =
                    when (val snapshot = astronomyEngine.snapshotAt(now, location)) {
                        is AppResult.Success ->
                            AlarmLoad.Ready(
                                muhurtas = snapshot.data.muhurtas,
                                now = now,
                                canScheduleExactAlarms = taskScheduler.canScheduleExactAlarms(),
                                usingDefaultLocation = usingDefault,
                            )

                        is AppResult.Failure -> AlarmLoad.Error(snapshot.cause.message ?: "Unknown error")
                    }
            }
        }

        /**
         * Enables or disables the reminder for [item]. Enabling schedules an exact alarm — fired
         * [ReminderItem.offsetMinutes] before the window (never in the past) — and persists it;
         * disabling cancels and forgets it. Past windows are ignored.
         */
        fun setReminder(
            item: ReminderItem,
            enabled: Boolean,
        ) {
            if (enabled && item.isPast) return
            viewModelScope.launch {
                if (enabled) {
                    scheduleAndPersist(item.id, item.name, item.start, item.quality, item.offsetMinutes)
                } else {
                    taskScheduler.cancel(item.id)
                    reminderRepository.remove(item.id)
                }
            }
        }

        /**
         * Sets how many minutes before the muhurta named [name] its reminder should fire. If a
         * reminder for that muhurta is currently enabled, it is immediately re-scheduled at the new
         * trigger time — the change isn't deferred until the next toggle.
         */
        fun setOffsetMinutes(
            name: String,
            minutes: Int,
        ) {
            viewModelScope.launch {
                reminderRepository.setOffsetMinutes(name, minutes)

                val id = "muhurta:$name"
                val isEnabled = reminderRepository.reminders.first().any { it.id == id }
                if (!isEnabled) return@launch

                val muhurta = (loadState.value as? AlarmLoad.Ready)?.muhurtas?.firstOrNull { it.name == name }
                if (muhurta != null) {
                    scheduleAndPersist(id, muhurta.name, muhurta.start, muhurta.quality, minutes)
                }
            }
        }

        /** Computes the lead-adjusted trigger time, schedules the alarm, and persists the reminder. */
        private suspend fun scheduleAndPersist(
            id: String,
            name: String,
            start: Instant,
            quality: MuhurtaQuality,
            offsetMinutes: Int,
        ) {
            val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
            val triggerAt = maxOf(start - offsetMinutes.minutes, now)
            val body = reminderBody(name, quality, offsetMinutes)

            taskScheduler.schedule(
                ScheduledTask(
                    id = id,
                    triggerAt = triggerAt,
                    notification =
                        AppNotification(
                            id = id.hashCode(),
                            channel = AppNotificationChannel.MUHURTA_REMINDERS,
                            title = name,
                            body = body,
                        ),
                ),
            )
            reminderRepository.upsert(
                PersistedReminder(
                    id = id,
                    triggerAtEpochMillis = triggerAt.toEpochMilliseconds(),
                    title = name,
                    body = body,
                ),
            )
        }

        private fun Muhurta.toReminderItem(
            enabledIds: Set<String>,
            now: Instant,
            offsetsByName: Map<String, Int>,
        ): ReminderItem {
            val id = "muhurta:$name"
            return ReminderItem(
                id = id,
                name = name,
                start = start,
                end = end,
                quality = quality,
                isEnabled = id in enabledIds,
                isPast = start <= now,
                offsetMinutes = offsetsByName[name] ?: ReminderRepository.DEFAULT_OFFSET_MINUTES,
            )
        }

        /** Notification body reflecting how far ahead of the window this reminder fires. */
        private fun reminderBody(
            name: String,
            quality: MuhurtaQuality,
            offsetMinutes: Int,
        ): String {
            val timing =
                if (offsetMinutes <= 0) {
                    "$name is starting now"
                } else {
                    val unit = if (offsetMinutes == 1) "minute" else "minutes"
                    "$name starts in $offsetMinutes $unit"
                }
            val tone =
                when (quality) {
                    MuhurtaQuality.AUSPICIOUS -> "an auspicious window"
                    MuhurtaQuality.INAUSPICIOUS -> "a window to be mindful of"
                }
            return "$timing — $tone."
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L

            // Used when the device location is unavailable (New Delhi).
            val DEFAULT_LOCATION = GeoCoordinates(latitude = 28.6139, longitude = 77.2090)
        }
    }

/** UI state for the reminders screen. */
sealed interface AlarmUiState {
    /** The muhurtas are being computed. */
    data object Loading : AlarmUiState

    /** Computation failed; [message] is human-readable. */
    data class Error(
        val message: String,
    ) : AlarmUiState

    /**
     * Today's muhurtas, ready to display.
     *
     * @property reminders the day's windows, each with its enabled state and lead-time offset.
     * @property canScheduleExactAlarms whether exact alarms are permitted; when false the UI should
     *   invite the user to grant the permission.
     * @property usingDefaultLocation whether a default location was used (device location
     *   unavailable).
     */
    data class Ready(
        val reminders: List<ReminderItem>,
        val canScheduleExactAlarms: Boolean,
        val usingDefaultLocation: Boolean,
    ) : AlarmUiState
}

/**
 * A single muhurta window as shown on the reminders screen.
 *
 * @property id stable id used to schedule/cancel and persist the reminder.
 * @property name the window's traditional name.
 * @property start when the window begins.
 * @property end when the window ends.
 * @property quality whether the window is auspicious or inauspicious.
 * @property isEnabled whether the user has a reminder set for it.
 * @property isPast whether the window has already begun (a reminder can no longer be set).
 * @property offsetMinutes how many minutes before [start] this reminder fires (0 = at start),
 *   resolved from the user's per-muhurta override or [ReminderRepository.DEFAULT_OFFSET_MINUTES].
 */
data class ReminderItem(
    val id: String,
    val name: String,
    val start: Instant,
    val end: Instant,
    val quality: MuhurtaQuality,
    val isEnabled: Boolean,
    val isPast: Boolean,
    val offsetMinutes: Int,
)

/** Internal load result, before it is combined with the persisted reminders and lead time. */
private sealed interface AlarmLoad {
    data object Loading : AlarmLoad

    data class Error(
        val message: String,
    ) : AlarmLoad

    data class Ready(
        val muhurtas: List<Muhurta>,
        val now: Instant,
        val canScheduleExactAlarms: Boolean,
        val usingDefaultLocation: Boolean,
    ) : AlarmLoad
}
