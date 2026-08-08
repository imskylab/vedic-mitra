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
import io.github.vedicmitra.core.astronomy.AstronomySnapshot
import io.github.vedicmitra.core.astronomy.MuhurtaQuality
import io.github.vedicmitra.core.common.model.AlertStyle
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
 * Presentation logic for the reminders screen (MVVM), clock-app style: the user **adds** reminders
 * from a catalog of the day's periods — the sun/weekday **muhurtas** (Brahma, Abhijit, Rahu Kalam,
 * …) and the **Choghadiya** windows (Amrit, Shubh, …) — and the screen lists only what has been
 * added. Each reminder is resolved to its **next upcoming** occurrence (today if still ahead, else
 * tomorrow), so it can always be set and never shows as passed. Every [load] also renews the added
 * reminders onto their next occurrence, rolling fired ones forward.
 *
 * A source is identified by a stable key: `muhurta:<name>` or `choghadiya:<TYPE>`. Lead-time and
 * alert-style overrides, and the scheduled alarm, are all keyed by it.
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
                reminderRepository.alertTypeByName,
            ) { load, reminders, offsets, alerts ->
                when (load) {
                    AlarmLoad.Loading -> AlarmUiState.Loading
                    is AlarmLoad.Error -> AlarmUiState.Error(load.message)
                    is AlarmLoad.Ready -> readyState(load, reminders.map { it.id }.toSet(), offsets, alerts)
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = AlarmUiState.Loading,
            )

        private fun readyState(
            load: AlarmLoad.Ready,
            addedKeys: Set<String>,
            offsets: Map<String, Int>,
            alerts: Map<String, AlertStyle>,
        ): AlarmUiState.Ready {
            val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
            val reminders =
                addedKeys
                    .mapNotNull { key ->
                        load.periods.nextWindow(key, now)?.toReminderItem(
                            key,
                            offsets[key] ?: ReminderRepository.DEFAULT_OFFSET_MINUTES,
                            alerts[key] ?: AlertStyle.NOTIFICATION,
                        )
                    }.sortedBy { it.start.toEpochMilliseconds() }
            return AlarmUiState.Ready(
                reminders = reminders,
                available = load.periods.catalog.filterNot { it.key in addedKeys },
                canScheduleExactAlarms = load.canScheduleExactAlarms,
                usingDefaultLocation = load.usingDefaultLocation,
            )
        }

        /** (Re)loads today's and tomorrow's periods, then renews the added reminders onto them. */
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
                        val periods = DayPeriods(periodsOf(today.data), periodsOf(tomorrow.data))
                        renewAddedReminders(periods, now)
                        reminderRepository.removePast(now.toEpochMilliseconds())
                        AlarmLoad.Ready(periods, taskScheduler.canScheduleExactAlarms(), usingDefault)
                    } else {
                        val message =
                            (today as? AppResult.Failure)?.cause?.message
                                ?: (tomorrow as? AppResult.Failure)?.cause?.message
                                ?: "Unknown error"
                        AlarmLoad.Error(message)
                    }
            }
        }

        /** Adds a reminder for the period [key], scheduling its next occurrence with saved settings. */
        fun addReminder(key: String) {
            viewModelScope.launch { rescheduleFor(key) }
        }

        /** Removes the reminder [id]: cancels its alarm and forgets it. */
        fun removeReminder(id: String) {
            viewModelScope.launch {
                taskScheduler.cancel(id)
                reminderRepository.remove(id)
            }
        }

        /** Sets the lead time for [key] and, if it is an added reminder, re-schedules it immediately. */
        fun setOffsetMinutes(
            key: String,
            minutes: Int,
        ) {
            viewModelScope.launch {
                reminderRepository.setOffsetMinutes(key, minutes)
                if (isAdded(key)) rescheduleFor(key)
            }
        }

        /** Sets the alert style for [key] and, if it is an added reminder, re-schedules it. */
        fun setAlertType(
            key: String,
            alert: AlertStyle,
        ) {
            viewModelScope.launch {
                reminderRepository.setAlertType(key, alert)
                if (isAdded(key)) rescheduleFor(key)
            }
        }

        private suspend fun isAdded(key: String): Boolean = reminderRepository.reminders.first().any { it.id == key }

        /** Renews every added reminder onto its next occurrence in [periods]. */
        private suspend fun renewAddedReminders(
            periods: DayPeriods,
            now: Instant,
        ) {
            val persisted = reminderRepository.reminders.first()
            if (persisted.isEmpty()) return
            val offsets = reminderRepository.offsetMinutesByName.first()
            val alerts = reminderRepository.alertTypeByName.first()
            persisted.forEach { reminder ->
                val window = periods.nextWindow(reminder.id, now) ?: return@forEach
                scheduleAndPersist(
                    reminder.id,
                    window,
                    offsets[reminder.id] ?: ReminderRepository.DEFAULT_OFFSET_MINUTES,
                    alerts[reminder.id] ?: AlertStyle.NOTIFICATION,
                )
            }
        }

        /** Resolves [key]'s next window from the loaded state and (re)schedules + persists it. */
        private suspend fun rescheduleFor(key: String) {
            val periods = (loadState.value as? AlarmLoad.Ready)?.periods ?: return
            val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
            val window = periods.nextWindow(key, now) ?: return
            val offset =
                reminderRepository.offsetMinutesByName.first()[key] ?: ReminderRepository.DEFAULT_OFFSET_MINUTES
            val alert = reminderRepository.alertTypeByName.first()[key] ?: AlertStyle.NOTIFICATION
            scheduleAndPersist(key, window, offset, alert)
        }

        /** Computes the lead-adjusted trigger time, schedules the alarm, and persists the reminder. */
        private suspend fun scheduleAndPersist(
            key: String,
            window: ResolvedWindow,
            offsetMinutes: Int,
            alert: AlertStyle,
        ) {
            val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
            val triggerAt = maxOf(window.period.start - offsetMinutes.minutes, now)
            val body = reminderBody(window.period.label, window.period.quality, offsetMinutes)

            taskScheduler.schedule(
                ScheduledTask(
                    id = key,
                    triggerAt = triggerAt,
                    notification =
                        AppNotification(
                            id = key.hashCode(),
                            channel = AppNotificationChannel.MUHURTA_REMINDERS,
                            title = window.period.label,
                            body = body,
                            alert = alert,
                        ),
                ),
            )
            reminderRepository.upsert(
                PersistedReminder(
                    id = key,
                    triggerAtEpochMillis = triggerAt.toEpochMilliseconds(),
                    title = window.period.label,
                    body = body,
                ),
            )
        }

        private fun ResolvedWindow.toReminderItem(
            key: String,
            offsetMinutes: Int,
            alert: AlertStyle,
        ): ReminderItem =
            ReminderItem(
                id = key,
                name = period.label,
                start = period.start,
                end = period.end,
                quality = period.quality,
                isTomorrow = isTomorrow,
                offsetMinutes = offsetMinutes,
                alertType = alert,
            )

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

