/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.scheduler

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.github.vedicmitra.core.common.coroutines.DispatcherProvider
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.notifications.AppNotification
import io.github.vedicmitra.core.notifications.AppNotificationChannel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class DefaultTaskSchedulerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val scheduler = DefaultTaskScheduler(context, alarmManager, RoboDispatcherProvider)

    private val task =
        ScheduledTask(
            id = "muhurta-abhijit",
            triggerAt = Instant.fromEpochMilliseconds(2_000_000_000_000L),
            notification =
                AppNotification(
                    id = 1,
                    channel = AppNotificationChannel.MUHURTA_REMINDERS,
                    title = "Abhijit Muhurta",
                    body = "Begins in 10 minutes.",
                ),
        )

    @Test
    fun `canScheduleExactAlarms is true below API 31`() {
        assertThat(scheduler.canScheduleExactAlarms()).isTrue()
    }

    @Test
    fun `schedule registers exactly one alarm`() =
        runTest {
            val result = scheduler.schedule(task)

            check(result is AppResult.Success)
            assertThat(shadowOf(alarmManager).scheduledAlarms).hasSize(1)
        }

    @Test
    fun `cancel removes a previously scheduled alarm`() =
        runTest {
            scheduler.schedule(task)

            val result = scheduler.cancel(task.id)

            check(result is AppResult.Success)
            assertThat(shadowOf(alarmManager).scheduledAlarms).isEmpty()
        }
}

private object RoboDispatcherProvider : DispatcherProvider {
    override val default: CoroutineDispatcher = Dispatchers.Unconfined
    override val io: CoroutineDispatcher = Dispatchers.Unconfined
    override val main: CoroutineDispatcher = Dispatchers.Unconfined
}
