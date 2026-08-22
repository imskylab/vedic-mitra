/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.astronomy

import kotlin.math.floor

/**
 * Exact bucketing of ecliptic longitudes into the panchanga's angular divisions.
 *
 * Every division of the zodiac used here is a whole number of **arcseconds**, while most are
 * non-terminating in degrees:
 *
 * | Division  | Arcseconds | Degrees   |
 * |-----------|------------|-----------|
 * | Rashi     | 108,000    | 30        |
 * | Nakshatra | 48,000     | 13.333…   |
 * | Pada      | 12,000     | 3.333…    |
 * | Tithi     | 43,200     | 12        |
 * | Karana    | 21,600     | 6         |
 *
 * Bucketing in degrees therefore divides by a value the machine cannot represent, and the boundary
 * comparison depends on which way two separate roundings happen to fall. Converting to integer
 * arcseconds once and dividing with integer arithmetic makes every boundary exact instead.
 *
 * All buckets are **half-open**, `[start, end)` — a longitude exactly on a boundary belongs to the
 * division that is starting, never the one that is ending. So 26°40′00″ is Krittika, not Bharani.
 */
internal object AngularBuckets {
    /** A full turn in arcseconds. Every division below is an exact divisor of this. */
    const val FULL_TURN_ARCSEC = 1_296_000L

    const val RASHI_ARCSEC = 108_000L
    const val NAKSHATRA_ARCSEC = 48_000L
    const val PADA_ARCSEC = 12_000L
    const val TITHI_ARCSEC = 43_200L
    const val KARANA_ARCSEC = 21_600L

    const val NAKSHATRA_COUNT = 27
    const val PADAS_PER_NAKSHATRA = 4

    /**
     * A longitude in degrees as integer arcseconds, normalised to `[0, 1_296_000)`.
     *
     * [floor] rather than truncation so negative inputs wrap correctly rather than folding toward
     * zero, and the modulo is taken after flooring so the result is never the full turn itself.
     */
    fun arcseconds(degrees: Double): Long {
        val arcsec = floor(degrees * ARCSEC_PER_DEGREE).toLong()
        return ((arcsec % FULL_TURN_ARCSEC) + FULL_TURN_ARCSEC) % FULL_TURN_ARCSEC
    }

    /** The zero-based index of [degrees] within divisions [spanArcsec] wide. */
    fun index(
        degrees: Double,
        spanArcsec: Long,
    ): Int = (arcseconds(degrees) / spanArcsec).toInt()

    /** The zero-based nakshatra (0 = Ashwini .. 26 = Revati) of a sidereal longitude. */
    fun nakshatraIndex(siderealDeg: Double): Int = index(siderealDeg, NAKSHATRA_ARCSEC)

    /** The pada (1..4) — which quarter of its nakshatra a sidereal longitude occupies. */
    fun pada(siderealDeg: Double): Int = ((arcseconds(siderealDeg) % NAKSHATRA_ARCSEC) / PADA_ARCSEC).toInt() + 1

    /** The zero-based rashi (0 = Mesha .. 11 = Meena) of a sidereal longitude. */
    fun rashiIndex(siderealDeg: Double): Int = index(siderealDeg, RASHI_ARCSEC)

    /** The zero-based tithi (0..29) of a Sun–Moon elongation. */
    fun tithiIndex(elongationDeg: Double): Int = index(elongationDeg, TITHI_ARCSEC)

    /** The zero-based karana (0..59) of a Sun–Moon elongation. */
    fun karanaIndex(elongationDeg: Double): Int = index(elongationDeg, KARANA_ARCSEC)

    /** The zero-based yoga (0..26) of the combined sidereal longitudes of the Sun and Moon. */
    fun yogaIndex(yogaSumDeg: Double): Int = index(yogaSumDeg, NAKSHATRA_ARCSEC)

    /**
     * How far through its division [degrees] has travelled, as a fraction in `[0, 1)`.
     *
     * This is an *angular* fraction, not a temporal one: the Moon's speed varies between roughly
     * 11.8°/day at apogee and 15.4°/day at perigee, so a limb that is 50% through by angle is not
     * 50% through by time. Use it for progress indicators; use [LimbWindow] for real timings.
     */
    fun fractionThrough(
        degrees: Double,
        spanArcsec: Long,
    ): Double = (arcseconds(degrees) % spanArcsec).toDouble() / spanArcsec.toDouble()
}

private const val ARCSEC_PER_DEGREE = 3600.0
