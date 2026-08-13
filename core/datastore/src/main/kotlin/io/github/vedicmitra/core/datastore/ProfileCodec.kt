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

import java.time.LocalDate
import java.time.LocalTime

/**
 * Encodes a [BirthProfile] to and from a single string so a set of them can live in a Preferences
 * DataStore. Fields are joined with the ASCII unit-separator (0x1F), which cannot be typed on a
 * keyboard; the free-text [BirthProfile.placeOfBirth] is placed last so any stray separators are
 * absorbed on decode, and [BirthProfile.name] is sanitised of the separator on encode.
 */
internal object ProfileCodec {
    private const val SEPARATOR_CODE = 0x1F
    private const val INDEX_ID = 0
    private const val INDEX_RELATION = 1
    private const val INDEX_DATE = 2
    private const val INDEX_TIME = 3
    private const val INDEX_NAME = 4
    private const val INDEX_PLACE = 5
    private const val FIELD_COUNT = 6
    private val separator = Char(SEPARATOR_CODE).toString()

    fun encode(profile: BirthProfile): String =
        listOf(
            profile.id,
            profile.relation.name,
            profile.dateOfBirth?.toString().orEmpty(),
            profile.timeOfBirth?.toString().orEmpty(),
            profile.name.replace(separator, " "),
            profile.placeOfBirth,
        ).joinToString(separator)

    /** Decodes a value produced by [encode], or `null` if it is malformed. */
    fun decode(value: String): BirthProfile? {
        val parts = value.split(separator, limit = FIELD_COUNT)
        if (parts.size != FIELD_COUNT) return null
        val relation = ProfileRelation.entries.firstOrNull { it.name == parts[INDEX_RELATION] } ?: return null
        return BirthProfile(
            id = parts[INDEX_ID],
            name = parts[INDEX_NAME],
            relation = relation,
            dateOfBirth = parts[INDEX_DATE].toLocalDateOrNull(),
            timeOfBirth = parts[INDEX_TIME].toLocalTimeOrNull(),
            placeOfBirth = parts[INDEX_PLACE],
        )
    }

    private fun String.toLocalDateOrNull(): LocalDate? =
        if (isBlank()) null else runCatching { LocalDate.parse(this) }.getOrNull()

    private fun String.toLocalTimeOrNull(): LocalTime? =
        if (isBlank()) null else runCatching { LocalTime.parse(this) }.getOrNull()
}
