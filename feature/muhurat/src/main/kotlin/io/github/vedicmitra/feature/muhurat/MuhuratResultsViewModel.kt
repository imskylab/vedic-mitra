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
import io.github.vedicmitra.core.astronomy.MuhurtaActivity
import io.github.vedicmitra.core.astronomy.RankedMuhurtaDay
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.domain.ResolveLocationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Instant

/** The `activity` nav argument — the [MuhurtaActivity] name whose best days are shown. */
internal const val MUHURAT_ACTIVITY_ARG = "activity"

/**
 * Presentation logic for the muhurta results screen: resolves the location and asks the engine for
 * the best upcoming days for the activity named in the [SavedStateHandle], keeping the top few.
 */
@HiltViewModel
class MuhuratResultsViewModel
    @Inject
    constructor(
        private val astronomyEngine: AstronomyEngine,
        private val resolveLocation: ResolveLocationUseCase,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val activity: MuhurtaActivity =
            savedStateHandle
                .get<String>(MUHURAT_ACTIVITY_ARG)
                ?.let { name -> runCatching { MuhurtaActivity.valueOf(name) }.getOrNull() }
                ?: MuhurtaActivity.GRIHA_PRAVESH

        private val _uiState = MutableStateFlow<MuhuratResultsUiState>(MuhuratResultsUiState.Loading)

        /** Observable UI state consumed by the results screen. */
        val uiState: StateFlow<MuhuratResultsUiState> = _uiState.asStateFlow()

        /** (Re)loads the ranked best days for the activity at the resolved location. */
        fun load() {
            viewModelScope.launch {
                _uiState.value = MuhuratResultsUiState.Loading
                val resolved = resolveLocation()
                val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
                val result = astronomyEngine.bestMuhurtasFor(activity, now, WINDOW_DAYS, resolved.coordinates)
                val days = (result as? AppResult.Success)?.data.orEmpty().take(RESULTS_LIMIT)
                _uiState.value =
                    MuhuratResultsUiState.Ready(
                        activity = activity,
                        days = days,
                        usingDefaultLocation = resolved.isDefault,
                        locationLabel = resolved.label,
                    )
            }
        }

        private companion object {
            const val WINDOW_DAYS = 45
            const val RESULTS_LIMIT = 10
        }
    }

/** UI state for the muhurta results screen. */
sealed interface MuhuratResultsUiState {
    /** The best days are being computed. */
    data object Loading : MuhuratResultsUiState

    /**
     * The ranked days are ready.
     *
     * @property activity the activity the days were ranked for.
     * @property days the best upcoming days, best-first (may be empty).
     * @property usingDefaultLocation whether a built-in default location was used.
     * @property locationLabel human-readable name of the location used.
     */
    data class Ready(
        val activity: MuhurtaActivity,
        val days: List<RankedMuhurtaDay>,
        val usingDefaultLocation: Boolean,
        val locationLabel: String,
    ) : MuhuratResultsUiState
}
