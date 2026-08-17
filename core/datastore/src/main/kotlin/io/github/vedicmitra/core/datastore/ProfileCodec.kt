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
import java.time.LocalDate
import java.time.LocalTime

/**
 * Encodes a [BirthProfile] to and from a single string so a set of them can live in a Preferences
 * DataStore. Fields are joined with the ASCII unit-separator (0x1F), which cannot be typed on a
 * keyboard; the free-text [BirthProfile.placeOfBirth] is placed last so any stray separators are
 * absorbed on decode, and [BirthProfile.name] is sanitised of the separator on encode.
 *
 * [gender][BirthProfile.gender] was added after the first release: it sits just before the place, and
 * [decode] still accepts the earlier field layout (no gender) so saved profiles survive an upgrade.
 */
internal object ProfileCodec {
    private const val SEPARATOR_CODE = 0x1F
    private const val INDEX_ID = 0
    private const val INDEX_RELATION = 1
    private const val INDEX_DATE = 2
    private const val INDEX_TIME = 3
    private const val INDEX_LATITUDE = 4
    private const val INDEX_LONGITUDE = 5
    private const val INDEX_ZONE = 6
    private const val INDEX_NAME = 7
    private const val INDEX_GENDER = 8
    private const val INDEX_PLACE = 9
    private const val FIELD_COUNT = 10

    // The original layout, before gender was added: same order, with place at index 8 and no gender.
    private const val LEGACY_FIELD_COUNT = 9
    private const val LEGACY_INDEX_PLACE = 8

    private val separator = Char(SEPARATOR_CODE).toString()

    fun encode(profile: BirthProfile): String =
        listOf(
            profile.id,
            profile.relation.name,
            profile.dateOfBirth?.toString().orEmpty(),
            profile.timeOfBirth?.toString().orEmpty(),
            profile.birthCoordinates
                ?.latitude
                ?.toString()
                .orEmpty(),
            profile.birthCoordinates
                ?.longitude
                ?.toString()
                .orEmpty(),
            profile.birthZoneId.orEmpty(),
            profile.name.replace(separator, " "),
            profile.gender?.name.orEmpty(),
            profile.placeOfBirth,
        ).joinToString(separator)

    /** Decodes a value produced by [encode] (current or legacy layout), or `null` if it is malformed. */
    fun decode(value: String): BirthProfile? {
        val parts = value.split(separator, limit = FIELD_COUNT)
        val isCurrent = parts.size == FIELD_COUNT
        val placeIndex =
            when (parts.size) {
                FIELD_COUNT -> INDEX_PLACE
                LEGACY_FIELD_COUNT -> LEGACY_INDEX_PLACE
                else -> return null
            }
        val relation = ProfileRelation.entries.firstOrNull { it.name == parts[INDEX_RELATION] } ?: return null
        val gender = if (isCurrent) Gender.entries.firstOrNull { it.name == parts[INDEX_GENDER] } else null
        return BirthProfile(
            id = parts[INDEX_ID],
            name = parts[INDEX_NAME],
            relation = relation,
            gender = gender,
            dateOfBirth = parts[INDEX_DATE].toLocalDateOrNull(),
            timeOfBirth = parts[INDEX_TIME].toLocalTimeOrNull(),
            placeOfBirth = parts[placeIndex],
            birthCoordinates = coordinatesOf(parts[INDEX_LATITUDE], parts[INDEX_LONGITUDE]),
            birthZoneId = parts[INDEX_ZONE].ifBlank { null },
        )
    }

    private fun coordinatesOf(
        latitude: String,
        longitude: String,
    ): GeoCoordinates? {
        val lat = latitude.toDoubleOrNull()
        val lng = longitude.toDoubleOrNull()
        return if (lat != null && lng != null) GeoCoordinates(latitude = lat, longitude = lng) else null
    }

    private fun String.toLocalDateOrNull(): LocalDate? =
        if (isBlank()) null else runCatching { LocalDate.parse(this) }.getOrNull()

    private fun String.toLocalTimeOrNull(): LocalTime? =
        if (isBlank()) null else runCatching { LocalTime.parse(this) }.getOrNull()
}
