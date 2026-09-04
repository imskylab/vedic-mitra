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

// Which of the eight equal daytime segments each inauspicious kalam occupies, indexed by weekday
// (0 = Sunday .. 6 = Saturday), 1-based segment number.
private val RAHU_KALAM_SEGMENT = intArrayOf(8, 2, 7, 5, 6, 4, 3)
private val YAMAGANDA_SEGMENT = intArrayOf(5, 4, 3, 2, 1, 7, 6)
private val GULIKA_SEGMENT = intArrayOf(7, 6, 5, 4, 3, 2, 1)

// Which of the day's fifteen equal parts (the same granularity as Abhijit Muhurta) is Dur
// Muhurta, indexed by weekday (0 = Sunday .. 6 = Saturday), 1-based segment number(s). Every day
// has exactly one, except Saturday which has two consecutive ones. Cross-checked against
// published almanacs for Delhi over 2026-08-02..08 (source, not folklore — a commonly repeated claim
// that every day but Friday has two Dur Muhurtas does not match this real data).
private val DUR_MUHURTA_SEGMENTS =
    arrayOf(
        intArrayOf(14), // Sunday
        intArrayOf(9), // Monday
        intArrayOf(4), // Tuesday
        intArrayOf(8), // Wednesday
        intArrayOf(6), // Thursday
        intArrayOf(4), // Friday
        intArrayOf(1, 2), // Saturday
    )

// Abhijit Muhurta's segment (8th of 15, from sunrise). On weekdays where Dur Muhurta falls in the
// same segment (Wednesday, per DUR_MUHURTA_SEGMENTS), Abhijit does not occur that day.
private const val ABHIJIT_SEGMENT = 8

private const val MILLIS_PER_MINUTE = 60_000L
private const val BRAHMA_START_BEFORE_SUNRISE_MIN = 96L
private const val BRAHMA_END_BEFORE_SUNRISE_MIN = 48L

/**
 * Computes the day's sun/weekday-based muhurta windows from the [sunTimes] and [dayOfWeek]:
 *
 * - **Brahma Muhurta** (auspicious) — 96 to 48 minutes before sunrise.
 * - **Abhijit Muhurta** (auspicious) — the 8th of the day's 15 equal parts, around solar noon;
 *   absent on days where [DUR_MUHURTA_SEGMENTS] occupies the same segment (Wednesday).
 * - **Rahu Kalam / Yamaganda / Gulika Kalam** (inauspicious) — one of the day's 8 equal parts,
 *   selected by weekday.
 * - **Dur Muhurta** (inauspicious) — one or two of the day's 15 equal parts, selected by weekday.
 *
 * Returns an empty list when the sun does not both rise and set (polar day/night). The Varjyam
 * window (which depends on the Moon's position, not just the Sun/weekday) is computed separately
 * by [varjyamOf] and appended by the caller.
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
        kind: MuhurtaKind,
        name: String,
        quality: MuhurtaQuality,
        startMs: Long,
        endMs: Long,
    ) = Muhurta(
        kind = kind,
        name = name,
        start = Instant.fromEpochMilliseconds(startMs),
        end = Instant.fromEpochMilliseconds(endMs),
        quality = quality,
    )

    fun daySegment(
        kind: MuhurtaKind,
        oneBasedSegment: Int,
        eighthOrFifteenth: Long,
        name: String = kind.label,
    ) = window(
        kind = kind,
        name = name,
        quality = MuhurtaQuality.INAUSPICIOUS,
        startMs = sunriseMs + (oneBasedSegment - 1) * eighthOrFifteenth,
        endMs = sunriseMs + oneBasedSegment * eighthOrFifteenth,
    )

    val durMuhurtaSegments = DUR_MUHURTA_SEGMENTS[dayOfWeek]
    val durMuhurtas =
        durMuhurtaSegments.mapIndexed { index, segment ->
            // Numbered only when there are two, and only for display -- both are DUR_MUHURTA.
            val name =
                if (durMuhurtaSegments.size == 1) {
                    MuhurtaKind.DUR_MUHURTA.label
                } else {
                    "${MuhurtaKind.DUR_MUHURTA.label} ${index + 1}"
                }
            daySegment(MuhurtaKind.DUR_MUHURTA, segment, fifteenth, name)
        }
    val abhijit =
        if (ABHIJIT_SEGMENT !in durMuhurtaSegments) {
            listOf(
                window(
                    kind = MuhurtaKind.ABHIJIT,
                    name = MuhurtaKind.ABHIJIT.label,
                    quality = MuhurtaQuality.AUSPICIOUS,
                    startMs = sunriseMs + 7 * fifteenth,
                    endMs = sunriseMs + 8 * fifteenth,
                ),
            )
        } else {
            emptyList()
        }

    return buildList {
        add(
            window(
                kind = MuhurtaKind.BRAHMA,
                name = MuhurtaKind.BRAHMA.label,
                quality = MuhurtaQuality.AUSPICIOUS,
                startMs = sunriseMs - BRAHMA_START_BEFORE_SUNRISE_MIN * MILLIS_PER_MINUTE,
                endMs = sunriseMs - BRAHMA_END_BEFORE_SUNRISE_MIN * MILLIS_PER_MINUTE,
            ),
        )
        addAll(abhijit)
        add(daySegment(MuhurtaKind.RAHU_KALAM, RAHU_KALAM_SEGMENT[dayOfWeek], eighth))
        add(daySegment(MuhurtaKind.YAMAGANDA, YAMAGANDA_SEGMENT[dayOfWeek], eighth))
        add(daySegment(MuhurtaKind.GULIKA_KALAM, GULIKA_SEGMENT[dayOfWeek], eighth))
        addAll(durMuhurtas)
    }
}
