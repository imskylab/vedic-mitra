/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.kundali

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vedicmitra.core.astronomy.AstronomyEngine
import io.github.vedicmitra.core.astronomy.NatalChart
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.datastore.BirthProfile
import io.github.vedicmitra.core.datastore.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject
import kotlin.time.Instant

/**
 * Presentation logic for the kundali screen. Resolves the primary profile, turns its birth details
 * into a chart via [AstronomyEngine.natalChartAt], and exposes it — or prompts for a profile when
 * one isn't set up with everything a chart needs (date, time and a located place of birth).
 */
@HiltViewModel
class KundaliViewModel
    @Inject
    constructor(
        private val profileRepository: ProfileRepository,
        private val astronomyEngine: AstronomyEngine,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<KundaliUiState>(KundaliUiState.Loading)

        /** Observable UI state consumed by the kundali screen. */
        val uiState: StateFlow<KundaliUiState> = _uiState.asStateFlow()

        /** (Re)loads the primary profile's chart. */
        fun load() {
            viewModelScope.launch {
                val profiles = profileRepository.profiles.first()
                val primaryId = profileRepository.primaryProfileId.first()
                val primary = profiles.firstOrNull { it.id == primaryId } ?: profiles.firstOrNull()
                _uiState.value = chartStateFor(primary)
            }
        }

        private suspend fun chartStateFor(profile: BirthProfile?): KundaliUiState {
            if (profile == null) return KundaliUiState.NeedsProfile
            val date = profile.dateOfBirth
            val time = profile.timeOfBirth
            val zone = profile.birthZoneId
            val coordinates = profile.birthCoordinates
            if (date == null || time == null) return KundaliUiState.NeedsProfile
            if (zone == null || coordinates == null) return KundaliUiState.NeedsProfile
            val epochMillis =
                date
                    .atTime(time)
                    .atZone(ZoneId.of(zone))
                    .toInstant()
                    .toEpochMilli()
            val instant = Instant.fromEpochMilliseconds(epochMillis)
            return when (val result = astronomyEngine.natalChartAt(instant, coordinates)) {
                is AppResult.Success -> {
                    val chart = result.data
                    if (chart != null) KundaliUiState.Ready(profile.name, chart) else KundaliUiState.NeedsProfile
                }

                is AppResult.Failure -> KundaliUiState.NeedsProfile
            }
        }
    }

/** UI state for the kundali screen. */
sealed interface KundaliUiState {
    /** The chart is being computed. */
    data object Loading : KundaliUiState

    /** No primary profile is set up with the details a chart needs; prompt the user to add them. */
    data object NeedsProfile : KundaliUiState

    /**
     * The chart is ready.
     *
     * @property name the profile's name.
     * @property chart the computed natal chart.
     */
    data class Ready(
        val name: String,
        val chart: NatalChart,
    ) : KundaliUiState
}
