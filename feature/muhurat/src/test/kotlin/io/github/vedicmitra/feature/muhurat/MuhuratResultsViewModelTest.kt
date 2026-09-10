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
import io.github.vedicmitra.core.astronomy.Graha
import io.github.vedicmitra.core.astronomy.Lagna
import io.github.vedicmitra.core.astronomy.MuhurtaActivity
import io.github.vedicmitra.core.astronomy.MuhurtaRating
import io.github.vedicmitra.core.astronomy.Nakshatra
import io.github.vedicmitra.core.astronomy.NatalChart
import io.github.vedicmitra.core.astronomy.NatalGraha
import io.github.vedicmitra.core.astronomy.RankedMuhurtaDay
import io.github.vedicmitra.core.astronomy.Rasi
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.datastore.BirthProfile
import io.github.vedicmitra.core.datastore.Gender
import io.github.vedicmitra.core.datastore.ProfileRepository
import io.github.vedicmitra.core.domain.ResolveLocationUseCase
import io.github.vedicmitra.core.domain.ResolvedLocation
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("MagicNumber")
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
            stubLocation(isDefault = true)
            val ranked =
                listOf(
                    RankedMuhurtaDay(
                        atSunrise = Instant.fromEpochMilliseconds(1_705_320_000_000L),
                        score = DayMuhurtaScore(score = 92, rating = MuhurtaRating.EXCELLENT, reasons = emptyList()),
                    ),
                )
            coEvery { astronomyEngine.bestMuhurtasFor(any(), any(), any(), any(), any()) } returns
                AppResult.Success(ranked)

            val viewModel = viewModel(activityName = "MUNDAN", profiles = noProfiles())
            viewModel.load()

            val state = viewModel.uiState.value as MuhuratResultsUiState.Ready
            assertThat(state.activity).isEqualTo(MuhurtaActivity.MUNDAN)
            assertThat(state.days).isEqualTo(ranked)
            assertThat(state.usingDefaultLocation).isTrue()
            assertThat(state.profiles).isEmpty()
        }

    @Test
    fun `falls back to a default activity when the saved name is missing or invalid`() =
        runTest {
            stubLocation(isDefault = false)
            coEvery { astronomyEngine.bestMuhurtasFor(any(), any(), any(), any(), any()) } returns
                AppResult.Success(emptyList())

            val viewModel = viewModel(activityName = "NOT_AN_ACTIVITY", profiles = noProfiles())
            viewModel.load()

            val state = viewModel.uiState.value as MuhuratResultsUiState.Ready
            assertThat(state.activity).isEqualTo(MuhurtaActivity.GRIHA_PRAVESH)
            assertThat(state.days).isEmpty()
        }

    @Test
    fun `setWindow reloads over the chosen number of days`() =
        runTest {
            stubLocation(isDefault = false)
            coEvery { astronomyEngine.bestMuhurtasFor(any(), any(), any(), any(), any()) } returns
                AppResult.Success(emptyList())

            val viewModel = viewModel(activityName = "MUNDAN", profiles = noProfiles())
            viewModel.load()
            viewModel.setWindow(90)

            val state = viewModel.uiState.value as MuhuratResultsUiState.Ready
            assertThat(state.windowDays).isEqualTo(90)
            coVerify { astronomyEngine.bestMuhurtasFor(MuhurtaActivity.MUNDAN, any(), 90, any(), any()) }
        }

    @Test
    fun `personalises the ranking for the primary profile by default`() =
        runTest {
            stubLocation(isDefault = false)
            coEvery { astronomyEngine.natalChartAt(any(), any()) } returns AppResult.Success(sampleChart())
            coEvery { astronomyEngine.bestMuhurtasFor(any(), any(), any(), any(), any()) } returns
                AppResult.Success(emptyList())

            val viewModel = viewModel(activityName = "MUNDAN", profiles = oneChartReadyPrimary())
            viewModel.load()

            val state = viewModel.uiState.value as MuhuratResultsUiState.Ready
            assertThat(state.profiles.map { it.id }).containsExactly("p1")
            assertThat(state.selectedProfileId).isEqualTo("p1")
            assertThat(state.couple).isNull()
            // The chart's birth star (5) and Moon sign (3) become the ranking's personalisation key.
            coVerify {
                astronomyEngine.bestMuhurtasFor(
                    any(),
                    any(),
                    any(),
                    any(),
                    match { people ->
                        people.singleOrNull()?.context?.birthNakshatraNumber == 5 &&
                            people.singleOrNull()?.context?.birthMoonRasiIndex == 3
                    },
                )
            }
        }

    @Test
    fun `selecting General drops the personalisation`() =
        runTest {
            stubLocation(isDefault = false)
            coEvery { astronomyEngine.natalChartAt(any(), any()) } returns AppResult.Success(sampleChart())
            coEvery { astronomyEngine.bestMuhurtasFor(any(), any(), any(), any(), any()) } returns
                AppResult.Success(emptyList())

            val viewModel = viewModel(activityName = "MUNDAN", profiles = oneChartReadyPrimary())
            viewModel.load()
            viewModel.selectProfile(null)

            val state = viewModel.uiState.value as MuhuratResultsUiState.Ready
            assertThat(state.selectedProfileId).isNull()
        }

    @Test
    fun `a couple activity asks for two people and ranks generally until it has both`() =
        runTest {
            stubLocation(isDefault = false)
            coEvery { astronomyEngine.natalChartAt(any(), any()) } returns AppResult.Success(sampleChart())
            coEvery { astronomyEngine.bestMuhurtasFor(any(), any(), any(), any(), any()) } returns
                AppResult.Success(emptyList())

            val viewModel = viewModel(activityName = "VIVAH", profiles = aCouple())
            viewModel.load()

            val state = viewModel.uiState.value as MuhuratResultsUiState.Ready
            val couple = checkNotNull(state.couple)
            assertThat(couple.grooms.map { it.id }).containsExactly("groom")
            assertThat(couple.brides.map { it.id }).containsExactly("bride")
            // Nobody is guessed into a role, so nothing is personalised yet.
            assertThat(state.personIds).isEmpty()
            assertThat(state.selectedProfileId).isNull()
        }

    @Test
    fun `a couple activity personalises for both once both are chosen`() =
        runTest {
            stubLocation(isDefault = false)
            coEvery { astronomyEngine.natalChartAt(any(), any()) } returns AppResult.Success(sampleChart())
            coEvery { astronomyEngine.bestMuhurtasFor(any(), any(), any(), any(), any()) } returns
                AppResult.Success(emptyList())

            val viewModel = viewModel(activityName = "VIVAH", profiles = aCouple())
            viewModel.load()
            viewModel.selectGroom("groom")
            viewModel.selectBride("bride")

            val state = viewModel.uiState.value as MuhuratResultsUiState.Ready
            assertThat(state.personIds).containsExactly("groom", "bride").inOrder()
            // The names travel with the state so a reason can be attributed without the engine
            // ever holding one.
            assertThat(state.personNames).containsEntry("bride", "Meera")
            coVerify { astronomyEngine.bestMuhurtasFor(any(), any(), any(), any(), match { it.size == 2 }) }
        }

    @Test
    fun `a profile with no gender is counted rather than silently dropped`() =
        runTest {
            stubLocation(isDefault = false)
            coEvery { astronomyEngine.natalChartAt(any(), any()) } returns AppResult.Success(sampleChart())
            coEvery { astronomyEngine.bestMuhurtasFor(any(), any(), any(), any(), any()) } returns
                AppResult.Success(emptyList())

            val viewModel = viewModel(activityName = "VIVAH", profiles = aCoupleAndAGenderlessProfile())
            viewModel.load()

            val couple = checkNotNull((viewModel.uiState.value as MuhuratResultsUiState.Ready).couple)
            assertThat(couple.withoutGender).isEqualTo(1)
        }

    private fun stubLocation(isDefault: Boolean) {
        coEvery { resolveLocation() } returns
            ResolvedLocation(
                coordinates = GeoCoordinates(latitude = 28.6139, longitude = 77.2090),
                zoneId = "Asia/Kolkata",
                label = "New Delhi",
                isDefault = isDefault,
            )
    }

    private fun viewModel(
        activityName: String,
        profiles: ProfileRepository,
    ) = MuhuratResultsViewModel(
        astronomyEngine = astronomyEngine,
        resolveLocation = resolveLocation,
        profileRepository = profiles,
        savedStateHandle = SavedStateHandle(mapOf(MUHURAT_ACTIVITY_ARG to activityName)),
    )
}

