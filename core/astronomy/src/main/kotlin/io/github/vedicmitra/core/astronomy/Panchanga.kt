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

/** The lunar fortnight: waxing (Shukla) or waning (Krishna). */
enum class Paksha { SHUKLA, KRISHNA }

/**
 * A tithi — one of the 30 lunar days, defined by 12° increments of the Moon's elongation from the
 * Sun. Numbers 1..15 fall in the [Paksha.SHUKLA] fortnight (Pratipada..Purnima) and 16..30 in
 * [Paksha.KRISHNA] (Pratipada..Amavasya).
 *
 * @property number the tithi number, 1..30.
 * @property paksha the fortnight this tithi belongs to.
 * @property name the traditional Sanskrit name.
 */
data class Tithi(
    val number: Int,
    val paksha: Paksha,
    val name: String,
)

/**
 * A nakshatra — one of the 27 lunar mansions, each spanning 13°20' of the sidereal zodiac,
 * determined by the Moon's sidereal ecliptic longitude.
 *
 * @property number the nakshatra number, 1..27.
 * @property name the traditional Sanskrit name.
 */
data class Nakshatra(
    val number: Int,
    val name: String,
)

/**
 * A yoga — one of the 27 divisions of the combined sidereal longitudes of the Sun and Moon, each
 * spanning 13°20'.
 *
 * @property number the yoga number, 1..27.
 * @property name the traditional Sanskrit name.
 */
data class Yoga(
    val number: Int,
    val name: String,
)

/**
 * A karana — half a tithi (6° of the Moon's elongation). A lunar month has 60 karana positions
 * drawn from 11 karanas: seven "movable" ones that repeat, plus four "fixed" ones.
 *
 * @property number the karana's position in the lunar month, 1..60.
 * @property name the traditional Sanskrit name.
 */
data class Karana(
    val number: Int,
    val name: String,
)

/**
 * A vara — the weekday, which in Vedic reckoning runs from sunrise to sunrise. Ordinal 0 = Sunday.
 */
enum class Vara(
    val displayName: String,
) {
    RAVIVARA("Ravivara"),
    SOMAVARA("Somavara"),
    MANGALAVARA("Mangalavara"),
    BUDHAVARA("Budhavara"),
    GURUVARA("Guruvara"),
    SHUKRAVARA("Shukravara"),
    SHANIVARA("Shanivara"),
}
