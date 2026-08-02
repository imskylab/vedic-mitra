/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.alarm

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.vedicmitra.core.astronomy.AstronomyEngine
import io.github.vedicmitra.core.astronomy.AstronomySnapshot
import io.github.vedicmitra.core.astronomy.Karana
import io.github.vedicmitra.core.astronomy.Muhurta
import io.github.vedicmitra.core.astronomy.MuhurtaQuality
import io.github.vedicmitra.core.astronomy.Nakshatra
import io.github.vedicmitra.core.astronomy.Paksha
import io.github.vedicmitra.core.astronomy.SunTimes
import io.github.vedicmitra.core.astronomy.Tithi
import io.github.vedicmitra.core.astronomy.Vara
import io.github.vedicmitra.core.astronomy.Yoga
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.datastore.PersistedReminder
import io.github.vedicmitra.core.datastore.ReminderRepository
import io.github.vedicmitra.core.location.LocationProvider
import io.github.vedicmitra.core.scheduler.ScheduledTask
import io.github.vedicmitra.core.scheduler.TaskScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class AlarmViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load derives togglable reminders from the day's muhurtas`() =
        runTest {
            val viewModel = viewModel()

            viewModel.uiState.test {
                assertThat(awaitItem()).isEqualTo(AlarmUiState.Loading)

                viewModel.load()

                val ready = awaitItem() as AlarmUiState.Ready
                assertThat(ready.reminders.map { it.name })
                    .containsExactly("Abhijit Muhurta", "Rahu Kalam")
                    .inOrder()
                assertThat(ready.reminders.first { it.name == "Abhijit Muhurta" }.isPast).isFalse()
                assertThat(ready.reminders.first { it.name == "Rahu Kalam" }.isPast).isTrue()
            }
        }

    @Test
    fun `enabling a reminder schedules a lead-adjusted alarm and persists it`() =
        runTest {
            val scheduler = FakeTaskScheduler()
            val repository = FakeReminderRepository() // default lead time = 10 minutes
            val viewModel = viewModel(scheduler = scheduler, repository = repository)

            viewModel.uiState.test {
                awaitItem() // Loading
                viewModel.load()
                val future = (awaitItem() as AlarmUiState.Ready).reminders.first { !it.isPast }

                viewModel.setReminder(future, enabled = true)

                val updated = awaitItem() as AlarmUiState.Ready
                assertThat(updated.reminders.first { it.id == future.id }.isEnabled).isTrue()

                // Fires 10 minutes (600_000 ms) before the window start.
                val expectedTrigger = future.start.toEpochMilliseconds() - 600_000L
                val scheduled = scheduler.scheduled.single()
                assertThat(scheduled.id).isEqualTo(future.id)
                assertThat(scheduled.triggerAt.toEpochMilliseconds()).isEqualTo(expectedTrigger)

                val persisted = repository.reminders.value.single()
                assertThat(persisted.id).isEqualTo(future.id)
                assertThat(persisted.triggerAtEpochMillis).isEqualTo(expectedTrigger)
                assertThat(persisted.title).isEqualTo(future.name)
            }
        }

    @Test
    fun `disabling a reminder cancels the alarm and forgets it`() =
        runTest {
            val scheduler = FakeTaskScheduler()
            val repository = FakeReminderRepository()
            val viewModel = viewModel(scheduler = scheduler, repository = repository)

            viewModel.uiState.test {
                awaitItem() // Loading
                viewModel.load()
                val future = (awaitItem() as AlarmUiState.Ready).reminders.first { !it.isPast }
                viewModel.setReminder(future, enabled = true)
                awaitItem() // enabled

                viewModel.setReminder(future.copy(isEnabled = true), enabled = false)

                val cleared = awaitItem() as AlarmUiState.Ready
                assertThat(cleared.reminders.first { it.id == future.id }.isEnabled).isFalse()
                assertThat(scheduler.cancelled).containsExactly(future.id)
                assertThat(repository.reminders.value).isEmpty()
            }
        }

    @Test
    fun `enabling a past reminder is ignored`() =
        runTest {
            val scheduler = FakeTaskScheduler()
            val viewModel = viewModel(scheduler = scheduler)

            viewModel.uiState.test {
                awaitItem() // Loading
                viewModel.load()
                val past = (awaitItem() as AlarmUiState.Ready).reminders.first { it.isPast }

                viewModel.setReminder(past, enabled = true)

                assertThat(scheduler.scheduled).isEmpty()
                expectNoEvents()
            }
        }

    @Test
    fun `setLeadTime updates the exposed preference`() =
        runTest {
            val repository = FakeReminderRepository()
            val viewModel = viewModel(repository = repository)

            viewModel.uiState.test {
                awaitItem() // Loading
                viewModel.load()
                assertThat((awaitItem() as AlarmUiState.Ready).leadTimeMinutes).isEqualTo(10)

                viewModel.setLeadTime(30)

                assertThat((awaitItem() as AlarmUiState.Ready).leadTimeMinutes).isEqualTo(30)
                assertThat(repository.leadTimeMinutes.value).isEqualTo(30)
            }
        }

    private fun viewModel(
        scheduler: TaskScheduler = FakeTaskScheduler(),
        repository: ReminderRepository = FakeReminderRepository(),
    ): AlarmViewModel =
        AlarmViewModel(
            astronomyEngine = FakeAstronomyEngine(AppResult.Success(SAMPLE)),
            locationProvider = FakeLocationProvider(AppResult.Success(BENGALURU)),
            taskScheduler = scheduler,
            reminderRepository = repository,
        )
}

