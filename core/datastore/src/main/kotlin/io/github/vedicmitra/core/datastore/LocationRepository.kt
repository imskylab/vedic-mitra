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

import io.github.vedicmitra.core.common.model.SavedLocation
import kotlinx.coroutines.flow.Flow

/**
 * Persists the user's saved locations and which one is currently selected, so the panchanga can be
 * computed for a place other than the device's GPS position. This stores the user's *choice*; the
 * device location itself is read live from the location port.
 */
interface LocationRepository {
    /** The locations the user has saved, emitting on every change. */
    val savedLocations: Flow<List<SavedLocation>>

    /**
     * The id of the currently selected saved location, or `null` when the user has not chosen one
     * (in which case callers fall back to the device location).
     */
    val selectedLocationId: Flow<String?>

    /** Adds [location], replacing any existing one with the same [SavedLocation.id]. */
    suspend fun upsert(location: SavedLocation)

    /** Removes the saved location with [id], clearing the selection if it was the selected one. */
    suspend fun remove(id: String)

    /** Marks the saved location with [id] as the selected one. */
    suspend fun select(id: String)

    /** Clears the selection, so callers fall back to the device location. */
    suspend fun clearSelection()
}
