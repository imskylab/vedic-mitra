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

import com.google.common.truth.Truth.assertThat
import io.github.vedicmitra.core.common.model.GeoCoordinates
import org.junit.Test
import java.time.ZoneId

class TimeZoneEstimatorTest {
    @Test
    fun `estimates a positive offset east of Greenwich`() {
        // New Delhi ~77.2 E -> +5h.
        val zone = TimeZoneEstimator.estimate(GeoCoordinates(latitude = 28.6139, longitude = 77.2090))

        assertThat(zone).isEqualTo("UTC+05:00")
    }

    @Test
    fun `estimates a negative offset west of Greenwich`() {
        // Washington, D.C. ~-77.0 -> -5h.
        val zone = TimeZoneEstimator.estimate(GeoCoordinates(latitude = 38.9072, longitude = -77.0369))

        assertThat(zone).isEqualTo("UTC-05:00")
    }

    @Test
    fun `returns UTC at the prime meridian`() {
        assertThat(TimeZoneEstimator.estimate(GeoCoordinates(latitude = 51.4769, longitude = 0.0))).isEqualTo("UTC")
    }

    @Test
    fun `clamps extreme longitudes to the valid offset range`() {
        val east = TimeZoneEstimator.estimate(GeoCoordinates(latitude = 0.0, longitude = 179.9))
        val west = TimeZoneEstimator.estimate(GeoCoordinates(latitude = 0.0, longitude = -179.9))

        assertThat(east).isEqualTo("UTC+12:00")
        assertThat(west).isEqualTo("UTC-12:00")
    }

    @Test
    fun `every estimate is a resolvable zone id`() {
        for (longitude in -180..180 step 5) {
            val zone = TimeZoneEstimator.estimate(GeoCoordinates(latitude = 0.0, longitude = longitude.toDouble()))
            // Should not throw.
            ZoneId.of(zone)
        }
    }
}
