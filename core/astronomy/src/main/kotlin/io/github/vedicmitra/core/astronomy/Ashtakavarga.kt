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
 * Ashtakavarga — how much benefic support each sign has, counted in bindus.
 *
 * Seven grahas each contribute a *binnashtakavarga*: for that graha, every one of eight reference
 * points (the seven grahas and the lagna) marks certain houses **counted from itself** as benefic,
 * and a sign collects one bindu for each reference point that marks it. So a sign holds 0 to 8
 * bindus in one graha's binnashtakavarga, and the seven summed give the *sarvashtakavarga*, which
 * always totals **337** across the twelve signs.
 *
 * There is no new astronomy here at all — only the rashi each body occupies, which the chart already
 * knows. That is what makes this cheap to compute and worth having: transits through a sign with 30
 * bindus are read very differently from the same transit through a sign with 20.
 *
 * ## Where the tables come from
 *
 * Read off an independent implementation's *prastara* — its per-reference-point breakdown — rather
 * than recalled, then replayed against **14 further charts spanning four cities and 1968 to 2083:
 * 98 binnashtakavarga rows and 14 sarvashtakavarga rows, with no disagreements.**
 *
 * Two independent checks say the reading is right rather than merely self-consistent. Each graha's
 * own total comes out at the classical figure — Sun 48, Moon 49, Mars 39, Mercury 54, Jupiter 56,
 * Venus 52, Saturn 39 — and those sum to 337, which is the number every text quotes. Neither was put
 * in; both fell out.
 *
 * Rahu and Ketu take no part. Neither has a binnashtakavarga of its own nor contributes a reference
 * point, which is why eight references cover seven grahas plus the lagna.
 */
object Ashtakavarga {
    /**
     * The bindus each sign holds in [graha]'s binnashtakavarga, Mesha first.
     *
     * @param graha one of the seven — the nodes have none.
     * @param signOf where each reference point sits, as a rashi index 0..11.
     */
    fun binna(
        graha: Graha,
        signOf: (AshtakavargaReference) -> Int,
    ): List<Int> {
        val table = BENEFIC_HOUSES[graha] ?: return List(RASHI_NAMES.size) { 0 }
        return List(RASHI_NAMES.size) { sign ->
            AshtakavargaReference.entries.count { reference ->
                houseFrom(signOf(reference), sign) in table[reference].orEmpty()
            }
        }
    }

    /**
     * The sarvashtakavarga: the seven binnashtakavargas summed sign by sign, totalling 337.
     *
     * @param signOf where each reference point sits, as a rashi index 0..11.
     */
    fun sarva(signOf: (AshtakavargaReference) -> Int): List<Int> {
        val rows = CONTRIBUTORS.map { binna(it, signOf) }
        return List(RASHI_NAMES.size) { sign -> rows.sumOf { it[sign] } }
    }

    /** The seven grahas with a binnashtakavarga of their own, in the classical order. */
    val CONTRIBUTORS: List<Graha> =
        listOf(
            Graha.SUN,
            Graha.MOON,
            Graha.MANGALA,
            Graha.BUDHA,
            Graha.GURU,
            Graha.SHUKRA,
            Graha.SHANI,
        )

    /** What every sarvashtakavarga sums to; a useful invariant to assert against. */
    const val SARVA_TOTAL: Int = 337
}

/**
 * A point a bindu is counted from.
 *
 * The seven grahas and the lagna. The nodes are absent because they take no part in ashtakavarga at
 * all — neither contributing a reference point nor having a binnashtakavarga of their own.
 */
enum class AshtakavargaReference(
    val displayName: String,
) {
    SUN("Sun"),
    MOON("Moon"),
    MANGALA("Mangala"),
    BUDHA("Budha"),
    GURU("Guru"),
    SHUKRA("Shukra"),
    SHANI("Shani"),
    LAGNA("Lagna"),
}

/**
 * Which houses, counted from each reference point, earn a bindu — the whole of ashtakavarga.
 *
 * Written out in full rather than compressed. Every row is an independent piece of classical data
 * with no pattern to lean on, so a reader checking one line against a printed table can find it
 * where they expect it.
 */
