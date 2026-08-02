/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

package io.github.vedicmitra.core.notifications

import io.github.vedicmitra.core.common.result.AppResult

/**
 * Port for presenting notifications to the user.
 *
 * **No notification code is implemented in Phase 1** — this declares only the contract. The
 * concrete implementation (channel creation, [android.app.NotificationManager] wiring) is added in
 * the notifications implementation phase.
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
 * @property channelId the channel the notification is posted to.
 * @property title short headline text.
 * @property body body text.
 */
data class AppNotification(
    val id: Int,
    val channelId: String,
    val title: String,
    val body: String,
)
