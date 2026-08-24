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

/**
 * Graha drishti — which houses a graha looks upon, counted in whole signs from its own.
 *
 * Every graha aspects the seventh from itself. Three have additional special aspects, and these are
 * the ones that make drishti worth having at all:
 *
 * | Graha   | Aspects |
 * |---------|---------|
 * | all     | 7th     |
 * | Mangala | 4th, 8th |
 * | Guru    | 5th, 9th |
 * | Shani   | 3rd, 10th |
 *
 * **This is Vedic drishti, not the Western aspect set.** They are not the same thing and must not be
 * mixed: Vedic drishti is asymmetric — Shani looks upon the third from itself, but the graha there
 * does not look back — whereas a Western trine or square is a mutual angular relationship. An app
 * that computed one and called it the other would be quietly wrong everywhere it mattered.
 *
 * **Rahu and Ketu are deliberately absent.** Authorities disagree on whether the nodes aspect at all,
 * and those who say they do disagree on which houses. There is no answer here that could be called
 * the classical one, so rather than pick one and present it as settled, [aspects] reports `false` for
 * them. Anything that needs node drishti should say which convention it is adopting and why.
 *
 * Whole-sign, matching the houses everywhere else in this engine. Degree-based drishti (with partial
 * strengths, as shadbala's *drik bala* wants) is a different and larger calculation.
 */
internal object Drishti {
    private val SPECIAL: Map<Graha, Set<Int>> =
        mapOf(
            Graha.MANGALA to setOf(4, 8),
            Graha.GURU to setOf(5, 9),
            Graha.SHANI to setOf(3, 10),
        )

    /** Whether [graha], sitting in [fromRasiIndex], aspects the sign at [toRasiIndex]. */
    fun aspects(
        graha: Graha,
        fromRasiIndex: Int,
        toRasiIndex: Int,
    ): Boolean {
        if (graha == Graha.RAHU || graha == Graha.KETU) return false
        val house = houseFrom(fromRasiIndex, toRasiIndex)
        return house == 7 || house in SPECIAL[graha].orEmpty()
    }

    /**
     * Whether [graha] either sits with or aspects the sign at [toRasiIndex] — the "conjunct or
     * aspected by" phrasing classical rules use, which treats the two as one influence.
     */
    fun influences(
        graha: Graha,
        fromRasiIndex: Int,
        toRasiIndex: Int,
    ): Boolean = fromRasiIndex == toRasiIndex || aspects(graha, fromRasiIndex, toRasiIndex)
}
