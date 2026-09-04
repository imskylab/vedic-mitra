/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.astronomy

import kotlin.time.Instant

/** Whether a muhurta window is favourable or to be avoided. */
enum class MuhurtaQuality { AUSPICIOUS, INAUSPICIOUS }

/**
 * Which of the day's named windows this is — the window's **identity**, as distinct from what it is
 * called.
 *
 * [id] is what a reminder is keyed on, and it is deliberately not a display name. A key built from a
 * label breaks the moment the label is translated or respelled: the same window computes a different
 * key and the user's reminder is orphaned rather than renamed (see ADR 0019, and the reason this
 * enum exists). These ids are therefore **frozen** — changing one is a data migration, not a rename.
 *
 * [label] is the English display name, and is free to change.
 *
 * Note that Dur Muhurta is one kind even on the Saturdays when it occurs twice. The two occurrences
 * used to be named "Dur Muhurta 1" and "Dur Muhurta 2" and so keyed differently from the single
 * "Dur Muhurta" of every other weekday — meaning a reminder set on a Sunday silently did not match
 * on a Saturday. Keying on the kind fixes that; the occurrences keep their numbered [Muhurta.name]
 * for display.
 */
enum class MuhurtaKind(
    val id: String,
    val label: String,
) {
    BRAHMA("brahma", "Brahma Muhurta"),
    ABHIJIT("abhijit", "Abhijit Muhurta"),
    RAHU_KALAM("rahu-kalam", "Rahu Kalam"),
    YAMAGANDA("yamaganda", "Yamaganda"),
    GULIKA_KALAM("gulika-kalam", "Gulika Kalam"),
    DUR_MUHURTA("dur-muhurta", "Dur Muhurta"),
    VARJYAM("varjyam", "Varjyam"),
}

/**
 * A named time window of the day with an astrological quality — an auspicious muhurta (e.g. Brahma,
 * Abhijit) or an inauspicious kalam (e.g. Rahu Kalam).
 *
 * @property kind which window this is, and the only thing anything durable may key on.
 * @property name the traditional name of the window, for display. Usually [MuhurtaKind.label], but
 *   not always: the two Saturday Dur Muhurtas are numbered to tell them apart on screen.
 * @property start when the window begins.
 * @property end when the window ends.
 * @property quality whether the window is auspicious or inauspicious.
 */
data class Muhurta(
    val kind: MuhurtaKind,
    val name: String,
    val start: Instant,
    val end: Instant,
    val quality: MuhurtaQuality,
)
