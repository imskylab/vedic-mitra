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

private const val RASHI_SPAN_DEGREES = 30.0
private const val HOUSE_COUNT = 12
private const val NAKSHATRA_SPAN_DEGREES = 360.0 / 27.0
private const val PADA_COUNT = 4
private const val DAY_MILLIS = 86_400_000L
private const val HALF_TURN_DEGREES = 180.0
private const val WRAP_OFFSET_DEGREES = 540.0

/**
 * The full birth [NatalChart] for a birth at [epochMillis] at [latitude]/[longitude] (degrees, east
 * longitude positive): the nine grahas (with house and retrograde state), the ascendant, whole-sign
 * houses, the Moon's nakshatra/pada, and the Vimshottari mahadasha timeline. Pure and offline.
 */
internal fun natalChart(
    epochMillis: Long,
    latitude: Double,
    longitude: Double,
): NatalChart {
    val t = Ephemeris.julianCenturies(epochMillis)
    val lagna = lagnaAt(epochMillis, latitude, longitude)
    val moonLongitude = siderealLongitude(Graha.MOON, t)
    return NatalChart(
        lagna = lagna,
        houses = wholeSignHouses(lagna.rasi.index),
        grahas = Graha.entries.map { natalGraha(it, epochMillis, t, lagna.rasi.index) },
        moonNakshatra = nakshatraOf(moonLongitude),
        moonPada = padaOf(moonLongitude),
        vimshottari = vimshottariFromMoon(moonLongitude, epochMillis),
    )
}

private fun natalGraha(
    graha: Graha,
    epochMillis: Long,
    t: Double,
    ascendantRasiIndex: Int,
): NatalGraha {
    val longitude = siderealLongitude(graha, t)
    val rasiIndex = (longitude / RASHI_SPAN_DEGREES).toInt()
    return NatalGraha(
        graha = graha,
        siderealLongitude = longitude,
        rasi = Rasi(rasiIndex, RASHI_NAMES[rasiIndex]),
        house = ((rasiIndex - ascendantRasiIndex + HOUSE_COUNT) % HOUSE_COUNT) + 1,
        retrograde = isRetrograde(graha, epochMillis),
    )
}

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

/** The Moon's pada — which quarter (1..4) of its nakshatra it occupies. */
private fun padaOf(moonSiderealDeg: Double): Int {
    val within = moonSiderealDeg % NAKSHATRA_SPAN_DEGREES
    return (within / (NAKSHATRA_SPAN_DEGREES / PADA_COUNT)).toInt() + 1
}
