/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
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
