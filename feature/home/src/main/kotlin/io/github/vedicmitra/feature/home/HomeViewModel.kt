/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vedicmitra.core.astronomy.AstronomyEngine
import io.github.vedicmitra.core.astronomy.AstronomySnapshot
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.location.LocationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Instant

/**
 * Presentation logic for the home screen (MVVM). Loads today's panchanga for the device's location,
 * falling back to a fixed default when the location is unavailable (e.g. permission not granted).
 *
 * [load] is driven by the screen once it has resolved the location permission.
 */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val astronomyEngine: AstronomyEngine,
        private val locationProvider: LocationProvider,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(HomeUiState())

        /** Observable UI state consumed by the home screen. */
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

        /** (Re)loads the panchanga, using the device location when available. */
        fun load() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                val locationResult = locationProvider.currentLocation()
                val usingDefault = locationResult !is AppResult.Success
                val location =
                    when (locationResult) {
                        is AppResult.Success -> locationResult.data
                        is AppResult.Failure -> DEFAULT_LOCATION
                    }

                val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
                when (val snapshot = astronomyEngine.snapshotAt(now, location)) {
                    is AppResult.Success ->
                        _uiState.update {
                            it.copy(isLoading = false, snapshot = snapshot.data, usingDefaultLocation = usingDefault)
                        }

                    is AppResult.Failure ->
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = snapshot.cause.message ?: "Unknown error")
                        }
                }
            }
        }

        private companion object {
            // Used when the device location is unavailable (New Delhi).
            val DEFAULT_LOCATION = GeoCoordinates(latitude = 28.6139, longitude = 77.2090)
        }
    }

/**
 * Immutable UI state for the home screen.
 *
 * @property isLoading whether the panchanga is being computed.
 * @property snapshot the computed panchanga, or `null` before it loads or on error.
 * @property errorMessage a human-readable error, or `null` when there is none.
 * @property usingDefaultLocation whether the default location was used because the device location
 *   was unavailable.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val snapshot: AstronomySnapshot? = null,
    val errorMessage: String? = null,
    val usingDefaultLocation: Boolean = false,
)
