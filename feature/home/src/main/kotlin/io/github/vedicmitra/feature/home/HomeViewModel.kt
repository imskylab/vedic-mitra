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
import io.github.vedicmitra.core.astronomy.Festival
import io.github.vedicmitra.core.astronomy.FestivalType
import io.github.vedicmitra.core.astronomy.MuhurtaQuality
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.domain.ResolveLocationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Instant

/**
 * Presentation logic for the home screen (MVVM). Home is a glanceable landing view — today's
 * identity, the auspicious window in effect right now, and the next festival — rather than the full
 * panchanga table (which lives on the calendar). Loads for the resolved location (selected saved,
 * else device, else default; see [ResolveLocationUseCase]).
 */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val astronomyEngine: AstronomyEngine,
        private val resolveLocation: ResolveLocationUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(HomeUiState())

        /** Observable UI state consumed by the home screen. */
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

        /** (Re)loads today's landing content for the resolved location. */
        fun load() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                val resolved = resolveLocation()
                val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
                when (val snapshot = astronomyEngine.snapshotAt(now, resolved.coordinates)) {
                    is AppResult.Success ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                snapshot = snapshot.data,
                                auspicious = auspiciousWindow(snapshot.data, now),
                                nextFestival = nextFestival(now, resolved.coordinates),
                                usingDefaultLocation = resolved.isDefault,
                                locationLabel = resolved.label,
                            )
                        }

                    is AppResult.Failure ->
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = snapshot.cause.message ?: "Unknown error")
                        }
                }
            }
        }

        /** The auspicious/inauspicious muhurta in effect now, else the next auspicious one. */
        private fun auspiciousWindow(
            snapshot: AstronomySnapshot,
            now: Instant,
        ): AuspiciousWindow? {
            val active = snapshot.muhurtas.filter { now >= it.start && now < it.end }
            val current = active.firstOrNull { it.quality == MuhurtaQuality.AUSPICIOUS } ?: active.firstOrNull()
            if (current != null) {
                return AuspiciousWindow(current.name, current.quality, boundary = current.end, isActive = true)
            }
            val next =
                snapshot.muhurtas
                    .filter { it.start > now && it.quality == MuhurtaQuality.AUSPICIOUS }
                    .minByOrNull { it.start } ?: return null
            return AuspiciousWindow(next.name, next.quality, boundary = next.start, isActive = false)
        }

        /** The next named festival if one is within range, otherwise the soonest observance. */
        private suspend fun nextFestival(
            now: Instant,
            coordinates: GeoCoordinates,
        ): Festival? {
            val result = astronomyEngine.upcomingFestivals(now, coordinates, FESTIVAL_WINDOW_DAYS, FESTIVAL_LIMIT)
            if (result !is AppResult.Success) return null
            return result.data.firstOrNull { it.type == FestivalType.FESTIVAL } ?: result.data.firstOrNull()
        }

        private companion object {
            const val FESTIVAL_WINDOW_DAYS = 210
            const val FESTIVAL_LIMIT = 12
        }
    }

/**
 * The auspicious window shown on Home: the one active now (its end), or the next auspicious one (its
 * start).
 *
 * @property name the muhurta's name (e.g. "Abhijit Muhurta", "Rahu Kalam").
 * @property quality whether it is auspicious or inauspicious.
 * @property boundary the end (when [isActive]) or the start (when upcoming).
 * @property isActive whether the window is in effect right now.
 */
data class AuspiciousWindow(
    val name: String,
    val quality: MuhurtaQuality,
    val boundary: Instant,
    val isActive: Boolean,
)

/**
 * Immutable UI state for the home screen.
 *
 * @property isLoading whether today's content is being computed.
 * @property snapshot today's panchanga, or `null` before it loads or on error.
 * @property auspicious the auspicious-now / next-auspicious window, or `null` if none.
 * @property nextFestival the upcoming festival or observance, or `null`.
 * @property errorMessage a human-readable error, or `null` when there is none.
 * @property usingDefaultLocation whether the built-in default location was used.
 * @property locationLabel human-readable name of the location.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val snapshot: AstronomySnapshot? = null,
    val auspicious: AuspiciousWindow? = null,
    val nextFestival: Festival? = null,
    val errorMessage: String? = null,
    val usingDefaultLocation: Boolean = false,
    val locationLabel: String? = null,
)
