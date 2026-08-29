/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.cosmicclock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vedicmitra.core.astronomy.AstronomyEngine
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.domain.ResolveLocationUseCase
import io.github.vedicmitra.feature.cosmicclock.domain.buildPanchangaClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Instant

/**
 * Presentation logic for the Cosmic Clock.
 *
 * Two flows, deliberately separate. [uiState] holds the clock's *structure* — which division of each
 * cycle is current — and changes only when a limb rolls over. [now] ticks once a minute and drives
 * nothing but the sweep of the active arcs and the countdowns beneath them.
 *
 * Keeping them apart is what stops a clock costing a full recomputation every minute: the rings, the
 * segment counts and the active indices are all rebuilt at most once a limb boundary, and the tick
 * touches only a handful of `Double`s. The screen pairs this with a cached static draw layer so the
 * dim ticks are not re-rendered either.
 */
@HiltViewModel
class CosmicClockViewModel
    @Inject
    constructor(
        private val astronomyEngine: AstronomyEngine,
        private val resolveLocation: ResolveLocationUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CosmicClockUiState())

        /** Observable UI state consumed by the screen. */
        val uiState: StateFlow<CosmicClockUiState> = _uiState.asStateFlow()

        /**
         * The current instant, re-emitted on each wall-clock minute.
         *
         * Aligned to the minute boundary rather than ticking every 60 seconds from whenever the
         * screen opened, so the countdowns change over at the same moment the phone's own clock
         * does. `WhileSubscribed` stops it when the screen is not resumed, which is the whole
         * battery story: no work at all while the clock is not being looked at.
         */
        val now: StateFlow<Instant> =
            flow {
                while (true) {
                    val millis = System.currentTimeMillis()
                    emit(Instant.fromEpochMilliseconds(millis))
                    delay(MINUTE_MILLIS - millis % MINUTE_MILLIS)
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
            )

        /** (Re)builds the clock for the resolved location. */
        fun load() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                val resolved = resolveLocation()
                val at = Instant.fromEpochMilliseconds(System.currentTimeMillis())
                // Unlike Home, this reads the limbs at *now* rather than at sunrise. Home names the
                // day, which is a sunrise convention; this shows what is running at the moment the
                // reader is looking, which is a different tithi for part of most days.
                when (val snapshot = astronomyEngine.snapshotAt(at, resolved.coordinates)) {
                    is AppResult.Success ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                model = buildPanchangaClock(snapshot.data, at),
                                locationLabel = resolved.label,
                                usingDefaultLocation = resolved.isDefault,
                                errorMessage = null,
                            )
                        }

                    is AppResult.Failure ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = snapshot.cause.message ?: "Could not read the panchanga.",
                            )
                        }
                }
            }
        }

        private companion object {
            const val MINUTE_MILLIS = 60_000L

            /** Long enough to survive a rotation without restarting the ticker. */
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
