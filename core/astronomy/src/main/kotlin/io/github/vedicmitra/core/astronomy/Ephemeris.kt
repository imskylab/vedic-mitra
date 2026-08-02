/*
 * Copyright (c) 2026 Vedic Mitra contributors
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

@file:Suppress("MagicNumber")

package io.github.vedicmitra.core.astronomy

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Low-precision solar/lunar ephemeris based on Jean Meeus, *Astronomical Algorithms* (2nd ed.).
 *
 * Accuracy is roughly an arc-minute for the Sun and a few arc-minutes for the Moon — ample for
 * panchanga, where tithi/nakshatra boundaries land within a few seconds of the exact time. All
 * angles are in degrees unless noted; times are Unix epoch milliseconds (UTC).
 */
internal object Ephemeris {
    private const val DEG2RAD = PI / 180.0
    private const val JD_UNIX_EPOCH = 2440587.5
    private const val JD_J2000 = 2451545.0
    private const val DAYS_PER_CENTURY = 36525.0
    private const val MILLIS_PER_DAY = 86_400_000.0

    /** Julian Day for a UTC instant given as Unix epoch milliseconds. */
    fun julianDay(epochMillis: Long): Double = epochMillis / MILLIS_PER_DAY + JD_UNIX_EPOCH

    /**
     * Julian centuries (TT) since J2000.0. A constant ΔT ≈ 69 s (adequate for the 2000–2050 range)
     * converts UTC to Terrestrial Time.
     */
    fun julianCenturies(epochMillis: Long): Double {
        val deltaTDays = 69.0 / 86_400.0
        return (julianDay(epochMillis) - JD_J2000 + deltaTDays) / DAYS_PER_CENTURY
    }

    /** Normalises an angle to the range [0, 360). */
    fun norm360(deg: Double): Double = ((deg % 360.0) + 360.0) % 360.0

    /** Sun's mean anomaly (degrees). */
    fun sunMeanAnomaly(t: Double): Double = norm360(357.52911 + 35999.05029 * t - 0.0001537 * t * t)

