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
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.vedicmitra.core.datastore.DefaultUserPreferencesRepository
import io.github.vedicmitra.core.datastore.UserPreferencesRepository
import javax.inject.Singleton

private val Context.userPreferencesDataStore by preferencesDataStore(name = "user_preferences")

/** Binds the preferences repository and provides the underlying [DataStore]. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataStoreModule {
    @Binds
    abstract fun bindUserPreferencesRepository(impl: DefaultUserPreferencesRepository): UserPreferencesRepository

    companion object {
        @Provides
        @Singleton
        fun provideUserPreferencesDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = context.userPreferencesDataStore
    }
}