private fun noProfiles(): ProfileRepository =
    mockk {
        every { profiles } returns flowOf(emptyList())
        every { primaryProfileId } returns flowOf(null)
    }

private fun oneChartReadyPrimary(): ProfileRepository {
    val profile =
        BirthProfile(
            id = "p1",
            name = "Leo",
            dateOfBirth = LocalDate.of(1995, 3, 14),
            timeOfBirth = LocalTime.of(9, 30),
            placeOfBirth = "Hyderabad, India",
            birthCoordinates = GeoCoordinates(latitude = 17.385, longitude = 78.4867),
            birthZoneId = "Asia/Kolkata",
        )
    return mockk {
        every { profiles } returns flowOf(listOf(profile))
        every { primaryProfileId } returns flowOf("p1")
    }
}

private fun chartReady(
    id: String,
    name: String,
    gender: Gender?,
): BirthProfile =
    BirthProfile(
        id = id,
        name = name,
        gender = gender,
        dateOfBirth = LocalDate.of(1995, 3, 14),
        timeOfBirth = LocalTime.of(9, 30),
        placeOfBirth = "Hyderabad, India",
        birthCoordinates = GeoCoordinates(latitude = 17.385, longitude = 78.4867),
        birthZoneId = "Asia/Kolkata",
    )

private fun repositoryOf(profiles: List<BirthProfile>): ProfileRepository =
    mockk {
        every { this@mockk.profiles } returns flowOf(profiles)
        every { primaryProfileId } returns flowOf(profiles.firstOrNull()?.id)
    }

private fun aCouple(): ProfileRepository =
    repositoryOf(
        listOf(
            chartReady("groom", "Ravi", Gender.MALE),
            chartReady("bride", "Meera", Gender.FEMALE),
        ),
    )

private fun aCoupleAndAGenderlessProfile(): ProfileRepository =
    repositoryOf(
        listOf(
            chartReady("groom", "Ravi", Gender.MALE),
            chartReady("bride", "Meera", Gender.FEMALE),
            chartReady("nobody", "Anil", gender = null),
        ),
    )

@Suppress("MagicNumber")
private fun sampleChart(): NatalChart =
    NatalChart(
        lagna = Lagna(siderealLongitude = 0.0, rasi = Rasi(index = 0, name = "Mesha")),
        houses = emptyList(),
        moonHouses = emptyList(),
        grahas =
            listOf(
                NatalGraha(
                    graha = Graha.MOON,
                    siderealLongitude = 90.0,
                    rasi = Rasi(index = 3, name = "Karka"),
                    house = 4,
                    houseFromMoon = 1,
                    retrograde = false,
                ),
            ),
        moonNakshatra = Nakshatra(number = 5, name = "Mrigashira"),
        moonPada = 1,
        vimshottari = emptyList(),
    )
