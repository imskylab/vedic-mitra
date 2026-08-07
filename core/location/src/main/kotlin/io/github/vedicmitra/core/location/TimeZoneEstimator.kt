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

import io.github.vedicmitra.core.common.model.GeoCoordinates
import java.time.ZoneOffset
import kotlin.math.roundToInt

/**
 * Produces a *best-guess* IANA time-zone id for a set of coordinates, offline, from longitude alone
 * (Earth turns 15° of longitude per hour). The guess is a fixed UTC offset with no daylight-saving
 * rules — it is only a sensible starting point the user confirms or overrides with a proper named
 * zone (e.g. "Asia/Kolkata") when saving a location.
 *
 * A precise coordinate-to-zone mapping would require bundling multi-megabyte timezone-boundary
 * data; that trade-off is deliberately avoided here (see docs/adr/0006).
 */
object TimeZoneEstimator {
    private const val DEGREES_PER_HOUR = 15.0
    private const val MIN_OFFSET_HOURS = -12
    private const val MAX_OFFSET_HOURS = 14

    /** Returns a fixed-offset zone id such as "UTC+05:00", or "UTC" at the prime meridian. */
    fun estimate(coordinates: GeoCoordinates): String {
        val hours =
            (coordinates.longitude / DEGREES_PER_HOUR)
                .roundToInt()
                .coerceIn(MIN_OFFSET_HOURS, MAX_OFFSET_HOURS)
        return if (hours == 0) "UTC" else "UTC" + ZoneOffset.ofHours(hours).id
    }
}
