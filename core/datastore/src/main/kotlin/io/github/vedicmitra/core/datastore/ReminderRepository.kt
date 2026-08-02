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
 * Tracks which reminders the user has enabled, so the UI can reflect them across app restarts. This
 * is the *intent* to be reminded; the actual alarm lives in the platform scheduler.
 */
interface ReminderRepository {
    /** The ids of the reminders the user currently has enabled, emitting on every change. */
    val enabledReminderIds: Flow<Set<String>>

    /** Adds or removes [id] from the enabled set. */
    suspend fun setEnabled(
        id: String,
        enabled: Boolean,
    )
}
