/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.japa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vedicmitra.core.astronomy.AstronomyEngine
import io.github.vedicmitra.core.astronomy.Graha
import io.github.vedicmitra.core.astronomy.Muhurta
import io.github.vedicmitra.core.astronomy.NatalChart
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.datastore.BirthProfile
import io.github.vedicmitra.core.datastore.JapaProgress
import io.github.vedicmitra.core.datastore.JapaRepository
import io.github.vedicmitra.core.datastore.JapaSession
import io.github.vedicmitra.core.datastore.ProfileRepository
import io.github.vedicmitra.core.domain.ResolveLocationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.time.Instant

private const val HISTORY_LIMIT = 20

/**
 * Presentation logic for the japa counter. Owns the current mala count in memory (so taps are
 * instant), persisting it as it goes so a sitting can be resumed, and logs a completed sitting to the
 * history stamped with the day's nakshatra/tithi. Also surfaces two panchanga hooks: the beeja mantra
 * of the primary profile's current mahadasha lord, and today's Brahma Muhurta window.
 */
@HiltViewModel
class JapaViewModel
    @Inject
    constructor(
        private val japaRepository: JapaRepository,
        private val profileRepository: ProfileRepository,
        private val astronomyEngine: AstronomyEngine,
        private val resolveLocation: ResolveLocationUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<JapaUiState>(JapaUiState.Loading)

        /** Observable UI state consumed by the japa screen. */
        val uiState: StateFlow<JapaUiState> = _uiState.asStateFlow()

        private var mantra: Mantra = MantraCatalog.default
        private var beads: Int = 0
        private var sessions: List<JapaSession> = emptyList()
        private var suggestion: MantraSuggestion? = null
        private var brahmaMuhurta: BrahmaMuhurtaView? = null
        private var todayNakshatra: Int? = null
        private var todayTithi: Int? = null

        /** Loads the panchanga hooks and any in-progress mala, then shows the counter. */
        fun load() {
            viewModelScope.launch {
                val resolved = resolveLocation()
                val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
                val snapshot = (astronomyEngine.snapshotAt(now, resolved.coordinates) as? AppResult.Success)?.data
                todayNakshatra = snapshot?.nakshatra?.number
                todayTithi = snapshot?.tithi?.number
                brahmaMuhurta =
                    snapshot
                        ?.muhurtas
                        ?.firstOrNull { it.name.contains("Brahma", ignoreCase = true) }
                        ?.let { BrahmaMuhurtaView(window(it)) }
                suggestion = suggestionFor()

                sessions = japaRepository.sessions.first()
                val progress = japaRepository.inProgress.first()
                mantra = progress?.mantraId?.let(MantraCatalog::byId) ?: suggestion?.mantra ?: MantraCatalog.default
                beads = progress?.beads ?: 0
                emit()
            }
        }

        /** Counts one bead. */
        fun countBead() {
            beads++
            val saved = beads
            val mantraId = mantra.id
            viewModelScope.launch { japaRepository.saveProgress(JapaProgress(mantraId = mantraId, beads = saved)) }
            emit()
        }

        /** Switches the mantra; a partial mala on the previous mantra is discarded. */
        fun selectMantra(id: String) {
            val chosen = MantraCatalog.byId(id) ?: return
            if (chosen.id != mantra.id) {
                mantra = chosen
                beads = 0
                viewModelScope.launch { japaRepository.clearProgress() }
            }
            emit()
        }

        /** Logs the current sitting to the history (stamped with the day's nakshatra/tithi) and resets. */
        fun finish() {
            if (beads <= 0) return
            val session =
                JapaSession(
                    completedAtEpochMillis = System.currentTimeMillis(),
                    dateEpochDay = LocalDate.now(ZoneId.systemDefault()).toEpochDay(),
                    mantraId = mantra.id,
                    beads = beads,
                    rounds = JapaLogic.rounds(beads),
                    nakshatraNumber = todayNakshatra,
                    tithiNumber = todayTithi,
                )
            viewModelScope.launch {
                japaRepository.completeSession(session)
                sessions = japaRepository.sessions.first()
                beads = 0
                emit()
            }
        }

        /** Discards the current count without logging it. */
        fun reset() {
            beads = 0
            viewModelScope.launch { japaRepository.clearProgress() }
            emit()
        }

        private fun emit() {
            val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
            _uiState.value =
                JapaUiState.Ready(
                    mantra = mantra,
                    mantras = MantraCatalog.all,
                    beads = beads,
                    rounds = JapaLogic.rounds(beads),
                    beadInMala = JapaLogic.beadInMala(beads),
                    todayBeads = JapaLogic.beadsOn(sessions, today) + beads,
                    streak = JapaLogic.currentStreak(sessions.map { it.dateEpochDay }.toSet(), today),
                    history = sessions.take(HISTORY_LIMIT).map { it.toView() },
                    suggestion = suggestion,
                    brahmaMuhurta = brahmaMuhurta,
                )
        }

        // The beeja mantra of the primary (or first) chart-ready profile's current mahadasha lord.
        private suspend fun suggestionFor(): MantraSuggestion? {
            val chart = primaryChart() ?: return null
            val lord = currentDashaLord(chart) ?: return null
            return MantraCatalog.forGraha(lord)?.let { MantraSuggestion(mantra = it, dashaLordName = lord.displayName) }
        }

        // The natal chart of the primary (or first) chart-ready profile, or null if none can be cast.
        private suspend fun primaryChart(): NatalChart? {
            val profiles = profileRepository.profiles.first()
            val primaryId = profileRepository.primaryProfileId.first()
            val profile =
                profiles.firstOrNull { it.id == primaryId && it.isChartReady }
                    ?: profiles.firstOrNull { it.isChartReady }
                    ?: return null
            val birth = birthMomentOf(profile) ?: return null
            return (astronomyEngine.natalChartAt(birth.first, birth.second) as? AppResult.Success)?.data
        }

        // The lord of the mahadasha the chart is currently in, or null if outside every period.
        private fun currentDashaLord(chart: NatalChart): Graha? {
            val nowMillis = System.currentTimeMillis()
            return chart.vimshottari
                .firstOrNull {
                    nowMillis >= it.start.toEpochMilliseconds() && nowMillis < it.end.toEpochMilliseconds()
                }?.lord
        }

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

        private fun JapaSession.toView(): JapaSessionView =
            JapaSessionView(
                dateLabel = LocalDate.ofEpochDay(dateEpochDay).format(historyDateFormatter),
                mantraName = MantraCatalog.byId(mantraId)?.name ?: mantraId,
                rounds = rounds,
                beads = beads,
                nakshatraLabel = nakshatraNumber?.let { NAKSHATRA_NAMES.getOrNull(it - 1) },
            )

        private fun window(muhurta: Muhurta): String = "${formatTime(muhurta.start)} – ${formatTime(muhurta.end)}"

        private fun formatTime(instant: Instant): String =
            java.time.Instant
                .ofEpochMilli(instant.toEpochMilliseconds())
                .atZone(ZoneId.systemDefault())
                .format(timeFormatter)
    }

