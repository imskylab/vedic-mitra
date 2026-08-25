/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
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
import com.google.android.gms.tasks.CancellationTokenSource
import io.github.vedicmitra.core.common.coroutines.DispatcherProvider
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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
        /**
         * The cached fix if there is one, and otherwise a fresh one.
         *
         * `lastLocation` alone is not enough. It returns `null` whenever nothing has recently asked
         * the system for a position — most obviously when the device's location setting was switched
         * off, since turning it back on does not by itself produce a fix. An app that only ever reads
         * the cache therefore stays on its fallback location indefinitely after the user enables
         * location and returns to it, which is exactly the bug this fixes.
         *
         * The cache is still tried first because it is instant and almost always populated. Only when
         * it is empty do we pay for [FusedLocationProviderClient.getCurrentLocation], which actively
         * obtains a position, and that is bounded by [FRESH_FIX_TIMEOUT_MILLIS] so a device indoors
         * with no signal falls back rather than hanging the caller.
         */
        @SuppressLint("MissingPermission")
        override suspend fun currentLocation(): AppResult<GeoCoordinates> =
            withContext(dispatchers.io) {
                try {
                    val location = fusedClient.lastLocation.await() ?: freshFix()
                    if (location != null) {
                        AppResult.Success(location.toGeoCoordinates())
                    } else {
                        AppResult.Failure(IllegalStateException("No location available"))
                    }
                } catch (e: SecurityException) {
                    AppResult.Failure(e)
                }
            }

        @SuppressLint("MissingPermission")
        private suspend fun freshFix(): Location? {
            val tokenSource = CancellationTokenSource()
            return try {
                withTimeoutOrNull(FRESH_FIX_TIMEOUT_MILLIS) {
                    fusedClient
                        .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, tokenSource.token)
                        .await()
                }
            } finally {
                // Stops the scan when the timeout wins the race, rather than leaving it running.
                tokenSource.cancel()
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

            /** Long enough for a balanced-power fix outdoors, short enough not to stall the screen. */
            const val FRESH_FIX_TIMEOUT_MILLIS = 8_000L
        }
    }

private fun Location.toGeoCoordinates() = GeoCoordinates(latitude = latitude, longitude = longitude)
