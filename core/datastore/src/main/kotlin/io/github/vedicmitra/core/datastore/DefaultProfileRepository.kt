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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * [ProfileRepository] backed by a Preferences [DataStore]. Profiles are stored as a set of
 * codec-encoded strings; the primary id is a single string key kept consistent with the set.
 */
class DefaultProfileRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : ProfileRepository {
        override val profiles: Flow<List<BirthProfile>> =
            dataStore.data.map { preferences -> preferences.decodeProfiles() }

        override val primaryProfileId: Flow<String?> =
            dataStore.data.map { preferences -> preferences[PRIMARY_PROFILE_ID] }

        override suspend fun upsert(profile: BirthProfile) {
            dataStore.edit { preferences ->
                val kept = preferences.decodeProfiles().filterNot { it.id == profile.id }
                preferences[PROFILES] = (kept + profile).encode()
                // The first profile ever added becomes the primary "Self".
                if (preferences[PRIMARY_PROFILE_ID] == null) preferences[PRIMARY_PROFILE_ID] = profile.id
            }
        }

        override suspend fun remove(id: String) {
            dataStore.edit { preferences ->
                val remaining = preferences.decodeProfiles().filterNot { it.id == id }
                preferences[PROFILES] = remaining.encode()
                if (preferences[PRIMARY_PROFILE_ID] == id) {
                    val next = remaining.firstOrNull()?.id
                    if (next == null) preferences.remove(PRIMARY_PROFILE_ID) else preferences[PRIMARY_PROFILE_ID] = next
                }
            }
        }

        override suspend fun setPrimary(id: String) {
            dataStore.edit { preferences ->
                if (preferences.decodeProfiles().any { it.id == id }) preferences[PRIMARY_PROFILE_ID] = id
            }
        }

        private fun Preferences.decodeProfiles(): List<BirthProfile> =
            this[PROFILES].orEmpty().mapNotNull(ProfileCodec::decode)

        private fun List<BirthProfile>.encode(): Set<String> = map(ProfileCodec::encode).toSet()

        private companion object {
            val PROFILES = stringSetPreferencesKey("profiles")
            val PRIMARY_PROFILE_ID = stringPreferencesKey("primary_profile_id")
        }
    }
