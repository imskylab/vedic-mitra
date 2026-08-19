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

import android.location.Address
import android.location.Geocoder
import io.github.vedicmitra.core.common.coroutines.DispatcherProvider
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * [GeocodingClient] backed by the platform [Geocoder]. Forward geocoding runs off the main thread
 * and reports "no backend" and I/O errors as [AppResult.Failure] rather than throwing.
 *
 * The blocking [Geocoder.getFromLocationName] overload is used deliberately: it works from
 * `minSdk` 26 (the callback overload is API 33+) and is safe here because the call is confined to
 * the I/O dispatcher.
 */
class DefaultGeocodingClient
    @Inject
    constructor(
        private val geocoder: Geocoder,
        private val dispatchers: DispatcherProvider,
    ) : GeocodingClient {
        override suspend fun search(
            query: String,
            maxResults: Int,
        ): AppResult<List<GeocodeResult>> =
            withContext(dispatchers.io) {
                if (!Geocoder.isPresent()) {
                    return@withContext AppResult.Failure(
                        IllegalStateException("No geocoder backend is available on this device"),
                    )
                }
                // runCatching guards against the platform Geocoder throwing anything (IOException on a
                // network/backend error, or a RuntimeException when the geocoder service is unavailable)
                // so a place search can never crash the app — it degrades to a Failure the UI reports.
                runCatching {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(query, maxResults).orEmpty().mapNotNull { it.toGeocodeResult() }
                }.fold(
                    onSuccess = { AppResult.Success(it) },
                    onFailure = { AppResult.Failure(it) },
                )
            }
    }

/** Converts an [Address] to a [GeocodeResult], or `null` if it has no usable coordinates. */
private fun Address.toGeocodeResult(): GeocodeResult? {
    if (!hasLatitude() || !hasLongitude()) return null
    val name =
        listOfNotNull(locality ?: featureName, adminArea, countryName)
            .distinct()
            .joinToString(", ")
            .ifBlank { "%.4f, %.4f".format(latitude, longitude) }
    return GeocodeResult(label = name, coordinates = GeoCoordinates(latitude = latitude, longitude = longitude))
}
