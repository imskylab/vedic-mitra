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
import io.github.vedicmitra.core.astronomy.Choghadiya
import io.github.vedicmitra.core.astronomy.ChoghadiyaName
import io.github.vedicmitra.core.astronomy.Festival
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
import io.github.vedicmitra.core.common.model.AlertStyle
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
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
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
    fun `offers the day's periods as sources, auspicious first, with nothing added`() =
        runTest {
            val viewModel = viewModel()

            viewModel.uiState.test {
                assertThat(awaitItem()).isEqualTo(AlarmUiState.Loading)
                viewModel.load()

                val ready = awaitItem() as AlarmUiState.Ready
                assertThat(ready.reminders).isEmpty()
                assertThat(ready.available.map { it.label })
                    .containsExactly("Abhijit Muhurta", "Rahu Kalam")
                    .inOrder()
            }
        }

    @Test
    fun `adding a reminder schedules its next occurrence, persists it, and drops it from available`() =
        runTest {
            val scheduler = FakeTaskScheduler()
            val repository = FakeReminderRepository()
            val viewModel = viewModel(scheduler = scheduler, repository = repository)

            viewModel.uiState.test {
                awaitItem() // Loading
                viewModel.load()
                awaitItem() // Ready (empty)

                viewModel.addReminder("muhurta:Abhijit Muhurta")

                val ready = awaitItem() as AlarmUiState.Ready
                assertThat(ready.reminders.map { it.id }).containsExactly("muhurta:Abhijit Muhurta")
                assertThat(ready.available.map { it.key }).doesNotContain("muhurta:Abhijit Muhurta")
                assertThat(scheduler.scheduled.single().id).isEqualTo("muhurta:Abhijit Muhurta")
                assertThat(
                    repository.reminders.value
                        .single()
                        .id,
                ).isEqualTo("muhurta:Abhijit Muhurta")
            }
        }

    @Test
    fun `removing a reminder cancels the alarm, forgets it, and returns it to available`() =
        runTest {
            val scheduler = FakeTaskScheduler()
            val repository = FakeReminderRepository()
            val viewModel = viewModel(scheduler = scheduler, repository = repository)

            viewModel.uiState.test {
                awaitItem() // Loading
                viewModel.load()
                awaitItem() // Ready (empty)
                viewModel.addReminder("muhurta:Rahu Kalam")
                awaitItem() // Ready (one reminder)

                viewModel.removeReminder("muhurta:Rahu Kalam")

                val ready = awaitItem() as AlarmUiState.Ready
                assertThat(ready.reminders).isEmpty()
                assertThat(ready.available.map { it.key }).contains("muhurta:Rahu Kalam")
                assertThat(scheduler.cancelled).containsExactly("muhurta:Rahu Kalam")
                assertThat(repository.reminders.value).isEmpty()
            }
        }

    @Test
    fun `offers Choghadiya windows as sources`() =
        runTest {
            val viewModel = viewModel(engine = FakeAstronomyEngine(withChoghadiya = true))

            viewModel.uiState.test {
                awaitItem() // Loading
                viewModel.load()

                val ready = awaitItem() as AlarmUiState.Ready
                assertThat(ready.available.map { it.label }).contains("Amrit")
            }
        }

    @Test
    fun `load renews an already-added reminder onto its upcoming occurrence`() =
        runTest {
            val scheduler = FakeTaskScheduler()
            val repository = FakeReminderRepository()
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

                val renewed = scheduler.scheduled.last { it.id == "muhurta:Abhijit Muhurta" }
                assertThat(renewed.triggerAt.toEpochMilliseconds()).isGreaterThan(before)
                assertThat(
                    repository.reminders.value
                        .single()
                        .triggerAtEpochMillis,
                ).isGreaterThan(before)
            }
        }

    @Test
    fun `setOffsetMinutes retroactively reschedules an added reminder`() =
        runTest {
            val scheduler = FakeTaskScheduler()
            val repository = FakeReminderRepository()
            val viewModel = viewModel(scheduler = scheduler, repository = repository)

            viewModel.uiState.test {
                awaitItem() // Loading
                viewModel.load()
                awaitItem() // Ready (empty)
                viewModel.addReminder("muhurta:Abhijit Muhurta")
                val added = awaitItem() as AlarmUiState.Ready
                val startMillis =
                    added.reminders
                        .single()
                        .start
                        .toEpochMilliseconds()

                viewModel.setOffsetMinutes("muhurta:Abhijit Muhurta", 30)
                cancelAndIgnoreRemainingEvents()

                val expectedTrigger = startMillis - 30 * MILLIS_PER_MINUTE
                assertThat(
                    scheduler.scheduled
                        .last()
                        .triggerAt
                        .toEpochMilliseconds(),
                ).isEqualTo(expectedTrigger)
                assertThat(repository.offsetMinutesByName.value).containsEntry("muhurta:Abhijit Muhurta", 30)
            }
        }

    @Test
    fun `adding a reminder for an imminent window fires at its start, not immediately`() =
        runTest {
            val scheduler = FakeTaskScheduler()
            val repository = FakeReminderRepository()
            val viewModel =
                viewModel(
                    engine = FakeAstronomyEngine(imminentMuhurta = true),
                    scheduler = scheduler,
                    repository = repository,
                )

            viewModel.uiState.test {
                awaitItem() // Loading
                viewModel.load()
                awaitItem() // Ready (empty)

                viewModel.addReminder("muhurta:Sandhya")

                val windowStart =
                    (awaitItem() as AlarmUiState.Ready)
                        .reminders
                        .single()
                        .start
                        .toEpochMilliseconds()
                // The window begins within the default lead time, so the reminder fires at the window
                // start (still in the future) rather than being clamped to now / firing immediately.
                assertThat(
                    scheduler.scheduled
                        .last()
                        .triggerAt
                        .toEpochMilliseconds(),
                ).isEqualTo(windowStart)
                assertThat(windowStart).isGreaterThan(System.currentTimeMillis())
            }
        }

    @Test
    fun `setAlertType persists the chosen alert style`() =
        runTest {
            val repository = FakeReminderRepository()
            val viewModel = viewModel(repository = repository)

            viewModel.uiState.test {
                awaitItem() // Loading
                viewModel.load()
                awaitItem() // Ready

                viewModel.setAlertType("muhurta:Rahu Kalam", AlertStyle.ALARM)
                cancelAndIgnoreRemainingEvents()
            }

            assertThat(repository.alertTypeByName.value).containsEntry("muhurta:Rahu Kalam", AlertStyle.ALARM)
        }

    @Test
    fun `adding a tithi reminder resolves its next date, schedules it, and persists it`() =
        runTest {
            val scheduler = FakeTaskScheduler()
            val repository = FakeReminderRepository()
            val viewModel = viewModel(scheduler = scheduler, repository = repository)

            viewModel.uiState.test {
                awaitItem() // Loading
                viewModel.load()
                awaitItem() // Ready (empty)

                viewModel.addTithiReminder(TithiTarget(maasa = null, tithis = setOf(30)))

                val item = (awaitItem() as AlarmUiState.Ready).reminders.single()
                assertThat(item.id).isEqualTo("tithi:*:30")
                assertThat(item.name).isEqualTo("Amavasya")
                assertThat(item.dateLabel).contains("Every month")
                assertThat(scheduler.scheduled.single().id).isEqualTo("tithi:*:30")
                assertThat(
                    repository.reminders.value
                        .single()
                        .id,
                ).isEqualTo("tithi:*:30")
            }
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
        engine: AstronomyEngine = FakeAstronomyEngine(),
        scheduler: TaskScheduler = FakeTaskScheduler(),
        repository: ReminderRepository = FakeReminderRepository(),
        location: AppResult<GeoCoordinates> = AppResult.Success(BENGALURU),
    ): AlarmViewModel =
        AlarmViewModel(
            astronomyEngine = engine,
            locationProvider = FakeLocationProvider(location),
            taskScheduler = scheduler,
            reminderRepository = repository,
        )

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
        val BENGALURU = GeoCoordinates(latitude = 12.9716, longitude = 77.5946)
    }
}

