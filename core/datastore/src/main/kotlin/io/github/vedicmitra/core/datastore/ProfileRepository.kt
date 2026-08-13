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

/** Reads and updates the user's persisted birth [UserProfile]. */
interface ProfileRepository {
    /** The user's current profile, emitting on every change. */
    val profile: Flow<UserProfile>

    /** Persists the given [profile], replacing what was stored. */
    suspend fun setProfile(profile: UserProfile)
}
