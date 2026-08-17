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
import com.google.common.truth.Truth.assertThat
import io.github.vedicmitra.core.astronomy.AstronomyEngine
import io.github.vedicmitra.core.astronomy.DayMuhurtaScore
import io.github.vedicmitra.core.astronomy.MuhurtaActivity
import io.github.vedicmitra.core.astronomy.MuhurtaRating
import io.github.vedicmitra.core.astronomy.RankedMuhurtaDay
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.domain.ResolveLocationUseCase
import io.github.vedicmitra.core.domain.ResolvedLocation
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class MuhuratResultsViewModelTest {
    private val astronomyEngine = mockk<AstronomyEngine>()
    private val resolveLocation = mockk<ResolveLocationUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads ranked days for the activity named in the saved state`() =
        runTest {
            coEvery { resolveLocation() } returns
                ResolvedLocation(
                    coordinates = GeoCoordinates(latitude = 28.6139, longitude = 77.2090),
                    zoneId = "Asia/Kolkata",
                    label = "New Delhi",
                    isDefault = true,
                )
            val ranked =
                listOf(
                    RankedMuhurtaDay(
                        atSunrise = Instant.fromEpochMilliseconds(1_705_320_000_000L),
                        score = DayMuhurtaScore(score = 92, rating = MuhurtaRating.EXCELLENT, reasons = emptyList()),
                    ),
                )
            coEvery { astronomyEngine.bestMuhurtasFor(any(), any(), any(), any()) } returns AppResult.Success(ranked)

            val viewModel =
                MuhuratResultsViewModel(
                    astronomyEngine = astronomyEngine,
                    resolveLocation = resolveLocation,
                    savedStateHandle = SavedStateHandle(mapOf(MUHURAT_ACTIVITY_ARG to "VIVAH")),
                )
            viewModel.load()

            val state = viewModel.uiState.value as MuhuratResultsUiState.Ready
            assertThat(state.activity).isEqualTo(MuhurtaActivity.VIVAH)
            assertThat(state.days).isEqualTo(ranked)
            assertThat(state.usingDefaultLocation).isTrue()
        }

    @Test
    fun `falls back to a default activity when the saved name is missing or invalid`() =
        runTest {
            coEvery { resolveLocation() } returns
                ResolvedLocation(
                    coordinates = GeoCoordinates(latitude = 28.6139, longitude = 77.2090),
                    zoneId = "Asia/Kolkata",
                    label = "New Delhi",
                    isDefault = false,
                )
            coEvery { astronomyEngine.bestMuhurtasFor(any(), any(), any(), any()) } returns
                AppResult.Success(emptyList())

            val viewModel =
                MuhuratResultsViewModel(
                    astronomyEngine = astronomyEngine,
                    resolveLocation = resolveLocation,
                    savedStateHandle = SavedStateHandle(mapOf(MUHURAT_ACTIVITY_ARG to "NOT_AN_ACTIVITY")),
                )
            viewModel.load()

            val state = viewModel.uiState.value as MuhuratResultsUiState.Ready
            assertThat(state.activity).isEqualTo(MuhurtaActivity.GRIHA_PRAVESH)
            assertThat(state.days).isEmpty()
        }
}
