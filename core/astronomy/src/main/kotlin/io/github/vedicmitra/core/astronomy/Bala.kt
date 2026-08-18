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
 * The classical Moon-based "strength" of a day for a person, in three grades. Used by both the
 * muhurta scorer (as score deltas) and the daily rashifal (as the day's verdict). [STRONG] is
 * favourable, [WEAK] is to be avoided, and [NEUTRAL] is neither.
 */
enum class Bala {
    STRONG,
    NEUTRAL,
    WEAK,
}

// The nine taras, in order, counted from the birth star. Favourable: Sampat, Kshema, Sadhaka, Mitra,
// Ati-Mitra; unfavourable: Vipat, Pratyari, Vadha; Janma (the birth star itself) is neutral.
internal val TARA_NAMES: List<String> =
    listOf("Janma", "Sampat", "Vipat", "Kshema", "Pratyari", "Sadhaka", "Vadha", "Mitra", "Ati-Mitra")
private val FAVOURABLE_TARAS = setOf(2, 4, 6, 8, 9)
private val UNFAVOURABLE_TARAS = setOf(3, 5, 7)

/**
 * Tarabala — the day's nakshatra counted from a birth star.
 *
 * @property number which of the nine taras (1 = Janma .. 9 = Ati-Mitra).
 * @property name the tara's traditional name.
 * @property strength whether the tara is favourable, neutral, or to be avoided.
 */
data class Tara(
    val number: Int,
    val name: String,
    val strength: Bala,
)

/** The [Tara] the [dayNakshatra] is, counted from [birthNakshatra] (both 1..27). */
internal fun taraBetween(
    dayNakshatra: Int,
    birthNakshatra: Int,
): Tara {
    val count = ((dayNakshatra - birthNakshatra + 27) % 27) + 1
    val number = ((count - 1) % 9) + 1
    val strength =
        when (number) {
            in FAVOURABLE_TARAS -> Bala.STRONG
            in UNFAVOURABLE_TARAS -> Bala.WEAK
            else -> Bala.NEUTRAL
        }
    return Tara(number = number, name = TARA_NAMES[number - 1], strength = strength)
}

// Chandrabala positions (1..12) of a Moon sign counted from a reference Moon sign. The 1, 3, 6, 7, 10
// and 11 positions are strong; 4, 8 and 12 are weak; the rest are neutral.
private val FAVOURABLE_CHANDRA = setOf(1, 3, 6, 7, 10, 11)
private val WEAK_CHANDRA = setOf(4, 8, 12)

/** The position (1..12) of Moon sign [dayMoonRasi] counted from reference sign [fromMoonRasi] (both 0..11). */
internal fun chandraPosition(
    dayMoonRasi: Int,
    fromMoonRasi: Int,
): Int = ((dayMoonRasi - fromMoonRasi + 12) % 12) + 1

/** The [Bala] strength of a Chandrabala [position] (1..12). */
internal fun chandraStrength(position: Int): Bala =
    when (position) {
        in FAVOURABLE_CHANDRA -> Bala.STRONG
        in WEAK_CHANDRA -> Bala.WEAK
        else -> Bala.NEUTRAL
    }
