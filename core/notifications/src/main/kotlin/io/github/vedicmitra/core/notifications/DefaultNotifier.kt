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
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.vedicmitra.core.common.coroutines.DispatcherProvider
import io.github.vedicmitra.core.common.result.AppResult
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [Notifier] backed by [NotificationManagerCompat].
 *
 * The target channel is created on demand before each post (channel creation is idempotent), so a
 * newly added [AppNotificationChannel] needs no separate registration step. Posting is guarded on
 * [NotificationManagerCompat.areNotificationsEnabled]: when the user has denied or revoked
 * notifications the call fails with an [AppResult.Failure] rather than silently dropping the post.
 */
@Singleton
class DefaultNotifier
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val notificationManager: NotificationManagerCompat,
        private val dispatchers: DispatcherProvider,
    ) : Notifier {
        override suspend fun show(notification: AppNotification): AppResult<Unit> =
            withContext(dispatchers.io) {
                runCatching {
                    if (!notificationManager.areNotificationsEnabled()) {
                        error("Notifications are disabled for this app")
                    }
                    ensureChannel(notification.channel)
                    val built =
                        NotificationCompat
                            .Builder(context, notification.channel.id)
                            .setSmallIcon(R.drawable.ic_stat_vedic_mitra)
                            .setContentTitle(notification.title)
                            .setContentText(notification.body)
                            .setAutoCancel(true)
                            .build()
                    notificationManager.notify(notification.id, built)
                }.fold(
                    onSuccess = { AppResult.Success(Unit) },
                    onFailure = { AppResult.Failure(it) },
                )
            }

        override suspend fun cancel(id: Int): AppResult<Unit> =
            withContext(dispatchers.io) {
                runCatching { notificationManager.cancel(id) }
                    .fold(
                        onSuccess = { AppResult.Success(Unit) },
                        onFailure = { AppResult.Failure(it) },
                    )
            }

        private fun ensureChannel(channel: AppNotificationChannel) {
            val compat =
                NotificationChannelCompat
                    .Builder(channel.id, channel.importance)
                    .setName(channel.channelName)
                    .setDescription(channel.description)
                    .build()
            notificationManager.createNotificationChannel(compat)
        }
    }
