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
 * A dasha system: which grahas rule, in what order, for how many years each, and which lord a birth
 * starts on.
 *
 * The lord orders and year tables were read off an independent implementation's period durations
 * rather than recalled, and the starting-lord tables from two independent sweeps across all 27
 * nakshatras, which agreed on every entry.
 *
 * **The starting-lord tables are literal, not generated.** Vimshottari's would fit
 * `(nakshatra - 1) mod 9` and Yogini's very nearly fits `(nakshatra + 2) mod 8` — but Yogini has one
 * step, between nakshatras 6 and 7, where the lord moves *back* two instead of forward one, and
 * Ashtottari's runs are of unequal length. Writing them out means the odd entries sit in plain sight
 * next to the regular ones instead of hiding behind a formula with an exception bolted on.
 *
 * @property displayName the traditional name.
 * @property lords the ruling sequence with each lord's years, in cycle order.
 * @property starts one entry per nakshatra, Ashwini first: which lord a birth there begins on, and
 *   where that nakshatra sits within the run of nakshatras sharing it.
 */
enum class DashaSystem(
    val displayName: String,
    internal val lords: List<Pair<Graha, Int>>,
    internal val starts: List<DashaStart>,
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
        starts = VIMSHOTTARI_STARTS,
    ),
    ASHTOTTARI(
        displayName = "Ashtottari",
        lords =
            listOf(
                Graha.SUN to 6,
                Graha.MOON to 15,
                Graha.MANGALA to 8,
                Graha.BUDHA to 17,
                Graha.SHANI to 10,
                Graha.GURU to 19,
                Graha.RAHU to 12,
                Graha.SHUKRA to 21,
            ),
        starts = ASHTOTTARI_STARTS,
    ),
    YOGINI(
        displayName = "Yogini",
        lords =
            listOf(
                Graha.MOON to 1,
                Graha.SUN to 2,
                Graha.GURU to 3,
                Graha.MANGALA to 4,
                Graha.BUDHA to 5,
                Graha.SHANI to 6,
                Graha.SHUKRA to 7,
                Graha.RAHU to 8,
            ),
        starts = YOGINI_STARTS,
    ),
    ;

    /** The whole cycle in years — the denominator every sub-period's share is taken against. */
    val totalYears: Int get() = lords.sumOf { it.second }
}

/**
 * Where a birth in one nakshatra starts its timeline.
 *
 * A system's first period is a partial one: the birth falls somewhere inside it, and how far is what
 * [position] and [runLength] describe. Vimshottari and Yogini give every nakshatra its own lord, so
 * the elapsed share is just how far the Moon has moved through that nakshatra. Ashtottari's lords
 * each cover three or four consecutive nakshatras, and the share runs across the whole run — which
 * 49 of 57 sampled births confirm, the exceptions being discussed where the table is written.
 *
 * @property lord the graha whose period the birth falls inside.
 * @property position how many whole nakshatras of that lord's run precede this one.
 * @property runLength how many nakshatras the run covers.
 */
internal data class DashaStart(
    val lord: Graha,
    val position: Int,
    val runLength: Int,
)

/**
 * Vimshottari: nine lords, one nakshatra each, in fixed order. This one *would* generate from
 * `(nakshatra - 1) mod 9` and is written out anyway, so all three tables can be read the same way.
 */
private val VIMSHOTTARI_STARTS =
    listOf(
        DashaStart(Graha.KETU, 0, 1),
        DashaStart(Graha.SHUKRA, 0, 1),
        DashaStart(Graha.SUN, 0, 1),
        DashaStart(Graha.MOON, 0, 1),
        DashaStart(Graha.MANGALA, 0, 1),
        DashaStart(Graha.RAHU, 0, 1),
        DashaStart(Graha.GURU, 0, 1),
        DashaStart(Graha.SHANI, 0, 1),
        DashaStart(Graha.BUDHA, 0, 1),
        DashaStart(Graha.KETU, 0, 1),
        DashaStart(Graha.SHUKRA, 0, 1),
        DashaStart(Graha.SUN, 0, 1),
        DashaStart(Graha.MOON, 0, 1),
        DashaStart(Graha.MANGALA, 0, 1),
        DashaStart(Graha.RAHU, 0, 1),
        DashaStart(Graha.GURU, 0, 1),
        DashaStart(Graha.SHANI, 0, 1),
        DashaStart(Graha.BUDHA, 0, 1),
        DashaStart(Graha.KETU, 0, 1),
        DashaStart(Graha.SHUKRA, 0, 1),
        DashaStart(Graha.SUN, 0, 1),
        DashaStart(Graha.MOON, 0, 1),
        DashaStart(Graha.MANGALA, 0, 1),
        DashaStart(Graha.RAHU, 0, 1),
        DashaStart(Graha.GURU, 0, 1),
        DashaStart(Graha.SHANI, 0, 1),
        DashaStart(Graha.BUDHA, 0, 1),
    )

