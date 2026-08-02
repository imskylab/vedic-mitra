/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

package io.github.vedicmitra.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vedicmitra.core.datastore.DarkThemeConfig
import io.github.vedicmitra.core.datastore.ThemeSettings
import io.github.vedicmitra.core.datastore.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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
            userPreferencesRepository.themeSettings
                .map { SettingsUiState.Loaded(it) }
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

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }

/** UI state for the settings screen. */
sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Loaded(
        val settings: ThemeSettings,
    ) : SettingsUiState
}
