/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

@file:Suppress("MagicNumber")

package io.github.vedicmitra.feature.cosmicclock

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import io.github.vedicmitra.core.astronomy.AstronomyEngine
import io.github.vedicmitra.core.astronomy.AstronomySnapshot
import io.github.vedicmitra.core.astronomy.Ayana
import io.github.vedicmitra.core.astronomy.GoldenHour
import io.github.vedicmitra.core.astronomy.Karana
import io.github.vedicmitra.core.astronomy.LimbWindow
import io.github.vedicmitra.core.astronomy.Maasa
import io.github.vedicmitra.core.astronomy.MoonPhase
import io.github.vedicmitra.core.astronomy.MoonTimes
import io.github.vedicmitra.core.astronomy.Nakshatra
import io.github.vedicmitra.core.astronomy.Paksha
import io.github.vedicmitra.core.astronomy.PanchangaConcept
import io.github.vedicmitra.core.astronomy.PanchangaLimbWindows
import io.github.vedicmitra.core.astronomy.Ritu
import io.github.vedicmitra.core.astronomy.Samvatsara
import io.github.vedicmitra.core.astronomy.SunTimes
import io.github.vedicmitra.core.astronomy.Tithi
import io.github.vedicmitra.core.astronomy.Vara
import io.github.vedicmitra.core.astronomy.Yoga
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.domain.ResolveLocationUseCase
import io.github.vedicmitra.core.domain.ResolvedLocation
import io.mockk.coEvery
import io.mockk.coVerify
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
class CosmicClockViewModelTest {
    private val engine = mockk<AstronomyEngine>()
    private val resolveLocation = mockk<ResolveLocationUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { resolveLocation() } returns
            ResolvedLocation(
                coordinates = GeoCoordinates(latitude = 17.385, longitude = 78.4867),
                zoneId = "Asia/Kolkata",
                label = "Hyderabad",
                isDefault = false,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a successful load builds the clock`() =
        runTest {
            coEvery { engine.snapshotAt(any(), any()) } returns AppResult.Success(snapshot())

            val viewModel = CosmicClockViewModel(engine, resolveLocation)
            viewModel.load()

            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.errorMessage).isNull()
            assertThat(state.locationLabel).isEqualTo("Hyderabad")
            assertThat(state.model?.rings).hasSize(5)
            assertThat(state.model?.ring(PanchangaConcept.KARANA)?.activeIndex).isEqualTo(26)
        }

    @Test
    fun `a failed load reports why rather than showing an empty clock`() =
        runTest {
            coEvery { engine.snapshotAt(any(), any()) } returns
                AppResult.Failure(IllegalArgumentException("Coordinates out of range"))

            val viewModel = CosmicClockViewModel(engine, resolveLocation)
            viewModel.load()

            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.model).isNull()
            assertThat(state.errorMessage).isEqualTo("Coordinates out of range")
        }

    @Test
    fun `a snapshot without limb windows leaves no clock to draw`() =
        runTest {
            // Not an error state — the engine succeeded — but there is nothing to render, and a clock
            // with no progress looks finished rather than pending.
            coEvery { engine.snapshotAt(any(), any()) } returns AppResult.Success(snapshot(limbs = null))

            val viewModel = CosmicClockViewModel(engine, resolveLocation)
            viewModel.load()

            assertThat(viewModel.uiState.value.model).isNull()
            assertThat(viewModel.uiState.value.errorMessage).isNull()
        }

    @Test
    fun `reading the clock does not touch the engine`() =
        runTest {
            // The battery claim in miniature: loading is what costs an ephemeris solve, and reading the
            // time is not loading. This asserts the read path is free -- it does **not** drive the
            // ticker, which needs a shared test scheduler to advance and is not worth the flakiness
            // here. What would actually break the claim is someone wiring the tick to call load(), and
            // catching that needs a device or a Robolectric test the repo has no infrastructure for.
            coEvery { engine.snapshotAt(any(), any()) } returns AppResult.Success(snapshot())

            val viewModel = CosmicClockViewModel(engine, resolveLocation)
            viewModel.load()
            repeat(5) { viewModel.now.value }

            coVerify(exactly = 1) { engine.snapshotAt(any(), any()) }
            coVerify(exactly = 1) { resolveLocation() }
        }

    @Test
    fun `the ticker reports a plausible instant before the first tick`() =
        runTest {
            // stateIn needs an initial value, and a clock that starts at the epoch would draw every arc
            // at zero for up to a minute.
            coEvery { engine.snapshotAt(any(), any()) } returns AppResult.Success(snapshot())

            val viewModel = CosmicClockViewModel(engine, resolveLocation)

            assertWithMessage("the ticker's seed must be the real clock, not the epoch")
                .that(viewModel.now.value.toEpochMilliseconds())
                .isGreaterThan(YEAR_2020_MILLIS)
        }

    @Test
    fun `reloading replaces the previous clock rather than accumulating`() =
        runTest {
            coEvery { engine.snapshotAt(any(), any()) } returns AppResult.Success(snapshot())

            val viewModel = CosmicClockViewModel(engine, resolveLocation)
            viewModel.load()
            viewModel.load()

            assertThat(
                viewModel.uiState.value.model
                    ?.rings,
            ).hasSize(5)
            coVerify(exactly = 2) { engine.snapshotAt(any(), any()) }
        }

    private fun snapshot(limbs: PanchangaLimbWindows? = limbWindows()): AstronomySnapshot =
        AstronomySnapshot(
            instant = AT,
            location = GeoCoordinates(latitude = 17.385, longitude = 78.4867),
            sunTimes = SunTimes(sunrise = AT, sunset = AT),
            moonTimes = MoonTimes(moonrise = AT, moonset = AT),
            tithi = Tithi(number = 14, paksha = Paksha.SHUKLA, name = "Chaturdashi"),
            nakshatra = Nakshatra(number = 4, name = "Rohini"),
            moonPada = 3,
            yoga = Yoga(number = 12, name = "Dhriti"),
            karana = Karana(number = 27, name = "Bava"),
            vara = Vara.SHUKRAVARA,
            maasa = Maasa(number = 5, name = "Shravana", adhika = false),
            samvatsara = Samvatsara(number = 39, name = "Vishvavasu", shakaYear = 1948),
            ayana = Ayana.DAKSHINAYANA,
            ritu = Ritu.VARSHA,
            moonPhase = MoonPhase.FULL_MOON,
            goldenHour = GoldenHour(null, null, null, null),
            muhurtas = emptyList(),
            limbs = limbs,
        )

    private fun limbWindows() =
        PanchangaLimbWindows(
            tithi = window(0.25),
            nakshatra = window(0.6),
            yoga = window(0.4),
            karana = window(0.8),
            moonPada = window(0.1),
            moonRashi = window(0.3),
            moonPhase = window(0.9),
            sunRashi = window(0.7),
            vara = window(0.5),
        )

    private companion object {
        val AT = Instant.fromEpochMilliseconds(1_787_000_000_000L)
        const val YEAR_2020_MILLIS = 1_577_836_800_000L

        fun window(fraction: Double) =
            LimbWindow(
                start = AT,
                end = Instant.fromEpochMilliseconds(AT.toEpochMilliseconds() + 86_400_000L),
                angularFraction = fraction,
            )
    }
}
