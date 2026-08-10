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
import io.github.vedicmitra.core.astronomy.Festival
import io.github.vedicmitra.core.astronomy.FestivalType
import io.github.vedicmitra.core.astronomy.GoldenHour
import io.github.vedicmitra.core.astronomy.Karana
import io.github.vedicmitra.core.astronomy.Maasa
import io.github.vedicmitra.core.astronomy.MoonPhase
import io.github.vedicmitra.core.astronomy.MoonTimes
import io.github.vedicmitra.core.astronomy.Muhurta
import io.github.vedicmitra.core.astronomy.MuhurtaQuality
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
import io.github.vedicmitra.core.domain.AddReminderUseCase
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
class HomeViewModelTest {
    private val addReminder = mockk<AddReminderUseCase>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `computes for the resolved location and lists upcoming festivals`() =
        runTest {
            val festival = Festival("Diwali", Instant.fromEpochMilliseconds(1_762_560_000_000L), FestivalType.FESTIVAL)
            val engine = FakeEngine(AppResult.Success(snapshot()), festivals = listOf(festival))
            val coordinates = GeoCoordinates(latitude = 12.9716, longitude = 77.5946)
            val viewModel = HomeViewModel(engine, resolveTo(coordinates, "Bengaluru", isDefault = false), addReminder)

            viewModel.load()

            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.snapshot).isNotNull()
            assertThat(state.festivals).containsExactly(festival)
            assertThat(state.locationLabel).isEqualTo("Bengaluru")
            assertThat(engine.lastLocation).isEqualTo(coordinates)
        }

    @Test
    fun `splits named festivals and lunar observances into separate lists`() =
        runTest {
            val observance =
                Festival("Ekadashi", Instant.fromEpochMilliseconds(1_760_000_000_000L), FestivalType.OBSERVANCE)
            val festival = Festival("Diwali", Instant.fromEpochMilliseconds(1_762_560_000_000L), FestivalType.FESTIVAL)
            val engine = FakeEngine(AppResult.Success(snapshot()), festivals = listOf(observance, festival))
            val viewModel = HomeViewModel(engine, resolveTo(SOMEWHERE, "Home", isDefault = false), addReminder)

            viewModel.load()

            val state = viewModel.uiState.value
            assertThat(state.festivals).containsExactly(festival)
            assertThat(state.events).containsExactly(observance)
        }

    @Test
    fun `marks an auspicious muhurta active now as the current window`() =
        runTest {
            val now = System.currentTimeMillis()
            val active =
                Muhurta(
                    name = "Abhijit Muhurta",
                    start = Instant.fromEpochMilliseconds(now - 60_000L),
                    end = Instant.fromEpochMilliseconds(now + 3_600_000L),
                    quality = MuhurtaQuality.AUSPICIOUS,
                )
            val engine = FakeEngine(AppResult.Success(snapshot(muhurtas = listOf(active))))
            val viewModel = HomeViewModel(engine, resolveTo(SOMEWHERE, "Home", isDefault = false), addReminder)

            viewModel.load()

            val window = viewModel.uiState.value.auspicious
            assertThat(window).isNotNull()
            assertThat(window?.name).isEqualTo("Abhijit Muhurta")
            assertThat(window?.isActive).isTrue()
            assertThat(window?.quality).isEqualTo(MuhurtaQuality.AUSPICIOUS)
        }

    @Test
    fun `flags when the default location was used`() =
        runTest {
            val engine = FakeEngine(AppResult.Success(snapshot()))
            val viewModel = HomeViewModel(engine, resolveTo(SOMEWHERE, "New Delhi", isDefault = true), addReminder)

            viewModel.load()

            assertThat(viewModel.uiState.value.usingDefaultLocation).isTrue()
        }

    @Test
    fun `surfaces engine failures as an error message`() =
        runTest {
            val engine = FakeEngine(AppResult.Failure(IllegalStateException("boom")))
            val viewModel = HomeViewModel(engine, resolveTo(SOMEWHERE, "Home", isDefault = false), addReminder)

            viewModel.load()

            val state = viewModel.uiState.value
            assertThat(state.snapshot).isNull()
            assertThat(state.errorMessage).isEqualTo("boom")
        }

    @Test
    fun `setReminder for a muhurta delegates to the add-reminder use case with the resolved location`() =
        runTest {
            coEvery { addReminder.addMuhurta(any(), any()) } returns AppResult.Success(Unit)
            val engine = FakeEngine(AppResult.Success(snapshot()))
            val viewModel = HomeViewModel(engine, resolveTo(SOMEWHERE, "Home", isDefault = false), addReminder)

            viewModel.setReminder(ReminderTarget.Muhurta("Abhijit Muhurta"))

            coVerify { addReminder.addMuhurta("Abhijit Muhurta", SOMEWHERE) }
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

    private companion object {
        val SOMEWHERE = GeoCoordinates(latitude = 28.6139, longitude = 77.2090)
    }
}

private class FakeEngine(
    private val snapshotResult: AppResult<AstronomySnapshot>,
    private val festivals: List<Festival> = emptyList(),
) : AstronomyEngine {
    var lastLocation: GeoCoordinates? = null

    override suspend fun snapshotAt(
        instant: Instant,
        location: GeoCoordinates,
    ): AppResult<AstronomySnapshot> {
        lastLocation = location
        return snapshotResult
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

    override suspend fun upcomingFestivals(
        instant: Instant,
        location: GeoCoordinates,
        withinDays: Int,
        limit: Int,
    ): AppResult<List<Festival>> = AppResult.Success(festivals)
}

private fun snapshot(muhurtas: List<Muhurta> = emptyList()): AstronomySnapshot =
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
        muhurtas = muhurtas,
    )
