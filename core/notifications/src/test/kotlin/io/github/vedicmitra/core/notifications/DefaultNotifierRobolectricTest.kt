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

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.github.vedicmitra.core.common.coroutines.DispatcherProvider
import io.github.vedicmitra.core.common.result.AppResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Happy-path coverage for [DefaultNotifier] against a real (Robolectric-shadowed) framework
 * notification stack — the path the mockk-only [DefaultNotifierTest] cannot exercise because it
 * builds an actual [android.app.Notification]. Pinned to API 30 (pre-POST_NOTIFICATIONS runtime
 * permission) so notifications are enabled by default and the guard does not short-circuit.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class DefaultNotifierRobolectricTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val notificationManager = NotificationManagerCompat.from(context)
    private val notifier = DefaultNotifier(context, notificationManager, RoboDispatcherProvider)

    @Test
    fun `show creates the channel and posts the notification`() =
        runTest {
            val result =
                notifier.show(
                    AppNotification(
                        id = 7,
                        channel = AppNotificationChannel.MUHURTA_REMINDERS,
                        title = "Abhijit Muhurta",
                        body = "Begins in 10 minutes.",
                    ),
                )

            check(result is AppResult.Success)
            val platform = context.getSystemService(NotificationManager::class.java)
            assertThat(platform.notificationChannels.map { it.id })
                .contains(AppNotificationChannel.MUHURTA_REMINDERS.id)
            assertThat(shadowOf(platform).allNotifications).hasSize(1)
        }

    @Test
    fun `cancel removes a previously posted notification`() =
        runTest {
            val notification =
                AppNotification(
                    id = 11,
                    channel = AppNotificationChannel.MUHURTA_REMINDERS,
                    title = "Brahma Muhurta",
                    body = "Time to rise.",
                )
            notifier.show(notification)

            val result = notifier.cancel(notification.id)

            check(result is AppResult.Success)
            val platform = context.getSystemService(NotificationManager::class.java)
            assertThat(shadowOf(platform).allNotifications).isEmpty()
        }
}

private object RoboDispatcherProvider : DispatcherProvider {
    override val default: CoroutineDispatcher = Dispatchers.Unconfined
    override val io: CoroutineDispatcher = Dispatchers.Unconfined
    override val main: CoroutineDispatcher = Dispatchers.Unconfined
}
