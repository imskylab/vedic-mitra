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
 * Encodes a [PersistedReminder] to and from a single string so a set of them can live in a
 * Preferences DataStore. Fields are joined with the ASCII unit-separator (code 0x1F), which never
 * occurs in the app-generated ids or notification text; the body field (last) absorbs any stray
 * separators on decode.
 *
 * The current format has five fields (`id | triggerAt | title | nickname | body`). Records written
 * by an earlier four-field version (`id | triggerAt | title | body`, no nickname) are still decoded,
 * so upgrading never drops a saved reminder.
 */
internal object ReminderCodec {
    private const val SEPARATOR_CODE = 0x1F
    private const val FIELD_COUNT = 5
    private const val LEGACY_FIELD_COUNT = 4
    private val separator = Char(SEPARATOR_CODE).toString()

    fun encode(reminder: PersistedReminder): String =
        listOf(
            reminder.id,
            reminder.triggerAtEpochMillis.toString(),
            reminder.title,
            reminder.nickname.orEmpty(),
            reminder.body,
        ).joinToString(separator)

    /** Decodes a value produced by [encode] (or the legacy four-field form), or `null` if malformed. */
    fun decode(value: String): PersistedReminder? {
        val parts = value.split(separator, limit = FIELD_COUNT)
        val triggerAt = parts.getOrNull(1)?.toLongOrNull() ?: return null
        return when (parts.size) {
            LEGACY_FIELD_COUNT ->
                PersistedReminder(
                    id = parts[0],
                    triggerAtEpochMillis = triggerAt,
                    title = parts[2],
                    body = parts[3],
                    nickname = null,
                )

            FIELD_COUNT ->
                PersistedReminder(
                    id = parts[0],
                    triggerAtEpochMillis = triggerAt,
                    title = parts[2],
                    body = parts[4],
                    nickname = parts[3].ifBlank { null },
                )

            else -> null
        }
    }
}
