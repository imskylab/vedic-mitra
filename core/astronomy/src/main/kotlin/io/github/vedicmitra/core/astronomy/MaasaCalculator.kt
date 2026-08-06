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

// The twelve amanta lunar months, in order (index 0 = Chaitra). A month is named after the solar
// rashi the Sun occupies at the new moon that begins it: Sun in Meena -> Chaitra, Mesha ->
// Vaishakha, ... Kumbha -> Phalguna, i.e. monthIndex = (sunRashiIndex + 1) mod 12.
private val MAASA_NAMES =
    listOf(
        "Chaitra",
        "Vaishakha",
        "Jyeshtha",
        "Ashadha",
        "Shravana",
        "Bhadrapada",
        "Ashwina",
        "Kartika",
        "Margashirsha",
        "Pausha",
        "Magha",
        "Phalguna",
    )

// The sixty samvatsaras of the Jovian cycle, in order (index 0 = Prabhava). Cross-checked against
// published Ugadi samvatsara names for 2019-2027 (Vikari, Sharvari, Plava, Shubhakruth,
// Shobhakruth, Krodhi, Vishvavasu, Parabhava, Plavanga).
private val SAMVATSARA_NAMES =
    listOf(
        "Prabhava",
        "Vibhava",
        "Shukla",
        "Pramoda",
        "Prajapati",
        "Angirasa",
        "Shrimukha",
        "Bhava",
        "Yuva",
        "Dhata",
        "Ishvara",
        "Bahudhanya",
        "Pramathi",
        "Vikrama",
        "Vrisha",
        "Chitrabhanu",
        "Svabhanu",
        "Tarana",
        "Parthiva",
        "Vyaya",
        "Sarvajit",
        "Sarvadhari",
        "Virodhi",
        "Vikruti",
        "Khara",
        "Nandana",
        "Vijaya",
        "Jaya",
        "Manmatha",
        "Durmukhi",
        "Hevilambi",
        "Vilambi",
        "Vikari",
        "Sharvari",
        "Plava",
        "Shubhakruth",
        "Shobhakruth",
        "Krodhi",
        "Vishvavasu",
        "Parabhava",
        "Plavanga",
        "Kilaka",
        "Saumya",
        "Sadharana",
        "Virodhikruth",
        "Paridhavi",
        "Pramadi",
        "Ananda",
        "Rakshasa",
        "Nala",
        "Pingala",
        "Kalayukti",
        "Siddharthi",
        "Raudra",
        "Durmati",
        "Dundubhi",
        "Rudhirodgari",
        "Raktakshi",
        "Krodhana",
        "Akshaya",
    )

private const val RASHI_SPAN_DEG = 30.0
private const val MONTHS_PER_YEAR = 12
private const val SAMVATSARA_CYCLE = 60

// Chandramana: elapsed Shaka year -> 0-based samvatsara index. Anchored so Shaka 1948 (Ugadi 2026)
// yields index 39 (Parabhava): (1948 + 11) mod 60 = 39.
private const val SHAKA_TO_SAMVATSARA_OFFSET = 11

// Gregorian year of a Shaka new year = ShakaYear + 78 (Shaka epoch is 78 CE).
private const val SHAKA_EPOCH_YEAR = 78

private const val SYNODIC_MONTH_MS = 2_551_442_877L // 29.530588853 days
private const val NEW_MOON_SEARCH_MARGIN_MS = 2L * 86_400_000L // +/- 2 days brackets the new moon
private const val NEW_MOON_BISECTION_ITERATIONS = 45
private const val MAX_MONTHS_BACK_TO_CHAITRA = 15

/**
 * Determines the current [Maasa] (amanta lunar month) for [atEpochMillis].
 *
 * The month is named after the solar rashi at the new moon that begins it. Finding that new moon
 * needs a search — the Moon's elongation from the Sun has no closed-form inverse — so this takes
 * [elongationAt] (Moon-minus-Sun longitude, degrees 0..360, used to locate new moons) and
 * [sunSiderealAt] (the Sun's sidereal longitude, degrees 0..360, used to read the rashi). A
 * lunation whose bounding new moons fall in the same rashi contains no Sankranti and is flagged
 * [Maasa.adhika].
 *
 * @param atEpochMillis the instant the snapshot is for (Unix epoch milliseconds, UTC).
 * @param elongationAt the Moon's elongation from the Sun at an arbitrary instant.
 * @param sunSiderealAt the Sun's sidereal ecliptic longitude at an arbitrary instant.
 */
internal fun maasaOf(
    atEpochMillis: Long,
    elongationAt: (Long) -> Double,
    sunSiderealAt: (Long) -> Double,
): Maasa {
    val startNewMoon = newMoonAtOrBefore(atEpochMillis, elongationAt)
    val nextNewMoon = newMoonAfter(startNewMoon + NEW_MOON_SEARCH_MARGIN_MS, elongationAt)

    val startRashi = rashiOf(sunSiderealAt(startNewMoon))
    val nextRashi = rashiOf(sunSiderealAt(nextNewMoon))
    val adhika = startRashi == nextRashi

    val index = (startRashi + 1) % MONTHS_PER_YEAR
    return Maasa(number = index + 1, name = MAASA_NAMES[index], adhika = adhika)
}

