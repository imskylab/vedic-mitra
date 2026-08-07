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

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.vedicmitra.core.notifications.AppNotificationChannel

/**
 * Raises and dismisses the full-screen ringing alarm for an alarm-mode reminder. Called by the
 * scheduler's receiver when such a reminder fires.
 *
 * It posts a **full-screen-intent** notification on a high-importance channel: on a locked or
 * sleeping device the system launches [AlarmActivity] directly (which rings); otherwise it shows as
 * a heads-up the user taps to open the same activity.
 */
object AlarmAlert {
    /** Posts the full-screen alarm notification for [id] with [title] and [body]. */
    fun raise(
        context: Context,
        id: Int,
        title: String,
        body: String,
    ) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        ensureChannel(manager)

        val fullScreen =
            PendingIntent.getActivity(
                context,
                id,
                AlarmActivity.intent(context, id, title, body),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat
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
                .build()
        manager.notify(id, notification)
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
