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
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** [JapaRepository] backed by the shared preferences [DataStore]. */
class DefaultJapaRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : JapaRepository {
        override val sessions: Flow<List<JapaSession>> =
            dataStore.data.map { preferences -> preferences.decodeSessions() }

        override val inProgress: Flow<JapaProgress?> =
            dataStore.data.map { preferences ->
                val mantraId = preferences[PROGRESS_MANTRA] ?: return@map null
                JapaProgress(mantraId = mantraId, beads = preferences[PROGRESS_BEADS] ?: 0)
            }

        override suspend fun saveProgress(progress: JapaProgress) {
            dataStore.edit { preferences ->
                preferences[PROGRESS_MANTRA] = progress.mantraId
                preferences[PROGRESS_BEADS] = progress.beads
            }
        }

        override suspend fun clearProgress() {
            dataStore.edit { preferences ->
                preferences.remove(PROGRESS_MANTRA)
                preferences.remove(PROGRESS_BEADS)
            }
        }

        override suspend fun completeSession(session: JapaSession) {
            dataStore.edit { preferences ->
                val kept = preferences.decodeSessions()
                preferences[SESSIONS] = (kept + session).map(JapaSessionCodec::encode).toSet()
                preferences.remove(PROGRESS_MANTRA)
                preferences.remove(PROGRESS_BEADS)
            }
        }

        // Decodes the stored sittings, dropping any malformed rows, newest first.
        private fun Preferences.decodeSessions(): List<JapaSession> =
            this[SESSIONS]
                .orEmpty()
                .mapNotNull(JapaSessionCodec::decode)
                .sortedByDescending { it.completedAtEpochMillis }

        private companion object {
            val SESSIONS = stringSetPreferencesKey("japa_sessions")
            val PROGRESS_MANTRA = stringPreferencesKey("japa_progress_mantra")
            val PROGRESS_BEADS = intPreferencesKey("japa_progress_beads")
        }
    }
