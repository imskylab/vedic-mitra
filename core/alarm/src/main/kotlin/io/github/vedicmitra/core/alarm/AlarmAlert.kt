/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.alarm

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.vedicmitra.core.notifications.AppNotificationChannel

/**
 * Builds and dismisses the ringing-alarm notification for an alarm-mode reminder. The notification
 * is used as the foreground notification of [AlarmService], which owns the actual ringtone so the
 * alarm sounds even when the system demotes the full-screen intent to a heads-up (the default on
 * Android 14+ until the user grants the full-screen-intent permission).
 *
 * It carries a **full-screen intent** so a locked/sleeping device still launches [AlarmActivity]
 * (the dismissable lock-screen UI) when the permission is granted, and a **Dismiss** action that
 * stops the service — the only stop affordance when no activity is shown.
 */
object AlarmAlert {
    /** Builds the ongoing full-screen alarm notification for [id] with [title] and [body]. */
    fun notification(
        context: Context,
        id: Int,
        title: String,
        body: String,
    ): Notification {
        ensureChannel(NotificationManagerCompat.from(context))

        val fullScreen =
            PendingIntent.getActivity(
                context,
                id,
                AlarmActivity.intent(context, id, title, body),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val dismiss =
            PendingIntent.getService(
                context,
                id,
                AlarmService.dismissIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        return NotificationCompat
            .Builder(context, AppNotificationChannel.MUHURTA_ALARMS.id)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Dismiss", dismiss)
            .build()
    }

    /** Cancels the alarm notification for [id] (called when the alarm is dismissed). */
    fun dismiss(
        context: Context,
        id: Int,
    ) {
        NotificationManagerCompat.from(context).cancel(id)
    }

    private fun ensureChannel(manager: NotificationManagerCompat) {
        val channel = AppNotificationChannel.MUHURTA_ALARMS
        manager.createNotificationChannel(
            NotificationChannelCompat
                .Builder(channel.id, channel.importance)
                .setName(channel.channelName)
                .setDescription(channel.description)
                .build(),
        )
    }
}
