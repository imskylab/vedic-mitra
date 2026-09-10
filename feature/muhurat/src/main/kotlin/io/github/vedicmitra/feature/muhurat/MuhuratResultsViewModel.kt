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
import io.github.vedicmitra.core.astronomy.MuhurtaParticipants
import io.github.vedicmitra.core.astronomy.MuhurtaPerson
import io.github.vedicmitra.core.astronomy.PersonalMuhurtaContext
import io.github.vedicmitra.core.astronomy.RankedMuhurtaDay
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.datastore.BirthProfile
import io.github.vedicmitra.core.datastore.Gender
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
 * The ranking is personalised to the user's chart-ready profiles (Tarabala + Chandrabala). **How many
 * people it asks for is the activity's own business** — [MuhurtaParticipants.COUPLE] activities are
 * chosen for two, and everything else for one. A one-person activity defaults to the primary profile,
 * with "General" as the opt-out; a couple activity asks for a groom and a bride and ranks generally
 * until it has both, because a marriage day chosen for one of the two is not the thing being asked
 * for.
 *
 * Roles and the gender each implies live here rather than in the engine: `Gender` is a
 * `:core:datastore` type, and `:core:astronomy` neither knows nor should know about it.
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

        // The couple activities' two roles. No default: guessing who is marrying whom from a profile
        // list would be a claim about the user's family, not a convenience.
        private var groomId: String? = null
        private var brideId: String? = null

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
                val chosen = chosenProfiles(chartReady, primaryId)
                val people = chosen.mapNotNull { muhurtaPersonFor(it) }
                val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
                val result = astronomyEngine.bestMuhurtasFor(activity, now, windowDays, resolved.coordinates, people)
                val days = (result as? AppResult.Success)?.data.orEmpty().take(RESULTS_LIMIT)
                _uiState.value =
                    MuhuratResultsUiState.Ready(
                        activity = activity,
                        days = days,
                        windowDays = windowDays,
                        usingDefaultLocation = resolved.isDefault,
                        locationLabel = resolved.label,
                        profiles = chartReady.map { it.asOption() },
                        selectedProfileId = chosen.singleOrNull()?.id.takeIf { !activity.isCouple },
                        couple = coupleSelectionFor(chartReady),
                        // Keyed by id so a reason can name a person without the engine ever holding
                        // a name, and ordered, so `personIds` reads groom-then-bride.
                        personNames = people.associate { p -> p.id to nameOf(chosen, p.id) },
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

        /** Sets the groom for a [MuhurtaParticipants.COUPLE] activity. */
        fun selectGroom(profileId: String?) {
            groomId = profileId
            load()
        }

        /** Sets the bride for a [MuhurtaParticipants.COUPLE] activity. */
        fun selectBride(profileId: String?) {
            brideId = profileId
            load()
        }

        // Whoever this run should be ranked for: two for a couple activity, at most one otherwise.
        // A couple with only one side chosen ranks generally rather than for the half it has -- ranking
        // a wedding for the groom alone is a different question from the one being asked.
        private fun chosenProfiles(
            chartReady: List<BirthProfile>,
            primaryId: String?,
        ): List<BirthProfile> {
            if (activity.isCouple) {
                val groom = chartReady.firstOrNull { it.id == groomId }
                val bride = chartReady.firstOrNull { it.id == brideId }
                return if (groom != null && bride != null) listOf(groom, bride) else emptyList()
            }
            val selectedId =
                if (userChoseProfile) {
                    selectedProfileId
                } else {
                    chartReady.firstOrNull { it.id == primaryId }?.id ?: chartReady.firstOrNull()?.id
                }
            return listOfNotNull(chartReady.firstOrNull { it.id == selectedId })
        }

        // The two gender-filtered candidate lists, or null when this activity is not a couple's.
        private fun coupleSelectionFor(chartReady: List<BirthProfile>): CoupleSelection? {
            if (!activity.isCouple) return null
            val grooms = chartReady.filter { it.gender == Gender.MALE }
            val brides = chartReady.filter { it.gender == Gender.FEMALE }
            return CoupleSelection(
                grooms = grooms.map { it.asOption() },
                brides = brides.map { it.asOption() },
                groomId = groomId,
                brideId = brideId,
                // Matchmaking drops these silently; the screen says so instead.
                withoutGender = chartReady.count { it.gender == null || it.gender == Gender.OTHER },
            )
        }

        /** Casts [profile]'s natal chart and reduces it to the Tarabala/Chandrabala key, or `null`. */
        private suspend fun muhurtaPersonFor(profile: BirthProfile): MuhurtaPerson? {
            val birth = birthMomentOf(profile) ?: return null
            val chart =
                (astronomyEngine.natalChartAt(birth.first, birth.second) as? AppResult.Success)?.data ?: return null
            val moonRasiIndex =
                chart.grahas
                    .firstOrNull { it.graha == Graha.MOON }
                    ?.rasi
                    ?.index ?: return null
            return MuhurtaPerson(
                id = profile.id,
                context =
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

        private companion object {
            const val DEFAULT_WINDOW_DAYS = 60
            const val RESULTS_LIMIT = 10
        }
    }

/** Whether this activity is chosen for two people rather than one. */
internal val MuhurtaActivity.isCouple: Boolean
    get() = participants == MuhurtaParticipants.COUPLE

private val BirthProfile.displayName: String
    get() = name.ifBlank { "Unnamed" }

private fun BirthProfile.asOption(): MuhuratProfileOption = MuhuratProfileOption(id = id, name = displayName)

private fun nameOf(
    profiles: List<BirthProfile>,
    id: String,
): String = profiles.firstOrNull { it.id == id }?.displayName.orEmpty()

/** A chart-ready profile the muhurta results can be personalised for. */
data class MuhuratProfileOption(
    val id: String,
    val name: String,
)

/**
 * The two sides of a [MuhurtaParticipants.COUPLE] activity.
 *
 * @property withoutGender how many chart-ready profiles neither list can offer, because the roles are
 *   gender-filtered and those profiles have no gender set (or `OTHER`). Carried so the screen can say
 *   why a profile it can see elsewhere is missing here, rather than leaving a reader to guess.
 */
data class CoupleSelection(
    val grooms: List<MuhuratProfileOption>,
    val brides: List<MuhuratProfileOption>,
    val groomId: String?,
    val brideId: String?,
    val withoutGender: Int,
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
     *   Always null for a couple activity, which uses [couple] instead.
     * @property couple the groom/bride selection, for a couple activity only.
     * @property personNames profile id to name, for the people the ranking used, in the order it
     *   used them — the screen resolves a reason's `personId` through this, so the engine never holds
     *   a name, and reads [personIds] off it rather than carrying the same list twice.
     */
    data class Ready(
        val activity: MuhurtaActivity,
        val days: List<RankedMuhurtaDay>,
        val windowDays: Int,
        val usingDefaultLocation: Boolean,
        val locationLabel: String,
        val profiles: List<MuhuratProfileOption>,
        val selectedProfileId: String?,
        val couple: CoupleSelection?,
        val personNames: Map<String, String>,
    ) : MuhuratResultsUiState {
        /** Who the ranking was for, in order. Carried into the day screen so the tap keeps it. */
        val personIds: List<String> get() = personNames.keys.toList()
    }
}
