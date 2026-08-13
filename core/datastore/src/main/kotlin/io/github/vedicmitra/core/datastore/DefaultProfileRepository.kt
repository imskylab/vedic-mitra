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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/**
 * [ProfileRepository] backed by the shared Preferences [DataStore]. Dates and times are stored as
 * ISO-8601 strings; an unparseable value reads back as `null` rather than throwing.
 */
class DefaultProfileRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : ProfileRepository {
        override val profile: Flow<UserProfile> =
            dataStore.data.map { preferences ->
                UserProfile(
                    name = preferences[NAME].orEmpty(),
                    dateOfBirth = preferences[DATE_OF_BIRTH]?.toLocalDateOrNull(),
                    timeOfBirth = preferences[TIME_OF_BIRTH]?.toLocalTimeOrNull(),
                    placeOfBirth = preferences[PLACE_OF_BIRTH].orEmpty(),
                )
            }

        override suspend fun setProfile(profile: UserProfile) {
            dataStore.edit { preferences ->
                preferences[NAME] = profile.name
                preferences[PLACE_OF_BIRTH] = profile.placeOfBirth
                profile.dateOfBirth
                    ?.let { preferences[DATE_OF_BIRTH] = it.toString() }
                    ?: preferences.remove(DATE_OF_BIRTH)
                profile.timeOfBirth
                    ?.let { preferences[TIME_OF_BIRTH] = it.toString() }
                    ?: preferences.remove(TIME_OF_BIRTH)
            }
        }

        private companion object {
            val NAME = stringPreferencesKey("profile_name")
            val DATE_OF_BIRTH = stringPreferencesKey("profile_date_of_birth")
            val TIME_OF_BIRTH = stringPreferencesKey("profile_time_of_birth")
            val PLACE_OF_BIRTH = stringPreferencesKey("profile_place_of_birth")

            fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

            fun String.toLocalTimeOrNull(): LocalTime? = runCatching { LocalTime.parse(this) }.getOrNull()
        }
    }
