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

import android.content.Intent
import io.github.vedicmitra.core.common.model.AlertStyle
import io.github.vedicmitra.core.notifications.AppNotification
import io.github.vedicmitra.core.notifications.AppNotificationChannel

/**
 * The wire format for a scheduled reminder, carried in the broadcast [Intent] between the scheduler
 * (which builds the alarm's PendingIntent) and [ReminderReceiver] (which fires when it elapses).
 *
 * Kept as a small pure mapping so the round-trip can be unit-tested without the alarm machinery.
 */
internal object ReminderIntent {
    const val ACTION = "io.github.vedicmitra.core.scheduler.action.FIRE_REMINDER"

    private const val EXTRA_ID = "extra_notification_id"
    private const val EXTRA_CHANNEL = "extra_channel"
    private const val EXTRA_TITLE = "extra_title"
    private const val EXTRA_BODY = "extra_body"
    private const val EXTRA_ALERT = "extra_alert"

    /** Writes [notification] onto [intent] as extras. */
    fun putNotification(
        intent: Intent,
        notification: AppNotification,
    ): Intent =
        intent.apply {
            putExtra(EXTRA_ID, notification.id)
            putExtra(EXTRA_CHANNEL, notification.channel.name)
            putExtra(EXTRA_TITLE, notification.title)
            putExtra(EXTRA_BODY, notification.body)
            putExtra(EXTRA_ALERT, notification.alert.name)
        }

    /**
     * Reconstructs the [AppNotification] previously written by [putNotification], or `null` if [intent]
     * does not carry one (e.g. a spurious broadcast). An unknown channel name falls back to the first
     * declared channel rather than throwing.
     */
    fun readNotification(intent: Intent): AppNotification? {
        if (!intent.hasExtra(EXTRA_ID)) return null
        val channelName = intent.getStringExtra(EXTRA_CHANNEL)
        val alertName = intent.getStringExtra(EXTRA_ALERT)
        return AppNotification(
            id = intent.getIntExtra(EXTRA_ID, 0),
            channel =
                AppNotificationChannel.entries.firstOrNull { it.name == channelName }
                    ?: AppNotificationChannel.entries.first(),
            title = intent.getStringExtra(EXTRA_TITLE).orEmpty(),
            body = intent.getStringExtra(EXTRA_BODY).orEmpty(),
            alert = AlertStyle.entries.firstOrNull { it.name == alertName } ?: AlertStyle.NOTIFICATION,
        )
    }
}
