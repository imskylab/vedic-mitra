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
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.vedicmitra.core.common.model.MaasaReckoning
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** [UserPreferencesRepository] backed by a Preferences [DataStore]. */
class DefaultUserPreferencesRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : UserPreferencesRepository {
        override val themeSettings: Flow<ThemeSettings> =
            dataStore.data.map { preferences ->
                ThemeSettings(
                    darkThemeConfig = preferences[DARK_THEME_CONFIG].toDarkThemeConfig(),
                    useDynamicColor = preferences[USE_DYNAMIC_COLOR] ?: false,
                )
            }

        override suspend fun setDarkThemeConfig(config: DarkThemeConfig) {
            dataStore.edit { it[DARK_THEME_CONFIG] = config.name }
        }

        override suspend fun setDynamicColor(enabled: Boolean) {
            dataStore.edit { it[USE_DYNAMIC_COLOR] = enabled }
        }

        override val maasaReckoning: Flow<MaasaReckoning> =
            dataStore.data.map { preferences -> preferences[MAASA_RECKONING].toMaasaReckoning() }

        override suspend fun setMaasaReckoning(reckoning: MaasaReckoning) {
            dataStore.edit { it[MAASA_RECKONING] = reckoning.name }
        }

        private companion object {
            val DARK_THEME_CONFIG = stringPreferencesKey("dark_theme_config")
            val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
            val MAASA_RECKONING = stringPreferencesKey("maasa_reckoning")

            fun String?.toDarkThemeConfig(): DarkThemeConfig =
                DarkThemeConfig.entries.firstOrNull { it.name == this } ?: DarkThemeConfig.FOLLOW_SYSTEM

            // Unset, or a value written by a future version this one does not know, both fall back
            // to what the engine computes rather than to a guess.
            fun String?.toMaasaReckoning(): MaasaReckoning =
                MaasaReckoning.entries.firstOrNull { it.name == this } ?: MaasaReckoning.AMANTA
        }
    }
