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

/**
 * Encodes a [JapaSession] to and from a single string so a set of them can live in a Preferences
 * DataStore. Fields are joined with the ASCII unit-separator (code 0x1F), which never occurs in the
 * app-generated mantra ids or the numeric fields.
 *
 * The format has seven fields
 * (`completedAtEpochMillis | dateEpochDay | mantraId | beads | rounds | nakshatra | tithi`); the two
 * trailing astronomy fields are blank when they weren't recorded.
 */
internal object JapaSessionCodec {
    private const val SEPARATOR_CODE = 0x1F
    private const val FIELD_COUNT = 7
    private const val INDEX_COMPLETED_AT = 0
    private const val INDEX_DATE = 1
    private const val INDEX_MANTRA = 2
    private const val INDEX_BEADS = 3
    private const val INDEX_ROUNDS = 4
    private const val INDEX_NAKSHATRA = 5
    private const val INDEX_TITHI = 6
    private val separator = Char(SEPARATOR_CODE).toString()

    fun encode(session: JapaSession): String =
        listOf(
            session.completedAtEpochMillis.toString(),
            session.dateEpochDay.toString(),
            session.mantraId,
            session.beads.toString(),
            session.rounds.toString(),
            session.nakshatraNumber?.toString().orEmpty(),
            session.tithiNumber?.toString().orEmpty(),
        ).joinToString(separator)

    /** Decodes a value produced by [encode], or `null` if malformed. */
    fun decode(value: String): JapaSession? {
        val parts = value.split(separator, limit = FIELD_COUNT)
        if (parts.size != FIELD_COUNT) return null
        val completedAt = parts[INDEX_COMPLETED_AT].toLongOrNull()
        val dateEpochDay = parts[INDEX_DATE].toLongOrNull()
        val beads = parts[INDEX_BEADS].toIntOrNull()
        val rounds = parts[INDEX_ROUNDS].toIntOrNull()
        if (completedAt == null || dateEpochDay == null) return null
        if (beads == null || rounds == null) return null
        return JapaSession(
            completedAtEpochMillis = completedAt,
            dateEpochDay = dateEpochDay,
            mantraId = parts[INDEX_MANTRA],
            beads = beads,
            rounds = rounds,
            nakshatraNumber = parts[INDEX_NAKSHATRA].toIntOrNull(),
            tithiNumber = parts[INDEX_TITHI].toIntOrNull(),
        )
    }
}