/**
 * Determines the current [Samvatsara] for [atEpochMillis] under the Chandramana convention: it
 * changes at Chaitra Shukla Pratipada (Ugadi). Walks back new moon by new moon to the Chaitra that
 * began the current lunar year, takes that new year's Gregorian year, converts it to the elapsed
 * Shaka year, and maps that onto the sixty-name cycle.
 *
 * @param atEpochMillis the instant the snapshot is for (Unix epoch milliseconds, UTC).
 * @param elongationAt the Moon's elongation from the Sun at an arbitrary instant.
 * @param sunSiderealAt the Sun's sidereal ecliptic longitude at an arbitrary instant.
 */
internal fun samvatsaraOf(
    atEpochMillis: Long,
    elongationAt: (Long) -> Double,
    sunSiderealAt: (Long) -> Double,
): Samvatsara {
    // Walk back new moon by new moon to the Chaitra that opened the current lunar year, bounded so
    // a pathological input can never loop forever.
    var found = newMoonAtOrBefore(atEpochMillis, elongationAt)
    var monthsBack = 0
    while (monthsBack < MAX_MONTHS_BACK_TO_CHAITRA && !startsChaitra(found, sunSiderealAt)) {
        found = newMoonAtOrBefore(found - NEW_MOON_SEARCH_MARGIN_MS, elongationAt)
        monthsBack++
    }

    val chaitraYear = gregorianYearUtc(found)
    val shakaYear = chaitraYear - SHAKA_EPOCH_YEAR
    val index = ((shakaYear + SHAKA_TO_SAMVATSARA_OFFSET) % SAMVATSARA_CYCLE + SAMVATSARA_CYCLE) % SAMVATSARA_CYCLE
    return Samvatsara(number = index + 1, name = SAMVATSARA_NAMES[index], shakaYear = shakaYear)
}

/** Whether the amanta month beginning at the new moon [newMoonMillis] is Chaitra (month index 0). */
private fun startsChaitra(
    newMoonMillis: Long,
    sunSiderealAt: (Long) -> Double,
): Boolean = (rashiOf(sunSiderealAt(newMoonMillis)) + 1) % MONTHS_PER_YEAR == 0

/** The sidereal rashi (zodiac sign) index 0..11 (0 = Mesha) for a sidereal longitude in [0, 360). */
private fun rashiOf(sunSiderealDeg: Double): Int =
    (Ephemeris.norm360(sunSiderealDeg) / RASHI_SPAN_DEG).toInt() % MONTHS_PER_YEAR

// Signed elongation in (-180, 180]: negative just before a new moon (waning), positive just after
// (waxing), so a new moon is the ascending zero crossing.
private fun signedElongation(elongationDeg: Double): Double =
    if (elongationDeg > 180.0) elongationDeg - 360.0 else elongationDeg

/**
 * The instant of the new moon at or before [epochMillis]. Elongation grows monotonically over a
 * lunation, so it seeds the search near the expected new moon (fraction of a synodic month back),
 * then bisects the +/-2-day bracket around it, where [signedElongation] is small and strictly
 * increasing — well clear of the full-moon wrap.
 */
internal fun newMoonAtOrBefore(
    epochMillis: Long,
    elongationAt: (Long) -> Double,
): Long {
    val elapsed = (elongationAt(epochMillis) / 360.0 * SYNODIC_MONTH_MS).toLong()
    return refineNewMoon(epochMillis - elapsed, elongationAt)
}

/**
 * The instant of the first new moon strictly after [epochMillis]. Seeds the search a synodic
 * month's remaining fraction ahead, then refines the same way as [newMoonAtOrBefore].
 */
internal fun newMoonAfter(
    epochMillis: Long,
    elongationAt: (Long) -> Double,
): Long {
    val remaining = ((360.0 - elongationAt(epochMillis)) / 360.0 * SYNODIC_MONTH_MS).toLong()
    return refineNewMoon(epochMillis + remaining, elongationAt)
}

/** Bisects the +/-2-day bracket around [approxEpochMillis] for the ascending zero of signed elongation. */
private fun refineNewMoon(
    approxEpochMillis: Long,
    elongationAt: (Long) -> Double,
): Long {
    var lo = approxEpochMillis - NEW_MOON_SEARCH_MARGIN_MS
    var hi = approxEpochMillis + NEW_MOON_SEARCH_MARGIN_MS
    repeat(NEW_MOON_BISECTION_ITERATIONS) {
        val mid = lo + (hi - lo) / 2
        if (signedElongation(elongationAt(mid)) >= 0.0) hi = mid else lo = mid
    }
    return hi
}

/**
 * The Gregorian (proleptic) year in UTC for a Unix-epoch-millisecond instant, via Howard Hinnant's
 * days-to-civil algorithm. Ugadi always falls in March, so UTC is precise enough to pin the year.
 */
private fun gregorianYearUtc(epochMillis: Long): Int {
    val days = Math.floorDiv(epochMillis, 86_400_000L)
    val z = days + 719_468L
    val era = (if (z >= 0) z else z - 146_096) / 146_097
    val doe = z - era * 146_097
    val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
    val year = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val month = if (mp < 10) mp + 3 else mp - 9
    return (if (month <= 2) year + 1 else year).toInt()
}
