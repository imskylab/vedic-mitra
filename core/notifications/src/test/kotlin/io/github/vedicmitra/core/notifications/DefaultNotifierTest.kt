/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.google.common.truth.Truth.assertThat
import io.github.vedicmitra.core.common.coroutines.DispatcherProvider
import io.github.vedicmitra.core.common.result.AppResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultNotifierTest {
    private val context = mockk<Context>(relaxed = true)
    private val notificationManager = mockk<NotificationManagerCompat>(relaxed = true)
    private val notifier = DefaultNotifier(context, notificationManager, UnconfinedDispatcherProvider)

    @Test
    fun `show fails when notifications are disabled`() =
        runTest {
            every { notificationManager.areNotificationsEnabled() } returns false

            val result = notifier.show(sampleNotification)

            assertThat(result).isInstanceOf(AppResult.Failure::class.java)
            // Guard short-circuits before any channel is created or notification posted.
            verify(exactly = 0) { notificationManager.notify(any(), any()) }
        }

    @Test
    fun `cancel dismisses the notification by id and succeeds`() =
        runTest {
            val result = notifier.cancel(id = 42)

            assertThat(result).isInstanceOf(AppResult.Success::class.java)
            verify { notificationManager.cancel(42) }
        }

    @Test
    fun `cancel fails when the platform throws`() =
        runTest {
            every { notificationManager.cancel(any<Int>()) } throws RuntimeException("boom")

            assertThat(notifier.cancel(id = 7)).isInstanceOf(AppResult.Failure::class.java)
        }

    private val sampleNotification =
        AppNotification(
            id = 1,
            channel = AppNotificationChannel.MUHURTA_REMINDERS,
            title = "Abhijit Muhurta",
            body = "Begins in 10 minutes.",
        )
}

private object UnconfinedDispatcherProvider : DispatcherProvider {
    override val default: CoroutineDispatcher = Dispatchers.Unconfined
    override val io: CoroutineDispatcher = Dispatchers.Unconfined
    override val main: CoroutineDispatcher = Dispatchers.Unconfined
}
