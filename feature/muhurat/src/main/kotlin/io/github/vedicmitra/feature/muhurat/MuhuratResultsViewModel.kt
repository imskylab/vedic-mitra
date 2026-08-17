/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.muhurat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vedicmitra.core.astronomy.AstronomyEngine
import io.github.vedicmitra.core.astronomy.Graha
import io.github.vedicmitra.core.astronomy.MuhurtaActivity
import io.github.vedicmitra.core.astronomy.PersonalMuhurtaContext
import io.github.vedicmitra.core.astronomy.RankedMuhurtaDay
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.datastore.BirthProfile
import io.github.vedicmitra.core.datastore.ProfileRepository
import io.github.vedicmitra.core.domain.ResolveLocationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject
import kotlin.time.Instant

/** The `activity` nav argument — the [MuhurtaActivity] name whose best days are shown. */
internal const val MUHURAT_ACTIVITY_ARG = "activity"

/** The search windows (days ahead) the results screen offers. */
val MUHURAT_WINDOW_OPTIONS: List<Int> = listOf(30, 60, 90)

/**
 * Presentation logic for the muhurta results screen: resolves the location and asks the engine for
 * the best upcoming days for the activity named in the [SavedStateHandle], keeping the top few.
 *
 * The ranking can be personalised to one of the user's chart-ready profiles (Tarabala + Chandrabala);
 * it defaults to the primary profile when there is one, and "General" gives the non-personalised
 * ranking. Switching profile or window re-runs the search.
 */
@HiltViewModel
class MuhuratResultsViewModel
    @Inject
    constructor(
        private val astronomyEngine: AstronomyEngine,
        private val resolveLocation: ResolveLocationUseCase,
        private val profileRepository: ProfileRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val activity: MuhurtaActivity =
            savedStateHandle
                .get<String>(MUHURAT_ACTIVITY_ARG)
                ?.let { name -> runCatching { MuhurtaActivity.valueOf(name) }.getOrNull() }
                ?: MuhurtaActivity.GRIHA_PRAVESH

        private var windowDays: Int = DEFAULT_WINDOW_DAYS

        // The user's explicit profile choice once they've made one: a profile id, or null for "General".
        // Until then the primary profile is used by default (personalised out of the box).
        private var selectedProfileId: String? = null
        private var userChoseProfile: Boolean = false

        private val _uiState = MutableStateFlow<MuhuratResultsUiState>(MuhuratResultsUiState.Loading)

        /** Observable UI state consumed by the results screen. */
        val uiState: StateFlow<MuhuratResultsUiState> = _uiState.asStateFlow()

        /** (Re)loads the ranked best days for the activity at the resolved location. */
        fun load() {
            viewModelScope.launch {
                _uiState.value = MuhuratResultsUiState.Loading
                val resolved = resolveLocation()
                val chartReady = profileRepository.profiles.first().filter { it.isChartReady }
                val primaryId = profileRepository.primaryProfileId.first()
                val selectedId = effectiveSelection(chartReady, primaryId)
                val selected = chartReady.firstOrNull { it.id == selectedId }
                val person = selected?.let { personalContextFor(it) }
                val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
                val result = astronomyEngine.bestMuhurtasFor(activity, now, windowDays, resolved.coordinates, person)
                val days = (result as? AppResult.Success)?.data.orEmpty().take(RESULTS_LIMIT)
                val options = chartReady.map { MuhuratProfileOption(id = it.id, name = it.name.ifBlank { "Unnamed" }) }
                _uiState.value =
                    MuhuratResultsUiState.Ready(
                        activity = activity,
                        days = days,
                        windowDays = windowDays,
                        usingDefaultLocation = resolved.isDefault,
                        locationLabel = resolved.label,
                        profiles = options,
                        selectedProfileId = selected?.id,
                    )
            }
        }

        /** Re-runs the search over a different number of [days] ahead (one of [MUHURAT_WINDOW_OPTIONS]). */
        fun setWindow(days: Int) {
            if (days == windowDays) return
            windowDays = days
            load()
        }

        /** Personalises the ranking to the profile with [profileId], or `null` for the general ranking. */
        fun selectProfile(profileId: String?) {
            userChoseProfile = true
            selectedProfileId = profileId
            load()
        }

        // The profile id to rank for: the user's explicit choice once made, else the primary (or any
        // chart-ready profile) by default.
        private fun effectiveSelection(
            chartReady: List<BirthProfile>,
            primaryId: String?,
        ): String? =
            if (userChoseProfile) {
                selectedProfileId
            } else {
                chartReady.firstOrNull { it.id == primaryId }?.id ?: chartReady.firstOrNull()?.id
            }

        /** Casts [profile]'s natal chart and reduces it to the Tarabala/Chandrabala key, or `null`. */
        private suspend fun personalContextFor(profile: BirthProfile): PersonalMuhurtaContext? {
            val birth = birthMomentOf(profile) ?: return null
            val chart =
                (astronomyEngine.natalChartAt(birth.first, birth.second) as? AppResult.Success)?.data ?: return null
            val moonRasiIndex =
                chart.grahas
                    .firstOrNull { it.graha == Graha.MOON }
                    ?.rasi
                    ?.index ?: return null
            return PersonalMuhurtaContext(
                birthNakshatraNumber = chart.moonNakshatra.number,
                birthMoonRasiIndex = moonRasiIndex,
            )
        }

        // The birth instant + birthplace coordinates for [profile], or null if any are missing.
        private fun birthMomentOf(profile: BirthProfile): Pair<Instant, GeoCoordinates>? {
            val date = profile.dateOfBirth
            val time = profile.timeOfBirth
            val zone = profile.birthZoneId
            val coordinates = profile.birthCoordinates
            if (date == null || time == null) return null
            if (zone == null || coordinates == null) return null
            val millis =
                date
                    .atTime(time)
                    .atZone(ZoneId.of(zone))
                    .toInstant()
                    .toEpochMilli()
            return Instant.fromEpochMilliseconds(millis) to coordinates
        }

        private companion object {
            const val DEFAULT_WINDOW_DAYS = 60
            const val RESULTS_LIMIT = 10
        }
    }

/** A chart-ready profile the muhurta results can be personalised for. */
data class MuhuratProfileOption(
    val id: String,
    val name: String,
)

/** UI state for the muhurta results screen. */
sealed interface MuhuratResultsUiState {
    /** The best days are being computed. */
    data object Loading : MuhuratResultsUiState

    /**
     * The ranked days are ready.
     *
     * @property activity the activity the days were ranked for.
     * @property days the best upcoming days, best-first (may be empty).
     * @property windowDays the number of days ahead currently searched.
     * @property usingDefaultLocation whether a built-in default location was used.
     * @property locationLabel human-readable name of the location used.
     * @property profiles the chart-ready profiles the ranking can be personalised for (may be empty).
     * @property selectedProfileId the profile the ranking is personalised for, or `null` for General.
     */
    data class Ready(
        val activity: MuhurtaActivity,
        val days: List<RankedMuhurtaDay>,
        val windowDays: Int,
        val usingDefaultLocation: Boolean,
        val locationLabel: String,
        val profiles: List<MuhuratProfileOption>,
        val selectedProfileId: String?,
    ) : MuhuratResultsUiState
}
