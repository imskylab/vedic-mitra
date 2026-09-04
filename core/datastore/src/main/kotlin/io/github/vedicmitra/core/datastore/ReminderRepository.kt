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
     * Lead-time overrides per **source key** — minutes before that window's start a reminder for it
     * should fire (0 = at the window start). Sparse: a key absent here has not been customized and
     * callers should fall back to [DEFAULT_OFFSET_MINUTES].
     *
     * The key is the reminder's own id (`muhurta:<kind id>`, `choghadiya:<TYPE>`, `tithi:...`), not
     * a display name. `name` in the parameter and property names below is historical and misleading:
     * putting a display name here is what orphaned reminders before [LegacyReminderKeys] existed.
     */
    val offsetMinutesByName: Flow<Map<String, Int>>

    /**
     * Alert-style overrides per **source key** — whether that window's reminder fires as a quiet
     * notification or a full-screen ringing alarm. Sparse: a key absent here uses the default
     * [AlertStyle.NOTIFICATION]. Keyed exactly as [offsetMinutesByName] is.
     */
    val alertTypeByName: Flow<Map<String, AlertStyle>>

    /** Adds [reminder], replacing any existing reminder with the same [PersistedReminder.id]. */
    suspend fun upsert(reminder: PersistedReminder)

    /** Removes the reminder with [id]. No-op if none is stored. */
    suspend fun remove(id: String)

    /**
     * Sets the user-chosen display name for the reminder with [id]; a blank or `null` [nickname]
     * clears it (reverting to the derived name). No-op if no reminder with [id] is stored.
     */
    suspend fun setNickname(
        id: String,
        nickname: String?,
    )

    /** Drops any reminder whose trigger time is at or before [nowEpochMillis] (already fired/stale). */
    suspend fun removePast(nowEpochMillis: Long)

    /** Sets the lead time in minutes for the source key [name] (0 = at the window start). */
    suspend fun setOffsetMinutes(
        name: String,
        minutes: Int,
    )

    /** Sets whether the source key [name] alerts as a notification or a ringing alarm. */
    suspend fun setAlertType(
        name: String,
        alert: AlertStyle,
    )

    companion object {
        /** Lead time, in minutes, used for any muhurta without an explicit override. */
        const val DEFAULT_OFFSET_MINUTES = 10
    }
}
