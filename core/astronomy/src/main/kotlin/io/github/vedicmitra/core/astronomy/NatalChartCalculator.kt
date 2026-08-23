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

private const val HOUSE_COUNT = 12
private const val DAY_MILLIS = 86_400_000L
private const val HALF_TURN_DEGREES = 180.0
private const val WRAP_OFFSET_DEGREES = 540.0

/**
 * The full birth [NatalChart] for a birth at [epochMillis] at [latitude]/[longitude] (degrees, east
 * longitude positive): the nine grahas (with house and retrograde state), the ascendant, whole-sign
 * houses, the Moon's nakshatra/pada, and the Vimshottari mahadasha timeline. Pure and offline.
 *
 * The chart carries **two framings of the same placements**: houses counted from the lagna, and
 * houses counted from the Moon's rashi (the Chandra or Rashi kundali). One chart with two framings
 * rather than two charts, so they cannot drift apart.
 */
internal fun natalChart(
    epochMillis: Long,
    latitude: Double,
    longitude: Double,
): NatalChart {
    val t = Ephemeris.julianCenturies(epochMillis)
    val lagna = lagnaAt(epochMillis, latitude, longitude)
    val moonLongitude = siderealLongitude(Graha.MOON, t)
    val moonRasiIndex = AngularBuckets.rashiIndex(moonLongitude)
    val grahas = Graha.entries.map { natalGraha(it, epochMillis, t, lagna.rasi.index, moonRasiIndex) }
    val moonNakshatra = nakshatraOf(moonLongitude)
    val moonPada = padaOf(moonLongitude)
    return NatalChart(
        lagna = lagna,
        houses = wholeSignHouses(lagna.rasi.index),
        moonHouses = wholeSignHouses(moonRasiIndex),
        grahas = grahas,
        moonNakshatra = moonNakshatra,
        moonPada = moonPada,
        jataka = jatakaProfileOf(grahas, lagna, moonNakshatra, moonPada, epochMillis),
        vimshottari = vimshottariFromMoon(moonLongitude, epochMillis),
    )
}

private fun natalGraha(
    graha: Graha,
    epochMillis: Long,
    t: Double,
    ascendantRasiIndex: Int,
    moonRasiIndex: Int,
): NatalGraha {
    val longitude = siderealLongitude(graha, t)
    val rasiIndex = AngularBuckets.rashiIndex(longitude)
    return NatalGraha(
        graha = graha,
        siderealLongitude = longitude,
        rasi = Rasi(rasiIndex, RASHI_NAMES[rasiIndex]),
        house = houseOf(rasiIndex, ascendantRasiIndex),
        houseFromMoon = houseOf(rasiIndex, moonRasiIndex),
        retrograde = isRetrograde(graha, epochMillis),
    )
}

/** Which whole-sign house [rasiIndex] falls in when house 1 is [firstHouseRasiIndex]. 1..12. */
private fun houseOf(
    rasiIndex: Int,
    firstHouseRasiIndex: Int,
): Int = ((rasiIndex - firstHouseRasiIndex + HOUSE_COUNT) % HOUSE_COUNT) + 1

/** Whether [graha] is retrograde: the Sun/Moon never are, the nodes always are, the rest by motion. */
private fun isRetrograde(
    graha: Graha,
    epochMillis: Long,
): Boolean =
    when (graha) {
        Graha.SUN, Graha.MOON -> false
        Graha.RAHU, Graha.KETU -> true
        else -> {
            val before = siderealLongitude(graha, Ephemeris.julianCenturies(epochMillis - DAY_MILLIS))
            val after = siderealLongitude(graha, Ephemeris.julianCenturies(epochMillis + DAY_MILLIS))
            // Signed shortest change in longitude across the birth; negative means retrograde.
            val motion = ((after - before + WRAP_OFFSET_DEGREES) % 360.0) - HALF_TURN_DEGREES
            motion < 0.0
        }
    }

/**
 * The Moon's pada — which quarter (1..4) of its nakshatra it occupies.
 *
 * Delegates to [AngularBuckets] rather than taking `longitude % nakshatraSpan` and dividing again:
 * that rounds twice, and 40 of the 108 pada boundaries land in the wrong quarter as a result.
 */
private fun padaOf(moonSiderealDeg: Double): Int = AngularBuckets.pada(moonSiderealDeg)
