/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vedicmitra.core.common.model.MaasaReckoning
import io.github.vedicmitra.core.datastore.DarkThemeConfig
import io.github.vedicmitra.core.datastore.ThemeSettings
import io.github.vedicmitra.core.datastore.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Presentation logic for the settings screen: exposes the theme settings and updates them. */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModel() {
        val uiState: StateFlow<SettingsUiState> =
            combine(
                userPreferencesRepository.themeSettings,
                userPreferencesRepository.maasaReckoning,
            ) { theme, reckoning -> SettingsUiState.Loaded(theme, reckoning) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = SettingsUiState.Loading,
                )

        fun setDarkThemeConfig(config: DarkThemeConfig) {
            viewModelScope.launch { userPreferencesRepository.setDarkThemeConfig(config) }
        }

        fun setDynamicColor(enabled: Boolean) {
            viewModelScope.launch { userPreferencesRepository.setDynamicColor(enabled) }
        }

        fun setMaasaReckoning(reckoning: MaasaReckoning) {
            viewModelScope.launch { userPreferencesRepository.setMaasaReckoning(reckoning) }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }

/** UI state for the settings screen. */
sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Loaded(
        val settings: ThemeSettings,
        val maasaReckoning: MaasaReckoning,
    ) : SettingsUiState
}