private val historyDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

// Display-only nakshatra names (1..27), for labelling history. Kept here rather than reaching into the
// astronomy module, since the session stores just the number to stay decoupled.
private val NAKSHATRA_NAMES: List<String> =
    listOf(
        "Ashwini",
        "Bharani",
        "Krittika",
        "Rohini",
        "Mrigashira",
        "Ardra",
        "Punarvasu",
        "Pushya",
        "Ashlesha",
        "Magha",
        "Purva Phalguni",
        "Uttara Phalguni",
        "Hasta",
        "Chitra",
        "Swati",
        "Vishakha",
        "Anuradha",
        "Jyeshtha",
        "Mula",
        "Purva Ashadha",
        "Uttara Ashadha",
        "Shravana",
        "Dhanishta",
        "Shatabhisha",
        "Purva Bhadrapada",
        "Uttara Bhadrapada",
        "Revati",
    )

/** A mantra offered because it's the beeja of the profile's current mahadasha lord. */
data class MantraSuggestion(
    val mantra: Mantra,
    val dashaLordName: String,
)

/** Today's Brahma Muhurta window, as a display label. */
data class BrahmaMuhurtaView(
    val label: String,
)

/** A logged sitting, formatted for the history list. */
data class JapaSessionView(
    val dateLabel: String,
    val mantraName: String,
    val rounds: Int,
    val beads: Int,
    val nakshatraLabel: String?,
)

/** UI state for the japa screen. */
sealed interface JapaUiState {
    /** The hooks and any saved count are loading. */
    data object Loading : JapaUiState

    /**
     * The counter is ready.
     *
     * @property mantra the mantra currently being chanted.
     * @property mantras the catalog, for the picker.
     * @property beads beads counted in the current sitting.
     * @property rounds completed malas in the current sitting.
     * @property beadInMala the bead within the current mala (0..107).
     * @property todayBeads total beads today (logged sittings plus the current one).
     * @property streak consecutive days with a logged sitting.
     * @property history recent logged sittings, newest first.
     * @property suggestion the mahadasha-lord beeja suggestion, or `null` when no chart is available.
     * @property brahmaMuhurta today's Brahma Muhurta window, or `null` if unavailable.
     */
    data class Ready(
        val mantra: Mantra,
        val mantras: List<Mantra>,
        val beads: Int,
        val rounds: Int,
        val beadInMala: Int,
        val todayBeads: Int,
        val streak: Int,
        val history: List<JapaSessionView>,
        val suggestion: MantraSuggestion?,
        val brahmaMuhurta: BrahmaMuhurtaView?,
    ) : JapaUiState
}
