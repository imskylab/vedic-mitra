/*
 * Copyright (c) 2026 Vedic Mitra contributors
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

package io.github.vedicmitra.feature.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Presentation logic for the home screen (MVVM). Phase 1 provides only the skeleton: an immutable
 * [HomeUiState] exposed as a [StateFlow]. Dependencies (astronomy/location ports) are injected via
 * the constructor once the corresponding features are implemented.
 */
@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())

    /** Observable UI state consumed by [HomeScreen]. */
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
}

/**
 * Immutable UI state for the home screen. Fields are added as the screen gains real content.
 *
 * @property isLoading whether initial data is still loading.
 */
data class HomeUiState(
    val isLoading: Boolean = false,
)
