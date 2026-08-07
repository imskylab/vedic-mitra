/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.datastore

import com.google.common.truth.Truth.assertThat
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.model.LocationSource
import io.github.vedicmitra.core.common.model.SavedLocation
import org.junit.Test

class SavedLocationCodecTest {
    @Test
    fun `round-trips a saved location`() {
        val location =
            SavedLocation(
                id = "loc-123",
                label = "Varanasi",
                coordinates = GeoCoordinates(latitude = 25.3176, longitude = 82.9739),
                zoneId = "Asia/Kolkata",
                source = LocationSource.CITY,
            )

        assertThat(SavedLocationCodec.decode(SavedLocationCodec.encode(location))).isEqualTo(location)
    }

    @Test
    fun `round-trips a label containing spaces and punctuation`() {
        val location =
            SavedLocation(
                id = "loc-9",
                label = "Washington, D.C., USA",
                coordinates = GeoCoordinates(latitude = 38.9072, longitude = -77.0369),
                zoneId = "America/New_York",
                source = LocationSource.MANUAL,
            )

        assertThat(SavedLocationCodec.decode(SavedLocationCodec.encode(location))).isEqualTo(location)
    }

    @Test
    fun `decode returns null for a malformed value`() {
        assertThat(SavedLocationCodec.decode("not-a-location")).isNull()
    }

    @Test
    fun `decode returns null when latitude is not a number`() {
        val location =
            SavedLocation(
                id = "id",
                label = "L",
                coordinates = GeoCoordinates(latitude = 1.0, longitude = 2.0),
                zoneId = "UTC",
                source = LocationSource.DEVICE,
            )
        val corrupted = SavedLocationCodec.encode(location).replace("1.0", "x")

        assertThat(SavedLocationCodec.decode(corrupted)).isNull()
    }

    @Test
    fun `decode returns null for an unknown source`() {
        val location =
            SavedLocation(
                id = "id",
                label = "L",
                coordinates = GeoCoordinates(latitude = 1.0, longitude = 2.0),
                zoneId = "UTC",
                source = LocationSource.DEVICE,
            )
        val corrupted = SavedLocationCodec.encode(location).replace(LocationSource.DEVICE.name, "SATELLITE")

        assertThat(SavedLocationCodec.decode(corrupted)).isNull()
    }
}
