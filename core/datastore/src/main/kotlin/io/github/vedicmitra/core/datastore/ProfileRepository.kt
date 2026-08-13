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
 * Persists the user's birth [BirthProfile]s and which one is the primary "Self" profile. The app
 * always keeps a primary while any profiles exist: the first profile added becomes primary, and
 * removing the primary promotes another.
 */
interface ProfileRepository {
    /** The saved profiles, in insertion order, emitting on every change. */
    val profiles: Flow<List<BirthProfile>>

    /** The id of the primary "Self" profile, or `null` when there are no profiles yet. */
    val primaryProfileId: Flow<String?>

    /** Adds [profile], replacing any existing one with the same [BirthProfile.id]. */
    suspend fun upsert(profile: BirthProfile)

    /** Removes the profile with [id], promoting another to primary if it was the primary. */
    suspend fun remove(id: String)

    /** Marks the profile with [id] as the primary "Self" profile. */
    suspend fun setPrimary(id: String)
}
