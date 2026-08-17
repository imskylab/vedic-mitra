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
 * Presentation logic for the kundali screen. Casts the chart for the selected chart-ready profile
 * (defaulting to the primary), and lets the user switch between profiles when they keep more than
 * one. Prompts to set up a profile when none has everything a chart needs (date, time, located place).
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

        // The chart-ready profiles currently offered, so [select] can recast without reloading.
        private var chartReady: List<BirthProfile> = emptyList()

        /** (Re)loads the chart-ready profiles and casts the primary's chart (or the first, if none is primary). */
        fun load() {
            viewModelScope.launch {
                val profiles = profileRepository.profiles.first()
                val primaryId = profileRepository.primaryProfileId.first()
                chartReady = profiles.filter { it.isChartReady }
                val selected = chartReady.firstOrNull { it.id == primaryId } ?: chartReady.firstOrNull()
                _uiState.value = if (selected == null) KundaliUiState.NeedsProfile else chartStateFor(selected)
            }
        }

        /** Switches the displayed chart to the chart-ready profile with [profileId]. */
        fun select(profileId: String) {
            val profile = chartReady.firstOrNull { it.id == profileId } ?: return
            viewModelScope.launch { _uiState.value = chartStateFor(profile) }
        }

        private suspend fun chartStateFor(profile: BirthProfile): KundaliUiState {
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
                    if (chart != null) readyState(profile, chart) else KundaliUiState.NeedsProfile
                }

                is AppResult.Failure -> KundaliUiState.NeedsProfile
            }
        }

        private fun readyState(
            profile: BirthProfile,
            chart: NatalChart,
        ): KundaliUiState.Ready =
            KundaliUiState.Ready(
                name = profile.name,
                chart = chart,
                selectedId = profile.id,
                options = chartReady.map { KundaliProfileOption(id = it.id, name = it.name.ifBlank { "Unnamed" }) },
            )
    }

/** A selectable chart-ready profile for the kundali picker. */
data class KundaliProfileOption(
    val id: String,
    val name: String,
)

/** UI state for the kundali screen. */
sealed interface KundaliUiState {
    /** The chart is being computed. */
    data object Loading : KundaliUiState

    /** No profile is set up with the details a chart needs; prompt the user to add them. */
    data object NeedsProfile : KundaliUiState

    /**
     * The chart is ready.
     *
     * @property name the profile's name.
     * @property chart the computed natal chart.
     * @property selectedId the id of the profile the chart is for.
     * @property options every chart-ready profile, offered as a picker when there's more than one.
     */
    data class Ready(
        val name: String,
        val chart: NatalChart,
        val selectedId: String,
        val options: List<KundaliProfileOption>,
    ) : KundaliUiState
}
