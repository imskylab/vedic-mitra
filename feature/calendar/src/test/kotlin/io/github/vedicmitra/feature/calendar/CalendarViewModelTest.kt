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

import com.google.common.truth.Truth.assertThat
import io.github.vedicmitra.core.astronomy.AstronomyEngine
import io.github.vedicmitra.core.astronomy.AstronomySnapshot
import io.github.vedicmitra.core.astronomy.Ayana
import io.github.vedicmitra.core.astronomy.Festival
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
import io.github.vedicmitra.core.common.model.MaasaReckoning
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.datastore.DarkThemeConfig
import io.github.vedicmitra.core.datastore.ThemeSettings
import io.github.vedicmitra.core.datastore.UserPreferencesRepository
import io.github.vedicmitra.core.domain.ResolveLocationUseCase
import io.github.vedicmitra.core.domain.ResolvedLocation
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load fills the month grid and selects a day`() =
        runTest {
            val viewModel = viewModel()

            viewModel.load()

            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            // One cell per day of the displayed month.
            assertThat(state.days).hasSize(state.yearMonth.lengthOfMonth())
            assertThat(state.days.map { it.date.dayOfMonth }).containsExactlyElementsIn(
                1..state.yearMonth.lengthOfMonth(),
            )
            // A day is selected and its full snapshot loaded.
            assertThat(state.selectedSnapshot).isEqualTo(SAMPLE)
        }

    @Test
    fun `showNextMonth advances the displayed month and refills the grid`() =
        runTest {
            val viewModel = viewModel()
            viewModel.load()
            val startMonth = viewModel.uiState.value.yearMonth

            viewModel.showNextMonth()

            val state = viewModel.uiState.value
            assertThat(state.yearMonth).isEqualTo(startMonth.plusMonths(1))
            assertThat(state.days).hasSize(state.yearMonth.lengthOfMonth())
            // A month change selects the first of the newly shown month.
            assertThat(state.selectedDate).isEqualTo(state.yearMonth.atDay(1))
        }

    @Test
    fun `showPreviousMonth moves back a month`() =
        runTest {
            val viewModel = viewModel()
            viewModel.load()
            val startMonth = viewModel.uiState.value.yearMonth

            viewModel.showPreviousMonth()

            assertThat(viewModel.uiState.value.yearMonth).isEqualTo(startMonth.minusMonths(1))
        }

    @Test
    fun `selectDate loads that day's snapshot`() =
        runTest {
            val viewModel = viewModel()
            viewModel.load()
            val target =
                viewModel.uiState.value.yearMonth
                    .atDay(15)

            viewModel.selectDate(target)

            val state = viewModel.uiState.value
            assertThat(state.selectedDate).isEqualTo(target)
            assertThat(state.selectedSnapshot).isEqualTo(SAMPLE)
        }

    @Test
    fun `flags when the default location was used`() =
        runTest {
            val fallback =
                ResolvedLocation(
                    coordinates = GeoCoordinates(latitude = 28.6139, longitude = 77.2090),
                    zoneId = "Asia/Kolkata",
                    label = "New Delhi",
                    isDefault = true,
                )
            val viewModel = viewModel(resolved = fallback)

            viewModel.load()

            assertThat(viewModel.uiState.value.usingDefaultLocation).isTrue()
        }

    private fun viewModel(
        resolved: ResolvedLocation =
            ResolvedLocation(
                coordinates = BENGALURU,
                zoneId = "Asia/Kolkata",
                label = "Bengaluru",
                isDefault = false,
            ),
    ): CalendarViewModel {
        val resolveLocation = mockk<ResolveLocationUseCase>()
        coEvery { resolveLocation() } returns resolved
        return CalendarViewModel(
            astronomyEngine = FakeAstronomyEngine(),
            resolveLocation = resolveLocation,
            userPreferences = FakePreferences(),
        )
    }
}

private class FakeAstronomyEngine : AstronomyEngine {
    override suspend fun snapshotAt(
        instant: Instant,
        location: GeoCoordinates,
    ): AppResult<AstronomySnapshot> = AppResult.Success(SAMPLE)

    override suspend fun daySummaryAt(
        instant: Instant,
        location: GeoCoordinates,
    ): AppResult<PanchangaDaySummary> =
        AppResult.Success(
            PanchangaDaySummary(
                tithi = SAMPLE.tithi,
                nakshatra = SAMPLE.nakshatra,
                moonPhase = SAMPLE.moonPhase,
            ),
        )

    override suspend fun upcomingFestivals(
        instant: Instant,
        location: GeoCoordinates,
        withinDays: Int,
        limit: Int,
    ): AppResult<List<Festival>> = AppResult.Success(emptyList())
}

private val BENGALURU = GeoCoordinates(latitude = 12.9716, longitude = 77.5946)

private val SAMPLE =
    AstronomySnapshot(
        instant = Instant.fromEpochMilliseconds(0L),
        location = BENGALURU,
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

// The month scheme is a display preference; these tests are about loading and reminders, so the
// fake just holds the default and never changes.
private class FakePreferences : UserPreferencesRepository {
    override val themeSettings: Flow<ThemeSettings> =
        flowOf(ThemeSettings(darkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM, useDynamicColor = false))
    override val maasaReckoning: Flow<MaasaReckoning> = flowOf(MaasaReckoning.AMANTA)

    override suspend fun setDarkThemeConfig(config: DarkThemeConfig) = Unit

    override suspend fun setDynamicColor(enabled: Boolean) = Unit

    override suspend fun setMaasaReckoning(reckoning: MaasaReckoning) = Unit
}
