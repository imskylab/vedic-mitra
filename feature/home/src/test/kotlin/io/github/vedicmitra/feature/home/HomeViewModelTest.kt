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

import com.google.common.truth.Truth.assertThat
import io.github.vedicmitra.core.astronomy.AstronomyEngine
import io.github.vedicmitra.core.astronomy.AstronomySnapshot
import io.github.vedicmitra.core.astronomy.Ayana
import io.github.vedicmitra.core.astronomy.GoldenHour
import io.github.vedicmitra.core.astronomy.Karana
import io.github.vedicmitra.core.astronomy.Maasa
import io.github.vedicmitra.core.astronomy.MoonPhase
import io.github.vedicmitra.core.astronomy.MoonTimes
import io.github.vedicmitra.core.astronomy.Nakshatra
import io.github.vedicmitra.core.astronomy.Paksha
import io.github.vedicmitra.core.astronomy.PanchangaDaySummary
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
class HomeViewModelTest {
    @Before
    fun setUp() {
        // Unconfined main runs viewModelScope work eagerly, so load() completes inline.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `computes for the resolved location`() =
        runTest {
            val engine = RecordingAstronomyEngine(AppResult.Success(SAMPLE))
            val coordinates = GeoCoordinates(latitude = 12.9716, longitude = 77.5946) // Bengaluru
            val viewModel = HomeViewModel(engine, resolveTo(coordinates, label = "Bengaluru", isDefault = false))

            viewModel.load()

            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.snapshot).isEqualTo(SAMPLE)
            assertThat(state.usingDefaultLocation).isFalse()
            assertThat(state.locationLabel).isEqualTo("Bengaluru")
            assertThat(engine.lastLocation).isEqualTo(coordinates)
        }

    @Test
    fun `flags when the default location was used`() =
        runTest {
            val engine = RecordingAstronomyEngine(AppResult.Success(SAMPLE))
            val fallback = GeoCoordinates(latitude = 28.6139, longitude = 77.2090)
            val viewModel = HomeViewModel(engine, resolveTo(fallback, label = "New Delhi", isDefault = true))

            viewModel.load()

            val state = viewModel.uiState.value
            assertThat(state.usingDefaultLocation).isTrue()
            assertThat(state.snapshot).isEqualTo(SAMPLE)
            assertThat(engine.lastLocation).isEqualTo(fallback)
        }

    @Test
    fun `surfaces engine failures as an error message`() =
        runTest {
            val engine = RecordingAstronomyEngine(AppResult.Failure(IllegalStateException("boom")))
            val location = GeoCoordinates(latitude = 0.0, longitude = 0.0)
            val viewModel = HomeViewModel(engine, resolveTo(location, label = "Nowhere", isDefault = false))

            viewModel.load()

            val state = viewModel.uiState.value
            assertThat(state.snapshot).isNull()
            assertThat(state.errorMessage).isEqualTo("boom")
        }

    private fun resolveTo(
        coordinates: GeoCoordinates,
        label: String,
        isDefault: Boolean,
    ): ResolveLocationUseCase {
        val useCase = mockk<ResolveLocationUseCase>()
        coEvery { useCase() } returns
            ResolvedLocation(coordinates = coordinates, zoneId = "UTC", label = label, isDefault = isDefault)
        return useCase
    }
}

private class RecordingAstronomyEngine(
    private val result: AppResult<AstronomySnapshot>,
) : AstronomyEngine {
    var lastLocation: GeoCoordinates? = null

    override suspend fun snapshotAt(
        instant: Instant,
        location: GeoCoordinates,
    ): AppResult<AstronomySnapshot> {
        lastLocation = location
        return result
    }

    override suspend fun daySummaryAt(
        instant: Instant,
        location: GeoCoordinates,
    ): AppResult<PanchangaDaySummary> =
        AppResult.Success(
            PanchangaDaySummary(
                tithi = Tithi(number = 5, paksha = Paksha.SHUKLA, name = "Panchami"),
                nakshatra = Nakshatra(number = 25, name = "Purva Bhadrapada"),
                moonPhase = MoonPhase.FULL_MOON,
            ),
        )
}

private val SAMPLE =
    AstronomySnapshot(
        instant = Instant.fromEpochMilliseconds(0L),
        location = GeoCoordinates(latitude = 0.0, longitude = 0.0),
        sunTimes = SunTimes(sunrise = null, sunset = null),
        moonTimes = MoonTimes(moonrise = null, moonset = null),
        tithi = Tithi(number = 5, paksha = Paksha.SHUKLA, name = "Panchami"),
        nakshatra = Nakshatra(number = 25, name = "Purva Bhadrapada"),
        yoga = Yoga(number = 18, name = "Variyana"),
        karana = Karana(number = 10, name = "Balava"),
        vara = Vara.SOMAVARA,
        maasa = Maasa(number = 4, name = "Ashadha", adhika = false),
        samvatsara = Samvatsara(number = 40, name = "Parabhava", shakaYear = 1948),
        ayana = Ayana.UTTARAYANA,
        ritu = Ritu.SHISHIRA,
        moonPhase = MoonPhase.FULL_MOON,
        goldenHour = GoldenHour(morningStart = null, morningEnd = null, eveningStart = null, eveningEnd = null),
        muhurtas = emptyList(),
    )