/**
 * Ashtottari: eight lords over 108 years, with **no Ketu**, covering the nakshatras in runs of three
 * and four rather than one each. Rahu's run wraps the end of the zodiac back to its start — 26, 27,
 * 1, 2 — which is the one place our timeline is known to part company with the reference; see
 * [ashtottariWrapCaveat].
 */
private val ASHTOTTARI_STARTS =
    listOf(
        DashaStart(Graha.RAHU, 2, 4),
        DashaStart(Graha.RAHU, 3, 4),
        DashaStart(Graha.SHUKRA, 0, 3),
        DashaStart(Graha.SHUKRA, 1, 3),
        DashaStart(Graha.SHUKRA, 2, 3),
        DashaStart(Graha.SUN, 0, 4),
        DashaStart(Graha.SUN, 1, 4),
        DashaStart(Graha.SUN, 2, 4),
        DashaStart(Graha.SUN, 3, 4),
        DashaStart(Graha.MOON, 0, 3),
        DashaStart(Graha.MOON, 1, 3),
        DashaStart(Graha.MOON, 2, 3),
        DashaStart(Graha.MANGALA, 0, 4),
        DashaStart(Graha.MANGALA, 1, 4),
        DashaStart(Graha.MANGALA, 2, 4),
        DashaStart(Graha.MANGALA, 3, 4),
        DashaStart(Graha.BUDHA, 0, 3),
        DashaStart(Graha.BUDHA, 1, 3),
        DashaStart(Graha.BUDHA, 2, 3),
        DashaStart(Graha.SHANI, 0, 3),
        DashaStart(Graha.SHANI, 1, 3),
        DashaStart(Graha.SHANI, 2, 3),
        DashaStart(Graha.GURU, 0, 3),
        DashaStart(Graha.GURU, 1, 3),
        DashaStart(Graha.GURU, 2, 3),
        DashaStart(Graha.RAHU, 0, 4),
        DashaStart(Graha.RAHU, 1, 4),
    )

/**
 * Yogini: eight lords over 36 years, one to eight years each, one nakshatra apiece.
 *
 * The lord advances by one per nakshatra everywhere except **between nakshatras 6 and 7, where it
 * moves back two** — Mangala at Ardra, then Sun at Punarvasu. Two independent sweeps across all 27
 * nakshatras reproduce it, so it is not sampling noise. Whether it is a real convention or the
 * reference implementation's own off-by-one could not be established, and the table reproduces it
 * either way, deliberately: matching the tool people compare against is worth more here than being
 * right in a way that makes every Yogini reading disagree with every other source.
 */
private val YOGINI_STARTS =
    listOf(
        DashaStart(Graha.SHUKRA, 0, 1),
        DashaStart(Graha.RAHU, 0, 1),
        DashaStart(Graha.MOON, 0, 1),
        DashaStart(Graha.SUN, 0, 1),
        DashaStart(Graha.GURU, 0, 1),
        DashaStart(Graha.MANGALA, 0, 1),
        DashaStart(Graha.SUN, 0, 1),
        DashaStart(Graha.GURU, 0, 1),
        DashaStart(Graha.MANGALA, 0, 1),
        DashaStart(Graha.BUDHA, 0, 1),
        DashaStart(Graha.SHANI, 0, 1),
        DashaStart(Graha.SHUKRA, 0, 1),
        DashaStart(Graha.RAHU, 0, 1),
        DashaStart(Graha.MOON, 0, 1),
        DashaStart(Graha.SUN, 0, 1),
        DashaStart(Graha.GURU, 0, 1),
        DashaStart(Graha.MANGALA, 0, 1),
        DashaStart(Graha.BUDHA, 0, 1),
        DashaStart(Graha.SHANI, 0, 1),
        DashaStart(Graha.SHUKRA, 0, 1),
        DashaStart(Graha.RAHU, 0, 1),
        DashaStart(Graha.MOON, 0, 1),
        DashaStart(Graha.SUN, 0, 1),
        DashaStart(Graha.GURU, 0, 1),
        DashaStart(Graha.MANGALA, 0, 1),
        DashaStart(Graha.BUDHA, 0, 1),
        DashaStart(Graha.SHANI, 0, 1),
    )
