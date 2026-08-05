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
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.time.Instant

/**
 * Moonrise/moonset via the Moon's topocentric altitude, using the same Meeus low-precision
 * ephemeris as the rest of the app. Unlike the Sun, the Moon moves too fast — and too
 * irregularly — for a closed-form hour-angle formula, so this searches numerically: a coarse
 * scan across the civil day brackets any sign change in altitude-minus-threshold, then bisection
 * narrows each bracket to the exact crossing.
 */
internal object LunarDay {
    private const val DEG2RAD = PI / 180.0
    private const val MILLIS_PER_DAY = 86_400_000L
    private const val MILLIS_PER_HOUR = 3_600_000L
    private const val EARTH_EQUATORIAL_RADIUS_KM = 6378.14

    // 34' atmospheric refraction + 16' the Moon's semi-diameter, matching SunTimes' fixed 0.833°
    // convention rather than varying semi-diameter with distance. Horizontal parallax (which does
    // vary with distance, by roughly a degree, far more than semi-diameter does) is added
    // separately per instant.
    private const val SEMI_DIAMETER_PLUS_REFRACTION_DEG = 50.0 / 60.0

    // Coarse samples across the civil day to bracket sign changes before bisecting — fine enough
    // that the Moon's single rise/set cycle per ~day can't hide between two samples.
    private const val SAMPLE_SEGMENTS = 48
    private const val BISECTION_ITERATIONS = 40

    /**
     * Computes moonrise and moonset for the civil day (in local mean solar time, matching
     * [SolarDay.sunTimes]) containing [epochMillis], at latitude [latDeg] and east-positive
     * longitude [lonEastDeg].
     */
    fun moonTimes(
        epochMillis: Long,
        latDeg: Double,
        lonEastDeg: Double,
    ): MoonTimes {
        val lonOffsetMillis = (lonEastDeg / 15.0 * MILLIS_PER_HOUR).toLong()
        val localMillis = epochMillis + lonOffsetMillis
        val localDay = floor(localMillis.toDouble() / MILLIS_PER_DAY)
        val dayStartUtc = (localDay * MILLIS_PER_DAY).toLong() - lonOffsetMillis
        val dayEndUtc = dayStartUtc + MILLIS_PER_DAY

        fun altitudeMinusThreshold(ms: Long): Double {
            val (altitudeDeg, parallaxDeg) = geocentricAltitudeAndParallax(ms, latDeg, lonEastDeg)
            return altitudeDeg - (parallaxDeg - SEMI_DIAMETER_PLUS_REFRACTION_DEG)
        }

        var moonrise: Long? = null
        var moonset: Long? = null
        val step = (dayEndUtc - dayStartUtc) / SAMPLE_SEGMENTS
        var prevMs = dayStartUtc
        var prevValue = altitudeMinusThreshold(prevMs)
        for (i in 1..SAMPLE_SEGMENTS) {
            val curMs = dayStartUtc + i * step
            val curValue = altitudeMinusThreshold(curMs)

            if (moonrise == null && prevValue < 0.0 && curValue >= 0.0) {
                moonrise = bisect(prevMs, curMs) { altitudeMinusThreshold(it) >= 0.0 }
            }
            if (moonset == null && prevValue >= 0.0 && curValue < 0.0) {
                moonset = bisect(prevMs, curMs) { altitudeMinusThreshold(it) < 0.0 }
            }

            prevMs = curMs
            prevValue = curValue
        }

        return MoonTimes(
            moonrise = moonrise?.let { Instant.fromEpochMilliseconds(it) },
            moonset = moonset?.let { Instant.fromEpochMilliseconds(it) },
        )
    }

    /** Narrows [lo]..[hi] to the instant where [isPastCrossing] first becomes true. */
    private fun bisect(
        lo: Long,
        hi: Long,
        isPastCrossing: (Long) -> Boolean,
    ): Long {
        var loBound = lo
        var hiBound = hi
        repeat(BISECTION_ITERATIONS) {
            val mid = (loBound + hiBound) / 2
            if (isPastCrossing(mid)) hiBound = mid else loBound = mid
        }
        return hiBound
    }

    /**
     * The Moon's geocentric altitude (degrees) as seen from [latDeg]/[lonEastDeg] at [epochMillis],
     * and its horizontal parallax (degrees, from its distance) — the dominant reason the Moon's
     * rise/set threshold isn't a fixed constant the way the Sun's is.
     */
    private fun geocentricAltitudeAndParallax(
        epochMillis: Long,
        latDeg: Double,
        lonEastDeg: Double,
    ): Pair<Double, Double> {
        val t = Ephemeris.julianCenturies(epochMillis)
        val eclipticLongitude = Ephemeris.moonLongitude(t)
        val eclipticLatitude = Ephemeris.moonLatitude(t)
        val distanceKm = Ephemeris.moonDistanceKm(t)
        val obliquity = Ephemeris.obliquity(t)
        val (rightAscension, declination) =
            Ephemeris.equatorialFromEcliptic(eclipticLongitude, eclipticLatitude, obliquity)

        val gmst = Ephemeris.greenwichMeanSiderealTimeDeg(epochMillis)
        val hourAngle = Ephemeris.norm360(gmst + lonEastDeg - rightAscension) * DEG2RAD
        val lat = latDeg * DEG2RAD
        val dec = declination * DEG2RAD

        val altitude = asin(sin(lat) * sin(dec) + cos(lat) * cos(dec) * cos(hourAngle)) / DEG2RAD
        val parallax = asin(EARTH_EQUATORIAL_RADIUS_KM / distanceKm) / DEG2RAD
        return altitude to parallax
    }
}
