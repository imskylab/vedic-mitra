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
import io.github.vedicmitra.core.common.model.AlertStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * [ReminderRepository] backed by a Preferences [DataStore]. Reminders and per-muhurta offset
 * overrides are each stored as a set of codec-encoded strings.
 */
class DefaultReminderRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : ReminderRepository {
        override val reminders: Flow<List<PersistedReminder>> =
            dataStore.data.map { preferences -> preferences.decodeReminders() }

        override val offsetMinutesByName: Flow<Map<String, Int>> =
            dataStore.data.map { preferences -> preferences.decodeOffsets().associate { it.name to it.offsetMinutes } }

        override val alertTypeByName: Flow<Map<String, AlertStyle>> =
            dataStore.data.map { preferences -> preferences.decodeAlerts().associate { it.name to it.alert } }

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

        override suspend fun setOffsetMinutes(
            name: String,
            minutes: Int,
        ) {
            dataStore.edit { preferences ->
                val kept = preferences.decodeOffsets().filterNot { it.name == name }
                preferences[MUHURTA_OFFSETS] =
                    (kept + MuhurtaOffset(name, minutes)).map(MuhurtaOffsetCodec::encode).toSet()
            }
        }

        override suspend fun setAlertType(
            name: String,
            alert: AlertStyle,
        ) {
            dataStore.edit { preferences ->
                val kept = preferences.decodeAlerts().filterNot { it.name == name }
                preferences[MUHURTA_ALERTS] =
                    (kept + MuhurtaAlert(name, alert)).map(MuhurtaAlertCodec::encode).toSet()
            }
        }

        private fun Preferences.decodeReminders(): List<PersistedReminder> =
            this[REMINDERS].orEmpty().mapNotNull(ReminderCodec::decode)

        private fun List<PersistedReminder>.encode(): Set<String> = map(ReminderCodec::encode).toSet()

        private fun Preferences.decodeOffsets(): List<MuhurtaOffset> =
            this[MUHURTA_OFFSETS].orEmpty().mapNotNull(MuhurtaOffsetCodec::decode)

        private fun Preferences.decodeAlerts(): List<MuhurtaAlert> =
            this[MUHURTA_ALERTS].orEmpty().mapNotNull(MuhurtaAlertCodec::decode)

        private companion object {
            val REMINDERS = stringSetPreferencesKey("reminders")
            val MUHURTA_OFFSETS = stringSetPreferencesKey("muhurta_offset_minutes")
            val MUHURTA_ALERTS = stringSetPreferencesKey("muhurta_alert_styles")
        }
    }
