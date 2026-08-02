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

import io.github.vedicmitra.core.datastore.ReminderRepository
import io.github.vedicmitra.core.notifications.AppNotification
import io.github.vedicmitra.core.notifications.AppNotificationChannel
import io.github.vedicmitra.core.scheduler.ScheduledTask
import io.github.vedicmitra.core.scheduler.TaskScheduler
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.time.Instant

/**
 * Re-arms the user's persisted reminders — used after a reboot, which clears the platform's alarms.
 * Because each [io.github.vedicmitra.core.datastore.PersistedReminder] carries its own trigger time
 * and notification text, re-arming needs no astronomy or location recomputation.
 */
class ReminderRescheduler
    @Inject
    constructor(
        private val reminderRepository: ReminderRepository,
        private val taskScheduler: TaskScheduler,
    ) {
        /** Drops reminders that have already fired, then re-schedules the rest. */
        suspend fun rescheduleEnabled(nowEpochMillis: Long) {
            reminderRepository.removePast(nowEpochMillis)
            reminderRepository.reminders.first().forEach { reminder ->
                taskScheduler.schedule(
                    ScheduledTask(
                        id = reminder.id,
                        triggerAt = Instant.fromEpochMilliseconds(reminder.triggerAtEpochMillis),
                        notification =
                            AppNotification(
                                id = reminder.id.hashCode(),
                                channel = AppNotificationChannel.MUHURTA_REMINDERS,
                                title = reminder.title,
                                body = reminder.body,
                            ),
                    ),
                )
            }
        }
    }