    /** Sun's apparent geocentric ecliptic longitude (degrees), Meeus ch. 25. */
    fun sunApparentLongitude(t: Double): Double {
        val l0 = 280.46646 + 36000.76983 * t + 0.0003032 * t * t
        val m = sunMeanAnomaly(t) * DEG2RAD
        val c =
            (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(m) +
                (0.019993 - 0.000101 * t) * sin(2 * m) +
                0.000289 * sin(3 * m)
        val trueLong = l0 + c
        val omega = 125.04 - 1934.136 * t
        return norm360(trueLong - 0.00569 - 0.00478 * sin(omega * DEG2RAD))
    }

    /** Mean obliquity of the ecliptic (degrees). */
    fun obliquity(t: Double): Double = 23.439291 - 0.0130042 * t - 1.64e-7 * t * t + 5.04e-7 * t * t * t

    /** Sun's declination (degrees). */
    fun sunDeclination(t: Double): Double {
        val lambda = sunApparentLongitude(t) * DEG2RAD
        val eps = obliquity(t) * DEG2RAD
        return asin(sin(eps) * sin(lambda)) / DEG2RAD
    }

    /** Equation of time (minutes), Meeus ch. 28. */
    fun equationOfTimeMinutes(t: Double): Double {
        val l0 = norm360(280.46646 + 36000.76983 * t + 0.0003032 * t * t) * DEG2RAD
        val m = sunMeanAnomaly(t) * DEG2RAD
        val e = 0.016708634 - 0.000042037 * t
        val y = tan(obliquity(t) / 2 * DEG2RAD).let { it * it }
        val et =
            y * sin(2 * l0) - 2 * e * sin(m) + 4 * e * y * sin(m) * cos(2 * l0) -
                0.5 * y * y * sin(4 * l0) - 1.25 * e * e * sin(2 * m)
        return (et / DEG2RAD) * 4.0
    }

    // Meeus Table 47.A (largest longitude terms): D, M, M', F multipliers and coefficient (1e-6 deg).
    private val moonTerms =
        listOf(
            intArrayOf(0, 0, 1, 0, 6288774),
            intArrayOf(2, 0, -1, 0, 1274027),
            intArrayOf(2, 0, 0, 0, 658314),
            intArrayOf(0, 0, 2, 0, 213618),
            intArrayOf(0, 1, 0, 0, -185116),
            intArrayOf(0, 0, 0, 2, -114332),
            intArrayOf(2, 0, -2, 0, 58793),
            intArrayOf(2, -1, -1, 0, 57066),
            intArrayOf(2, 0, 1, 0, 53322),
            intArrayOf(2, -1, 0, 0, 45758),
            intArrayOf(0, 1, -1, 0, -40923),
            intArrayOf(1, 0, 0, 0, -34720),
            intArrayOf(0, 1, 1, 0, -30383),
            intArrayOf(2, 0, 0, -2, 15327),
            intArrayOf(0, 0, 1, 2, -12528),
            intArrayOf(0, 0, 1, -2, 10980),
            intArrayOf(4, 0, -1, 0, 10675),
            intArrayOf(0, 0, 3, 0, 10034),
            intArrayOf(4, 0, -2, 0, 8548),
            intArrayOf(2, 1, -1, 0, -7888),
            intArrayOf(2, 1, 0, 0, -6766),
            intArrayOf(1, 0, -1, 0, -5163),
            intArrayOf(1, 1, 0, 0, 4987),
            intArrayOf(2, -1, 1, 0, 4036),
            intArrayOf(2, 0, 2, 0, 3994),
            intArrayOf(4, 0, 0, 0, 3861),
            intArrayOf(2, 0, -3, 0, 3665),
            intArrayOf(0, 1, -2, 0, -2689),
            intArrayOf(2, 0, -1, 2, -2602),
            intArrayOf(2, -1, -2, 0, 2390),
            intArrayOf(1, 0, 1, 0, -2348),
            intArrayOf(2, -2, 0, 0, 2236),
            intArrayOf(0, 1, 2, 0, -2120),
            intArrayOf(0, 2, 0, 0, -2069),
            intArrayOf(2, -2, -1, 0, 2048),
        )

    /** Moon's apparent geocentric ecliptic longitude (degrees), Meeus ch. 47 (truncated). */
    fun moonLongitude(t: Double): Double {
        val t2 = t * t
        val t3 = t2 * t
        val t4 = t3 * t
        val lp = norm360(218.3164477 + 481267.88123421 * t - 0.0015786 * t2 + t3 / 538841 - t4 / 65194000)
        val d = norm360(297.8501921 + 445267.1114034 * t - 0.0018819 * t2 + t3 / 545868 - t4 / 113065000)
        val m = norm360(357.5291092 + 35999.0502909 * t - 0.0001536 * t2 + t3 / 24490000)
        val mp = norm360(134.9633964 + 477198.8675055 * t + 0.0087414 * t2 + t3 / 69699 - t4 / 14712000)
        val f = norm360(93.2720950 + 483202.0175233 * t - 0.0036539 * t2 - t3 / 3526000 + t4 / 863310000)
        val ecc = 1 - 0.002516 * t - 0.0000074 * t2
        val a1 = norm360(119.75 + 131.849 * t)
        val a2 = norm360(53.09 + 479264.290 * t)

        var sum = 0.0
        for (term in moonTerms) {
            val arg = (term[0] * d + term[1] * m + term[2] * mp + term[3] * f) * DEG2RAD
            val eccFactor =
                when {
                    term[1] == 1 || term[1] == -1 -> ecc
                    term[1] == 2 || term[1] == -2 -> ecc * ecc
                    else -> 1.0
                }
            sum += term[4] * eccFactor * sin(arg)
        }
        sum += 3958 * sin(a1 * DEG2RAD) + 1962 * sin((lp - f) * DEG2RAD) + 318 * sin(a2 * DEG2RAD)
        return norm360(lp + sum / 1_000_000.0)
    }

    /**
     * Lahiri (Chitrapaksha) ayanamsa in degrees — the offset between the tropical and sidereal
     * zodiacs. Linear approximation about the current era (≈50.29″/yr), accurate to well under an
     * arc-minute over the app's date range.
     */
    fun lahiriAyanamsa(t: Double): Double = 23.853 + 1.397 * t
}
