/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.matchmaking

import com.google.common.truth.Truth.assertThat
import io.github.vedicmitra.core.astronomy.AstronomyEngine
import io.github.vedicmitra.core.astronomy.Graha
import io.github.vedicmitra.core.astronomy.GunaMilanVerdict
import io.github.vedicmitra.core.astronomy.Lagna
import io.github.vedicmitra.core.astronomy.Nakshatra
import io.github.vedicmitra.core.astronomy.NatalChart
import io.github.vedicmitra.core.astronomy.NatalGraha
import io.github.vedicmitra.core.astronomy.Rasi
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.datastore.BirthProfile
import io.github.vedicmitra.core.datastore.Gender
import io.github.vedicmitra.core.datastore.ProfileRepository
import io.mockk.coEvery
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

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("MagicNumber")
class MatchmakingViewModelTest {
    private val astronomyEngine = mockk<AstronomyEngine>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `scores the default male-female pairing`() =
        runTest {
            val groom = profile("g", "Ravi", Gender.MALE, GeoCoordinates(11.0, 11.0))
            val bride = profile("b", "Sita", Gender.FEMALE, GeoCoordinates(22.0, 22.0))
            // Pushya (8) in Karka (3) with Hasta (13) in Kanya (5) — the good, dosha-free pair (26.5).
            coEvery { astronomyEngine.natalChartAt(any(), GeoCoordinates(11.0, 11.0)) } returns
                AppResult.Success(chart(nakshatra = 8, moonRasiIndex = 3))
            coEvery { astronomyEngine.natalChartAt(any(), GeoCoordinates(22.0, 22.0)) } returns
                AppResult.Success(chart(nakshatra = 13, moonRasiIndex = 5, marsRasiIndex = 2))

            val viewModel = MatchmakingViewModel(repository(listOf(groom, bride)), astronomyEngine)
            viewModel.load()

            val state = viewModel.uiState.value as MatchmakingUiState.Ready
            assertThat(state.males.map { it.id }).containsExactly("g")
            assertThat(state.females.map { it.id }).containsExactly("b")
            assertThat(state.selectedGroomId).isEqualTo("g")
            assertThat(state.selectedBrideId).isEqualTo("b")
            assertThat(state.result?.total).isEqualTo(26.5)
            assertThat(state.result?.verdict).isEqualTo(GunaMilanVerdict.GOOD)
            assertThat(state.result?.doshas).isEmpty()
            // Mars is in the 6th from this groom's lagna and the 3rd from his Moon, and in the 3rd
            // and 10th for the bride, so neither carries Mangal dosha.
            assertThat(state.mangal?.standing).isFalse()
            assertThat(state.mangal?.groom?.afflicted).isFalse()
            assertThat(state.mangal?.bride?.afflicted).isFalse()
        }

    @Test
    fun `when both charts carry Mangal dosha it cancels between them`() =
        runTest {
            val groom = profile("g", "Ravi", Gender.MALE, GeoCoordinates(11.0, 11.0))
            val bride = profile("b", "Sita", Gender.FEMALE, GeoCoordinates(22.0, 22.0))
            // Mars in Karka is the 4th from a Mesha lagna, and nothing in these charts answers it:
            // Karka is neither Mars's own sign nor one where it is held harmless, and there is no
            // Jupiter to aspect it.
            coEvery { astronomyEngine.natalChartAt(any(), GeoCoordinates(11.0, 11.0)) } returns
                AppResult.Success(chart(nakshatra = 8, moonRasiIndex = 3, marsRasiIndex = 3))
            coEvery { astronomyEngine.natalChartAt(any(), GeoCoordinates(22.0, 22.0)) } returns
                AppResult.Success(chart(nakshatra = 13, moonRasiIndex = 5, marsRasiIndex = 3))

            val viewModel = MatchmakingViewModel(repository(listOf(groom, bride)), astronomyEngine)
            viewModel.load()

            val state = viewModel.uiState.value as MatchmakingUiState.Ready
            assertThat(state.mangal?.groom?.present).isTrue()
            assertThat(state.mangal?.bride?.present).isTrue()
            assertThat(state.mangal?.mutuallyCancelled).isTrue()
            assertThat(state.mangal?.standing).isFalse()
        }

