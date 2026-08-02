/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.vedicmitra.core.common.coroutines.DispatcherProvider
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.notifications.AppNotification
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [TaskScheduler] backed by [AlarmManager].
 *
 * Each task becomes a broadcast [PendingIntent] addressed to [ReminderReceiver], keyed by a request
 * code derived from [ScheduledTask.id] so rescheduling the same id replaces its alarm and [cancel]
 * can target it. Exact delivery uses `setExactAndAllowWhileIdle`; when exact alarms are not
 * permitted (API 31+ without `SCHEDULE_EXACT_ALARM`) it degrades to `setAndAllowWhileIdle` rather
 * than throwing, so a reminder is still delivered — just not to the minute.
 */
@Singleton
class DefaultTaskScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val alarmManager: AlarmManager,
        private val dispatchers: DispatcherProvider,
    ) : TaskScheduler {
        override suspend fun schedule(request: ScheduledTask): AppResult<Unit> =
            withContext(dispatchers.io) {
                runCatching {
                    val operation = pendingIntent(request.id, request.notification)
                    val triggerAtMillis = request.triggerAt.toEpochMilliseconds()
                    if (request.exact && canScheduleExactAlarms()) {
                        AlarmManagerCompat.setExactAndAllowWhileIdle(
                            alarmManager,
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            operation,
                        )
                    } else {
                        AlarmManagerCompat.setAndAllowWhileIdle(
                            alarmManager,
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            operation,
                        )
                    }
                }.fold(
                    onSuccess = { AppResult.Success(Unit) },
                    onFailure = { AppResult.Failure(it) },
                )
            }

        override suspend fun cancel(id: String): AppResult<Unit> =
            withContext(dispatchers.io) {
                runCatching {
                    existingPendingIntent(id)?.let { operation ->
                        alarmManager.cancel(operation)
                        operation.cancel()
                    }
                }.fold(
                    onSuccess = { AppResult.Success(Unit) },
                    onFailure = { AppResult.Failure(it) },
                )
            }

        override fun canScheduleExactAlarms(): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

        /** Builds (creating if needed) the alarm's broadcast intent, carrying the notification. */
        private fun pendingIntent(
            id: String,
            notification: AppNotification,
        ): PendingIntent {
            val intent = reminderIntent().also { ReminderIntent.putNotification(it, notification) }
            return PendingIntent.getBroadcast(
                context,
                id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        /** Resolves the existing PendingIntent for [id] without creating one, or null if none. */
        private fun existingPendingIntent(id: String): PendingIntent? =
            PendingIntent.getBroadcast(
                context,
                id.hashCode(),
                reminderIntent(),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )

        // The request code (id.hashCode()) is what distinguishes one task's PendingIntent from
        // another and lets cancel/replace target it; the intent itself only needs to name the
        // receiver and action. (Intent.identifier would be cleaner but is API 29+, and minSdk is 26.)
        private fun reminderIntent(): Intent =
            Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderIntent.ACTION
                setPackage(context.packageName)
            }
    }
