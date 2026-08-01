/*
 * Copyright (c) 2026 Vedic Mitra contributors
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

package io.github.vedicmitra.feature.alarm

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Presentation logic for the alarm screen (MVVM). Phase 1 skeleton only — scheduling and alarm
 * management are implemented in the alarm implementation phase, injected via the constructor.
 */
@HiltViewModel
class AlarmViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmUiState())

    /** Observable UI state consumed by [AlarmScreen]. */
    val uiState: StateFlow<AlarmUiState> = _uiState.asStateFlow()
}

/**
 * Immutable UI state for the alarm screen. Fields grow as the screen gains real content.
 *
 * @property alarms the alarms shown to the user (empty in the skeleton).
 */
data class AlarmUiState(
    val alarms: List<String> = emptyList(),
)
