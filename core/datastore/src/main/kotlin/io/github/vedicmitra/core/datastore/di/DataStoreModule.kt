/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.vedicmitra.core.datastore.DefaultJapaRepository
import io.github.vedicmitra.core.datastore.DefaultLocationRepository
import io.github.vedicmitra.core.datastore.DefaultMeditationRepository
import io.github.vedicmitra.core.datastore.DefaultProfileRepository
import io.github.vedicmitra.core.datastore.DefaultReminderRepository
import io.github.vedicmitra.core.datastore.DefaultUserPreferencesRepository
import io.github.vedicmitra.core.datastore.JapaRepository
import io.github.vedicmitra.core.datastore.LocationRepository
import io.github.vedicmitra.core.datastore.MeditationRepository
import io.github.vedicmitra.core.datastore.ProfileRepository
import io.github.vedicmitra.core.datastore.ReminderRepository
import io.github.vedicmitra.core.datastore.UserPreferencesRepository
import javax.inject.Singleton

// All settings (theme, saved locations, reminders) share this store. A corruption handler lets a
// damaged file self-heal to empty on next read instead of throwing on every launch — which would
// otherwise brick the whole app, since the home screen reads theme/location at startup.
private val Context.userPreferencesDataStore by preferencesDataStore(
    name = "user_preferences",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/** Binds the preferences repositories and provides the underlying [DataStore]. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataStoreModule {
    @Binds
    abstract fun bindUserPreferencesRepository(impl: DefaultUserPreferencesRepository): UserPreferencesRepository

    @Binds
    abstract fun bindReminderRepository(impl: DefaultReminderRepository): ReminderRepository

    @Binds
    abstract fun bindLocationRepository(impl: DefaultLocationRepository): LocationRepository

    @Binds
    abstract fun bindProfileRepository(impl: DefaultProfileRepository): ProfileRepository

    @Binds
    abstract fun bindJapaRepository(impl: DefaultJapaRepository): JapaRepository

    @Binds
    abstract fun bindMeditationRepository(impl: DefaultMeditationRepository): MeditationRepository

    companion object {
        @Provides
        @Singleton
        fun provideUserPreferencesDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = context.userPreferencesDataStore
    }
}
