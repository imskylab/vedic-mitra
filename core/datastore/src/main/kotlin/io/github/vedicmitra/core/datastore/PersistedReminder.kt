/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.datastore

/**
 * A reminder the user has scheduled, persisted so it can be re-armed after a reboot (which clears
 * the platform's alarms). It carries everything needed to reconstruct the alarm — the exact trigger
 * time and the notification text — so re-arming needs no recomputation.
 *
 * @property id stable id shared with the scheduler and the muhurta it was set for.
 * @property triggerAtEpochMillis when the reminder should fire (already lead-time adjusted).
 * @property title notification title to post when it fires.
 * @property body notification body to post when it fires.
 * @property nickname a user-chosen display name for this reminder, or `null` to use the derived name.
 */
data class PersistedReminder(
    val id: String,
    val triggerAtEpochMillis: Long,
    val title: String,
    val body: String,
    val nickname: String? = null,
)
