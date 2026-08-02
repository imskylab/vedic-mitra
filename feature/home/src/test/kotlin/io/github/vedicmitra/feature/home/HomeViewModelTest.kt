/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

package io.github.vedicmitra.feature.home

import com.google.common.truth.Truth.assertThat
import io.github.vedicmitra.core.astronomy.AstronomyEngine
import io.github.vedicmitra.core.astronomy.AstronomySnapshot
import io.github.vedicmitra.core.astronomy.Karana
import io.github.vedicmitra.core.astronomy.Nakshatra
import io.github.vedicmitra.core.astronomy.Paksha
import io.github.vedicmitra.core.astronomy.SunTimes
import io.github.vedicmitra.core.astronomy.Tithi
import io.github.vedicmitra.core.astronomy.Vara
import io.github.vedicmitra.core.astronomy.Yoga
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.location.LocationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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
    fun `uses the device location when available`() =
        runTest {
            val engine = RecordingAstronomyEngine(AppResult.Success(SAMPLE))
            val deviceLocation = GeoCoordinates(latitude = 12.9716, longitude = 77.5946) // Bengaluru
            val viewModel = HomeViewModel(engine, FakeLocationProvider(AppResult.Success(deviceLocation)))

            viewModel.load()

            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.snapshot).isEqualTo(SAMPLE)
            assertThat(state.usingDefaultLocation).isFalse()
            assertThat(engine.lastLocation).isEqualTo(deviceLocation)
        }

    @Test
    fun `falls back to the default location when unavailable`() =
        runTest {
            val engine = RecordingAstronomyEngine(AppResult.Success(SAMPLE))
            val failure = AppResult.Failure(SecurityException("no permission"))
            val viewModel = HomeViewModel(engine, FakeLocationProvider(failure))

            viewModel.load()

            val state = viewModel.uiState.value
            assertThat(state.usingDefaultLocation).isTrue()
            assertThat(state.snapshot).isEqualTo(SAMPLE)
            // The fallback location was passed to the engine (not the caller-provided device location).
            assertThat(engine.lastLocation).isNotNull()
        }

    @Test
    fun `surfaces engine failures as an error message`() =
        runTest {
            val engine = RecordingAstronomyEngine(AppResult.Failure(IllegalStateException("boom")))
            val location = GeoCoordinates(latitude = 0.0, longitude = 0.0)
            val viewModel = HomeViewModel(engine, FakeLocationProvider(AppResult.Success(location)))

            viewModel.load()

            val state = viewModel.uiState.value
            assertThat(state.snapshot).isNull()
            assertThat(state.errorMessage).isEqualTo("boom")
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
}

private class FakeLocationProvider(
    private val result: AppResult<GeoCoordinates>,
) : LocationProvider {
    override suspend fun currentLocation(): AppResult<GeoCoordinates> = result

    override fun locationUpdates(): Flow<GeoCoordinates> = emptyFlow()
}

private val SAMPLE =
    AstronomySnapshot(
        instant = Instant.fromEpochMilliseconds(0L),
        location = GeoCoordinates(latitude = 0.0, longitude = 0.0),
        sunTimes = SunTimes(sunrise = null, sunset = null),
        tithi = Tithi(number = 5, paksha = Paksha.SHUKLA, name = "Panchami"),
        nakshatra = Nakshatra(number = 25, name = "Purva Bhadrapada"),
        yoga = Yoga(number = 18, name = "Variyana"),
        karana = Karana(number = 10, name = "Balava"),
        vara = Vara.SOMAVARA,
        muhurtas = emptyList(),
    )
