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

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

private const val DEG2RAD = PI / 180.0
private const val HOUSE_COUNT = 12

/**
 * The lagna (ascendant) at [epochMillis] for an observer at [latitude]/[longitude] (degrees; east
 * longitude positive). Computed from the local sidereal time, the obliquity, and the latitude, then
 * converted to sidereal (Lahiri) like the graha longitudes.
 */
internal fun lagnaAt(
    epochMillis: Long,
    latitude: Double,
    longitude: Double,
): Lagna {
    val t = Ephemeris.julianCenturies(epochMillis)
    val localSiderealTime = Ephemeris.norm360(Ephemeris.greenwichMeanSiderealTimeDeg(epochMillis) + longitude)
    val tropical = ascendantTropical(localSiderealTime, latitude, Ephemeris.obliquity(t))
    val sidereal = Ephemeris.norm360(tropical - Ephemeris.lahiriAyanamsa(t))
    val index = AngularBuckets.rashiIndex(sidereal)
    return Lagna(siderealLongitude = sidereal, rasi = Rasi(index, RASHI_NAMES[index]))
}

/**
 * The tropical ecliptic longitude (degrees 0..360) of the ascendant, from the local sidereal time
 * [lstDeg] (the RA of the meridian, RAMC), the observer [latDeg], and the [obliquityDeg]. Sanity
 * checks: (LST 0°, lat 0°) → 90°; (LST 90°, lat 0°) → 180°.
 */
internal fun ascendantTropical(
    lstDeg: Double,
    latDeg: Double,
    obliquityDeg: Double,
): Double {
    val theta = lstDeg * DEG2RAD
    val eps = obliquityDeg * DEG2RAD
    val phi = latDeg * DEG2RAD
    val ascendant = atan2(cos(theta), -(sin(theta) * cos(eps) + tan(phi) * sin(eps))) / DEG2RAD
    return Ephemeris.norm360(ascendant)
}

/** Whole-sign houses: house 1 is the ascendant's rashi ([ascendantRasiIndex]); the twelve follow in
 *  zodiacal order. Returns the rashi occupying each house, house 1 first. */
internal fun wholeSignHouses(ascendantRasiIndex: Int): List<Rasi> =
    (0 until HOUSE_COUNT).map { offset ->
        val index = (ascendantRasiIndex + offset) % HOUSE_COUNT
        Rasi(index, RASHI_NAMES[index])
    }