private val BENEFIC_HOUSES: Map<Graha, Map<AshtakavargaReference, Set<Int>>> =
    mapOf(
        // Sun: 48 bindus in all.
        Graha.SUN to
            mapOf(
                AshtakavargaReference.SUN to setOf(1, 2, 4, 7, 8, 9, 10, 11),
                AshtakavargaReference.MOON to setOf(3, 6, 10, 11),
                AshtakavargaReference.MANGALA to setOf(1, 2, 4, 7, 8, 9, 10, 11),
                AshtakavargaReference.BUDHA to setOf(3, 5, 6, 9, 10, 11, 12),
                AshtakavargaReference.GURU to setOf(5, 6, 9, 11),
                AshtakavargaReference.SHUKRA to setOf(6, 7, 12),
                AshtakavargaReference.SHANI to setOf(1, 2, 4, 7, 8, 9, 10, 11),
                AshtakavargaReference.LAGNA to setOf(3, 4, 6, 10, 11, 12),
            ),
        // Moon: 49 bindus in all.
        Graha.MOON to
            mapOf(
                AshtakavargaReference.SUN to setOf(3, 6, 7, 8, 10, 11),
                AshtakavargaReference.MOON to setOf(1, 3, 6, 7, 9, 10, 11),
                AshtakavargaReference.MANGALA to setOf(2, 3, 5, 6, 10, 11),
                AshtakavargaReference.BUDHA to setOf(1, 3, 4, 5, 7, 8, 10, 11),
                AshtakavargaReference.GURU to setOf(1, 2, 4, 7, 8, 10, 11),
                AshtakavargaReference.SHUKRA to setOf(3, 4, 5, 7, 9, 10, 11),
                AshtakavargaReference.SHANI to setOf(3, 5, 6, 11),
                AshtakavargaReference.LAGNA to setOf(3, 6, 10, 11),
            ),
        // Mars: 39 bindus in all.
        Graha.MANGALA to
            mapOf(
                AshtakavargaReference.SUN to setOf(3, 5, 6, 10, 11),
                AshtakavargaReference.MOON to setOf(3, 6, 11),
                AshtakavargaReference.MANGALA to setOf(1, 2, 4, 7, 8, 10, 11),
                AshtakavargaReference.BUDHA to setOf(3, 5, 6, 11),
                AshtakavargaReference.GURU to setOf(6, 10, 11, 12),
                AshtakavargaReference.SHUKRA to setOf(6, 8, 11, 12),
                AshtakavargaReference.SHANI to setOf(1, 4, 7, 8, 9, 10, 11),
                AshtakavargaReference.LAGNA to setOf(1, 3, 6, 10, 11),
            ),
        // Mercury: 54 bindus in all.
        Graha.BUDHA to
            mapOf(
                AshtakavargaReference.SUN to setOf(5, 6, 9, 11, 12),
                AshtakavargaReference.MOON to setOf(2, 4, 6, 8, 10, 11),
                AshtakavargaReference.MANGALA to setOf(1, 2, 4, 7, 8, 9, 10, 11),
                AshtakavargaReference.BUDHA to setOf(1, 3, 5, 6, 9, 10, 11, 12),
                AshtakavargaReference.GURU to setOf(6, 8, 11, 12),
                AshtakavargaReference.SHUKRA to setOf(1, 2, 3, 4, 5, 8, 9, 11),
                AshtakavargaReference.SHANI to setOf(1, 2, 4, 7, 8, 9, 10, 11),
                AshtakavargaReference.LAGNA to setOf(1, 2, 4, 6, 8, 10, 11),
            ),
        // Jupiter: 56 bindus in all.
        Graha.GURU to
            mapOf(
                AshtakavargaReference.SUN to setOf(1, 2, 3, 4, 7, 8, 9, 10, 11),
                AshtakavargaReference.MOON to setOf(2, 5, 7, 9, 11),
                AshtakavargaReference.MANGALA to setOf(1, 2, 4, 7, 8, 10, 11),
                AshtakavargaReference.BUDHA to setOf(1, 2, 4, 5, 6, 9, 10, 11),
                AshtakavargaReference.GURU to setOf(1, 2, 3, 4, 7, 8, 10, 11),
                AshtakavargaReference.SHUKRA to setOf(2, 5, 6, 9, 10, 11),
                AshtakavargaReference.SHANI to setOf(3, 5, 6, 12),
                AshtakavargaReference.LAGNA to setOf(1, 2, 4, 5, 6, 7, 9, 10, 11),
            ),
        // Venus: 52 bindus in all.
        Graha.SHUKRA to
            mapOf(
                AshtakavargaReference.SUN to setOf(8, 11, 12),
                AshtakavargaReference.MOON to setOf(1, 2, 3, 4, 5, 8, 9, 11, 12),
                AshtakavargaReference.MANGALA to setOf(3, 4, 6, 9, 11, 12),
                AshtakavargaReference.BUDHA to setOf(3, 5, 6, 9, 11),
                AshtakavargaReference.GURU to setOf(5, 8, 9, 10, 11),
                AshtakavargaReference.SHUKRA to setOf(1, 2, 3, 4, 5, 8, 9, 10, 11),
                AshtakavargaReference.SHANI to setOf(3, 4, 5, 8, 9, 10, 11),
                AshtakavargaReference.LAGNA to setOf(1, 2, 3, 4, 5, 8, 9, 11),
            ),
        // Saturn: 39 bindus in all.
        Graha.SHANI to
            mapOf(
                AshtakavargaReference.SUN to setOf(1, 2, 4, 7, 8, 10, 11),
                AshtakavargaReference.MOON to setOf(3, 6, 11),
                AshtakavargaReference.MANGALA to setOf(3, 5, 6, 10, 11, 12),
                AshtakavargaReference.BUDHA to setOf(6, 8, 9, 10, 11, 12),
                AshtakavargaReference.GURU to setOf(5, 6, 11, 12),
                AshtakavargaReference.SHUKRA to setOf(6, 11, 12),
                AshtakavargaReference.SHANI to setOf(3, 5, 6, 11),
                AshtakavargaReference.LAGNA to setOf(1, 3, 4, 6, 10, 11),
            ),
    )
