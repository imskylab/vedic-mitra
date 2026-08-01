/*
 * Copyright (c) 2026 Vedic Mitra contributors
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

package io.github.vedicmitra.feature.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Presentation logic for the settings screen (MVVM). Phase 1 skeleton only — preference reads and
 * writes are added when settings are implemented.
 */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor() : ViewModel() {
        private val _uiState = MutableStateFlow(SettingsUiState())

        /** Observable UI state consumed by [SettingsScreen]. */
        val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    }

/**
 * Immutable UI state for the settings screen.
 *
 * @property useDynamicColor whether Material You dynamic colour is enabled.
 */
data class SettingsUiState(
    val useDynamicColor: Boolean = true,
)
