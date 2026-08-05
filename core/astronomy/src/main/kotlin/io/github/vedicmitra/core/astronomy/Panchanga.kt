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

/**
 * The Moon's phase — one of the eight traditional divisions of its cycle, based on its elongation
 * from the Sun. Each phase spans 45°, centred on the four named syzygies/quadratures: New Moon at
 * 0°, First Quarter at 90°, Full Moon at 180°, Last Quarter at 270°.
 */
enum class MoonPhase(
    val displayName: String,
) {
    NEW_MOON("New Moon"),
    WAXING_CRESCENT("Waxing Crescent"),
    FIRST_QUARTER("First Quarter"),
    WAXING_GIBBOUS("Waxing Gibbous"),
    FULL_MOON("Full Moon"),
    WANING_GIBBOUS("Waning Gibbous"),
    LAST_QUARTER("Last Quarter"),
    WANING_CRESCENT("Waning Crescent"),
}

/**
 * Ayana — the Sun's half-year sidereal journey. Uttarayana runs from the Makara (Capricorn)
 * ingress at sidereal longitude 270° to the Karka (Cancer) ingress at 90°; Dakshinayana is the
 * other half, 90°..270°. This is the Drik (observed-position) convention; some traditional
 * almanacs use a separate "Vedic Ayana" that can disagree near the transition — this app follows
 * Drik, consistent with computing astronomy from position rather than a fixed calendar rule.
 */
enum class Ayana(
    val displayName: String,
) {
    UTTARAYANA("Uttarayana"),
    DAKSHINAYANA("Dakshinayana"),
}

/**
 * Ritu — one of the six Indian seasons, each spanning 60° of the Sun's sidereal longitude,
 * starting at the Meena/Mesha boundary (330°) for Vasanta. This is the Drik (solar-longitude)
 * convention; some almanacs instead derive Ritu from the lunar month name ("Vedic Ritu"), which can
 * disagree by up to a season — this app follows Drik for the same reason as [Ayana].
 */
enum class Ritu(
    val displayName: String,
) {
    VASANTA("Vasanta"),
    GRISHMA("Grishma"),
    VARSHA("Varsha"),
    SHARAD("Sharad"),
    HEMANTA("Hemanta"),
    SHISHIRA("Shishira"),
}