private class FakeAstronomyEngine(
    private val result: AppResult<AstronomySnapshot>,
) : AstronomyEngine {
    override suspend fun snapshotAt(
        instant: Instant,
        location: GeoCoordinates,
    ): AppResult<AstronomySnapshot> = result
}

private class FakeLocationProvider(
    private val result: AppResult<GeoCoordinates>,
) : LocationProvider {
    override suspend fun currentLocation(): AppResult<GeoCoordinates> = result

    override fun locationUpdates(): Flow<GeoCoordinates> = emptyFlow()
}

private class FakeTaskScheduler : TaskScheduler {
    val scheduled = mutableListOf<ScheduledTask>()
    val cancelled = mutableListOf<String>()

    override suspend fun schedule(request: ScheduledTask): AppResult<Unit> {
        scheduled += request
        return AppResult.Success(Unit)
    }

    override suspend fun cancel(id: String): AppResult<Unit> {
        cancelled += id
        return AppResult.Success(Unit)
    }

    override fun canScheduleExactAlarms(): Boolean = true
}

private class FakeReminderRepository : ReminderRepository {
    override val reminders = MutableStateFlow<List<PersistedReminder>>(emptyList())
    override val leadTimeMinutes = MutableStateFlow(10)

    override suspend fun upsert(reminder: PersistedReminder) {
        reminders.value = reminders.value.filterNot { it.id == reminder.id } + reminder
    }

    override suspend fun remove(id: String) {
        reminders.value = reminders.value.filterNot { it.id == id }
    }

    override suspend fun removePast(nowEpochMillis: Long) {
        reminders.value = reminders.value.filter { it.triggerAtEpochMillis > nowEpochMillis }
    }

    override suspend fun setLeadTimeMinutes(minutes: Int) {
        leadTimeMinutes.value = minutes
    }
}

private val BENGALURU = GeoCoordinates(latitude = 12.9716, longitude = 77.5946)

// A future window (year 2100) and a past window (epoch) so isPast is deterministic regardless of the
// wall clock at test time.
private val SAMPLE =
    AstronomySnapshot(
        instant = Instant.fromEpochMilliseconds(0L),
        location = BENGALURU,
        sunTimes = SunTimes(sunrise = null, sunset = null),
        tithi = Tithi(number = 5, paksha = Paksha.SHUKLA, name = "Panchami"),
        nakshatra = Nakshatra(number = 25, name = "Purva Bhadrapada"),
        yoga = Yoga(number = 18, name = "Variyana"),
        karana = Karana(number = 10, name = "Balava"),
        vara = Vara.SOMAVARA,
        muhurtas =
            listOf(
                Muhurta(
                    name = "Abhijit Muhurta",
                    start = Instant.fromEpochMilliseconds(4_102_444_800_000L),
                    end = Instant.fromEpochMilliseconds(4_102_448_400_000L),
                    quality = MuhurtaQuality.AUSPICIOUS,
                ),
                Muhurta(
                    name = "Rahu Kalam",
                    start = Instant.fromEpochMilliseconds(0L),
                    end = Instant.fromEpochMilliseconds(3_600_000L),
                    quality = MuhurtaQuality.INAUSPICIOUS,
                ),
            ),
    )
