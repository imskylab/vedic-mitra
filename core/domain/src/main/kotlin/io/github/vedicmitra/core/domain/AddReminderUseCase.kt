/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.domain

import io.github.vedicmitra.core.astronomy.AstronomyEngine
import io.github.vedicmitra.core.astronomy.Muhurta
import io.github.vedicmitra.core.astronomy.MuhurtaKind
import io.github.vedicmitra.core.common.coroutines.DispatcherProvider
import io.github.vedicmitra.core.common.model.AlertStyle
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.datastore.PersistedReminder
import io.github.vedicmitra.core.datastore.ReminderRepository
import io.github.vedicmitra.core.notifications.AppNotification
import io.github.vedicmitra.core.notifications.AppNotificationChannel
import io.github.vedicmitra.core.scheduler.ScheduledTask
import io.github.vedicmitra.core.scheduler.TaskScheduler
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Schedules and persists a reminder for a panchanga item chosen elsewhere in the app (e.g. from the
 * home screen's lists), resolving its next occurrence via the [AstronomyEngine] and keying it exactly
 * as the reminders screen expects — so the reminder shows up and renews there too.
 *
 * Reminders are created as quiet notifications with the default lead time; the user can switch them
 * to a ringing alarm or change the lead time on the reminders screen.
 */
class AddReminderUseCase
    @Inject
    constructor(
        private val astronomyEngine: AstronomyEngine,
        private val taskScheduler: TaskScheduler,
        private val reminderRepository: ReminderRepository,
        private val dispatchers: DispatcherProvider,
    ) {
        /**
         * Schedules a reminder for the next occurrence of [kind].
         *
         * Keyed on [MuhurtaKind.id], never on the label: the key is persisted, and a key built from
         * display copy is orphaned the moment that copy is translated or respelled.
         */
        suspend fun addMuhurta(
            kind: MuhurtaKind,
            location: GeoCoordinates,
        ): AppResult<Unit> =
            withContext(dispatchers.default) {
                val now = now()
                val label = kind.label
                val start =
                    nextMuhurtaStart(kind, location, now)
                        ?: return@withContext AppResult.Failure(NoSuchElementException("No upcoming $label"))
                schedule("muhurta:${kind.id}", triggerFor(start, now), label, "$label is coming up.")
            }

        /**
         * Schedules a recurring monthly reminder for the observance named [name], which falls on the
         * global [tithis] (1..30) — e.g. Ekadashi on `{11, 26}`.
         */
        suspend fun addObservance(
            name: String,
            tithis: Set<Int>,
            location: GeoCoordinates,
        ): AppResult<Unit> =
            withContext(dispatchers.default) {
                val now = now()
                val sunrise =
                    (
                        astronomyEngine.nextTithiOccurrence(
                            now,
                            location,
                            null,
                            tithis,
                            WINDOW_DAYS,
                        ) as? AppResult.Success
                    )?.data
                        ?: return@withContext AppResult.Failure(NoSuchElementException("No upcoming $name"))
                val key = "tithi:*:${tithis.sorted().joinToString(",")}"
                schedule(key, triggerFor(sunrise, now), name, "$name is coming up.")
            }

        private suspend fun nextMuhurtaStart(
            kind: MuhurtaKind,
            location: GeoCoordinates,
            now: Instant,
        ): Instant? {
            muhurtasAt(now, location).firstOrNull { it.kind == kind && it.start > now }?.let { return it.start }
            return muhurtasAt(now + 1.days, location).firstOrNull { it.kind == kind }?.start
        }

        private suspend fun muhurtasAt(
            instant: Instant,
            location: GeoCoordinates,
        ): List<Muhurta> =
            (astronomyEngine.snapshotAt(instant, location) as? AppResult.Success)?.data?.muhurtas.orEmpty()

        /** The lead-time trigger, falling back to the occurrence itself if the lead is already past. */
        private fun triggerFor(
            occurrence: Instant,
            now: Instant,
        ): Instant {
            val lead = occurrence - ReminderRepository.DEFAULT_OFFSET_MINUTES.minutes
            return if (lead > now) lead else occurrence
        }

        private suspend fun schedule(
            key: String,
            triggerAt: Instant,
            title: String,
            body: String,
        ): AppResult<Unit> {
            val scheduled =
                taskScheduler.schedule(
                    ScheduledTask(
                        id = key,
                        triggerAt = triggerAt,
                        notification =
                            AppNotification(
                                id = key.hashCode(),
                                channel = AppNotificationChannel.MUHURTA_REMINDERS,
                                title = title,
                                body = body,
                                alert = AlertStyle.NOTIFICATION,
                            ),
                    ),
                )
            if (scheduled is AppResult.Failure) return scheduled
            reminderRepository.upsert(
                PersistedReminder(
                    id = key,
                    triggerAtEpochMillis = triggerAt.toEpochMilliseconds(),
                    title = title,
                    body = body,
                ),
            )
            return AppResult.Success(Unit)
        }

        private fun now(): Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())

        private companion object {
            // A little over a year, so any monthly observance resolves to its next occurrence.
            const val WINDOW_DAYS = 400
        }
    }
