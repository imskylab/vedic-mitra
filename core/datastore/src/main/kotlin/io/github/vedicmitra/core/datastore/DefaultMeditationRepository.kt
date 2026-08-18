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
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** [MeditationRepository] backed by the shared preferences [DataStore]. */
class DefaultMeditationRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : MeditationRepository {
        override val sessions: Flow<List<MeditationSession>> =
            dataStore.data.map { preferences -> preferences.decodeSessions() }

        override suspend fun add(session: MeditationSession) {
            dataStore.edit { preferences ->
                val kept = preferences.decodeSessions()
                preferences[SESSIONS] = (kept + session).map(MeditationSessionCodec::encode).toSet()
            }
        }

        // Decodes the stored sits, dropping any malformed rows, newest first.
        private fun Preferences.decodeSessions(): List<MeditationSession> =
            this[SESSIONS]
                .orEmpty()
                .mapNotNull(MeditationSessionCodec::decode)
                .sortedByDescending { it.completedAtEpochMillis }

        private companion object {
            val SESSIONS = stringSetPreferencesKey("meditation_sessions")
        }
    }
