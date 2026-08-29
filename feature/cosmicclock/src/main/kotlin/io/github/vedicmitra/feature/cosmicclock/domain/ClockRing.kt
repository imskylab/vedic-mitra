/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.cosmicclock.domain

import io.github.vedicmitra.core.astronomy.LimbWindow
import io.github.vedicmitra.core.astronomy.PanchangaConcept
import kotlin.time.Instant

/**
 * One ring of the Panchanga clock: a whole cycle, with the reader's current place in it.
 *
 * Every ring means the same thing — **position within this limb's cycle** — which is what lets the
 * five sit on one face without the angle changing meaning between them. Time of day is deliberately
 * not represented here; that is a different clock.
 *
 * @property concept what this ring measures, and the key to its explanation in `PanchangaPrimer`.
 * @property label the ring's name, as shown beside it.
 * @property segmentCount how many divisions the full cycle holds — 30 tithis, 7 varas, 27
 *   nakshatras, 27 yogas, 60 karanas.
 * @property activeIndex which division is current, **0-based** for drawing. The engine's own numbers
 *   are 1-based (except [io.github.vedicmitra.core.astronomy.Vara], which is an ordinal), so this is
 *   the one place that conversion happens.
 * @property activeName what the current division is called, e.g. "Shukla Chaturdashi".
 * @property window when the current division began and ends, or `null` when that is not known —
 *   which happens for vara at latitudes where the Sun does not rise, since the vedic day is bounded
 *   by sunrise rather than by an angle.
 */
data class ClockRing(
    val concept: PanchangaConcept,
    val label: String,
    val segmentCount: Int,
    val activeIndex: Int,
    val activeName: String,
    val window: LimbWindow?,
) {
    /**
     * How far through the current division we are, `[0, 1)`, or `null` if unknown.
     *
     * For the four angular limbs this is `LimbWindow.angularFraction` — progress by *angle*, which
     * is exact and does not need the boundary solved. For vara the engine already stores a temporal
     * fraction in that same field, deliberately, because the vedic day is bounded by sunrise and has
     * no driving angle. Both are the right notion of "how far through" for their own ring, so the
     * clock can use one field throughout.
     */
    val fraction: Double? get() = window?.angularFraction

    /** When the current division gives way to the next, or `null` if unknown. */
    val endsAt: Instant? get() = window?.end

    init {
        require(segmentCount > 0) { "$label needs at least one segment, got $segmentCount" }
        require(activeIndex in 0 until segmentCount) {
            "$label active index $activeIndex is outside 0..${segmentCount - 1}"
        }
    }
}

/**
 * The current pada, drawn *inside* the active nakshatra segment rather than as a ring of its own.
 *
 * A pada ring would carry 108 divisions — the least legible thing on the face, for a subdivision
 * most readers never ask about directly.
 *
 * **Nesting it inside the active nakshatra arc was tried and does not work either.** That arc spans
 * 13.3°, so a quarter of it is 3.3° — about five density-independent pixels at a 160dp radius, which
 * is not four distinguishable things. It is carried here for the limb list and the hub, which have
 * room to name it, and the clock face does not draw it at all.
 *
 * @property index which quarter of the nakshatra, 0-based.
 * @property window when this pada began and ends.
 */
data class PadaMarker(
    val index: Int,
    val window: LimbWindow?,
) {
    init {
        require(index in 0 until PADAS_PER_NAKSHATRA) { "pada index $index is outside 0..3" }
    }

    companion object {
        const val PADAS_PER_NAKSHATRA = 4
    }
}
