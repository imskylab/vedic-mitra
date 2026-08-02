/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

@file:Suppress("MagicNumber")

package io.github.vedicmitra.core.astronomy

import kotlin.time.Instant

// Which of the eight equal daytime segments each inauspicious kalam occupies, indexed by weekday
// (0 = Sunday .. 6 = Saturday), 1-based segment number.
private val RAHU_KALAM_SEGMENT = intArrayOf(8, 2, 7, 5, 6, 4, 3)
private val YAMAGANDA_SEGMENT = intArrayOf(5, 4, 3, 2, 1, 7, 6)
private val GULIKA_SEGMENT = intArrayOf(7, 6, 5, 4, 3, 2, 1)

private const val MILLIS_PER_MINUTE = 60_000L
private const val BRAHMA_START_BEFORE_SUNRISE_MIN = 96L
private const val BRAHMA_END_BEFORE_SUNRISE_MIN = 48L

/**
 * Computes the day's muhurta windows from the [sunTimes] and [dayOfWeek]:
 *
 * - **Brahma Muhurta** (auspicious) — 96 to 48 minutes before sunrise.
 * - **Abhijit Muhurta** (auspicious) — the 8th of the day's 15 equal parts, around solar noon.
 * - **Rahu Kalam / Yamaganda / Gulika Kalam** (inauspicious) — one of the day's 8 equal parts,
 *   selected by weekday.
 *
 * Returns an empty list when the sun does not both rise and set (polar day/night).
 *
 * @param dayOfWeek 0 = Sunday .. 6 = Saturday (matching [Vara.ordinal]).
 */
internal fun muhurtasOf(
    sunTimes: SunTimes,
    dayOfWeek: Int,
): List<Muhurta> {
    val sunrise = sunTimes.sunrise ?: return emptyList()
    val sunset = sunTimes.sunset ?: return emptyList()

    val sunriseMs = sunrise.toEpochMilliseconds()
    val dayMillis = sunset.toEpochMilliseconds() - sunriseMs
    if (dayMillis <= 0L) return emptyList()

    val eighth = dayMillis / 8
    val fifteenth = dayMillis / 15

    fun window(
        name: String,
        quality: MuhurtaQuality,
        startMs: Long,
        endMs: Long,
    ) = Muhurta(
        name = name,
        start = Instant.fromEpochMilliseconds(startMs),
        end = Instant.fromEpochMilliseconds(endMs),
        quality = quality,
    )

    fun daySegment(
        name: String,
        oneBasedSegment: Int,
    ) = window(
        name = name,
        quality = MuhurtaQuality.INAUSPICIOUS,
        startMs = sunriseMs + (oneBasedSegment - 1) * eighth,
        endMs = sunriseMs + oneBasedSegment * eighth,
    )

    return listOf(
        window(
            name = "Brahma Muhurta",
            quality = MuhurtaQuality.AUSPICIOUS,
            startMs = sunriseMs - BRAHMA_START_BEFORE_SUNRISE_MIN * MILLIS_PER_MINUTE,
            endMs = sunriseMs - BRAHMA_END_BEFORE_SUNRISE_MIN * MILLIS_PER_MINUTE,
        ),
        window(
            name = "Abhijit Muhurta",
            quality = MuhurtaQuality.AUSPICIOUS,
            startMs = sunriseMs + 7 * fifteenth,
            endMs = sunriseMs + 8 * fifteenth,
        ),
        daySegment("Rahu Kalam", RAHU_KALAM_SEGMENT[dayOfWeek]),
        daySegment("Yamaganda", YAMAGANDA_SEGMENT[dayOfWeek]),
        daySegment("Gulika Kalam", GULIKA_SEGMENT[dayOfWeek]),
    )
}
