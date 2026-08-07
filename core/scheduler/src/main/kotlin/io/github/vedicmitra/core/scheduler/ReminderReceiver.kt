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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import io.github.vedicmitra.core.alarm.AlarmAlert
import io.github.vedicmitra.core.common.coroutines.DispatcherProvider
import io.github.vedicmitra.core.common.model.AlertStyle
import io.github.vedicmitra.core.notifications.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fires when a scheduled alarm elapses and posts the reminder's notification via [Notifier].
 *
 * A broadcast receiver's [onReceive] runs on the main thread and the process may be torn down as
 * soon as it returns, so the actual (suspending) post is hoisted onto a coroutine wrapped in
 * [goAsync]'s keep-alive, which is released in `finally` once the post completes.
 */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject
    lateinit var notifier: Notifier

    @Inject
    lateinit var dispatchers: DispatcherProvider

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val notification = ReminderIntent.readNotification(intent) ?: return

        // Alarm-mode reminders ring full-screen; the rest post a quiet notification.
        if (notification.alert == AlertStyle.ALARM) {
            AlarmAlert.raise(context, notification.id, notification.title, notification.body)
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + dispatchers.default).launch {
            try {
                notifier.show(notification)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
