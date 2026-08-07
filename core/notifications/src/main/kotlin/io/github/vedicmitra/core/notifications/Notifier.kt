/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.notifications

import io.github.vedicmitra.core.common.model.AlertStyle
import io.github.vedicmitra.core.common.result.AppResult

/**
 * Port for presenting notifications to the user. Implementations own channel creation and the
 * platform notification wiring; callers describe *what* to show via [AppNotification] and handle the
 * returned [AppResult] (posting fails, rather than throwing, when notifications are disabled).
 */
interface Notifier {
    /** Posts (or updates) the notification described by [notification]. */
    suspend fun show(notification: AppNotification): AppResult<Unit>

    /** Dismisses a previously shown notification by its [id]. */
    suspend fun cancel(id: Int): AppResult<Unit>
}

/**
 * A user-facing notification request.
 *
 * @property id stable identifier used to update or cancel the notification.
 * @property channel the channel the notification is posted to.
 * @property title short headline text.
 * @property body body text.
 * @property alert how the reminder should alert the user when it fires (quiet notification, or a
 *   full-screen ringing alarm). The scheduler and receiver branch on this.
 */
data class AppNotification(
    val id: Int,
    val channel: AppNotificationChannel,
    val title: String,
    val body: String,
    val alert: AlertStyle = AlertStyle.NOTIFICATION,
)
