/*
 * Copyright (c) 2026 Vedic Mitra contributors
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

package io.github.vedicmitra.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vedicmitra.core.astronomy.AstronomyEngine
import io.github.vedicmitra.core.astronomy.AstronomySnapshot
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Instant

/**
 * Presentation logic for the home screen (MVVM). Loads today's panchanga from the [AstronomyEngine]
 * for a default location and exposes it as immutable [HomeUiState].
 */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val astronomyEngine: AstronomyEngine,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(HomeUiState())

        /** Observable UI state consumed by the home screen. */
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        /** (Re)loads the panchanga for the current instant. */
        fun load() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
                when (val result = astronomyEngine.snapshotAt(now, DEFAULT_LOCATION)) {
                    is AppResult.Success ->
                        _uiState.update { it.copy(isLoading = false, snapshot = result.data) }

                    is AppResult.Failure ->
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = result.cause.message ?: "Unknown error")
                        }
                }
            }
        }

        private companion object {
            // Fixed observer location until :core:location is implemented (New Delhi).
            val DEFAULT_LOCATION = GeoCoordinates(latitude = 28.6139, longitude = 77.2090)
        }
    }

/**
 * Immutable UI state for the home screen.
 *
 * @property isLoading whether the panchanga is being computed.
 * @property snapshot the computed panchanga, or `null` before it loads or on error.
 * @property errorMessage a human-readable error, or `null` when there is none.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val snapshot: AstronomySnapshot? = null,
    val errorMessage: String? = null,
)
