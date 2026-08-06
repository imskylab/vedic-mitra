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
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Presentation logic for the reminders screen (MVVM). For each muhurta it resolves the **next
 * upcoming** occurrence — today's window if it is still ahead, otherwise tomorrow's — so a reminder
 * can always be set and never shows as "already passed". Combined with the user's persisted
 * reminders and per-muhurta lead-time overrides, it exposes a togglable list. Toggling on schedules
 * (or off cancels) an exact alarm — lead-time adjusted — and persists it so it survives a reboot.
 *
 * Each [load] also **renews** every already-enabled reminder onto its next upcoming occurrence, so
 * simply reopening the screen rolls fired reminders forward to the following day.
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
                            reminders = load.muhurtas.map { it.toReminderItem(enabledIds, offsets) },
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

        /**
         * (Re)loads the next upcoming occurrence of each muhurta, using the device location when
         * available, then renews any already-enabled reminders onto those upcoming windows.
         */
        fun load() {
            viewModelScope.launch {
                loadState.value = AlarmLoad.Loading

                val locationResult = locationProvider.currentLocation()
                val usingDefault = locationResult !is AppResult.Success
                val location = (locationResult as? AppResult.Success)?.data ?: DEFAULT_LOCATION
                val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())

                val today = astronomyEngine.snapshotAt(now, location)
                val tomorrow = astronomyEngine.snapshotAt(now + 1.days, location)

                loadState.value =
                    if (today is AppResult.Success && tomorrow is AppResult.Success) {
                        val upcoming = upcomingMuhurtas(today.data.muhurtas, tomorrow.data.muhurtas, now)
                        renewEnabledReminders(upcoming)
                        reminderRepository.removePast(now.toEpochMilliseconds())
                        AlarmLoad.Ready(
                            muhurtas = upcoming,
                            canScheduleExactAlarms = taskScheduler.canScheduleExactAlarms(),
                            usingDefaultLocation = usingDefault,
                        )
                    } else {
                        val message =
                            (today as? AppResult.Failure)?.cause?.message
                                ?: (tomorrow as? AppResult.Failure)?.cause?.message
                                ?: "Unknown error"
                        AlarmLoad.Error(message)
                    }
            }
        }

        /**
         * For each of [today]'s muhurtas, keeps it if its window is still ahead of [now], otherwise
         * substitutes the same-named window from [tomorrow] (flagged [UpcomingMuhurta.isTomorrow]).
         * The result therefore never contains a window that has already begun.
         */
        private fun upcomingMuhurtas(
            today: List<Muhurta>,
            tomorrow: List<Muhurta>,
            now: Instant,
        ): List<UpcomingMuhurta> =
            today.map { window ->
                if (window.start > now) {
                    UpcomingMuhurta(window, isTomorrow = false)
                } else {
                    // Names are stable day to day; fall back to today's window on the rare day a
                    // name differs (e.g. Saturday's split "Dur Muhurta 1/2").
                    val next = tomorrow.firstOrNull { it.name == window.name }
                    if (next !=
                        null
                    ) {
                        UpcomingMuhurta(next, isTomorrow = true)
                    } else {
                        UpcomingMuhurta(window, isTomorrow = false)
                    }
                }
            }

        /**
         * Re-schedules every persisted (i.e. enabled) reminder onto its [upcoming] occurrence, so a
         * reminder that has already fired rolls forward to the next day the moment the screen loads.
         */
        private suspend fun renewEnabledReminders(upcoming: List<UpcomingMuhurta>) {
            val persisted = reminderRepository.reminders.first()
            if (persisted.isEmpty()) return

            val offsets = reminderRepository.offsetMinutesByName.first()
            val byName = upcoming.associateBy { it.muhurta.name }
            persisted.forEach { reminder ->
                val name = reminder.id.removePrefix(MUHURTA_ID_PREFIX)
                val window = byName[name]?.muhurta ?: return@forEach
                val offset = offsets[name] ?: ReminderRepository.DEFAULT_OFFSET_MINUTES
                scheduleAndPersist(reminder.id, window.name, window.start, window.quality, offset)
            }
        }

        /**
         * Enables or disables the reminder for [item]. Enabling schedules an exact alarm — fired
         * [ReminderItem.offsetMinutes] before the (always upcoming) window — and persists it;
         * disabling cancels and forgets it.
         */
        fun setReminder(
            item: ReminderItem,
            enabled: Boolean,
        ) {
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

                val id = "$MUHURTA_ID_PREFIX$name"
                val isEnabled = reminderRepository.reminders.first().any { it.id == id }
                if (!isEnabled) return@launch

                val window =
                    (loadState.value as? AlarmLoad.Ready)
                        ?.muhurtas
                        ?.firstOrNull { it.muhurta.name == name }
                        ?.muhurta
                if (window != null) {
                    scheduleAndPersist(id, window.name, window.start, window.quality, minutes)
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

        private fun UpcomingMuhurta.toReminderItem(
            enabledIds: Set<String>,
            offsetsByName: Map<String, Int>,
        ): ReminderItem {
            val id = "$MUHURTA_ID_PREFIX${muhurta.name}"
            return ReminderItem(
                id = id,
                name = muhurta.name,
                start = muhurta.start,
                end = muhurta.end,
                quality = muhurta.quality,
                isEnabled = id in enabledIds,
                isTomorrow = isTomorrow,
                offsetMinutes = offsetsByName[muhurta.name] ?: ReminderRepository.DEFAULT_OFFSET_MINUTES,
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
            const val MUHURTA_ID_PREFIX = "muhurta:"

            // Used when the device location is unavailable (New Delhi).
            val DEFAULT_LOCATION = GeoCoordinates(latitude = 28.6139, longitude = 77.2090)
        }
    }

/** A muhurta window resolved to its next upcoming occurrence, with whether that falls tomorrow. */
private data class UpcomingMuhurta(
    val muhurta: Muhurta,
    val isTomorrow: Boolean,
)

/** UI state for the reminders screen. */
sealed interface AlarmUiState {
    /** The muhurtas are being computed. */
    data object Loading : AlarmUiState

    /** Computation failed; [message] is human-readable. */
    data class Error(
        val message: String,
    ) : AlarmUiState

    /**
     * The next upcoming occurrence of each muhurta, ready to display.
     *
     * @property reminders the windows, each with its enabled state and lead-time offset.
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
 * @property isTomorrow whether this upcoming occurrence falls on the next day (today's has passed).
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
    val isTomorrow: Boolean,
    val offsetMinutes: Int,
)

/** Internal load result, before it is combined with the persisted reminders and lead time. */
private sealed interface AlarmLoad {
    data object Loading : AlarmLoad

    data class Error(
        val message: String,
    ) : AlarmLoad

    data class Ready(
        val muhurtas: List<UpcomingMuhurta>,
        val canScheduleExactAlarms: Boolean,
        val usingDefaultLocation: Boolean,
    ) : AlarmLoad
}
