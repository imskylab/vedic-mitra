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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Instant

/**
 * Presentation logic for the reminders screen (MVVM). Loads today's muhurta windows for the device
 * location and, combined with the user's enabled-reminder set, exposes a togglable list. Toggling a
 * reminder schedules (or cancels) an exact alarm and persists the choice.
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
            combine(loadState, reminderRepository.enabledReminderIds) { load, enabledIds ->
                when (load) {
                    AlarmLoad.Loading -> AlarmUiState.Loading
                    is AlarmLoad.Error -> AlarmUiState.Error(load.message)
                    is AlarmLoad.Ready ->
                        AlarmUiState.Ready(
                            reminders = load.muhurtas.map { it.toReminderItem(enabledIds, load.now) },
                            canScheduleExactAlarms = load.canScheduleExactAlarms,
                            usingDefaultLocation = load.usingDefaultLocation,
                        )
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
         * Enables or disables the reminder for [item]. Enabling schedules an exact alarm to post a
         * notification when the window begins; disabling cancels it. Past windows are ignored.
         */
        fun setReminder(
            item: ReminderItem,
            enabled: Boolean,
        ) {
            if (enabled && item.isPast) return
            viewModelScope.launch {
                if (enabled) {
                    taskScheduler.schedule(
                        ScheduledTask(
                            id = item.id,
                            triggerAt = item.start,
                            notification =
                                AppNotification(
                                    id = item.id.hashCode(),
                                    channel = AppNotificationChannel.MUHURTA_REMINDERS,
                                    title = item.name,
                                    body = item.quality.reminderBody(),
                                ),
                        ),
                    )
                    reminderRepository.setEnabled(item.id, enabled = true)
                } else {
                    taskScheduler.cancel(item.id)
                    reminderRepository.setEnabled(item.id, enabled = false)
                }
            }
        }

        private fun Muhurta.toReminderItem(
            enabledIds: Set<String>,
            now: Instant,
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
            )
        }

        private fun MuhurtaQuality.reminderBody(): String =
            when (this) {
                MuhurtaQuality.AUSPICIOUS -> "This auspicious window is beginning now."
                MuhurtaQuality.INAUSPICIOUS -> "This window to be mindful of is beginning now."
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
     * @property reminders the day's windows, each with its enabled state.
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
 * @property start when the window begins (and when the reminder fires).
 * @property end when the window ends.
 * @property quality whether the window is auspicious or inauspicious.
 * @property isEnabled whether the user has a reminder set for it.
 * @property isPast whether the window has already begun (a reminder can no longer be set).
 */
data class ReminderItem(
    val id: String,
    val name: String,
    val start: Instant,
    val end: Instant,
    val quality: MuhurtaQuality,
    val isEnabled: Boolean,
    val isPast: Boolean,
)

/** Internal load result, before it is combined with the enabled-reminder set. */
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
