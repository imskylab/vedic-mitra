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

private val YOGA_NAMES =
    listOf(
        "Vishkambha",
        "Priti",
        "Ayushman",
        "Saubhagya",
        "Shobhana",
        "Atiganda",
        "Sukarman",
        "Dhriti",
        "Shula",
        "Ganda",
        "Vriddhi",
        "Dhruva",
        "Vyaghata",
        "Harshana",
        "Vajra",
        "Siddhi",
        "Vyatipata",
        "Variyana",
        "Parigha",
        "Shiva",
        "Siddha",
        "Sadhya",
        "Shubha",
        "Shukla",
        "Brahma",
        "Indra",
        "Vaidhriti",
    )

// The seven movable (chara) karanas, which repeat eight times through the lunar month.
private val KARANA_MOVABLE =
    listOf("Bava", "Balava", "Kaulava", "Taitila", "Gara", "Vanija", "Vishti")

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

/**
 * Derives the [MoonPhase] from the Moon's elongation from the Sun (degrees, 0..360), dividing the
 * cycle into eight 45°-wide phases centred on New Moon (0°), First Quarter (90°), Full Moon (180°),
 * and Last Quarter (270°).
 */
internal fun moonPhaseOf(elongationDeg: Double): MoonPhase {
    val phaseSpan = 45.0
    val index = ((elongationDeg + phaseSpan / 2) / phaseSpan).toInt() % MoonPhase.entries.size
    return MoonPhase.entries[index]
}

/**
 * Derives the [Ayana] from the Sun's sidereal ecliptic longitude (degrees, 0..360): Dakshinayana
 * spans 90°..270° (Karka to Makara ingress), Uttarayana the rest.
 */
internal fun ayanaOf(sunSiderealDeg: Double): Ayana =
    if (sunSiderealDeg in 90.0..<270.0) Ayana.DAKSHINAYANA else Ayana.UTTARAYANA

/**
 * Derives the [Ritu] from the Sun's sidereal ecliptic longitude (degrees, 0..360), dividing the
 * year into six 60°-wide seasons starting at the Meena/Mesha boundary (330°) for Vasanta.
 */
internal fun rituOf(sunSiderealDeg: Double): Ritu {
    val rituSpan = 60.0
    val vasantaStart = 330.0
    val index = (Ephemeris.norm360(sunSiderealDeg - vasantaStart) / rituSpan).toInt() % Ritu.entries.size
    return Ritu.entries[index]
}

/** Derives the [Nakshatra] from the Moon's sidereal ecliptic longitude (degrees, 0..360). */
internal fun nakshatraOf(moonSiderealDeg: Double): Nakshatra {
    val span = 360.0 / 27.0
    val number = (moonSiderealDeg / span).toInt() + 1
    return Nakshatra(number = number, name = NAKSHATRA_NAMES[number - 1])
}

/** Derives the [Rasi] (0 = Mesha .. 11 = Meena) from a sidereal ecliptic longitude (degrees, 0..360). */
internal fun rasiOf(siderealDeg: Double): Rasi {
    val index = (siderealDeg / 30.0).toInt() % RASHI_NAMES.size
    return Rasi(index = index, name = RASHI_NAMES[index])
}

/** Derives the [Yoga] from the combined sidereal longitudes of the Sun and Moon (degrees, 0..360). */
internal fun yogaOf(yogaSumDeg: Double): Yoga {
    val span = 360.0 / 27.0
    val number = (yogaSumDeg / span).toInt() + 1
    return Yoga(number = number, name = YOGA_NAMES[number - 1])
}

/**
 * Derives the [Karana] from the Moon's elongation from the Sun (degrees, 0..360). The 60 half-tithi
 * positions map to Kimstughna (first), the seven movable karanas repeating, then the three fixed
 * karanas Shakuni, Chatushpada, and Naga.
 */
internal fun karanaOf(elongationDeg: Double): Karana {
    val index = (elongationDeg / 6.0).toInt() // 0..59
    val name =
        when {
            index == 0 -> "Kimstughna"
            index <= 56 -> KARANA_MOVABLE[(index - 1) % 7]
            index == 57 -> "Shakuni"
            index == 58 -> "Chatushpada"
            else -> "Naga"
        }
    return Karana(number = index + 1, name = name)
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
