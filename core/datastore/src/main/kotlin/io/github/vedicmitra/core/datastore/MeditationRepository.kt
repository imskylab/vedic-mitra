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
 * Stores meditation history. Completed sits accumulate as [MeditationSession]s (newest first); unlike
 * japa there is no in-progress state to resume — a sit is logged only once it finishes.
 */
interface MeditationRepository {
    /** Every logged sit, newest first. */
    val sessions: Flow<List<MeditationSession>>

    /** Logs [session] to the history. */
    suspend fun add(session: MeditationSession)
}
