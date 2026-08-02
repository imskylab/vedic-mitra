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
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * [ReminderRepository] backed by a Preferences [DataStore]. Reminders are stored as a set of
 * [ReminderCodec]-encoded strings; the lead time as an int.
 */
class DefaultReminderRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : ReminderRepository {
        override val reminders: Flow<List<PersistedReminder>> =
            dataStore.data.map { preferences -> preferences.decodeReminders() }

        override val leadTimeMinutes: Flow<Int> =
            dataStore.data.map { preferences -> preferences[LEAD_TIME_MINUTES] ?: DEFAULT_LEAD_TIME_MINUTES }

        override suspend fun upsert(reminder: PersistedReminder) {
            dataStore.edit { preferences ->
                val kept = preferences.decodeReminders().filterNot { it.id == reminder.id }
                preferences[REMINDERS] = (kept + reminder).encode()
            }
        }

        override suspend fun remove(id: String) {
            dataStore.edit { preferences ->
                preferences[REMINDERS] = preferences.decodeReminders().filterNot { it.id == id }.encode()
            }
        }

        override suspend fun removePast(nowEpochMillis: Long) {
            dataStore.edit { preferences ->
                preferences[REMINDERS] =
                    preferences.decodeReminders().filter { it.triggerAtEpochMillis > nowEpochMillis }.encode()
            }
        }

        override suspend fun setLeadTimeMinutes(minutes: Int) {
            dataStore.edit { preferences -> preferences[LEAD_TIME_MINUTES] = minutes }
        }

        private fun Preferences.decodeReminders(): List<PersistedReminder> =
            this[REMINDERS].orEmpty().mapNotNull(ReminderCodec::decode)

        private fun List<PersistedReminder>.encode(): Set<String> = map(ReminderCodec::encode).toSet()

        private companion object {
            val REMINDERS = stringSetPreferencesKey("reminders")
            val LEAD_TIME_MINUTES = intPreferencesKey("reminder_lead_time_minutes")
            const val DEFAULT_LEAD_TIME_MINUTES = 10
        }
    }
