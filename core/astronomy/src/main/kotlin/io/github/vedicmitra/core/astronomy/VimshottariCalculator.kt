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

private const val NAKSHATRA_SPAN_DEGREES = 360.0 / 27.0

// A dasha year is 365.25 days: 365.25 * 86_400_000 ms.
private const val DASHA_YEAR_MILLIS = 31_557_600_000.0

// The nine Vimshottari lords in fixed order with their period lengths in years (summing to 120).
// The nakshatra the Moon occupies at birth selects the starting lord (Ashwini → Ketu, repeating
// every nine nakshatras).
private val VIMSHOTTARI_ORDER: List<Pair<Graha, Int>> =
    listOf(
        Graha.KETU to 7,
        Graha.SHUKRA to 20,
        Graha.SUN to 6,
        Graha.MOON to 10,
        Graha.MANGALA to 7,
        Graha.RAHU to 18,
        Graha.GURU to 16,
        Graha.SHANI to 19,
        Graha.BUDHA to 17,
    )

/** The whole cycle, in years — the denominator every antardasha share is taken against. */
private const val VIMSHOTTARI_TOTAL_YEARS = 120.0

/**
 * The nine antardashas of [mahadasha], in order.
 *
 * The sub-periods run through the same lord sequence starting from the mahadasha's own lord, each
 * taking a share of the parent proportional to its own dasha years: `parentYears × lordYears / 120`.
 * Those shares sum to the parent exactly, so the last one is clamped to the parent's end rather than
 * left a few milliseconds short by integer truncation — the same clamping the Choghadiya windows use
 * so the divisions tile without a visible gap.
 */
internal fun antardashasOf(mahadasha: MahadashaPeriod): List<AntardashaPeriod> {
    val startOrdinal = VIMSHOTTARI_ORDER.indexOfFirst { it.first == mahadasha.lord }
    val parentYears = VIMSHOTTARI_ORDER[startOrdinal].second
    val periods = mutableListOf<AntardashaPeriod>()
    var start = mahadasha.start.toEpochMilliseconds()
    val parentEnd = mahadasha.end.toEpochMilliseconds()

    for (offset in VIMSHOTTARI_ORDER.indices) {
        val (lord, lordYears) = VIMSHOTTARI_ORDER[(startOrdinal + offset) % VIMSHOTTARI_ORDER.size]
        val span = (parentYears.toDouble() * lordYears / VIMSHOTTARI_TOTAL_YEARS * DASHA_YEAR_MILLIS).toLong()
        val last = offset == VIMSHOTTARI_ORDER.lastIndex
        val end = if (last) parentEnd else start + span
        periods +=
            AntardashaPeriod(
                lord = lord,
                start = Instant.fromEpochMilliseconds(start),
                end = Instant.fromEpochMilliseconds(end),
            )
        start = end
    }
    return periods
}

/**
 * The Vimshottari mahadasha timeline for a birth at [epochMillis]: the nine major periods over the
 * 120-year cycle, in order, the first of which contains the birth. Derived from the Moon's sidereal
 * nakshatra and how far the Moon has moved through it.
 */
internal fun vimshottariDasha(epochMillis: Long): List<MahadashaPeriod> =
    vimshottariFromMoon(siderealLongitude(Graha.MOON, Ephemeris.julianCenturies(epochMillis)), epochMillis)

/** [vimshottariDasha] with the Moon's sidereal longitude supplied directly (so it can be tested). */
internal fun vimshottariFromMoon(
    moonLongitude: Double,
    epochMillis: Long,
): List<MahadashaPeriod> {
    val nakshatraIndex = AngularBuckets.nakshatraIndex(moonLongitude)
    val fractionElapsed = (moonLongitude - nakshatraIndex * NAKSHATRA_SPAN_DEGREES) / NAKSHATRA_SPAN_DEGREES
    val startOrdinal = nakshatraIndex % VIMSHOTTARI_ORDER.size
    val firstLordYears = VIMSHOTTARI_ORDER[startOrdinal].second
    val elapsedMillis = (fractionElapsed * firstLordYears * DASHA_YEAR_MILLIS).toLong()

    val periods = mutableListOf<MahadashaPeriod>()
    var periodStart = epochMillis - elapsedMillis
    for (offset in VIMSHOTTARI_ORDER.indices) {
        val (lord, years) = VIMSHOTTARI_ORDER[(startOrdinal + offset) % VIMSHOTTARI_ORDER.size]
        val periodEnd = periodStart + (years * DASHA_YEAR_MILLIS).toLong()
        periods +=
            MahadashaPeriod(
                lord = lord,
                start = Instant.fromEpochMilliseconds(periodStart),
                end = Instant.fromEpochMilliseconds(periodEnd),
            )
        periodStart = periodEnd
    }
    return periods
}
