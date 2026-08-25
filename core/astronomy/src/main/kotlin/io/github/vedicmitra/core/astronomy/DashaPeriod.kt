/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

@file:Suppress("MagicNumber")

package io.github.vedicmitra.core.astronomy

import kotlin.time.Instant

/**
 * One period of a dasha timeline, at whatever depth.
 *
 * Mahadasha, antardasha and pratyantardasha were three names for one thing, and they used to be two
 * types here with the nesting written out by hand. They are the same rule applied again: a period
 * divides into sub-periods that run through the system's lord sequence starting from its own lord,
 * each taking a share of the parent proportional to that lord's own dasha years. Nothing about that
 * changes with depth, so neither does the code — [subPeriods] is the whole of it, and a fourth level
 * would be free if anyone wanted one.
 *
 * Checked against an independent implementation at all three levels: **729 of 729 periods matched by
 * lord and order**, for a birth whose first mahadasha is a partial one.
 *
 * @property lord the graha ruling this period.
 * @property start when it begins.
 * @property end when it ends, and the next lord's period begins.
 * @property level 1 for a mahadasha, 2 for an antardasha, 3 for a pratyantardasha.
 * @property system which dasha system this belongs to — it decides the lord sequence and the shares.
 */
data class DashaPeriod(
    val lord: Graha,
    val start: Instant,
    val end: Instant,
    val level: Int,
    val system: DashaSystem = DashaSystem.VIMSHOTTARI,
) {
    /**
     * The sub-periods this divides into, or empty at the deepest level offered.
     *
     * Derived rather than stored, for the same reason the Spashta Graha columns are: they are a pure
     * function of the lord and the span, and storing them would let a period carry sub-periods that
     * contradict it.
     *
     * The depth is capped because the recursion has no natural floor — the rule keeps applying, and
     * the fourth level (sookshma) divides a pratyantardasha of a few days into pieces of a few hours,
     * which is finer than a birth time known to the minute can support.
     */
    val subPeriods: List<DashaPeriod> get() = if (level >= DEEPEST_LEVEL) emptyList() else subPeriodsOf(this)

    /** The nine antardashas, kept as a name because that is what a reader asks for. */
    val antardashas: List<DashaPeriod> get() = subPeriods

    private companion object {
        const val DEEPEST_LEVEL = 3
    }
}

/**
 * A dasha system: which grahas rule, in what order, for how many years each.
 *
 * Only Vimshottari is here. Ashtottari and Yogini were derived alongside it and are **deliberately
 * withheld** — see the note in [VimshottariCalculator]'s file for exactly what did not check out.
 *
 * @property displayName the traditional name.
 * @property lords the ruling sequence with each lord's years, in cycle order.
 */
enum class DashaSystem(
    val displayName: String,
    internal val lords: List<Pair<Graha, Int>>,
) {
    VIMSHOTTARI(
        displayName = "Vimshottari",
        lords =
            listOf(
                Graha.KETU to 7,
                Graha.SHUKRA to 20,
                Graha.SUN to 6,
                Graha.MOON to 10,
                Graha.MANGALA to 7,
                Graha.RAHU to 18,
                Graha.GURU to 16,
                Graha.SHANI to 19,
                Graha.BUDHA to 17,
            ),
    ),
    ;

    /** The whole cycle in years — the denominator every sub-period's share is taken against. */
    val totalYears: Int get() = lords.sumOf { it.second }
}
