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
import io.github.vedicmitra.core.astronomy.PanchangaDaySummary
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
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

private const val MILLIS_PER_MINUTE = 60_000L

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
    fun `load resolves an upcoming, never-past window for every muhurta`() =
        runTest {
            val viewModel = viewModel()

            viewModel.uiState.test {
                assertThat(awaitItem()).isEqualTo(AlarmUiState.Loading)
                viewModel.load()

                val ready = awaitItem() as AlarmUiState.Ready
                assertThat(ready.reminders.map { it.name })
                    .containsExactly("Abhijit Muhurta", "Rahu Kalam")
                    .inOrder()
                // Abhijit is still ahead today; Rahu has passed today so it rolls to tomorrow.
                assertThat(ready.reminders.first { it.name == "Abhijit Muhurta" }.isTomorrow).isFalse()
                assertThat(ready.reminders.first { it.name == "Rahu Kalam" }.isTomorrow).isTrue()
                // Nothing is ever in the past, so every window can be toggled.
                val now = System.currentTimeMillis()
                assertThat(ready.reminders.all { it.start.toEpochMilliseconds() > now }).isTrue()
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
                val abhijit = (awaitItem() as AlarmUiState.Ready).reminders.first { it.name == "Abhijit Muhurta" }

                viewModel.setReminder(abhijit, enabled = true)

                val updated = awaitItem() as AlarmUiState.Ready
                assertThat(updated.reminders.first { it.id == abhijit.id }.isEnabled).isTrue()

                // Fires 10 minutes (600_000 ms) before the upcoming window start.
                val expectedTrigger = abhijit.start.toEpochMilliseconds() - 10 * MILLIS_PER_MINUTE
                val scheduled = scheduler.scheduled.single()
                assertThat(scheduled.id).isEqualTo(abhijit.id)
                assertThat(scheduled.triggerAt.toEpochMilliseconds()).isEqualTo(expectedTrigger)

                val persisted = repository.reminders.value.single()
                assertThat(persisted.id).isEqualTo(abhijit.id)
                assertThat(persisted.triggerAtEpochMillis).isEqualTo(expectedTrigger)
                assertThat(persisted.title).isEqualTo(abhijit.name)
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
                val abhijit = (awaitItem() as AlarmUiState.Ready).reminders.first { it.name == "Abhijit Muhurta" }
                viewModel.setReminder(abhijit, enabled = true)
                awaitItem() // enabled

                viewModel.setReminder(abhijit.copy(isEnabled = true), enabled = false)

                val cleared = awaitItem() as AlarmUiState.Ready
                assertThat(cleared.reminders.first { it.id == abhijit.id }.isEnabled).isFalse()
                assertThat(scheduler.cancelled).containsExactly(abhijit.id)
                assertThat(repository.reminders.value).isEmpty()
            }
        }

    @Test
    fun `load renews an already-fired reminder onto its upcoming occurrence`() =
        runTest {
            val scheduler = FakeTaskScheduler()
            val repository = FakeReminderRepository()
            // Simulate a reminder set on a previous day whose trigger time is now in the past.
            repository.upsert(
                PersistedReminder(
                    id = "muhurta:Abhijit Muhurta",
                    triggerAtEpochMillis = 1L,
                    title = "Abhijit Muhurta",
                    body = "old body",
                ),
            )
            val before = System.currentTimeMillis()
            val viewModel = viewModel(scheduler = scheduler, repository = repository)

            viewModel.uiState.test {
                awaitItem() // Loading
                viewModel.load()
                awaitItem() // Ready

                // The fired reminder was rolled forward to a future trigger and kept, not dropped.
                val renewed = scheduler.scheduled.last { it.id == "muhurta:Abhijit Muhurta" }
                assertThat(renewed.triggerAt.toEpochMilliseconds()).isGreaterThan(before)
                assertThat(repository.reminders.value.map { it.id }).contains("muhurta:Abhijit Muhurta")
                assertThat(
                    repository.reminders.value
                        .single()
                        .triggerAtEpochMillis,
                ).isGreaterThan(before)
            }
        }

    @Test
    fun `setOffsetMinutes retroactively reschedules an enabled reminder`() =
        runTest {
            val scheduler = FakeTaskScheduler()
            val repository = FakeReminderRepository()
            val viewModel = viewModel(scheduler = scheduler, repository = repository)
            var abhijitStartMillis = 0L

            viewModel.uiState.test {
                awaitItem() // Loading
                viewModel.load()
                val abhijit = (awaitItem() as AlarmUiState.Ready).reminders.first { it.name == "Abhijit Muhurta" }
                abhijitStartMillis = abhijit.start.toEpochMilliseconds()
                viewModel.setReminder(abhijit, enabled = true)
                awaitItem() // enabled at the default 10-minute offset

                viewModel.setOffsetMinutes(abhijit.name, 30)
                cancelAndIgnoreRemainingEvents()
            }

            // After the 30-minute offset, the reminder is rescheduled to start - 30 min.
            val expectedTrigger = abhijitStartMillis - 30 * MILLIS_PER_MINUTE
            val scheduled = scheduler.scheduled.last()
            assertThat(scheduled.triggerAt.toEpochMilliseconds()).isEqualTo(expectedTrigger)
            assertThat(scheduled.notification.body).contains("30 minutes")
        }

    @Test
    fun `setOffsetMinutes on a disabled reminder does not touch the scheduler`() =
        runTest {
            val scheduler = FakeTaskScheduler()
            val repository = FakeReminderRepository()
            val viewModel = viewModel(scheduler = scheduler, repository = repository)

            viewModel.uiState.test {
                awaitItem() // Loading
                viewModel.load()
                awaitItem() // Ready

                viewModel.setOffsetMinutes("Rahu Kalam", 30)

                val updated = awaitItem() as AlarmUiState.Ready
                assertThat(updated.reminders.first { it.name == "Rahu Kalam" }.offsetMinutes).isEqualTo(30)
                assertThat(scheduler.scheduled).isEmpty()
                assertThat(repository.offsetMinutesByName.value).containsEntry("Rahu Kalam", 30)
            }
        }

    @Test
    fun `reminder body reflects the configured offset and quality`() =
        runTest {
            val scheduler = FakeTaskScheduler()
            val repository = FakeReminderRepository()
            val viewModel = viewModel(scheduler = scheduler, repository = repository)

            viewModel.uiState.test {
                awaitItem() // Loading
                viewModel.load()
                awaitItem() // Ready

                viewModel.setOffsetMinutes("Abhijit Muhurta", 0)
                val auspicious = (awaitItem() as AlarmUiState.Ready).reminders.first { it.name == "Abhijit Muhurta" }
                viewModel.setReminder(auspicious, enabled = true)
                val afterAbhijit = awaitItem() as AlarmUiState.Ready
                val inauspicious = afterAbhijit.reminders.first { it.name == "Rahu Kalam" }
                viewModel.setReminder(inauspicious, enabled = true)
                cancelAndIgnoreRemainingEvents()
            }

            val bodies = scheduler.scheduled.associate { it.id to it.notification.body }
            assertThat(bodies.getValue("muhurta:Abhijit Muhurta")).contains("is starting now")
            assertThat(bodies.getValue("muhurta:Abhijit Muhurta")).contains("auspicious")
            assertThat(bodies.getValue("muhurta:Rahu Kalam")).contains("starts in 10 minutes")
            assertThat(bodies.getValue("muhurta:Rahu Kalam")).contains("mindful of")
        }

    @Test
    fun `falls back to the default location when unavailable`() =
        runTest {
            val viewModel = viewModel(location = AppResult.Failure(SecurityException("no permission")))

            viewModel.uiState.test {
                awaitItem() // Loading
                viewModel.load()
                assertThat((awaitItem() as AlarmUiState.Ready).usingDefaultLocation).isTrue()
            }
        }

    private fun viewModel(
        scheduler: TaskScheduler = FakeTaskScheduler(),
        repository: ReminderRepository = FakeReminderRepository(),
        location: AppResult<GeoCoordinates> = AppResult.Success(BENGALURU),
    ): AlarmViewModel =
        AlarmViewModel(
            astronomyEngine = FakeAstronomyEngine(),
            locationProvider = FakeLocationProvider(location),
            taskScheduler = scheduler,
            reminderRepository = repository,
        )
}

