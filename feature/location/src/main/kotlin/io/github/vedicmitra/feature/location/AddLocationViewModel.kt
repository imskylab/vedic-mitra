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
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.model.LocationSource
import io.github.vedicmitra.core.common.model.SavedLocation
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.datastore.LocationRepository
import io.github.vedicmitra.core.location.GeocodeResult
import io.github.vedicmitra.core.location.GeocodingClient
import io.github.vedicmitra.core.location.TimeZoneResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Presentation logic for adding a location, shared by the city-search and custom-coordinates
 * screens. Runs city searches through the geocoder and saves the chosen place, auto-selecting it so
 * it takes effect immediately. Each new location's IANA time zone is resolved from its coordinates
 * (see [TimeZoneResolver]); the coordinates screen can override it.
 */
@HiltViewModel
class AddLocationViewModel
    @Inject
    constructor(
        private val locationRepository: LocationRepository,
        private val geocodingClient: GeocodingClient,
        private val timeZoneResolver: TimeZoneResolver,
    ) : ViewModel() {
        private val _searchState = MutableStateFlow(CitySearchState())

        /** Observable city-search state consumed by the add-city screen. */
        val searchState: StateFlow<CitySearchState> = _searchState.asStateFlow()

        /** Runs a forward-geocoding search for [query]. A blank query clears the results. */
        fun search(query: String) {
            if (query.isBlank()) {
                _searchState.value = CitySearchState()
                return
            }
            viewModelScope.launch {
                _searchState.update { it.copy(isSearching = true, error = null) }
                when (val result = geocodingClient.search(query)) {
                    is AppResult.Success ->
                        _searchState.update {
                            it.copy(
                                isSearching = false,
                                results = result.data,
                                error = if (result.data.isEmpty()) "No matching places found" else null,
                            )
                        }

                    is AppResult.Failure ->
                        _searchState.update {
                            it.copy(
                                isSearching = false,
                                results = emptyList(),
                                error = result.cause.message ?: "Search failed",
                            )
                        }
                }
            }
        }

        /** Saves a geocoded [result] as a new location (with its resolved time zone) and selects it. */
        fun saveFromResult(
            result: GeocodeResult,
            onSaved: () -> Unit,
        ) {
            viewModelScope.launch {
                val zoneId = timeZoneResolver.resolve(result.coordinates)
                persist(result.label, result.coordinates, zoneId, LocationSource.CITY, onSaved)
            }
        }

        /** Saves a manually entered location and selects it. A blank [zoneId] is auto-detected. */
        fun saveManual(
            label: String,
            latitude: Double,
            longitude: Double,
            zoneId: String,
            onSaved: () -> Unit,
        ) {
            viewModelScope.launch {
                val coordinates = GeoCoordinates(latitude = latitude, longitude = longitude)
                val resolvedZone = zoneId.ifBlank { timeZoneResolver.resolve(coordinates) }
                val resolvedLabel = label.ifBlank { "%.4f, %.4f".format(latitude, longitude) }
                persist(resolvedLabel, coordinates, resolvedZone, LocationSource.MANUAL, onSaved)
            }
        }

        private suspend fun persist(
            label: String,
            coordinates: GeoCoordinates,
            zoneId: String,
            source: LocationSource,
            onSaved: () -> Unit,
        ) {
            val location =
                SavedLocation(
                    id = UUID.randomUUID().toString(),
                    label = label,
                    coordinates = coordinates,
                    zoneId = zoneId,
                    source = source,
                )
            locationRepository.upsert(location)
            locationRepository.select(location.id)
            onSaved()
        }
    }

/**
 * City-search UI state.
 *
 * @property isSearching whether a search is in flight.
 * @property results the geocoder's candidate places.
 * @property error a human-readable error or empty-results note, or `null` when there is none.
 */
data class CitySearchState(
    val isSearching: Boolean = false,
    val results: List<GeocodeResult> = emptyList(),
    val error: String? = null,
)
