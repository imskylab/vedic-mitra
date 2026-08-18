/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.matchmaking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vedicmitra.core.astronomy.AstronomyEngine
import io.github.vedicmitra.core.astronomy.Graha
import io.github.vedicmitra.core.astronomy.GunaMilanProfile
import io.github.vedicmitra.core.astronomy.GunaMilanResult
import io.github.vedicmitra.core.astronomy.gunaMilan
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.datastore.BirthProfile
import io.github.vedicmitra.core.datastore.Gender
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
 * Presentation logic for kundali matching: resolves the chart-ready male and female profiles, casts
 * each one's Moon position, and scores the Ashtakoota match with [gunaMilan]. The groom must be male
 * and the bride female; both default to the first of their gender so a result shows immediately.
 */
@HiltViewModel
class MatchmakingViewModel
    @Inject
    constructor(
        private val profileRepository: ProfileRepository,
        private val astronomyEngine: AstronomyEngine,
    ) : ViewModel() {
        // Moon positions for every matchable profile, keyed by id, cast once on load.
        private val positions = mutableMapOf<String, GunaMilanProfile>()
        private var males: List<MatchProfileOption> = emptyList()
        private var females: List<MatchProfileOption> = emptyList()
        private var groomId: String? = null
        private var brideId: String? = null

        private val _uiState = MutableStateFlow<MatchmakingUiState>(MatchmakingUiState.Loading)

        /** Observable UI state consumed by the matchmaking screen. */
        val uiState: StateFlow<MatchmakingUiState> = _uiState.asStateFlow()

        /** Loads the matchable profiles and scores the default pairing. */
        fun load() {
            viewModelScope.launch {
                val chartReady = profileRepository.profiles.first().filter { it.isChartReady }
                positions.clear()
                chartReady.forEach { profile -> positionOf(profile)?.let { positions[profile.id] = it } }
                val matchable = chartReady.filter { positions.containsKey(it.id) }
                males = matchable.filter { it.gender == Gender.MALE }.map { it.toOption() }
                females = matchable.filter { it.gender == Gender.FEMALE }.map { it.toOption() }
                groomId = males.firstOrNull()?.id
                brideId = females.firstOrNull()?.id
                emitReady()
            }
        }

        /** Selects the groom (a male profile) and re-scores. */
        fun selectGroom(id: String) {
            groomId = id
            emitReady()
        }

        /** Selects the bride (a female profile) and re-scores. */
        fun selectBride(id: String) {
            brideId = id
            emitReady()
        }

        private fun emitReady() {
            val groom = groomId?.let { positions[it] }
            val bride = brideId?.let { positions[it] }
            val result = if (groom != null && bride != null) gunaMilan(groom = groom, bride = bride) else null
            _uiState.value =
                MatchmakingUiState.Ready(
                    males = males,
                    females = females,
                    selectedGroomId = groomId,
                    selectedBrideId = brideId,
                    result = result,
                )
        }

        /** Casts [profile]'s natal chart and reduces it to the Moon nakshatra + sign, or `null`. */
        private suspend fun positionOf(profile: BirthProfile): GunaMilanProfile? {
            val birth = birthMomentOf(profile) ?: return null
            val chart =
                (astronomyEngine.natalChartAt(birth.first, birth.second) as? AppResult.Success)?.data ?: return null
            val moonRasiIndex =
                chart.grahas
                    .firstOrNull { it.graha == Graha.MOON }
                    ?.rasi
                    ?.index ?: return null
            return GunaMilanProfile(nakshatraNumber = chart.moonNakshatra.number, moonRasiIndex = moonRasiIndex)
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

        private fun BirthProfile.toOption(): MatchProfileOption =
            MatchProfileOption(id = id, name = name.ifBlank { "Unnamed" })
    }

/** A chart-ready profile of a given gender, offered for selection. */
data class MatchProfileOption(
    val id: String,
    val name: String,
)

/** UI state for the matchmaking screen. */
sealed interface MatchmakingUiState {
    /** The matchable profiles are being resolved. */
    data object Loading : MatchmakingUiState

    /**
     * The matchable profiles are ready.
     *
     * @property males the chart-ready male profiles (groom candidates).
     * @property females the chart-ready female profiles (bride candidates).
     * @property selectedGroomId the chosen groom, or `null` if none available.
     * @property selectedBrideId the chosen bride, or `null` if none available.
     * @property result the match for the current pairing, or `null` until both are chosen.
     */
    data class Ready(
        val males: List<MatchProfileOption>,
        val females: List<MatchProfileOption>,
        val selectedGroomId: String?,
        val selectedBrideId: String?,
        val result: GunaMilanResult?,
    ) : MatchmakingUiState
}
