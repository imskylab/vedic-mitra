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

/** The twelve rashis (zodiac signs), index 0 = Mesha .. 11 = Meena. */
val RASHI_NAMES: List<String> =
    listOf(
        "Mesha",
        "Vrishabha",
        "Mithuna",
        "Karka",
        "Simha",
        "Kanya",
        "Tula",
        "Vrishchika",
        "Dhanu",
        "Makara",
        "Kumbha",
        "Meena",
    )

/** A graha (planet) whose rashi the app tracks, in the classical order Sun … Ketu. */
enum class Graha(
    val displayName: String,
) {
    SUN("Sun"),
    MOON("Moon"),
    MANGALA("Mangala"),
    BUDHA("Budha"),
    GURU("Guru"),
    SHUKRA("Shukra"),
    SHANI("Shani"),
    RAHU("Rahu"),
    KETU("Ketu"),
}

/**
 * A rashi (zodiac sign) a graha occupies.
 *
 * @property index 0 = Mesha .. 11 = Meena.
 * @property name the traditional Sanskrit name.
 */
data class Rasi(
    val index: Int,
    val name: String,
)

/**
 * Whole-sign house count from [fromRasiIndex] to [toRasiIndex], 1..12.
 *
 * The sign counted from is always house 1, so a graha in the same sign as the reference point is in
 * the first house, not the twelfth. Shared because every framing in the app — houses from the lagna,
 * from the Moon, from a varga lagna, from Venus — is this one count with a different starting point,
 * and three private copies of it had already accumulated.
 */
internal fun houseFrom(
    fromRasiIndex: Int,
    toRasiIndex: Int,
): Int = ((toRasiIndex - fromRasiIndex + RASHI_NAMES.size) % RASHI_NAMES.size) + 1

/**
 * A graha's current rashi and its next rashi ingress (pravesh).
 *
 * @property graha which planet.
 * @property rasi the rashi it currently occupies.
 * @property pravesh the instant it next changes rashi, or `null` if none within the search horizon.
 */
data class GrahaPosition(
    val graha: Graha,
    val rasi: Rasi,
    val pravesh: Instant?,
)

/** The rashi positions of the tracked grahas, in [Graha] order. */
data class PlanetaryPositions(
    val positions: List<GrahaPosition>,
)
