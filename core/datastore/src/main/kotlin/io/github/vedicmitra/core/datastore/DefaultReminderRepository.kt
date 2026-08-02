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

/** [ReminderRepository] backed by a Preferences [DataStore], storing the enabled ids as a string set. */
class DefaultReminderRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : ReminderRepository {
        override val enabledReminderIds: Flow<Set<String>> =
            dataStore.data.map { preferences -> preferences[ENABLED_REMINDER_IDS].orEmpty() }

        override suspend fun setEnabled(
            id: String,
            enabled: Boolean,
        ) {
            dataStore.edit { preferences ->
                val current = preferences[ENABLED_REMINDER_IDS].orEmpty()
                preferences[ENABLED_REMINDER_IDS] = if (enabled) current + id else current - id
            }
        }

        private companion object {
            val ENABLED_REMINDER_IDS = stringSetPreferencesKey("enabled_reminder_ids")
        }
    }
