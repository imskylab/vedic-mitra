/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vedicmitra.core.common.model.SavedLocation
import io.github.vedicmitra.core.datastore.LocationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Presentation logic for the saved-locations list. Exposes the saved locations and which one is
 * selected, and lets the user select one, fall back to the current device location, or delete one.
 */
@HiltViewModel
class LocationViewModel
    @Inject
    constructor(
        private val locationRepository: LocationRepository,
    ) : ViewModel() {
        /** Observable UI state consumed by the location screen. */
        val uiState: StateFlow<LocationUiState> =
            combine(
                locationRepository.savedLocations,
                locationRepository.selectedLocationId,
            ) { locations, selectedId ->
                LocationUiState(locations = locations, selectedId = selectedId)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = LocationUiState(),
            )

        /** Selects the saved location with [id] as the one the panchanga is computed for. */
        fun select(id: String) {
            viewModelScope.launch { locationRepository.select(id) }
        }

        /** Clears the selection so the app falls back to the current device location. */
        fun useCurrentLocation() {
            viewModelScope.launch { locationRepository.clearSelection() }
        }

        /** Deletes the saved location with [id]. */
        fun delete(id: String) {
            viewModelScope.launch { locationRepository.remove(id) }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }

/**
 * UI state for the location screen.
 *
 * @property locations the saved locations, in insertion order.
 * @property selectedId the id of the selected location, or `null` when using the device location.
 */
data class LocationUiState(
    val locations: List<SavedLocation> = emptyList(),
    val selectedId: String? = null,
)