// Returns muhurtas relative to the requested instant: Abhijit is always ahead of it, Rahu always
// behind — so at "today's" instant Rahu has passed and rolls to tomorrow, while both windows of the
// resolved "upcoming" set are always in the future.
private class FakeAstronomyEngine : AstronomyEngine {
    override suspend fun snapshotAt(
        instant: Instant,
        location: GeoCoordinates,
    ): AppResult<AstronomySnapshot> = AppResult.Success(snapshotFor(instant))

    override suspend fun daySummaryAt(
        instant: Instant,
        location: GeoCoordinates,
    ): AppResult<PanchangaDaySummary> {
        val snapshot = snapshotFor(instant)
        return AppResult.Success(
            PanchangaDaySummary(
                tithi = snapshot.tithi,
                nakshatra = snapshot.nakshatra,
                moonPhase = snapshot.moonPhase,
            ),
        )
    }
}

private fun snapshotFor(instant: Instant): AstronomySnapshot =
    AstronomySnapshot(
        instant = instant,
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
        muhurtas =
            listOf(
                Muhurta(
                    name = "Abhijit Muhurta",
                    start = instant + 2.hours,
                    end = instant + 3.hours,
                    quality = MuhurtaQuality.AUSPICIOUS,
                ),
                Muhurta(
                    name = "Rahu Kalam",
                    start = instant - 1.hours,
                    end = instant,
                    quality = MuhurtaQuality.INAUSPICIOUS,
                ),
            ),
    )

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
    override val offsetMinutesByName = MutableStateFlow<Map<String, Int>>(emptyMap())

    override suspend fun upsert(reminder: PersistedReminder) {
        reminders.value = reminders.value.filterNot { it.id == reminder.id } + reminder
    }

    override suspend fun remove(id: String) {
        reminders.value = reminders.value.filterNot { it.id == id }
    }

    override suspend fun removePast(nowEpochMillis: Long) {
        reminders.value = reminders.value.filter { it.triggerAtEpochMillis > nowEpochMillis }
    }

    override suspend fun setOffsetMinutes(
        name: String,
        minutes: Int,
    ) {
        offsetMinutesByName.value = offsetMinutesByName.value + (name to minutes)
    }
}

private val BENGALURU = GeoCoordinates(latitude = 12.9716, longitude = 77.5946)
