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

/**
 * The seven Choghadiya, listed in their fixed **cyclic order** (Udveg → Char → Labh → Amrit → Kaal
 * → Shubh → Rog → repeat). Each day's and night's eight windows step through this cycle from a
 * weekday-determined starting position; see [choghadiyaOf].
 *
 * Nature: **Amrit, Shubh, Labh** are auspicious. **Char** ("chal", movable) is traditionally
 * neutral but generally usable, so it is classified auspicious here to fit the app's binary
 * [MuhurtaQuality]. **Udveg, Kaal, Rog** are inauspicious.
 *
 * @property label the display name.
 * @property quality the auspicious/inauspicious nature.
 */
enum class ChoghadiyaName(
    val label: String,
    val quality: MuhurtaQuality,
) {
    UDVEG("Udveg", MuhurtaQuality.INAUSPICIOUS),
    CHAR("Char", MuhurtaQuality.AUSPICIOUS),
    LABH("Labh", MuhurtaQuality.AUSPICIOUS),
    AMRIT("Amrit", MuhurtaQuality.AUSPICIOUS),
    KAAL("Kaal", MuhurtaQuality.INAUSPICIOUS),
    SHUBH("Shubh", MuhurtaQuality.AUSPICIOUS),
    ROG("Rog", MuhurtaQuality.INAUSPICIOUS),
}

/**
 * A single Choghadiya window — one of the eight equal divisions of the day (sunrise→sunset) or the
 * night (sunset→next sunrise).
 *
 * @property name which of the seven Choghadiya this window is.
 * @property start when the window begins.
 * @property end when the window ends.
 * @property isDay `true` for a daytime window, `false` for a night window.
 */
data class Choghadiya(
    val name: ChoghadiyaName,
    val start: Instant,
    val end: Instant,
    val isDay: Boolean,
) {
    /** The window's auspicious/inauspicious nature, derived from [name]. */
    val quality: MuhurtaQuality get() = name.quality
}
