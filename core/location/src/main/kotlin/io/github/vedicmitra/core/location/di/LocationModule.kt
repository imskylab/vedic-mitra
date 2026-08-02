/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.location.di

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.vedicmitra.core.location.DefaultLocationProvider
import io.github.vedicmitra.core.location.LocationProvider
import javax.inject.Singleton

/** Binds the [LocationProvider] port and provides the fused location client it depends on. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class LocationModule {
    @Binds
    abstract fun bindLocationProvider(impl: DefaultLocationProvider): LocationProvider

    companion object {
        @Provides
        @Singleton
        fun provideFusedLocationProviderClient(
            @ApplicationContext context: Context,
        ): FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    }
}
