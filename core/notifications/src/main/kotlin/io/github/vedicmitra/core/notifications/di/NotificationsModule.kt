/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.notifications.di

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.vedicmitra.core.notifications.DefaultNotifier
import io.github.vedicmitra.core.notifications.Notifier
import javax.inject.Singleton

/** Binds the [Notifier] port and provides the [NotificationManagerCompat] it depends on. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class NotificationsModule {
    @Binds
    abstract fun bindNotifier(impl: DefaultNotifier): Notifier

    companion object {
        @Provides
        @Singleton
        fun provideNotificationManagerCompat(
            @ApplicationContext context: Context,
        ): NotificationManagerCompat = NotificationManagerCompat.from(context)
    }
}
