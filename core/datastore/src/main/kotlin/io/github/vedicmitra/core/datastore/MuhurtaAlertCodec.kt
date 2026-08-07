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

import io.github.vedicmitra.core.common.model.AlertStyle

/**
 * Encodes a [MuhurtaAlert] to and from a single string so a set of them can live in a Preferences
 * DataStore. Fields are joined with the ASCII unit-separator (code 0x1F), mirroring [ReminderCodec].
 */
internal object MuhurtaAlertCodec {
    private const val SEPARATOR_CODE = 0x1F
    private const val FIELD_COUNT = 2
    private val separator = Char(SEPARATOR_CODE).toString()

    fun encode(alert: MuhurtaAlert): String = listOf(alert.name, alert.alert.name).joinToString(separator)

    /** Decodes a value produced by [encode], or `null` if it is malformed. */
    fun decode(value: String): MuhurtaAlert? {
        val parts = value.split(separator, limit = FIELD_COUNT)
        if (parts.size != FIELD_COUNT) return null
        val style = AlertStyle.entries.firstOrNull { it.name == parts[1] } ?: return null
        return MuhurtaAlert(name = parts[0], alert = style)
    }
}
