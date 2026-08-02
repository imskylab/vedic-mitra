/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

package io.github.vedicmitra.core.location

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import io.github.vedicmitra.core.common.coroutines.DispatcherProvider
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * [LocationProvider] backed by the Google Play Services fused location provider.
 *
 * The **caller must hold a location permission** (`ACCESS_COARSE_LOCATION` or
 * `ACCESS_FINE_LOCATION`) before invoking these. A missing permission surfaces as an
 * [AppResult.Failure] from [currentLocation] and closes the [locationUpdates] flow with an error,
 * rather than throwing.
 */
class DefaultLocationProvider
    @Inject
    constructor(
        private val fusedClient: FusedLocationProviderClient,
        private val dispatchers: DispatcherProvider,
    ) : LocationProvider {
        @SuppressLint("MissingPermission")
        override suspend fun currentLocation(): AppResult<GeoCoordinates> =
            withContext(dispatchers.io) {
                try {
                    val location = fusedClient.lastLocation.await()
                    if (location != null) {
                        AppResult.Success(location.toGeoCoordinates())
                    } else {
                        AppResult.Failure(IllegalStateException("No last known location available"))
                    }
                } catch (e: SecurityException) {
                    AppResult.Failure(e)
                }
            }

        @SuppressLint("MissingPermission")
        override fun locationUpdates(): Flow<GeoCoordinates> =
            callbackFlow {
                val request =
                    LocationRequest
                        .Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, UPDATE_INTERVAL_MILLIS)
                        .build()

                val callback =
                    object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            result.lastLocation?.let { trySend(it.toGeoCoordinates()) }
                        }
                    }

                try {
                    fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
                } catch (e: SecurityException) {
                    close(e)
                }

                awaitClose { fusedClient.removeLocationUpdates(callback) }
            }

        private companion object {
            const val UPDATE_INTERVAL_MILLIS = 10_000L
        }
    }

private fun Location.toGeoCoordinates() = GeoCoordinates(latitude = latitude, longitude = longitude)
