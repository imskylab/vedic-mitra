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
import kotlin.math.atan2
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

    /** The Moon's mean orbital elements at [t] (Julian centuries), shared by longitude/latitude/distance. */
    private class MoonArguments(
        val meanLongitude: Double,
        val elongation: Double,
        val sunMeanAnomaly: Double,
        val meanAnomaly: Double,
        val argumentOfLatitude: Double,
        val eccentricityCorrection: Double,
        val a1: Double,
        val a2: Double,
        val a3: Double,
    )

    private fun moonArguments(t: Double): MoonArguments {
        val t2 = t * t
        val t3 = t2 * t
        val t4 = t3 * t
        return MoonArguments(
            meanLongitude = norm360(218.3164477 + 481267.88123421 * t - 0.0015786 * t2 + t3 / 538841 - t4 / 65194000),
            elongation = norm360(297.8501921 + 445267.1114034 * t - 0.0018819 * t2 + t3 / 545868 - t4 / 113065000),
            sunMeanAnomaly = norm360(357.5291092 + 35999.0502909 * t - 0.0001536 * t2 + t3 / 24490000),
            meanAnomaly = norm360(134.9633964 + 477198.8675055 * t + 0.0087414 * t2 + t3 / 69699 - t4 / 14712000),
            argumentOfLatitude =
                norm360(93.2720950 + 483202.0175233 * t - 0.0036539 * t2 - t3 / 3526000 + t4 / 863310000),
            eccentricityCorrection = 1 - 0.002516 * t - 0.0000074 * t2,
            a1 = norm360(119.75 + 131.849 * t),
            a2 = norm360(53.09 + 479264.290 * t),
            a3 = norm360(313.45 + 481266.484 * t),
        )
    }

    /** The eccentricity correction a term needs, based on its M (Sun mean anomaly) multiplier. */
    private fun eccentricityFactor(
        mCoefficient: Int,
        eccentricityCorrection: Double,
    ): Double =
        when (mCoefficient) {
            1, -1 -> eccentricityCorrection
            2, -2 -> eccentricityCorrection * eccentricityCorrection
            else -> 1.0
        }

    // Meeus Table 47.A (largest longitude/distance terms): D, M, M', F multipliers, longitude
    // coefficient (1e-6 deg), and distance coefficient (1e-3 km). Verified against the PyMeeus
    // reference implementation (github.com/architest/pymeeus).
    private val moonLongitudeDistanceTerms =
        listOf(
            intArrayOf(0, 0, 1, 0, 6288774, -20905355),
            intArrayOf(2, 0, -1, 0, 1274027, -3699111),
            intArrayOf(2, 0, 0, 0, 658314, -2955968),
            intArrayOf(0, 0, 2, 0, 213618, -569925),
            intArrayOf(0, 1, 0, 0, -185116, 48888),
            intArrayOf(0, 0, 0, 2, -114332, -3149),
            intArrayOf(2, 0, -2, 0, 58793, 246158),
            intArrayOf(2, -1, -1, 0, 57066, -152138),
            intArrayOf(2, 0, 1, 0, 53322, -170733),
            intArrayOf(2, -1, 0, 0, 45758, -204586),
            intArrayOf(0, 1, -1, 0, -40923, -129620),
            intArrayOf(1, 0, 0, 0, -34720, 108743),
            intArrayOf(0, 1, 1, 0, -30383, 104755),
            intArrayOf(2, 0, 0, -2, 15327, 10321),
            intArrayOf(0, 0, 1, 2, -12528, 0),
            intArrayOf(0, 0, 1, -2, 10980, 79661),
            intArrayOf(4, 0, -1, 0, 10675, -34782),
            intArrayOf(0, 0, 3, 0, 10034, -23210),
            intArrayOf(4, 0, -2, 0, 8548, -21636),
            intArrayOf(2, 1, -1, 0, -7888, 24208),
            intArrayOf(2, 1, 0, 0, -6766, 30824),
            intArrayOf(1, 0, -1, 0, -5163, -8379),
            intArrayOf(1, 1, 0, 0, 4987, -16675),
            intArrayOf(2, -1, 1, 0, 4036, -12831),
            intArrayOf(2, 0, 2, 0, 3994, -10445),
            intArrayOf(4, 0, 0, 0, 3861, -11650),
            intArrayOf(2, 0, -3, 0, 3665, 14403),
            intArrayOf(0, 1, -2, 0, -2689, -7003),
            intArrayOf(2, 0, -1, 2, -2602, 0),
            intArrayOf(2, -1, -2, 0, 2390, 10056),
            intArrayOf(1, 0, 1, 0, -2348, 6322),
            intArrayOf(2, -2, 0, 0, 2236, -9884),
            intArrayOf(0, 1, 2, 0, -2120, 5751),
            intArrayOf(0, 2, 0, 0, -2069, 0),
            intArrayOf(2, -2, -1, 0, 2048, -4950),
        )

    // Meeus Table 47.B (largest latitude terms): D, M, M', F multipliers and coefficient (1e-6
    // deg). Verified against the PyMeeus reference implementation.
    private val moonLatitudeTerms =
        listOf(
            intArrayOf(0, 0, 0, 1, 5128122),
            intArrayOf(0, 0, 1, 1, 280602),
            intArrayOf(0, 0, 1, -1, 277693),
            intArrayOf(2, 0, 0, -1, 173237),
            intArrayOf(2, 0, -1, 1, 55413),
            intArrayOf(2, 0, -1, -1, 46271),
            intArrayOf(2, 0, 0, 1, 32573),
            intArrayOf(0, 0, 2, 1, 17198),
            intArrayOf(2, 0, 1, -1, 9266),
            intArrayOf(0, 0, 2, -1, 8822),
            intArrayOf(2, -1, 0, -1, 8216),
            intArrayOf(2, 0, -2, -1, 4324),
            intArrayOf(2, 0, 1, 1, 4200),
            intArrayOf(2, 1, 0, -1, -3359),
            intArrayOf(2, -1, -1, 1, 2463),
            intArrayOf(2, -1, 0, 1, 2211),
            intArrayOf(2, -1, -1, -1, 2065),
            intArrayOf(0, 1, -1, -1, -1870),
            intArrayOf(4, 0, -1, -1, 1828),
            intArrayOf(0, 1, 0, 1, -1794),
            intArrayOf(0, 0, 0, 3, -1749),
            intArrayOf(0, 1, -1, 1, -1565),
            intArrayOf(1, 0, 0, 1, -1491),
            intArrayOf(0, 1, 1, 1, -1475),
            intArrayOf(0, 1, 1, -1, -1410),
            intArrayOf(0, 1, 0, -1, -1344),
            intArrayOf(1, 0, 0, -1, -1335),
            intArrayOf(0, 0, 3, 1, 1107),
            intArrayOf(4, 0, 0, -1, 1021),
            intArrayOf(4, 0, -1, 1, 833),
            intArrayOf(0, 0, 1, -3, 777),
            intArrayOf(4, 0, -2, 1, 671),
            intArrayOf(2, 0, 0, -3, 607),
            intArrayOf(2, 0, 2, -1, 596),
            intArrayOf(2, -1, 1, -1, 491),
            intArrayOf(2, 0, -2, 1, -451),
            intArrayOf(0, 0, 3, -1, 439),
            intArrayOf(2, 0, 2, 1, 422),
            intArrayOf(2, 0, -3, -1, 421),
            intArrayOf(2, 1, -1, 1, -366),
            intArrayOf(2, 1, 0, 1, -351),
            intArrayOf(4, 0, 0, 1, 331),
            intArrayOf(2, -1, 1, 1, 315),
            intArrayOf(2, -2, 0, -1, 302),
            intArrayOf(0, 0, 1, 3, -283),
            intArrayOf(2, 1, 1, -1, -229),
            intArrayOf(1, 1, 0, -1, 223),
            intArrayOf(1, 1, 0, 1, 223),
            intArrayOf(0, 1, -2, -1, -220),
            intArrayOf(2, 1, -1, -1, -220),
            intArrayOf(1, 0, 1, 1, -185),
            intArrayOf(2, -1, -2, -1, 181),
            intArrayOf(0, 1, 2, 1, -177),
            intArrayOf(4, 0, -2, -1, 176),
            intArrayOf(4, -1, -1, -1, 166),
            intArrayOf(1, 0, 1, -1, -164),
            intArrayOf(4, 0, 1, -1, 132),
            intArrayOf(1, 0, -1, -1, -119),
            intArrayOf(4, -1, 0, -1, 115),
            intArrayOf(2, -2, 0, 1, 107),
        )

    private const val MEAN_EARTH_MOON_DISTANCE_KM = 385000.56

    /** The D/M/M'/F argument (radians) a periodic term's multipliers select, per Meeus ch. 47. */
    private fun MoonArguments.termArgument(term: IntArray): Double {
        val degrees =
            term[0] * elongation + term[1] * sunMeanAnomaly + term[2] * meanAnomaly + term[3] * argumentOfLatitude
        return degrees * DEG2RAD
    }

    /** Moon's apparent geocentric ecliptic longitude (degrees), Meeus ch. 47 (truncated). */
    fun moonLongitude(t: Double): Double {
        val a = moonArguments(t)
        var sum = 0.0
        for (term in moonLongitudeDistanceTerms) {
            sum += term[4] * eccentricityFactor(term[1], a.eccentricityCorrection) * sin(a.termArgument(term))
        }

        val a1Term = 3958 * sin(a.a1 * DEG2RAD)
        val evectionTerm = 1962 * sin((a.meanLongitude - a.argumentOfLatitude) * DEG2RAD)
        val a2Term = 318 * sin(a.a2 * DEG2RAD)
        sum += a1Term + evectionTerm + a2Term

        return norm360(a.meanLongitude + sum / 1_000_000.0)
    }

    /** Moon's geocentric ecliptic latitude (degrees), Meeus ch. 47 (truncated). */
    fun moonLatitude(t: Double): Double {
        val a = moonArguments(t)
        var sum = 0.0
        for (term in moonLatitudeTerms) {
            sum += term[4] * eccentricityFactor(term[1], a.eccentricityCorrection) * sin(a.termArgument(term))
        }

        val meanLongitudeTerm = -2235 * sin(a.meanLongitude * DEG2RAD)
        val a3Term = 382 * sin(a.a3 * DEG2RAD)
        val a1MinusFTerm = 175 * sin((a.a1 - a.argumentOfLatitude) * DEG2RAD)
        val a1PlusFTerm = 175 * sin((a.a1 + a.argumentOfLatitude) * DEG2RAD)
        val meanLongitudeMinusMeanAnomalyTerm = 127 * sin((a.meanLongitude - a.meanAnomaly) * DEG2RAD)
        val meanLongitudePlusMeanAnomalyTerm = 115 * sin((a.meanLongitude + a.meanAnomaly) * DEG2RAD)
        sum += meanLongitudeTerm + a3Term + a1MinusFTerm + a1PlusFTerm +
            meanLongitudeMinusMeanAnomalyTerm - meanLongitudePlusMeanAnomalyTerm

        return sum / 1_000_000.0
    }

    /** Earth–Moon centre distance (kilometres), Meeus ch. 47 (truncated). */
    fun moonDistanceKm(t: Double): Double {
        val a = moonArguments(t)
        var sum = 0.0
        for (term in moonLongitudeDistanceTerms) {
            sum += term[5] * eccentricityFactor(term[1], a.eccentricityCorrection) * cos(a.termArgument(term))
        }
        return MEAN_EARTH_MOON_DISTANCE_KM + sum / 1000.0
    }

    /**
     * Converts ecliptic coordinates to equatorial right ascension and declination, Meeus ch. 13.
     *
     * @return (rightAscensionDeg, declinationDeg); right ascension normalised to [0, 360).
     */
    fun equatorialFromEcliptic(
        eclipticLongitudeDeg: Double,
        eclipticLatitudeDeg: Double,
        obliquityDeg: Double,
    ): Pair<Double, Double> {
        val lambda = eclipticLongitudeDeg * DEG2RAD
        val beta = eclipticLatitudeDeg * DEG2RAD
        val eps = obliquityDeg * DEG2RAD
        val declination = asin(sin(beta) * cos(eps) + cos(beta) * sin(eps) * sin(lambda))
        val rightAscension = atan2(sin(lambda) * cos(eps) - tan(beta) * sin(eps), cos(lambda))
        return norm360(rightAscension / DEG2RAD) to declination / DEG2RAD
    }

    /** Greenwich Mean Sidereal Time (degrees) for a UTC instant, Meeus ch. 12. */
    fun greenwichMeanSiderealTimeDeg(epochMillis: Long): Double {
        val daysSinceJ2000 = julianDay(epochMillis) - JD_J2000
        val t = daysSinceJ2000 / DAYS_PER_CENTURY
        val gmst = 280.46061837 + 360.98564736629 * daysSinceJ2000 + 0.000387933 * t * t - t * t * t / 38_710_000.0
        return norm360(gmst)
    }

    /**
     * Lahiri (Chitrapaksha) ayanamsa in degrees — the offset between the tropical and sidereal
     * zodiacs. Linear approximation about the current era (≈50.29″/yr), accurate to well under an
     * arc-minute over the app's date range.
     */
    fun lahiriAyanamsa(t: Double): Double = 23.853 + 1.397 * t
}
