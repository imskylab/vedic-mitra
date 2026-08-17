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
import io.github.vedicmitra.core.astronomy.AstronomySnapshot
import io.github.vedicmitra.core.astronomy.Ayana
import io.github.vedicmitra.core.astronomy.GoldenHour
import io.github.vedicmitra.core.astronomy.Karana
import io.github.vedicmitra.core.astronomy.Maasa
import io.github.vedicmitra.core.astronomy.MoonPhase
import io.github.vedicmitra.core.astronomy.MoonTimes
import io.github.vedicmitra.core.astronomy.Muhurta
import io.github.vedicmitra.core.astronomy.MuhurtaQuality
import io.github.vedicmitra.core.astronomy.Nakshatra
import io.github.vedicmitra.core.astronomy.Paksha
import io.github.vedicmitra.core.astronomy.Ritu
import io.github.vedicmitra.core.astronomy.Samvatsara
import io.github.vedicmitra.core.astronomy.SunTimes
import io.github.vedicmitra.core.astronomy.Tithi
import io.github.vedicmitra.core.astronomy.Vara
import io.github.vedicmitra.core.astronomy.Yoga
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.datastore.PersistedReminder
import io.github.vedicmitra.core.datastore.ReminderRepository
import io.github.vedicmitra.core.domain.ResolveLocationUseCase
import io.github.vedicmitra.core.domain.ResolvedLocation
import io.github.vedicmitra.core.scheduler.ScheduledTask
import io.github.vedicmitra.core.scheduler.TaskScheduler
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
class MuhuratDayViewModelTest {
    private val astronomyEngine = mockk<AstronomyEngine>()
    private val resolveLocation = mockk<ResolveLocationUseCase>()
    private val taskScheduler = mockk<TaskScheduler>(relaxed = true)
    private val reminderRepository = mockk<ReminderRepository>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `splits the day's windows into auspicious and inauspicious in time order`() =
        runTest {
            coEvery { resolveLocation() } returns
                ResolvedLocation(
                    coordinates = GeoCoordinates(latitude = 28.6139, longitude = 77.2090),
                    zoneId = "Asia/Kolkata",
                    label = "New Delhi",
                    isDefault = false,
                )
            val snapshot =
                daySnapshot(
                    muhurtas =
                        listOf(
                            muhurta("Rahu Kalam", 3_000L, 4_000L, MuhurtaQuality.INAUSPICIOUS),
                            muhurta("Abhijit Muhurta", 1_000L, 2_000L, MuhurtaQuality.AUSPICIOUS),
                        ),
                )
            coEvery { astronomyEngine.snapshotAt(any(), any()) } returns AppResult.Success(snapshot)

            val viewModel = dayViewModel(activityName = "VIVAH", dayMillis = 1_705_320_000_000L)
            viewModel.load()

            val state = viewModel.uiState.value as MuhuratDayUiState.Ready
            assertThat(state.activityLabel).isEqualTo("Vivah")
            assertThat(state.auspicious.map { it.label }).containsExactly("Abhijit Muhurta")
            assertThat(state.inauspicious.map { it.label }).containsExactly("Rahu Kalam")
            assertThat(state.summary).contains("Rohini")
        }

    @Test
    fun `setReminder schedules and persists a one-shot for a future day`() =
        runTest {
            coEvery { taskScheduler.schedule(any()) } returns AppResult.Success(Unit)
            val futureDay = System.currentTimeMillis() + WEEK_MILLIS

            val viewModel = dayViewModel(activityName = "VIVAH", dayMillis = futureDay)
            viewModel.setReminder()

            coVerify {
                taskScheduler.schedule(
                    match<ScheduledTask> {
                        it.id == "muhurat:$futureDay" && it.triggerAt == Instant.fromEpochMilliseconds(futureDay)
                    },
                )
            }
            coVerify { reminderRepository.upsert(match<PersistedReminder> { it.id == "muhurat:$futureDay" }) }
        }

    @Test
    fun `setReminder does not schedule a day that has already begun`() =
        runTest {
            val viewModel = dayViewModel(activityName = "VIVAH", dayMillis = 0L)
            viewModel.setReminder()

            coVerify(exactly = 0) { taskScheduler.schedule(any()) }
        }

    private fun dayViewModel(
        activityName: String,
        dayMillis: Long,
    ): MuhuratDayViewModel =
        MuhuratDayViewModel(
            astronomyEngine = astronomyEngine,
            resolveLocation = resolveLocation,
            taskScheduler = taskScheduler,
            reminderRepository = reminderRepository,
            savedStateHandle =
                SavedStateHandle(mapOf(MUHURAT_ACTIVITY_ARG to activityName, MUHURAT_DAY_ARG to dayMillis)),
        )
}

private const val WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000

private fun muhurta(
    name: String,
    startMillis: Long,
    endMillis: Long,
    quality: MuhurtaQuality,
): Muhurta =
    Muhurta(
        name = name,
        start = Instant.fromEpochMilliseconds(startMillis),
        end = Instant.fromEpochMilliseconds(endMillis),
        quality = quality,
    )

private fun daySnapshot(muhurtas: List<Muhurta>): AstronomySnapshot =
    AstronomySnapshot(
        instant = Instant.fromEpochMilliseconds(0L),
        location = GeoCoordinates(latitude = 28.6139, longitude = 77.2090),
        sunTimes = SunTimes(sunrise = null, sunset = null),
        moonTimes = MoonTimes(moonrise = null, moonset = null),
        tithi = Tithi(number = 5, paksha = Paksha.SHUKLA, name = "Panchami"),
        nakshatra = Nakshatra(number = 4, name = "Rohini"),
        yoga = Yoga(number = 1, name = "Vishkambha"),
        karana = Karana(number = 2, name = "Bava"),
        vara = Vara.GURUVARA,
        maasa = Maasa(number = 1, name = "Chaitra", adhika = false),
        samvatsara = Samvatsara(number = 1, name = "Prabhava", shakaYear = 1948),
        ayana = Ayana.UTTARAYANA,
        ritu = Ritu.SHISHIRA,
        moonPhase = MoonPhase.WAXING_GIBBOUS,
        goldenHour = GoldenHour(morningStart = null, morningEnd = null, eveningStart = null, eveningEnd = null),
        muhurtas = muhurtas,
    )
