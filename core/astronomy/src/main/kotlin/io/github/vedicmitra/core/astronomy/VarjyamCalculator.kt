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

import kotlin.time.Instant

// Nakshatra Thyajyam ("Varjyam" / "Visha Ghatis") ghati ranges, in NAKSHATRA_NAMES order (index
// 0 = Ashwini .. 26 = Revati). A ghati is 1/60th of a day (24 minutes); the range is counted from
// the moment the Moon enters that nakshatra, not from sunrise or midnight. Every nakshatra's window
// is exactly 4 ghatis (96 minutes). Source: a published Nakshatra Thyajyam table, cross-checked against almanacs.
private val VARJYAM_GHATIS =
    arrayOf(
        51 to 54, // Ashwini
        25 to 28, // Bharani
        31 to 34, // Krittika
        41 to 44, // Rohini
        15 to 18, // Mrigashira
        22 to 25, // Ardra
        31 to 34, // Punarvasu
        21 to 24, // Pushya
        33 to 36, // Ashlesha
        31 to 34, // Magha
        21 to 24, // Purva Phalguni
        19 to 22, // Uttara Phalguni
        22 to 25, // Hasta
        21 to 24, // Chitra
        15 to 18, // Swati
        15 to 18, // Vishakha
        11 to 14, // Anuradha
        15 to 18, // Jyeshtha
        57 to 60, // Mula
        25 to 28, // Purva Ashadha
        21 to 24, // Uttara Ashadha
        11 to 14, // Shravana
        11 to 14, // Dhanishta
        19 to 22, // Shatabhisha
        17 to 20, // Purva Bhadrapada
        25 to 28, // Uttara Bhadrapada
        31 to 34, // Revati
    )

private const val NAKSHATRA_SPAN_DEG = 360.0 / 27.0
private const val GHATI_MINUTES = 24L
private const val MILLIS_PER_MINUTE = 60_000L

// Wide enough to safely contain one nakshatra's transit (~21.9-26.8 hours depending on the Moon's
// real, non-uniform speed), with margin.
private const val SEARCH_WINDOW_HOURS = 32L
private const val BISECTION_ITERATIONS = 30

/**
 * Computes the current Varjyam (Nakshatra Thyajyam) window: an inauspicious period within the
 * current nakshatra, positioned by a traditional ghati (24-minute unit) range counted from the
 * moment the Moon entered that nakshatra. Finding that start requires a backward search for when
 * the Moon's sidereal longitude crossed the nakshatra boundary, since — unlike the Sun's sunrise
 * and sunset — the Moon moves too fast, and too irregularly, for a closed-form formula.
 *
 * @param atEpochMillis the instant the snapshot is for.
 * @param moonSiderealDeg the Moon's sidereal ecliptic longitude at [atEpochMillis] (degrees, 0..360).
 * @param moonSiderealDegAt recomputes the Moon's sidereal longitude at an arbitrary past instant;
 *   used only to search backward for the nakshatra's start.
 */
internal fun varjyamOf(
    atEpochMillis: Long,
    moonSiderealDeg: Double,
    moonSiderealDegAt: (Long) -> Double,
): Muhurta {
    val nakshatraIndex = AngularBuckets.nakshatraIndex(moonSiderealDeg) // 0..26
    val boundaryDeg = nakshatraIndex * NAKSHATRA_SPAN_DEG
    val nakshatraStartMs = findNakshatraStart(atEpochMillis, boundaryDeg, moonSiderealDegAt)

    val (startGhati, endGhati) = VARJYAM_GHATIS[nakshatraIndex]
    val startMs = nakshatraStartMs + (startGhati - 1) * GHATI_MINUTES * MILLIS_PER_MINUTE
    val endMs = nakshatraStartMs + endGhati * GHATI_MINUTES * MILLIS_PER_MINUTE

    return Muhurta(
        kind = MuhurtaKind.VARJYAM,
        name = MuhurtaKind.VARJYAM.label,
        start = Instant.fromEpochMilliseconds(startMs),
        end = Instant.fromEpochMilliseconds(endMs),
        quality = MuhurtaQuality.INAUSPICIOUS,
    )
}

/**
 * Finds the instant, at or before [beforeEpochMillis], at which the Moon's sidereal longitude last
 * crossed [boundaryDeg] — i.e. when the current nakshatra began. Uses bisection over a window wide
 * enough to safely contain one nakshatra's transit, comparing signed angular distance from the
 * boundary (wrapped to the nearest branch) so it works correctly even when the boundary is near the
 * 0°/360° wrap point (the Ashwini/Revati seam).
 */
internal fun findNakshatraStart(
    beforeEpochMillis: Long,
    boundaryDeg: Double,
    moonSiderealDegAt: (Long) -> Double,
): Long {
    fun distancePastBoundary(epochMillis: Long): Double {
        var delta = moonSiderealDegAt(epochMillis) - boundaryDeg
        if (delta > 180.0) delta -= 360.0
        if (delta < -180.0) delta += 360.0
        return delta
    }

    var lo = beforeEpochMillis - SEARCH_WINDOW_HOURS * 3_600_000L
    var hi = beforeEpochMillis
    repeat(BISECTION_ITERATIONS) {
        val mid = (lo + hi) / 2
        if (distancePastBoundary(mid) >= 0.0) hi = mid else lo = mid
    }
    return hi
}
