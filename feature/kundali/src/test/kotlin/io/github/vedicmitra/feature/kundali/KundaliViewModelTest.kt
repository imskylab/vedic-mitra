/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.kundali

import com.google.common.truth.Truth.assertThat
import io.github.vedicmitra.core.astronomy.AstronomyEngine
import io.github.vedicmitra.core.astronomy.AstronomySnapshot
import io.github.vedicmitra.core.astronomy.Festival
import io.github.vedicmitra.core.astronomy.Lagna
import io.github.vedicmitra.core.astronomy.Nakshatra
import io.github.vedicmitra.core.astronomy.NatalChart
import io.github.vedicmitra.core.astronomy.PanchangaDaySummary
import io.github.vedicmitra.core.astronomy.Rasi
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.datastore.BirthProfile
import io.github.vedicmitra.core.datastore.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
class KundaliViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `shows the needs-profile state when there is no primary profile`() =
        runTest {
            val viewModel = KundaliViewModel(FakeProfileRepository(), FakeAstronomyEngine(sampleChart()))

            viewModel.load()

            assertThat(viewModel.uiState.value).isEqualTo(KundaliUiState.NeedsProfile)
        }

    @Test
    fun `shows needs-profile when the primary is not geocoded`() =
        runTest {
            val incomplete =
                BirthProfile(
                    id = "a",
                    name = "Leo",
                    dateOfBirth = LocalDate.of(1995, 3, 14),
                    timeOfBirth = LocalTime.of(9, 30),
                    placeOfBirth = "Hyderabad",
                    // no coordinates / zone
                )
            val viewModel =
                KundaliViewModel(FakeProfileRepository(listOf(incomplete), "a"), FakeAstronomyEngine(sampleChart()))

            viewModel.load()

            assertThat(viewModel.uiState.value).isEqualTo(KundaliUiState.NeedsProfile)
        }

    @Test
    fun `computes the chart for a chart-ready primary profile`() =
        runTest {
            val primary =
                BirthProfile(
                    id = "a",
                    name = "Leo",
                    dateOfBirth = LocalDate.of(1995, 3, 14),
                    timeOfBirth = LocalTime.of(9, 30),
                    placeOfBirth = "Hyderabad, India",
                    birthCoordinates = GeoCoordinates(17.385, 78.4867),
                    birthZoneId = "Asia/Kolkata",
                )
            val viewModel =
                KundaliViewModel(FakeProfileRepository(listOf(primary), "a"), FakeAstronomyEngine(sampleChart()))

            viewModel.load()

            val state = viewModel.uiState.value
            assertThat(state).isInstanceOf(KundaliUiState.Ready::class.java)
            assertThat((state as KundaliUiState.Ready).name).isEqualTo("Leo")
        }
}

private fun sampleChart(): NatalChart =
    NatalChart(
        lagna = Lagna(siderealLongitude = 0.0, rasi = Rasi(0, "Mesha")),
        houses = emptyList(),
        grahas = emptyList(),
        moonNakshatra = Nakshatra(number = 1, name = "Ashwini"),
        moonPada = 1,
        vimshottari = emptyList(),
    )

private class FakeProfileRepository(
    initial: List<BirthProfile> = emptyList(),
    primary: String? = null,
) : ProfileRepository {
    private val profileState = MutableStateFlow(initial)
    private val primaryState = MutableStateFlow(primary)
    override val profiles: Flow<List<BirthProfile>> = profileState.asStateFlow()
    override val primaryProfileId: Flow<String?> = primaryState.asStateFlow()

    override suspend fun upsert(profile: BirthProfile) {
        profileState.value = profileState.value.filterNot { it.id == profile.id } + profile
    }

    override suspend fun remove(id: String) {
        profileState.value = profileState.value.filterNot { it.id == id }
    }

    override suspend fun setPrimary(id: String) {
        primaryState.value = id
    }
}

private class FakeAstronomyEngine(
    private val chart: NatalChart?,
) : AstronomyEngine {
    override suspend fun snapshotAt(
        instant: Instant,
        location: GeoCoordinates,
    ): AppResult<AstronomySnapshot> = AppResult.Failure(UnsupportedOperationException())

    override suspend fun daySummaryAt(
        instant: Instant,
        location: GeoCoordinates,
    ): AppResult<PanchangaDaySummary> = AppResult.Failure(UnsupportedOperationException())

    override suspend fun upcomingFestivals(
        instant: Instant,
        location: GeoCoordinates,
        withinDays: Int,
        limit: Int,
    ): AppResult<List<Festival>> = AppResult.Success(emptyList())

    override suspend fun natalChartAt(
        instant: Instant,
        location: GeoCoordinates,
    ): AppResult<NatalChart?> = AppResult.Success(chart)
}
