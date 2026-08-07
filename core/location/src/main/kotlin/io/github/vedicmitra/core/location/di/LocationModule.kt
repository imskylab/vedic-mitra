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
import android.location.Geocoder
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.vedicmitra.core.location.DefaultGeocodingClient
import io.github.vedicmitra.core.location.DefaultLocationProvider
import io.github.vedicmitra.core.location.DefaultTimeZoneResolver
import io.github.vedicmitra.core.location.GeocodingClient
import io.github.vedicmitra.core.location.LocationProvider
import io.github.vedicmitra.core.location.TimeZoneResolver
import javax.inject.Singleton

/** Binds the [LocationProvider] port and provides the fused location client it depends on. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class LocationModule {
    @Binds
    abstract fun bindLocationProvider(impl: DefaultLocationProvider): LocationProvider

    @Binds
    abstract fun bindGeocodingClient(impl: DefaultGeocodingClient): GeocodingClient

    @Binds
    abstract fun bindTimeZoneResolver(impl: DefaultTimeZoneResolver): TimeZoneResolver

    companion object {
        @Provides
        @Singleton
        fun provideFusedLocationProviderClient(
            @ApplicationContext context: Context,
        ): FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

        @Provides
        fun provideGeocoder(
            @ApplicationContext context: Context,
        ): Geocoder = Geocoder(context)
    }
}