    @Test
    fun `one afflicted chart against a clear one leaves the dosha standing`() =
        runTest {
            val groom = profile("g", "Ravi", Gender.MALE, GeoCoordinates(11.0, 11.0))
            val bride = profile("b", "Sita", Gender.FEMALE, GeoCoordinates(22.0, 22.0))
            coEvery { astronomyEngine.natalChartAt(any(), GeoCoordinates(11.0, 11.0)) } returns
                AppResult.Success(chart(nakshatra = 8, moonRasiIndex = 3, marsRasiIndex = 3))
            coEvery { astronomyEngine.natalChartAt(any(), GeoCoordinates(22.0, 22.0)) } returns
                AppResult.Success(chart(nakshatra = 13, moonRasiIndex = 5, marsRasiIndex = 2))

            val viewModel = MatchmakingViewModel(repository(listOf(groom, bride)), astronomyEngine)
            viewModel.load()

            val state = viewModel.uiState.value as MatchmakingUiState.Ready
            assertThat(state.mangal?.groom?.present).isTrue()
            assertThat(state.mangal?.bride?.present).isFalse()
            assertThat(state.mangal?.mutuallyCancelled).isFalse()
            assertThat(state.mangal?.standing).isTrue()
        }

    @Test
    fun `without both genders there is no pairing to score`() =
        runTest {
            val groom = profile("g", "Ravi", Gender.MALE, GeoCoordinates(11.0, 11.0))
            coEvery { astronomyEngine.natalChartAt(any(), any()) } returns
                AppResult.Success(chart(nakshatra = 8, moonRasiIndex = 3))

            val viewModel = MatchmakingViewModel(repository(listOf(groom)), astronomyEngine)
            viewModel.load()

            val state = viewModel.uiState.value as MatchmakingUiState.Ready
            assertThat(state.males).hasSize(1)
            assertThat(state.females).isEmpty()
            assertThat(state.result).isNull()
        }

    private fun repository(profiles: List<BirthProfile>): ProfileRepository =
        mockk {
            every { this@mockk.profiles } returns flowOf(profiles)
            every { primaryProfileId } returns flowOf(null)
        }

    private fun profile(
        id: String,
        name: String,
        gender: Gender,
        coordinates: GeoCoordinates,
    ): BirthProfile =
        BirthProfile(
            id = id,
            name = name,
            gender = gender,
            dateOfBirth = LocalDate.of(1995, 3, 14),
            timeOfBirth = LocalTime.of(9, 30),
            placeOfBirth = "Somewhere",
            birthCoordinates = coordinates,
            birthZoneId = "Asia/Kolkata",
        )

    /**
     * A chart carrying only what a match reads: the Moon for Ashtakoota, and Mars and Venus for
     * Mangal dosha. The lagna is always Mesha, so a Mars sign is also its house from the lagna.
     */
    private fun chart(
        nakshatra: Int,
        moonRasiIndex: Int,
        marsRasiIndex: Int = 5,
        venusRasiIndex: Int = 0,
    ): NatalChart =
        NatalChart(
            lagna = Lagna(siderealLongitude = 0.0, rasi = Rasi(index = 0, name = "Mesha")),
            houses = emptyList(),
            moonHouses = emptyList(),
            grahas =
                listOf(
                    Graha.MOON to moonRasiIndex,
                    Graha.MANGALA to marsRasiIndex,
                    Graha.SHUKRA to venusRasiIndex,
                ).map { (graha, rasiIndex) ->
                    NatalGraha(
                        graha = graha,
                        siderealLongitude = rasiIndex * 30.0,
                        rasi = Rasi(index = rasiIndex, name = "Sign"),
                        house = 1,
                        houseFromMoon = 1,
                        retrograde = false,
                    )
                },
            moonNakshatra = Nakshatra(number = nakshatra, name = "Nakshatra"),
            moonPada = 1,
            vimshottari = emptyList(),
        )
}
