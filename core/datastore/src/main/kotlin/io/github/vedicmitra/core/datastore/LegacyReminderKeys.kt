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
 * Translates reminder keys written before muhurta windows had a stable identity.
 *
 * Until then a reminder was keyed `muhurta:<display name>` — `"muhurta:Brahma Muhurta"`. That made a
 * piece of UI copy into persisted state: respelling a label, or translating it for a locale, made
 * the same window compute a different key, so the reminder was not renamed but **orphaned**. It
 * stopped being renewed and the Reminders screen could not reconcile it, which reads to a user as
 * the alarm feature quietly breaking. Keys are now `muhurta:<MuhurtaKind.id>`, which no display
 * decision can move.
 *
 * The names below are **frozen history**, not live vocabulary — they are the strings that were
 * actually written to a device, so they are hardcoded here rather than derived from anything that
 * can still change. That is also why this lives in `:core:datastore`, which does not depend on the
 * astronomy engine: the mapping must keep working even if every one of these labels is rewritten.
 *
 * Translation is applied on **read** rather than in a one-shot migration pass, so it needs no
 * app-start hook and cannot half-complete. A record keeps its old bytes on disk until whatever set
 * it belongs to is next written, but always decodes to the current key, and every write uses the
 * current key. Applying it twice is the same as applying it once.
 */
internal object LegacyReminderKeys {
    private const val PREFIX = "muhurta:"

    /**
     * Old key suffix (the display name) to current [io.github.vedicmitra.core.astronomy.MuhurtaKind]
     * id. Never edit an entry: these describe what is already on disk.
     *
     * Both numbered Dur Muhurtas map to the one kind. They were only ever numbered to tell the two
     * Saturday occurrences apart on screen, and keying them separately meant a reminder set on a
     * Sunday — where the window is plain "Dur Muhurta" — silently failed to match on a Saturday.
     */
    private val BY_LEGACY_NAME =
        mapOf(
            "Brahma Muhurta" to "brahma",
            "Abhijit Muhurta" to "abhijit",
            "Rahu Kalam" to "rahu-kalam",
            "Yamaganda" to "yamaganda",
            "Gulika Kalam" to "gulika-kalam",
            "Dur Muhurta" to "dur-muhurta",
            "Dur Muhurta 1" to "dur-muhurta",
            "Dur Muhurta 2" to "dur-muhurta",
            "Varjyam" to "varjyam",
        )

    /**
     * [key] translated to its current form, or returned unchanged if it is already current or is not
     * a muhurta key at all — `choghadiya:` keys carry an enum name and `tithi:` keys carry tithi
     * numbers, so neither was ever built from a label.
     */
    fun canonical(key: String): String {
        if (!key.startsWith(PREFIX)) return key
        val id = BY_LEGACY_NAME[key.removePrefix(PREFIX)] ?: return key
        return PREFIX + id
    }
}
