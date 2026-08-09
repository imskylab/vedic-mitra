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
import kotlin.math.sqrt

private const val DEG2RAD = PI / 180.0
private const val KEPLER_ITERATIONS = 8

// Lahiri's ayanamsa advances at the precession rate (~1.397°/century), so adding the same rate to a
// planet's J2000-referenced longitude before subtracting the ayanamsa yields a fixed sidereal frame
// consistent with how the Sun/Moon sidereal longitudes are derived elsewhere.
private const val PRECESSION_DEG_PER_CENTURY = 1.397

// JPL "Keplerian Elements for Approximate Positions of the Major Planets" (Standish, 1800–2050 AD).
// base = [a(AU), e, inclination°, mean longitude°, longitude of perihelion°, longitude of node°],
// rate = the same six per Julian century. Accurate to arcminutes — ample for 30° rashi buckets.
private class KeplerElements(
    val base: DoubleArray,
    val rate: DoubleArray,
)

private val EARTH =
    KeplerElements(
        base = doubleArrayOf(1.00000261, 0.01671123, -0.00001531, 100.46457166, 102.93768193, 0.0),
        rate = doubleArrayOf(0.00000562, -0.00004392, -0.01294668, 35999.37244981, 0.32327364, 0.0),
    )

private val VENUS =
    KeplerElements(
        base = doubleArrayOf(0.72333566, 0.00677672, 3.39467605, 181.97909950, 131.60246718, 76.67984255),
        rate = doubleArrayOf(0.00000390, -0.00004107, -0.00078890, 58517.81538729, 0.00268329, -0.27769418),
    )

private val JUPITER =
    KeplerElements(
        base = doubleArrayOf(5.20288700, 0.04838624, 1.30439695, 34.39644051, 14.72847983, 100.47390909),
        rate = doubleArrayOf(-0.00011607, -0.00013253, -0.00183714, 3034.74612775, 0.21252668, 0.20469106),
    )

/**
 * The sidereal (Lahiri) ecliptic longitude of [graha] at [t] Julian centuries, in degrees 0..360.
 * The Sun and Moon reuse the app's of-date ephemeris; Guru (Jupiter) and Shukra (Venus) are computed
 * geocentrically from the JPL Keplerian elements.
 */
internal fun siderealLongitude(
    graha: Graha,
    t: Double,
): Double =
    when (graha) {
        Graha.SUN -> Ephemeris.norm360(Ephemeris.sunApparentLongitude(t) - Ephemeris.lahiriAyanamsa(t))
        Graha.MOON -> Ephemeris.norm360(Ephemeris.moonLongitude(t) - Ephemeris.lahiriAyanamsa(t))
        Graha.GURU -> planetSidereal(JUPITER, t)
        Graha.SHUKRA -> planetSidereal(VENUS, t)
    }

private fun planetSidereal(
    element: KeplerElements,
    t: Double,
): Double {
    val tropicalOfDate = geocentricLongitude(element, t) + PRECESSION_DEG_PER_CENTURY * t
    return Ephemeris.norm360(tropicalOfDate - Ephemeris.lahiriAyanamsa(t))
}

/** The geocentric J2000 ecliptic longitude (degrees) of the planet [element] at [t]. */
private fun geocentricLongitude(
    element: KeplerElements,
    t: Double,
): Double {
    val planet = heliocentric(element, t)
    val earth = heliocentric(EARTH, t)
    return Ephemeris.norm360(atan2(planet[1] - earth[1], planet[0] - earth[0]) / DEG2RAD)
}

/** Heliocentric J2000 ecliptic rectangular coordinates (AU) of [element] at [t]. */
private fun heliocentric(
    element: KeplerElements,
    t: Double,
): DoubleArray {
    val a = element.base[0] + element.rate[0] * t
    val e = element.base[1] + element.rate[1] * t
    val inclination = (element.base[2] + element.rate[2] * t) * DEG2RAD
    val meanLongitude = element.base[3] + element.rate[3] * t
    val perihelion = element.base[4] + element.rate[4] * t
    val node = (element.base[5] + element.rate[5] * t) * DEG2RAD
    val argPerihelion = perihelion * DEG2RAD - node
    val eccentric = solveKepler(normDeg180(meanLongitude - perihelion) * DEG2RAD, e)

    val xOrbital = a * (cos(eccentric) - e)
    val yOrbital = a * sqrt(1.0 - e * e) * sin(eccentric)

    val cosW = cos(argPerihelion)
    val sinW = sin(argPerihelion)
    val cosN = cos(node)
    val sinN = sin(node)
    val cosI = cos(inclination)
    val sinI = sin(inclination)

    val x = (cosW * cosN - sinW * sinN * cosI) * xOrbital + (-sinW * cosN - cosW * sinN * cosI) * yOrbital
    val y = (cosW * sinN + sinW * cosN * cosI) * xOrbital + (-sinW * sinN + cosW * cosN * cosI) * yOrbital
    val z = (sinW * sinI) * xOrbital + (cosW * sinI) * yOrbital
    return doubleArrayOf(x, y, z)
}

/** Solves Kepler's equation E − e·sin E = M (radians) by Newton's method. */
private fun solveKepler(
    meanAnomaly: Double,
    e: Double,
): Double {
    var eccentric = meanAnomaly
    repeat(KEPLER_ITERATIONS) {
        eccentric -= (eccentric - e * sin(eccentric) - meanAnomaly) / (1.0 - e * cos(eccentric))
    }
    return eccentric
}

/** Normalises degrees to the range −180..180. */
private fun normDeg180(deg: Double): Double {
    val normalised = Ephemeris.norm360(deg)
    return if (normalised > 180.0) normalised - 360.0 else normalised
}
