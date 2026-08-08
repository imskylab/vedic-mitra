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

private const val SLOTS_PER_HALF = 8
private const val CYCLE_LENGTH = 7

/**
 * Computes the day's sixteen Choghadiya windows: eight daytime (sunrise→sunset) and eight night
 * (sunset→next sunrise), each an equal division of its half. The names step through
 * [ChoghadiyaName]'s fixed cyclic order from a weekday-determined start.
 *
 * The day's first window is at cyclic index `(dayOfWeek * 3) mod 7`, and the night's first is five
 * further along `((dayOfWeek * 3) + 5) mod 7`. This reproduces the traditional vaar tables — e.g.
 * Sunday's day begins **Udveg** and night begins **Shubh**; Monday's day begins **Amrit**.
 *
 * Returns an empty list when the sun does not both rise and set, or the following sunrise is
 * unknown (polar day/night), since the halves cannot be bounded.
 *
 * @param dayOfWeek 0 = Sunday .. 6 = Saturday (matching [Vara.ordinal]).
 */
internal fun choghadiyaOf(
    sunTimes: SunTimes,
    nextSunrise: Instant?,
    dayOfWeek: Int,
): List<Choghadiya> {
    val sunriseMs = sunTimes.sunrise?.toEpochMilliseconds()
    val sunsetMs = sunTimes.sunset?.toEpochMilliseconds()
    val nextSunriseMs = nextSunrise?.toEpochMilliseconds()
    // Bail on polar days: the sun must both rise and set, and the following sunrise must be known.
    if (sunriseMs == null || sunsetMs == null || nextSunriseMs == null) return emptyList()
    // The day half must be bounded (sunrise < sunset) and the night half (sunset < next sunrise).
    if (sunsetMs <= sunriseMs || nextSunriseMs <= sunsetMs) return emptyList()

    val dayStart = (dayOfWeek * 3).mod(CYCLE_LENGTH)
    val nightStart = (dayStart + 5).mod(CYCLE_LENGTH)

    return half(sunriseMs, sunsetMs, dayStart, isDay = true) +
        half(sunsetMs, nextSunriseMs, nightStart, isDay = false)
}

/** The eight equal Choghadiya windows filling [startMs, endMs], named from [startIndex] onward. */
private fun half(
    startMs: Long,
    endMs: Long,
    startIndex: Int,
    isDay: Boolean,
): List<Choghadiya> {
    val names = ChoghadiyaName.entries
    val slot = (endMs - startMs) / SLOTS_PER_HALF
    return (0 until SLOTS_PER_HALF).map { i ->
        Choghadiya(
            name = names[(startIndex + i).mod(CYCLE_LENGTH)],
            start = Instant.fromEpochMilliseconds(startMs + i * slot),
            // Clamp the final window to the exact boundary so the halves tile without an integer-
            // division gap (start + 8*slot can fall a few ms short of endMs).
            end = Instant.fromEpochMilliseconds(if (i == SLOTS_PER_HALF - 1) endMs else startMs + (i + 1) * slot),
            isDay = isDay,
        )
    }
}
