/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.location

import io.github.vedicmitra.core.common.model.SavedLocation
import io.github.vedicmitra.core.datastore.LocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [LocationRepository] fake shared by the location feature's ViewModel tests. */
internal class FakeLocationRepository(
    initial: List<SavedLocation> = emptyList(),
    selected: String? = null,
) : LocationRepository {
    private val saved = MutableStateFlow(initial)
    private val selectedId = MutableStateFlow(selected)

    override val savedLocations: StateFlow<List<SavedLocation>> = saved.asStateFlow()
    override val selectedLocationId: StateFlow<String?> = selectedId.asStateFlow()

    override suspend fun upsert(location: SavedLocation) {
        saved.update { current -> current.filterNot { it.id == location.id } + location }
    }

    override suspend fun remove(id: String) {
        saved.update { current -> current.filterNot { it.id == id } }
        if (selectedId.value == id) selectedId.value = null
    }

    override suspend fun select(id: String) {
        selectedId.value = id
    }

    override suspend fun clearSelection() {
        selectedId.value = null
    }
}
