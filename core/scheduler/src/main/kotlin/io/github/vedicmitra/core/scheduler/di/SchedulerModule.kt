/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.scheduler.di

import android.app.AlarmManager
import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.vedicmitra.core.scheduler.DefaultTaskScheduler
import io.github.vedicmitra.core.scheduler.TaskScheduler
import javax.inject.Singleton

/** Binds the [TaskScheduler] port and provides the [AlarmManager] it depends on. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class SchedulerModule {
    @Binds
    abstract fun bindTaskScheduler(impl: DefaultTaskScheduler): TaskScheduler

    companion object {
        @Provides
        @Singleton
        fun provideAlarmManager(
            @ApplicationContext context: Context,
        ): AlarmManager = context.getSystemService(AlarmManager::class.java)
    }
}
