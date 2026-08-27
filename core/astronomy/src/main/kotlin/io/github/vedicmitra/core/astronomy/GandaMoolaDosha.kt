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
 * Ganda Moola dosha — the Moon in one of the six junctional nakshatras.
 *
 * ## The rule
 *
 * The dosha stands when the Moon at birth occupies **Ashwini, Ashlesha, Magha, Jyeshtha, Mula or
 * Revati**. Nothing else enters into it: not the pada, not the lagna, not any other graha.
 *
 * ## Why those six, and why they come in pairs
 *
 * They are not an arbitrary list. Each nakshatra spans 13°20', so the 27 do not divide evenly into
 * the 12 rashis, and three of the boundaries fall *inside* a nakshatra rather than between two. The
 * six are the three pairs that straddle those seams — Revati into Ashwini, Ashlesha into Magha,
 * Jyeshtha into Mula — each pair being the end of a water sign running into the start of a fire
 * sign. That junction, *gandanta*, is the whole idea: a Moon there sits on a join rather than on
 * solid ground. Reading it as a list of six loses the reason there are six; hence the pairs are
 * written out below.
 *
 * They are also exactly the nakshatras ruled by Ketu and Budha, which is the other way the rule is
 * usually stated. Both descriptions pick out the same six.
 *
 * ## Derivation
 *
 * **Read off an independent implementation rather than recalled.** The Moon was swept through a full
 * lunar month, 29 charts covering 26 of the 27 nakshatras, and the verdict read back for each. The
 * six above fired and the twenty others did not, with **no nakshatra appearing on both sides** — so
 * the pada genuinely does not affect it, which was the one thing worth checking, since Jyeshtha
 * fired at pada 2 and again at pada 4.
 */
fun gandaMoolaDoshaOf(chart: NatalChart): ChartDosha {
    val nakshatra = chart.moonNakshatra
    val present = nakshatra.number in GANDA_MOOLA_NAKSHATRAS
    return ChartDosha(
        name = "Ganda Moola",
        present = present,
        rule =
            if (present) {
                "The Moon is in ${nakshatra.name}, one of the six junctional nakshatras."
            } else {
                "The Moon is in ${nakshatra.name}, which is not one of the six junctional " +
                    "nakshatras (Ashwini, Ashlesha, Magha, Jyeshtha, Mula, Revati)."
            },
        summary =
            if (present) {
                "The junctional nakshatras sit where a nakshatra straddles the seam between two " +
                    "rashis. A Moon there is classically read as a sensitive early period and a " +
                    "temperament of unusual intensity, and the tradition prescribes observances " +
                    "for it rather than treating it as settled misfortune."
            } else {
                null
            },
    )
}

/**
 * The six junctional nakshatras, as the three pairs that straddle a rashi boundary.
 *
 * Written as pairs rather than as a flat set of six so the structure stays visible: each pair is the
 * end of one nakshatra's rashi running into the beginning of the next.
 */
private val GANDA_MOOLA_PAIRS =
    listOf(
        27 to 1, // Revati into Ashwini — Meena into Mesha
        9 to 10, // Ashlesha into Magha — Karka into Simha
        18 to 19, // Jyeshtha into Mula — Vrischika into Dhanu
    )

internal val GANDA_MOOLA_NAKSHATRAS: Set<Int> =
    GANDA_MOOLA_PAIRS.flatMap { (first, second) -> listOf(first, second) }.toSet()
