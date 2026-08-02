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

import kotlinx.coroutines.flow.Flow

/**
 * Persists the user's scheduled reminders and reminder preferences, so the UI can reflect them and
 * they can be re-armed after a reboot. This stores the *intent* to be reminded; the actual alarm
 * lives in the platform scheduler.
 */
interface ReminderRepository {
    /** The reminders the user currently has scheduled, emitting on every change. */
    val reminders: Flow<List<PersistedReminder>>

    /** How many minutes before a window the reminder should fire (0 = at the window start). */
    val leadTimeMinutes: Flow<Int>

    /** Adds [reminder], replacing any existing reminder with the same [PersistedReminder.id]. */
    suspend fun upsert(reminder: PersistedReminder)

    /** Removes the reminder with [id]. No-op if none is stored. */
    suspend fun remove(id: String)

    /** Drops any reminder whose trigger time is at or before [nowEpochMillis] (already fired/stale). */
    suspend fun removePast(nowEpochMillis: Long)

    /** Sets the reminder lead time in minutes. */
    suspend fun setLeadTimeMinutes(minutes: Int)
}
