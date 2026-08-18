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
 * Stores japa (chant-counter) history and the current in-progress mala. Completed sittings accumulate
 * as [JapaSession]s (newest first); the [inProgress] mala is kept separately so a count can be resumed
 * after leaving the screen, and is cleared once the sitting is logged.
 */
interface JapaRepository {
    /** Every logged sitting, newest first. */
    val sessions: Flow<List<JapaSession>>

    /** The mala currently being counted, or `null` when none is in progress. */
    val inProgress: Flow<JapaProgress?>

    /** Saves (or overwrites) the in-progress mala so it can be resumed. */
    suspend fun saveProgress(progress: JapaProgress)

    /** Discards any in-progress mala without logging it. */
    suspend fun clearProgress()

    /** Logs [session] to the history and clears the in-progress mala. */
    suspend fun completeSession(session: JapaSession)
}