/** Builds the day's period windows (muhurtas + Choghadiya) with their stable source keys. */
private fun periodsOf(snapshot: AstronomySnapshot): List<PeriodWindow> =
    snapshot.muhurtas.map {
        PeriodWindow("muhurta:${it.name}", it.name, it.start, it.end, it.quality)
    } +
        snapshot.choghadiya.map {
            PeriodWindow("choghadiya:${it.name.name}", it.name.label, it.start, it.end, it.quality)
        }

/** One selectable period occurrence on a given day, keyed by its source. */
private data class PeriodWindow(
    val key: String,
    val label: String,
    val start: Instant,
    val end: Instant,
    val quality: MuhurtaQuality,
)

/** A period resolved to a concrete upcoming occurrence, with whether it falls tomorrow. */
private data class ResolvedWindow(
    val period: PeriodWindow,
    val isTomorrow: Boolean,
)

/** Today's and tomorrow's period windows, with helpers to resolve the next upcoming one per key. */
private class DayPeriods(
    private val today: List<PeriodWindow>,
    private val tomorrow: List<PeriodWindow>,
) {
    /** The distinct sources available to add, in a stable order (auspicious first, then by label). */
    val catalog: List<SourceOption> =
        (today + tomorrow)
            .distinctBy { it.key }
            .sortedWith(compareBy({ it.quality != MuhurtaQuality.AUSPICIOUS }, { it.label }))
            .map { SourceOption(it.key, it.label, it.quality) }

    /** The next occurrence of [key] after [now] — today if still ahead, else tomorrow's first. */
    fun nextWindow(
        key: String,
        now: Instant,
    ): ResolvedWindow? {
        today.filter { it.key == key && it.start > now }.minByOrNull { it.start }?.let {
            return ResolvedWindow(it, isTomorrow = false)
        }
        tomorrow.filter { it.key == key }.minByOrNull { it.start }?.let {
            return ResolvedWindow(it, isTomorrow = true)
        }
        return null
    }
}

/** UI state for the reminders screen. */
sealed interface AlarmUiState {
    /** The periods are being computed. */
    data object Loading : AlarmUiState

    /** Computation failed; [message] is human-readable. */
    data class Error(
        val message: String,
    ) : AlarmUiState

    /**
     * @property reminders the added reminders, each resolved to its next occurrence, by time.
     * @property available the sources not yet added, for the "add reminder" picker.
     * @property canScheduleExactAlarms whether exact alarms are permitted; when false the UI should
     *   invite the user to grant the permission.
     * @property usingDefaultLocation whether a default location was used (device location
     *   unavailable).
     */
    data class Ready(
        val reminders: List<ReminderItem>,
        val available: List<SourceOption>,
        val canScheduleExactAlarms: Boolean,
        val usingDefaultLocation: Boolean,
    ) : AlarmUiState
}

/**
 * A period the user can add a reminder for.
 *
 * @property key the stable source key (`muhurta:<name>` or `choghadiya:<TYPE>`).
 * @property label the display name.
 * @property quality whether the period is auspicious or inauspicious.
 */
data class SourceOption(
    val key: String,
    val label: String,
    val quality: MuhurtaQuality,
)

/**
 * An added reminder as shown on the screen, resolved to its next occurrence.
 *
 * @property id the source key used to schedule/cancel and persist the reminder.
 * @property name the period's display name.
 * @property start when the next window begins.
 * @property end when the next window ends.
 * @property quality whether the window is auspicious or inauspicious.
 * @property isTomorrow whether this occurrence falls on the next day (today's has passed).
 * @property offsetMinutes minutes before [start] this reminder fires (0 = at start).
 * @property alertType whether this reminder alerts as a notification or a ringing alarm.
 */
data class ReminderItem(
    val id: String,
    val name: String,
    val start: Instant,
    val end: Instant,
    val quality: MuhurtaQuality,
    val isTomorrow: Boolean,
    val offsetMinutes: Int,
    val alertType: AlertStyle,
)

/** Internal load result, before it is combined with the persisted reminders and preferences. */
private sealed interface AlarmLoad {
    data object Loading : AlarmLoad

    data class Error(
        val message: String,
    ) : AlarmLoad

    data class Ready(
        val periods: DayPeriods,
        val canScheduleExactAlarms: Boolean,
        val usingDefaultLocation: Boolean,
    ) : AlarmLoad
}
