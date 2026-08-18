/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.rashifal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vedicmitra.core.astronomy.AstronomyEngine
import io.github.vedicmitra.core.astronomy.Graha
import io.github.vedicmitra.core.astronomy.PersonalMuhurtaContext
import io.github.vedicmitra.core.astronomy.RASHI_NAMES
import io.github.vedicmitra.core.astronomy.RashiOutlook
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

/** How many days ahead the outlook covers (today + the next six). */
private const val OUTLOOK_DAYS = 7

/**
 * Presentation logic for the rashifal screen. Resolves the location, loads the chart-ready profiles
 * (so a reading can be personalised to one's birth Moon sign), and asks the engine for the selected
 * sign's transit outlook.
 *
 * The reading defaults to the primary profile's Moon sign and is then fully personalised (Chandrabala
 * + Tarabala). The user can tap any of the twelve rashis to browse it; browsing a sign other than
 * their own drops to the sign-only transit reading (Chandrabala), since Tarabala is tied to their
 * birth star. Switching profile jumps the reading back to that profile's own sign.
 */
@HiltViewModel
class RashifalViewModel
    @Inject
    constructor(
        private val astronomyEngine: AstronomyEngine,
        private val resolveLocation: ResolveLocationUseCase,
        private val profileRepository: ProfileRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<RashifalUiState>(RashifalUiState.Loading)

        /** Observable UI state consumed by the rashifal screen. */
        val uiState: StateFlow<RashifalUiState> = _uiState.asStateFlow()

        private var coordinates: GeoCoordinates? = null
        private var locationLabel: String = ""
        private var usingDefaultLocation: Boolean = false

        // The chart-ready profiles, each reduced to its Tarabala/Chandrabala key and Moon sign.
        private var profiles: List<ProfileOutlook> = emptyList()
        private var selectedProfileId: String? = null
        private var selectedRasiIndex: Int = 0

        /** (Re)loads the location and profiles, then computes the default sign's outlook. */
        fun load() {
            viewModelScope.launch {
                _uiState.value = RashifalUiState.Loading
                val resolved = resolveLocation()
                coordinates = resolved.coordinates
                locationLabel = resolved.label
                usingDefaultLocation = resolved.isDefault

                val chartReady = profileRepository.profiles.first().filter { it.isChartReady }
                val primaryId = profileRepository.primaryProfileId.first()
                profiles = chartReady.mapNotNull { profile -> outlookFor(profile) }

                val default = profiles.firstOrNull { it.id == primaryId } ?: profiles.firstOrNull()
                selectedProfileId = default?.id
                selectedRasiIndex = default?.rasiIndex ?: 0
                emitOutlook()
            }
        }

        /** Reads a different rashi (0 = Mesha .. 11 = Meena); the index comes from the fixed sign list. */
        fun selectSign(rasiIndex: Int) {
            if (rasiIndex == selectedRasiIndex) return
            selectedRasiIndex = rasiIndex
            viewModelScope.launch { emitOutlook() }
        }

        /** Switches to the profile with [profileId] and jumps the reading to their own Moon sign. */
        fun selectProfile(profileId: String) {
            val profile = profiles.firstOrNull { it.id == profileId } ?: return
            selectedProfileId = profileId
            selectedRasiIndex = profile.rasiIndex
            viewModelScope.launch { emitOutlook() }
        }

        private suspend fun emitOutlook() {
            val location = coordinates ?: return
            val selectedProfile = profiles.firstOrNull { it.id == selectedProfileId }
            // Tarabala only applies when reading the selected profile's own birth Moon sign.
            val personalized = selectedProfile != null && selectedProfile.rasiIndex == selectedRasiIndex
            val person = if (personalized) selectedProfile?.person else null
            val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
            val result = astronomyEngine.rashiOutlook(selectedRasiIndex, now, location, person, OUTLOOK_DAYS)
            val outlook = (result as? AppResult.Success)?.data

            _uiState.value =
                RashifalUiState.Ready(
                    signs = RASHI_NAMES.mapIndexed { index, name -> RashiOption(index = index, name = name) },
                    selectedRasiIndex = selectedRasiIndex,
                    profiles = profiles.map { RashifalProfileOption(id = it.id, name = it.name) },
                    selectedProfileId = selectedProfileId,
                    yourRasiIndex = selectedProfile?.rasiIndex,
                    personalized = personalized,
                    locationLabel = locationLabel,
                    usingDefaultLocation = usingDefaultLocation,
                    outlook = outlook,
                )
        }

        /** Casts [profile]'s natal chart and reduces it to its Moon sign + Tarabala/Chandrabala key, or `null`. */
        private suspend fun outlookFor(profile: BirthProfile): ProfileOutlook? {
            val birth = birthMomentOf(profile) ?: return null
            val chart =
                (astronomyEngine.natalChartAt(birth.first, birth.second) as? AppResult.Success)?.data ?: return null
            val moonRasiIndex =
                chart.grahas
                    .firstOrNull { it.graha == Graha.MOON }
                    ?.rasi
                    ?.index ?: return null
            return ProfileOutlook(
                id = profile.id,
                name = profile.name.ifBlank { "Unnamed" },
                rasiIndex = moonRasiIndex,
                person =
                    PersonalMuhurtaContext(
                        birthNakshatraNumber = chart.moonNakshatra.number,
                        birthMoonRasiIndex = moonRasiIndex,
                    ),
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
    }

/** A chart-ready profile reduced to what the rashifal needs: its Moon sign and personalisation key. */
private data class ProfileOutlook(
    val id: String,
    val name: String,
    val rasiIndex: Int,
    val person: PersonalMuhurtaContext,
)

/** A selectable rashi for the sign strip. */
data class RashiOption(
    val index: Int,
    val name: String,
)

/** A selectable chart-ready profile the reading can be personalised for. */
data class RashifalProfileOption(
    val id: String,
    val name: String,
)

/** UI state for the rashifal screen. */
sealed interface RashifalUiState {
    /** The location, profiles, and first outlook are being resolved. */
    data object Loading : RashifalUiState

    /**
     * The reading is ready.
     *
     * @property signs the twelve rashis, for the sign selector.
     * @property selectedRasiIndex the sign currently being read (0..11).
     * @property profiles the chart-ready profiles the reading can be personalised for (may be empty).
     * @property selectedProfileId the profile currently in context, or `null` when none is set up.
     * @property yourRasiIndex the selected profile's own Moon sign, or `null` when no profile.
     * @property personalized whether Tarabala is layered in (reading the profile's own sign).
     * @property locationLabel human-readable name of the location used.
     * @property usingDefaultLocation whether a built-in default location was used.
     * @property outlook the computed outlook, or `null` if it could not be computed.
     */
    data class Ready(
        val signs: List<RashiOption>,
        val selectedRasiIndex: Int,
        val profiles: List<RashifalProfileOption>,
        val selectedProfileId: String?,
        val yourRasiIndex: Int?,
        val personalized: Boolean,
        val locationLabel: String,
        val usingDefaultLocation: Boolean,
        val outlook: RashiOutlook?,
    ) : RashifalUiState
}
