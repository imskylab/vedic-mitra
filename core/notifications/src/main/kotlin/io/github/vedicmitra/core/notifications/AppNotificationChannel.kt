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

import androidx.core.app.NotificationManagerCompat

/**
 * The set of notification channels the app posts to. Every notification targets exactly one of
 * these, giving users per-category control in system settings. Adding a category means adding an
 * entry here; the [Notifier] creates the channel on demand before its first post.
 *
 * @property id stable channel id persisted by the system — never rename an existing value, as that
 *   orphans the user's per-channel settings; add a new entry instead.
 * @property channelName user-visible channel name shown in system notification settings.
 * @property description user-visible explanation of what the channel is used for.
 * @property importance one of the `NotificationManagerCompat.IMPORTANCE_*` levels.
 */
enum class AppNotificationChannel(
    val id: String,
    val channelName: String,
    val description: String,
    val importance: Int,
) {
    /** Alerts that an auspicious time window (muhurta) is about to begin. */
    MUHURTA_REMINDERS(
        id = "muhurta_reminders",
        channelName = "Muhurta reminders",
        description = "Alerts for upcoming auspicious time windows (muhurta).",
        importance = NotificationManagerCompat.IMPORTANCE_HIGH,
    ),
}
