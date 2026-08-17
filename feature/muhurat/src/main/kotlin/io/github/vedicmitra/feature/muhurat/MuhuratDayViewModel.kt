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
import io.github.vedicmitra.core.astronomy.AstronomySnapshot
import io.github.vedicmitra.core.astronomy.MuhurtaQuality
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.domain.ResolveLocationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Instant

/** The `day` nav argument — the chosen day's sunrise instant, as epoch milliseconds. */
internal const val MUHURAT_DAY_ARG = "day"

/** One named time window on a day, e.g. "Abhijit Muhurta" or "Amrit (Choghadiya)". */
data class DayWindow(
    val label: String,
    val start: Instant,
    val end: Instant,
)

/**
 * Presentation logic for the muhurta day-detail screen: computes the full panchanga for the chosen
 * day (from the `day` nav argument) at the resolved location and splits its time windows — the
 * muhurtas and Choghadiya — into the auspicious ones to prefer and the inauspicious ones to avoid.
 */
@HiltViewModel
class MuhuratDayViewModel
    @Inject
    constructor(
        private val astronomyEngine: AstronomyEngine,
        private val resolveLocation: ResolveLocationUseCase,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val dayMillis: Long = savedStateHandle[MUHURAT_DAY_ARG] ?: System.currentTimeMillis()

        private val _uiState = MutableStateFlow<MuhuratDayUiState>(MuhuratDayUiState.Loading)

        /** Observable UI state consumed by the day-detail screen. */
        val uiState: StateFlow<MuhuratDayUiState> = _uiState.asStateFlow()

        /** (Re)loads the chosen day's panchanga and its auspicious/inauspicious windows. */
        fun load() {
            viewModelScope.launch {
                _uiState.value = MuhuratDayUiState.Loading
                val resolved = resolveLocation()
                val instant = Instant.fromEpochMilliseconds(dayMillis)
                _uiState.value =
                    when (val result = astronomyEngine.snapshotAt(instant, resolved.coordinates)) {
                        is AppResult.Success -> readyState(result.data)
                        is AppResult.Failure -> MuhuratDayUiState.Ready(dayMillis, "—", emptyList(), emptyList())
                    }
            }
        }

        private fun readyState(snapshot: AstronomySnapshot): MuhuratDayUiState.Ready =
            MuhuratDayUiState.Ready(
                dateMillis = dayMillis,
                summary = "${snapshot.vara.displayName} · ${snapshot.tithi.name} · ${snapshot.nakshatra.name}",
                auspicious = windowsOf(snapshot, MuhurtaQuality.AUSPICIOUS),
                inauspicious = windowsOf(snapshot, MuhurtaQuality.INAUSPICIOUS),
            )

        /** The muhurtas and Choghadiya of the given [quality] on [snapshot]'s day, in time order. */
        private fun windowsOf(
            snapshot: AstronomySnapshot,
            quality: MuhurtaQuality,
        ): List<DayWindow> {
            val muhurtas =
                snapshot.muhurtas
                    .filter { it.quality == quality }
                    .map { DayWindow(it.name, it.start, it.end) }
            val choghadiya =
                snapshot.choghadiya
                    .filter { it.quality == quality }
                    .map { DayWindow("${it.name.label} (Choghadiya)", it.start, it.end) }
            return (muhurtas + choghadiya).sortedBy { it.start }
        }
    }

/** UI state for the muhurta day-detail screen. */
sealed interface MuhuratDayUiState {
    /** The day's detail is being computed. */
    data object Loading : MuhuratDayUiState

    /**
     * The day's detail is ready.
     *
     * @property dateMillis the day's sunrise instant as epoch milliseconds, for the date header.
     * @property summary a short "weekday · tithi · nakshatra" line.
     * @property auspicious the auspicious windows to prefer, in time order.
     * @property inauspicious the inauspicious windows to avoid, in time order.
     */
    data class Ready(
        val dateMillis: Long,
        val summary: String,
        val auspicious: List<DayWindow>,
        val inauspicious: List<DayWindow>,
    ) : MuhuratDayUiState
}