// Abhijit is always ahead of the requested instant, Rahu always behind — so Abhijit resolves to
// today and Rahu (passed) rolls to tomorrow. Optionally includes a Choghadiya window.
private class FakeAstronomyEngine(
    private val withChoghadiya: Boolean = false,
    private val imminentMuhurta: Boolean = false,
) : AstronomyEngine {
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

    override suspend fun upcomingFestivals(
        instant: Instant,
        location: GeoCoordinates,
        withinDays: Int,
        limit: Int,
    ): AppResult<List<Festival>> = AppResult.Success(emptyList())

    // A fixed future occurrence three days out, so tithi reminders resolve to a real date.
    override suspend fun nextTithiOccurrence(
        instant: Instant,
        location: GeoCoordinates,
        maasa: String?,
        tithis: Set<Int>,
        withinDays: Int,
    ): AppResult<Instant?> = AppResult.Success(instant + 3.days)

    private fun snapshotFor(instant: Instant): AstronomySnapshot =
        AstronomySnapshot(
            instant = instant,
            location = GeoCoordinates(latitude = 12.9716, longitude = 77.5946),
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
                buildList {
                    add(
                        Muhurta(
                            name = "Abhijit Muhurta",
                            start = instant + 2.hours,
                            end = instant + 3.hours,
                            quality = MuhurtaQuality.AUSPICIOUS,
                        ),
                    )
                    add(
                        Muhurta(
                            name = "Rahu Kalam",
                            start = instant - 1.hours,
                            end = instant,
                            quality = MuhurtaQuality.INAUSPICIOUS,
                        ),
                    )
                    if (imminentMuhurta) {
                        // Starts inside the default 10-minute lead time.
                        add(
                            Muhurta(
                                name = "Sandhya",
                                start = instant + 1.minutes,
                                end = instant + 2.minutes,
                                quality = MuhurtaQuality.AUSPICIOUS,
                            ),
                        )
                    }
                },
            choghadiya =
                if (withChoghadiya) {
                    listOf(
                        Choghadiya(
                            name = ChoghadiyaName.AMRIT,
                            start = instant + 4.hours,
                            end = instant + 5.hours,
                            isDay = true,
                        ),
                    )
                } else {
                    emptyList()
                },
        )
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
    override val offsetMinutesByName = MutableStateFlow<Map<String, Int>>(emptyMap())
    override val alertTypeByName = MutableStateFlow<Map<String, AlertStyle>>(emptyMap())

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

    override suspend fun setAlertType(
        name: String,
        alert: AlertStyle,
    ) {
        alertTypeByName.value = alertTypeByName.value + (name to alert)
    }
}
