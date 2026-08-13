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

private const val DAY_MILLIS = 86_400_000L
private const val RASHI_SPAN_DEGREES = 30.0
private const val BISECT_ITERATIONS = 30

/**
 * Each tracked graha's rashi at [epochMillis], with its next rashi ingress (pravesh). The pravesh is
 * found by stepping forward a day at a time until the rashi index changes (in either direction, so
 * retrograde motion is handled) and then bisecting to the crossing instant.
 */
internal fun planetaryPositions(epochMillis: Long): PlanetaryPositions =
    PlanetaryPositions(
        Graha.entries.map { graha ->
            val rashiIndex = rashiIndexAt(graha, epochMillis)
            GrahaPosition(
                graha = graha,
                rasi = Rasi(rashiIndex, RASHI_NAMES[rashiIndex]),
                pravesh = nextPravesh(graha, epochMillis, rashiIndex),
            )
        },
    )

private fun rashiIndexAt(
    graha: Graha,
    epochMillis: Long,
): Int = (siderealLongitude(graha, Ephemeris.julianCenturies(epochMillis)) / RASHI_SPAN_DEGREES).toInt()

/** The next instant [graha] leaves [currentRashi], or `null` if none within its search horizon. */
private fun nextPravesh(
    graha: Graha,
    fromEpochMillis: Long,
    currentRashi: Int,
): Instant? {
    var previous = fromEpochMillis
    var day = 1
    val horizon = praveshHorizonDays(graha)
    while (day <= horizon) {
        val at = fromEpochMillis + day * DAY_MILLIS
        if (rashiIndexAt(graha, at) != currentRashi) {
            return Instant.fromEpochMilliseconds(bisectPravesh(graha, previous, at, currentRashi))
        }
        previous = at
        day++
    }
    return null
}

/** Bisects [lowMillis, highMillis] for the instant [graha] first leaves [currentRashi]. */
private fun bisectPravesh(
    graha: Graha,
    lowMillis: Long,
    highMillis: Long,
    currentRashi: Int,
): Long {
    var low = lowMillis
    var high = highMillis
    repeat(BISECT_ITERATIONS) {
        val mid = low + (high - low) / 2
        if (rashiIndexAt(graha, mid) == currentRashi) low = mid else high = mid
    }
    return high
}

// Search horizons for the next rashi ingress, padded generously to cover retrograde lingering: the
// Moon changes rashi ~every 2.25 days, Mercury/Sun/Venus roughly monthly, Mars ~1.5 months, the
// nodes ~1.5 years, Jupiter ~1 year, Saturn ~2.5 years. Beyond the horizon the pravesh reads null.
private fun praveshHorizonDays(graha: Graha): Int =
    when (graha) {
        Graha.MOON -> 4
        Graha.BUDHA -> 90
        Graha.SUN -> 45
        Graha.SHUKRA -> 150
        Graha.MANGALA -> 260
        Graha.GURU -> 450
        Graha.RAHU -> 700
        Graha.KETU -> 700
        Graha.SHANI -> 1200
    }
