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

import io.github.vedicmitra.core.common.coroutines.DispatcherProvider
import io.github.vedicmitra.core.common.model.GeoCoordinates
import kotlinx.coroutines.withContext
import us.dustinj.timezonemap.TimeZoneMap
import javax.inject.Inject

/**
 * [TimeZoneResolver] backed by `timezonemap`, which does an offline point-in-polygon lookup against
 * OpenStreetMap-derived time-zone boundaries. Only a small region around the point is loaded (rather
 * than the whole world) to keep memory and latency low, and the work runs off the main thread.
 */
class DefaultTimeZoneResolver
    @Inject
    constructor(
        private val dispatchers: DispatcherProvider,
    ) : TimeZoneResolver {
        override suspend fun resolve(coordinates: GeoCoordinates): String =
            withContext(dispatchers.default) {
                // The `timezonemap` lookup loads bundled polygon data and can fail at runtime (e.g. on a
                // low-memory device, or if the data is stripped by a release build's minifier). Any
                // failure must never crash the app — fall back to the longitude-based estimate.
                runCatching { lookupZoneId(coordinates) }.getOrNull()
                    ?: TimeZoneEstimator.estimate(coordinates)
            }

        // The precise time-zone id from the offline polygon map, or `null` if the point isn't covered.
        private fun lookupZoneId(coordinates: GeoCoordinates): String? {
            val latitude = coordinates.latitude
            val longitude = coordinates.longitude
            val map =
                TimeZoneMap.forRegion(
                    (latitude - PADDING_DEGREES).coerceAtLeast(MIN_LATITUDE),
                    (longitude - PADDING_DEGREES).coerceAtLeast(MIN_LONGITUDE),
                    (latitude + PADDING_DEGREES).coerceAtMost(MAX_LATITUDE),
                    (longitude + PADDING_DEGREES).coerceAtMost(MAX_LONGITUDE),
                )
            return map.getOverlappingTimeZone(latitude, longitude)?.zoneId
        }

        private companion object {
            // A small bounding box around the point is enough for a point-in-polygon test and keeps
            // the loaded data (and init time) minimal.
            const val PADDING_DEGREES = 0.5
            const val MIN_LATITUDE = -90.0
            const val MAX_LATITUDE = 90.0
            const val MIN_LONGITUDE = -180.0
            const val MAX_LONGITUDE = 180.0
        }
    }
