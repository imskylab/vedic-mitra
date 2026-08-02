/*
 * Copyright (c) 2026 Vedic Mitra contributors
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

@file:Suppress("MagicNumber")

package io.github.vedicmitra.core.astronomy

import kotlin.math.floor
import kotlin.time.Instant

// Tithi names 1..14 within a paksha; the 15th is Purnima (Shukla) or Amavasya (Krishna).
private val TITHI_NAMES =
    listOf(
        "Pratipada",
        "Dwitiya",
        "Tritiya",
        "Chaturthi",
        "Panchami",
        "Shashthi",
        "Saptami",
        "Ashtami",
        "Navami",
        "Dashami",
        "Ekadashi",
        "Dwadashi",
        "Trayodashi",
        "Chaturdashi",
    )

private val NAKSHATRA_NAMES =
    listOf(
        "Ashwini",
        "Bharani",
        "Krittika",
        "Rohini",
        "Mrigashira",
        "Ardra",
        "Punarvasu",
        "Pushya",
        "Ashlesha",
        "Magha",
        "Purva Phalguni",
        "Uttara Phalguni",
        "Hasta",
        "Chitra",
        "Swati",
        "Vishakha",
        "Anuradha",
        "Jyeshtha",
        "Mula",
        "Purva Ashadha",
        "Uttara Ashadha",
        "Shravana",
        "Dhanishta",
        "Shatabhisha",
        "Purva Bhadrapada",
        "Uttara Bhadrapada",
        "Revati",
    )

/** Derives the [Tithi] from the Moon's elongation from the Sun (degrees, 0..360). */
internal fun tithiOf(elongationDeg: Double): Tithi {
    val number = (elongationDeg / 12.0).toInt() + 1
    val paksha = if (number <= 15) Paksha.SHUKLA else Paksha.KRISHNA
    val inPaksha = if (number <= 15) number else number - 15
    val name =
        if (inPaksha == 15) {
            if (paksha == Paksha.SHUKLA) "Purnima" else "Amavasya"
        } else {
            TITHI_NAMES[inPaksha - 1]
        }
    return Tithi(number = number, paksha = paksha, name = name)
}

/** Derives the [Nakshatra] from the Moon's sidereal ecliptic longitude (degrees, 0..360). */
internal fun nakshatraOf(moonSiderealDeg: Double): Nakshatra {
    val span = 360.0 / 27.0
    val number = (moonSiderealDeg / span).toInt() + 1
    return Nakshatra(number = number, name = NAKSHATRA_NAMES[number - 1])
}

/**
 * Determines the [Vara] (weekday), which runs from sunrise to sunrise. The civil day is taken in
 * local mean solar time; if [instant] precedes the day's [sunrise], the previous vara still holds.
 */
internal fun varaOf(
    instant: Instant,
    epochMillis: Long,
    lonEastDeg: Double,
    sunrise: Instant?,
): Vara {
    val localMillis = epochMillis + (lonEastDeg / 15.0 * 3_600_000.0).toLong()
    val localDay = floor(localMillis.toDouble() / 86_400_000.0).toLong()
    // 1970-01-01 (day 0) was a Thursday, which is ordinal 4 with Sunday = 0.
    var dayOfWeek = ((localDay % 7).toInt() + 4).mod(7)
    if (sunrise != null && instant < sunrise) {
        dayOfWeek = (dayOfWeek + 6) % 7
    }
    return Vara.entries[dayOfWeek]
}
