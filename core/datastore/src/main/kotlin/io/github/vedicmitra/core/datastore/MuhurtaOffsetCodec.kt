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
 * Encodes a [MuhurtaOffset] to and from a single string so a set of them can live in a Preferences
 * DataStore. Fields are joined with the ASCII unit-separator (code 0x1F), mirroring [ReminderCodec].
 */
internal object MuhurtaOffsetCodec {
    private const val SEPARATOR_CODE = 0x1F
    private const val FIELD_COUNT = 2
    private val separator = Char(SEPARATOR_CODE).toString()

    fun encode(offset: MuhurtaOffset): String =
        listOf(offset.name, offset.offsetMinutes.toString()).joinToString(separator)

    /** Decodes a value produced by [encode], or `null` if it is malformed. */
    fun decode(value: String): MuhurtaOffset? {
        val parts = value.split(separator, limit = FIELD_COUNT)
        if (parts.size != FIELD_COUNT) return null
        val minutes = parts[1].toIntOrNull() ?: return null
        return MuhurtaOffset(name = parts[0], offsetMinutes = minutes)
    }
}
