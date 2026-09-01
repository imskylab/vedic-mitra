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

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import io.github.vedicmitra.core.common.model.AlertStyle
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.datastore.PersistedReminder
import io.github.vedicmitra.core.datastore.ReminderRepository
import io.github.vedicmitra.core.scheduler.ScheduledTask
import io.github.vedicmitra.core.scheduler.TaskScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ReminderReschedulerTest {
    @Test
    fun `re-arms future reminders and drops stale ones`() =
        runTest {
            val now = 1_000L
            val future = PersistedReminder("future", triggerAtEpochMillis = 5_000L, title = "Abhijit", body = "soon")
            val stale = PersistedReminder("stale", triggerAtEpochMillis = 500L, title = "Rahu", body = "past")
            val repository = StubReminderRepository(listOf(future, stale))
            val scheduler = RecordingTaskScheduler()
            val rescheduler = ReminderRescheduler(repository, scheduler)

            rescheduler.rescheduleEnabled(nowEpochMillis = now)

            // Stale reminder pruned; only the future one is re-scheduled.
            assertThat(repository.reminders.value.map { it.id }).containsExactly("future")
            val scheduled = scheduler.scheduled.single()
            assertThat(scheduled.id).isEqualTo("future")
            assertThat(scheduled.triggerAt.toEpochMilliseconds()).isEqualTo(5_000L)
            assertThat(scheduled.notification.title).isEqualTo("Abhijit")
            assertThat(scheduled.notification.body).isEqualTo("soon")
        }

    @Test
    fun `an alarm-style reminder is re-armed as an alarm`() =
        runTest {
            // The bug this pins: the rescheduler built its notification without an alert style, so
            // it defaulted to NOTIFICATION. BootReceiver fires on boot *and* on app update, so every
            // restart and every update silently turned a ringing alarm into a quiet notification —
            // with the reminder still firing, so nothing looked broken.
            val ringing = PersistedReminder("brahma", triggerAtEpochMillis = 5_000L, title = "Brahma", body = "wake")
            val quiet = PersistedReminder("rahu", triggerAtEpochMillis = 6_000L, title = "Rahu", body = "avoid")
            val repository = StubReminderRepository(listOf(ringing, quiet))
            repository.setAlertType("brahma", AlertStyle.ALARM)
            val scheduler = RecordingTaskScheduler()

            ReminderRescheduler(repository, scheduler).rescheduleEnabled(nowEpochMillis = 1_000L)

            val byId = scheduler.scheduled.associateBy { it.id }
            assertThat(byId.getValue("brahma").notification.alert).isEqualTo(AlertStyle.ALARM)
            assertWithMessage("a reminder with no stored style stays a notification")
                .that(byId.getValue("rahu").notification.alert)
                .isEqualTo(AlertStyle.NOTIFICATION)
        }

    @Test
    fun `a reminder with no stored alert style defaults to a notification`() =
        runTest {
            val reminder = PersistedReminder("abhijit", triggerAtEpochMillis = 5_000L, title = "Abhijit", body = "now")
            val scheduler = RecordingTaskScheduler()

            ReminderRescheduler(StubReminderRepository(listOf(reminder)), scheduler)
                .rescheduleEnabled(nowEpochMillis = 1_000L)

            assertThat(
                scheduler.scheduled
                    .single()
                    .notification.alert,
            ).isEqualTo(AlertStyle.NOTIFICATION)
        }

    @Test
    fun `does nothing when there are no reminders`() =
        runTest {
            val scheduler = RecordingTaskScheduler()
            val rescheduler = ReminderRescheduler(StubReminderRepository(emptyList()), scheduler)

            rescheduler.rescheduleEnabled(nowEpochMillis = 1_000L)

            assertThat(scheduler.scheduled).isEmpty()
        }
}

// Offsets really are irrelevant to rescheduling: a re-armed reminder's trigger time and body were
// baked in when it was first scheduled and are replayed verbatim. The **alert style** is not, and an
// earlier version of this comment lumped the two together — which is why the stub held an empty
// alertTypeByName and the downgrade-on-reboot bug survived two test cases.
private class StubReminderRepository(
    initial: List<PersistedReminder>,
) : ReminderRepository {
    override val reminders = MutableStateFlow(initial)
    override val offsetMinutesByName = MutableStateFlow<Map<String, Int>>(emptyMap())
    override val alertTypeByName = MutableStateFlow<Map<String, AlertStyle>>(emptyMap())

    override suspend fun upsert(reminder: PersistedReminder) {
        reminders.value = reminders.value.filterNot { it.id == reminder.id } + reminder
    }

    override suspend fun remove(id: String) {
        reminders.value = reminders.value.filterNot { it.id == id }
    }

    override suspend fun setNickname(
        id: String,
        nickname: String?,
    ) {
        reminders.value =
            reminders.value.map { if (it.id == id) it.copy(nickname = nickname?.ifBlank { null }) else it }
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

private class RecordingTaskScheduler : TaskScheduler {
    val scheduled = mutableListOf<ScheduledTask>()

    override suspend fun schedule(request: ScheduledTask): AppResult<Unit> {
        scheduled += request
        return AppResult.Success(Unit)
    }

    override suspend fun cancel(id: String): AppResult<Unit> = AppResult.Success(Unit)

    override fun canScheduleExactAlarms(): Boolean = true
}
