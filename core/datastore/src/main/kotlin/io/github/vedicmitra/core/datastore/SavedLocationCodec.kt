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

import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.model.LocationSource
import io.github.vedicmitra.core.common.model.SavedLocation

/**
 * Encodes a [SavedLocation] to and from a single string so a set of them can live in a Preferences
 * DataStore. Fields are joined with the ASCII unit-separator (code 0x1F), which never occurs in the
 * app-generated id, numeric coordinates, zone id, or source name; the free-text [SavedLocation.label]
 * is placed last so any stray separators in it are absorbed on decode.
 */
internal object SavedLocationCodec {
    private const val SEPARATOR_CODE = 0x1F
    private const val INDEX_ID = 0
    private const val INDEX_LATITUDE = 1
    private const val INDEX_LONGITUDE = 2
    private const val INDEX_ZONE = 3
    private const val INDEX_SOURCE = 4
    private const val INDEX_LABEL = 5
    private const val FIELD_COUNT = 6
    private val separator = Char(SEPARATOR_CODE).toString()

    fun encode(location: SavedLocation): String =
        listOf(
            location.id,
            location.coordinates.latitude.toString(),
            location.coordinates.longitude.toString(),
            location.zoneId,
            location.source.name,
            location.label,
        ).joinToString(separator)

    /** Decodes a value produced by [encode], or `null` if it is malformed. */
    fun decode(value: String): SavedLocation? {
        val parts = value.split(separator, limit = FIELD_COUNT)
        if (parts.size != FIELD_COUNT) return null
        val latitude = parts[INDEX_LATITUDE].toDoubleOrNull()
        val longitude = parts[INDEX_LONGITUDE].toDoubleOrNull()
        val source = LocationSource.entries.firstOrNull { it.name == parts[INDEX_SOURCE] }
        if (latitude == null || longitude == null || source == null) return null
        return SavedLocation(
            id = parts[INDEX_ID],
            label = parts[INDEX_LABEL],
            coordinates = GeoCoordinates(latitude = latitude, longitude = longitude),
            zoneId = parts[INDEX_ZONE],
            source = source,
        )
    }
}
