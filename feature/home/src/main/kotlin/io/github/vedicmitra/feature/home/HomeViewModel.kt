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
import io.github.vedicmitra.core.astronomy.GrahaPosition
import io.github.vedicmitra.core.astronomy.MuhurtaKind
import io.github.vedicmitra.core.astronomy.MuhurtaQuality
import io.github.vedicmitra.core.astronomy.PanchangaNow
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.model.MaasaReckoning
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.datastore.UserPreferencesRepository
import io.github.vedicmitra.core.domain.AddReminderUseCase
import io.github.vedicmitra.core.domain.ResolveLocationUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
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
        private val addReminder: AddReminderUseCase,
        userPreferences: UserPreferencesRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(HomeUiState())

        init {
            // Relabelling only -- see CalendarViewModel. Home and the calendar read the same
            // preference so the two can never name the same month differently.
            viewModelScope.launch {
                userPreferences.maasaReckoning.collect { reckoning ->
                    _uiState.update { it.copy(maasaReckoning = reckoning) }
                }
            }
        }

        private val _messages = MutableSharedFlow<String>()

        /** One-shot user messages (e.g. reminder-set confirmations) for the screen to surface. */
        val messages: SharedFlow<String> = _messages.asSharedFlow()

        /** Observable UI state consumed by the home screen. */
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

        /** (Re)loads today's landing content for the resolved location. */
        fun load() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                val resolved = resolveLocation()
                val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
                // Read the day's identity (tithi, nakshatra, …) at today's *sunrise* — the convention
                // by which panchangas name the day. Sampling at sunrise (not noon) matches published
                // almanacs on days where the tithi rolls over between sunrise and midday, and the
                // Panchanga view anchors to the same instant so the two never disagree. The "auspicious
                // now" band below still uses `now`.
                val today = LocalDate.now(ZoneId.of(resolved.zoneId))
                val dayReference =
                    sunriseReference(noonOf(today, resolved.zoneId), resolved.coordinates)
                when (val snapshot = astronomyEngine.snapshotAt(dayReference, resolved.coordinates)) {
                    is AppResult.Success -> {
                        val upcoming = upcomingEntries(now, resolved.coordinates)
                        // Only three FestivalTypes exist; anything that isn't a lunar OBSERVANCE
                        // (Amavasya/Purnima/Ekadashi) is a named festival or a Sankranti.
                        val festivals = upcoming.filter { it.type != FestivalType.OBSERVANCE }
                        val events = upcoming.filter { it.type == FestivalType.OBSERVANCE }
                        val planets =
                            (astronomyEngine.planetaryPositionsAt(now) as? AppResult.Success)?.data?.positions
                                ?: emptyList()
                        // The day is named for its sunrise tithi; this is what is actually running
                        // now, which differs for part of most days.
                        val nowPanchanga = (astronomyEngine.panchangaNowAt(now) as? AppResult.Success)?.data
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                snapshot = snapshot.data,
                                nowPanchanga = nowPanchanga,
                                auspicious = auspiciousWindow(snapshot.data, now),
                                festivals = festivals,
                                events = events,
                                planets = planets,
                                usingDefaultLocation = resolved.isDefault,
                                locationLabel = resolved.label,
                            )
                        }
                    }

                    is AppResult.Failure ->
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = snapshot.cause.message ?: "Unknown error")
                        }
                }
            }
        }

        /** Sets a reminder for a tapped list item, then emits a confirmation to [messages]. */
        fun setReminder(target: ReminderTarget) {
            viewModelScope.launch {
                val location = resolveLocation().coordinates
                val result =
                    when (target) {
                        is ReminderTarget.Muhurta -> addReminder.addMuhurta(target.kind, location)
                        is ReminderTarget.Observance -> addReminder.addObservance(target.name, target.tithis, location)
                    }
                _messages.emit(
                    if (result is AppResult.Success) "Reminder set for ${target.name}" else "Couldn't set the reminder",
                )
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

        /** All upcoming festivals, observances and Sankrantis within the window, in date order. */
        private suspend fun upcomingEntries(
            now: Instant,
            coordinates: GeoCoordinates,
        ): List<Festival> {
            val result = astronomyEngine.upcomingFestivals(now, coordinates, UPCOMING_WINDOW_DAYS, UPCOMING_LIMIT)
            return if (result is AppResult.Success) result.data else emptyList()
        }

        /**
         * The day's sunrise instant for [coordinates], resolved from a representative [dayInstant]
         * within the civil day, falling back to [dayInstant] itself if the sun does not rise (polar).
         */
        private suspend fun sunriseReference(
            dayInstant: Instant,
            coordinates: GeoCoordinates,
        ): Instant = (astronomyEngine.sunriseAt(dayInstant, coordinates) as? AppResult.Success)?.data ?: dayInstant

        /** Local noon on [date] in [zoneId] — a representative time of day for the day's identity. */
        private fun noonOf(
            date: LocalDate,
            zoneId: String,
        ): Instant {
            val epochMillis =
                date
                    .atTime(NOON_HOUR, 0)
                    .atZone(ZoneId.of(zoneId))
                    .toInstant()
                    .toEpochMilli()
            return Instant.fromEpochMilliseconds(epochMillis)
        }

        private companion object {
            const val NOON_HOUR = 12

            // A little over a year, so every annual festival appears once; the engine dedupes by name.
            const val UPCOMING_WINDOW_DAYS = 400
            const val UPCOMING_LIMIT = 60
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

/** A home-list item the user can set a reminder for, carrying what [HomeViewModel.setReminder] needs. */
sealed interface ReminderTarget {
    /** The item's display name, used in the confirmation message. */
    val name: String

    /**
     * A muhurta window, reminded for by its [kind] — never by [name], which is display copy and so
     * cannot be what a persisted reminder is keyed on.
     */
    data class Muhurta(
        val kind: MuhurtaKind,
        override val name: String,
    ) : ReminderTarget

    /** A recurring observance (e.g. "Ekadashi"), reminded for by its global [tithis]. */
    data class Observance(
        override val name: String,
        val tithis: Set<Int>,
    ) : ReminderTarget
}

/**
 * Immutable UI state for the home screen.
 *
 * @property isLoading whether today's content is being computed.
 * @property snapshot today's panchanga, or `null` before it loads or on error.
 * @property auspicious the auspicious-now / next-auspicious window, or `null` if none.
 * @property festivals upcoming named festivals and Sankrantis, in date order.
 * @property events upcoming lunar observances (Amavasya, Purnima, Ekadashi), in date order.
 * @property planets the grahas' current rashi positions with their next pravesh (ingress).
 * @property errorMessage a human-readable error, or `null` when there is none.
 * @property usingDefaultLocation whether the built-in default location was used.
 * @property locationLabel human-readable name of the location.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val snapshot: AstronomySnapshot? = null,
    val nowPanchanga: PanchangaNow? = null,
    val auspicious: AuspiciousWindow? = null,
    val festivals: List<Festival> = emptyList(),
    val events: List<Festival> = emptyList(),
    val planets: List<GrahaPosition> = emptyList(),
    val errorMessage: String? = null,
    val usingDefaultLocation: Boolean = false,
    val locationLabel: String? = null,
    val maasaReckoning: MaasaReckoning = MaasaReckoning.AMANTA,
)
