/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vedicmitra.core.astronomy.AstronomyEngine
import io.github.vedicmitra.core.astronomy.AstronomySnapshot
import io.github.vedicmitra.core.astronomy.MoonPhase
import io.github.vedicmitra.core.astronomy.Tithi
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.location.LocationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlin.time.Instant

/**
 * Presentation logic for the calendar screen (MVVM). Shows a month of days — each with a cheap
 * per-day panchanga summary — and the full panchanga for whichever day is selected. Uses the device
 * location, falling back to a fixed default when it is unavailable (e.g. permission not granted).
 *
 * [load] is driven by the screen once it has resolved the location permission.
 */
@HiltViewModel
class CalendarViewModel
    @Inject
    constructor(
        private val astronomyEngine: AstronomyEngine,
        private val locationProvider: LocationProvider,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CalendarUiState())

        /** Observable UI state consumed by the calendar screen. */
        val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

        private var location: GeoCoordinates = DEFAULT_LOCATION

        /** (Re)loads the current month, using the device location when available. */
        fun load() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                val locationResult = locationProvider.currentLocation()
                val usingDefault = locationResult !is AppResult.Success
                location = (locationResult as? AppResult.Success)?.data ?: DEFAULT_LOCATION
                _uiState.update { it.copy(usingDefaultLocation = usingDefault) }

                val today = LocalDate.now(ZoneId.systemDefault())
                refreshMonth(YearMonth.from(today), select = today)
            }
        }

        /** Shows the month before the one currently displayed. */
        fun showPreviousMonth() = changeMonth(months = -1)

        /** Shows the month after the one currently displayed. */
        fun showNextMonth() = changeMonth(months = 1)

        private fun changeMonth(months: Long) {
            viewModelScope.launch {
                val target = _uiState.value.yearMonth.plusMonths(months)
                refreshMonth(target, select = target.atDay(1))
            }
        }

        /** Loads the full panchanga for [date] and marks it selected. */
        fun selectDate(date: LocalDate) {
            viewModelScope.launch { loadSelected(date) }
        }

        private suspend fun refreshMonth(
            yearMonth: YearMonth,
            select: LocalDate,
        ) {
            _uiState.update { it.copy(isLoading = true, yearMonth = yearMonth, errorMessage = null) }

            val days =
                (1..yearMonth.lengthOfMonth()).mapNotNull { dayOfMonth ->
                    val date = yearMonth.atDay(dayOfMonth)
                    val summary = astronomyEngine.daySummaryAt(noonOf(date), location)
                    (summary as? AppResult.Success)?.data?.let { data ->
                        CalendarDay(date = date, tithi = data.tithi, moonPhase = data.moonPhase)
                    }
                }

            _uiState.update { it.copy(isLoading = false, days = days) }
            loadSelected(select)
        }

        private suspend fun loadSelected(date: LocalDate) {
            when (val snapshot = astronomyEngine.snapshotAt(noonOf(date), location)) {
                is AppResult.Success ->
                    _uiState.update {
                        it.copy(selectedDate = date, selectedSnapshot = snapshot.data, errorMessage = null)
                    }

                is AppResult.Failure ->
                    _uiState.update {
                        it.copy(selectedDate = date, errorMessage = snapshot.cause.message ?: "Unknown error")
                    }
            }
        }

        /** Local noon on [date] — a representative time of day for that date's panchanga. */
        private fun noonOf(date: LocalDate): Instant {
            val epochMillis =
                date
                    .atTime(NOON_HOUR, 0)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            return Instant.fromEpochMilliseconds(epochMillis)
        }

        private companion object {
            const val NOON_HOUR = 12

            // Used when the device location is unavailable (New Delhi).
            val DEFAULT_LOCATION = GeoCoordinates(latitude = 28.6139, longitude = 77.2090)
        }
    }

/**
 * A single day's cell in the month grid.
 *
 * @property date the Gregorian date.
 * @property tithi the day's lunar day (shown in the cell).
 * @property moonPhase the day's moon phase.
 */
data class CalendarDay(
    val date: LocalDate,
    val tithi: Tithi,
    val moonPhase: MoonPhase,
)

/**
 * Immutable UI state for the calendar screen.
 *
 * @property yearMonth the month currently displayed.
 * @property selectedDate the day whose detail is shown.
 * @property days the displayed month's per-day summaries, in date order.
 * @property selectedSnapshot the selected day's full panchanga, or `null` before it loads.
 * @property isLoading whether the month grid is being computed.
 * @property errorMessage a human-readable error, or `null` when there is none.
 * @property usingDefaultLocation whether the default location was used (device location unavailable).
 */
data class CalendarUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val days: List<CalendarDay> = emptyList(),
    val selectedSnapshot: AstronomySnapshot? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val usingDefaultLocation: Boolean = false,
)
