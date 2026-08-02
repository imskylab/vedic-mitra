/*
 * Copyright (c) 2026 Vedic Mitra contributors
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

package io.github.vedicmitra.feature.home

import com.google.common.truth.Truth.assertThat
import io.github.vedicmitra.core.astronomy.AstronomyEngine
import io.github.vedicmitra.core.astronomy.AstronomySnapshot
import io.github.vedicmitra.core.astronomy.Nakshatra
import io.github.vedicmitra.core.astronomy.Paksha
import io.github.vedicmitra.core.astronomy.SunTimes
import io.github.vedicmitra.core.astronomy.Tithi
import io.github.vedicmitra.core.astronomy.Vara
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
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
        // Unconfined main runs viewModelScope work eagerly, so init-time loading completes inline.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads panchanga into the success state`() =
        runTest {
            val viewModel = HomeViewModel(FakeAstronomyEngine(AppResult.Success(SAMPLE)))

            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.snapshot).isEqualTo(SAMPLE)
            assertThat(state.errorMessage).isNull()
        }

    @Test
    fun `surfaces failures as an error message`() =
        runTest {
            val failure = AppResult.Failure(IllegalStateException("boom"))
            val viewModel = HomeViewModel(FakeAstronomyEngine(failure))

            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.snapshot).isNull()
            assertThat(state.errorMessage).isEqualTo("boom")
        }
}

private class FakeAstronomyEngine(
    private val result: AppResult<AstronomySnapshot>,
) : AstronomyEngine {
    override suspend fun snapshotAt(
        instant: Instant,
        location: GeoCoordinates,
    ) = result
}

private val SAMPLE =
    AstronomySnapshot(
        instant = Instant.fromEpochMilliseconds(0L),
        location = GeoCoordinates(latitude = 0.0, longitude = 0.0),
        sunTimes = SunTimes(sunrise = null, sunset = null),
        tithi = Tithi(number = 5, paksha = Paksha.SHUKLA, name = "Panchami"),
        nakshatra = Nakshatra(number = 25, name = "Purva Bhadrapada"),
        vara = Vara.SOMAVARA,
    )
