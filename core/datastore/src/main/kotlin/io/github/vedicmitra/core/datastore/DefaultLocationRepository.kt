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

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import io.github.vedicmitra.core.common.model.SavedLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * [LocationRepository] backed by a Preferences [DataStore]. Saved locations are stored as a set of
 * codec-encoded strings; the selected id is a single string key.
 */
class DefaultLocationRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : LocationRepository {
        override val savedLocations: Flow<List<SavedLocation>> =
            dataStore.data.map { preferences -> preferences.decodeLocations() }

        override val selectedLocationId: Flow<String?> =
            dataStore.data.map { preferences -> preferences[SELECTED_LOCATION_ID] }

        override suspend fun upsert(location: SavedLocation) {
            dataStore.edit { preferences ->
                val kept = preferences.decodeLocations().filterNot { it.id == location.id }
                preferences[SAVED_LOCATIONS] = (kept + location).encode()
            }
        }

        override suspend fun remove(id: String) {
            dataStore.edit { preferences ->
                preferences[SAVED_LOCATIONS] = preferences.decodeLocations().filterNot { it.id == id }.encode()
                if (preferences[SELECTED_LOCATION_ID] == id) preferences.remove(SELECTED_LOCATION_ID)
            }
        }

        override suspend fun select(id: String) {
            dataStore.edit { preferences -> preferences[SELECTED_LOCATION_ID] = id }
        }

        override suspend fun clearSelection() {
            dataStore.edit { preferences -> preferences.remove(SELECTED_LOCATION_ID) }
        }

        private fun Preferences.decodeLocations(): List<SavedLocation> =
            this[SAVED_LOCATIONS].orEmpty().mapNotNull(SavedLocationCodec::decode)

        private fun List<SavedLocation>.encode(): Set<String> = map(SavedLocationCodec::encode).toSet()

        private companion object {
            val SAVED_LOCATIONS = stringSetPreferencesKey("saved_locations")
            val SELECTED_LOCATION_ID = stringPreferencesKey("selected_location_id")
        }
    }
