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

import io.github.vedicmitra.core.common.model.AlertStyle
import kotlinx.coroutines.flow.Flow

/**
 * Persists the user's scheduled reminders and reminder preferences, so the UI can reflect them and
 * they can be re-armed after a reboot. This stores the *intent* to be reminded; the actual alarm
 * lives in the platform scheduler.
 */
interface ReminderRepository {
    /** The reminders the user currently has scheduled, emitting on every change. */
    val reminders: Flow<List<PersistedReminder>>

    /**
     * Per-muhurta-name lead-time overrides — minutes before that window's start a reminder for it
     * should fire (0 = at the window start). Sparse: a name absent here has not been customized and
     * callers should fall back to [DEFAULT_OFFSET_MINUTES].
     */
    val offsetMinutesByName: Flow<Map<String, Int>>

    /**
     * Per-muhurta-name alert-style overrides — whether that muhurta's reminder fires as a quiet
     * notification or a full-screen ringing alarm. Sparse: a name absent here uses the default
     * [AlertStyle.NOTIFICATION].
     */
    val alertTypeByName: Flow<Map<String, AlertStyle>>

    /** Adds [reminder], replacing any existing reminder with the same [PersistedReminder.id]. */
    suspend fun upsert(reminder: PersistedReminder)

    /** Removes the reminder with [id]. No-op if none is stored. */
    suspend fun remove(id: String)

    /** Drops any reminder whose trigger time is at or before [nowEpochMillis] (already fired/stale). */
    suspend fun removePast(nowEpochMillis: Long)

    /** Sets the lead time in minutes for the muhurta named [name] (0 = at the window start). */
    suspend fun setOffsetMinutes(
        name: String,
        minutes: Int,
    )

    /** Sets whether the muhurta named [name] alerts as a notification or a ringing alarm. */
    suspend fun setAlertType(
        name: String,
        alert: AlertStyle,
    )

    companion object {
        /** Lead time, in minutes, used for any muhurta without an explicit override. */
        const val DEFAULT_OFFSET_MINUTES = 10
    }
}
