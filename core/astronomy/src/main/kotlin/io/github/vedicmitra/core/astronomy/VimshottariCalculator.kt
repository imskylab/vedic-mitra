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

/**
 * A dasha year is the **sidereal** year, 365.2564 days.
 *
 * This was 365.25, the Julian year, and the difference is not academic: it is about six hours per
 * century of elapsed timeline, which reaches **eighteen hours** by the end of the 120-year cycle.
 * Fitting the constant against an independent implementation's 729 published period boundaries put
 * the worst disagreement at 66,255 seconds for 365.25 and 604 seconds for 365.2564, and that
 * remaining ten minutes is fully accounted for by the four-decimal rounding of the Moon longitude
 * fed into the comparison. Candidate values that are not the sidereal year — the Gregorian and
 * tropical years — came out four to six times worse than the Julian one, so this is not curve
 * fitting; the sidereal year is what is actually in use.
 *
 * Authorities do differ, and some texts intend a 360-day year, which is a different timeline
 * altogether rather than a refinement of this one. This app follows the sidereal year, consistent
 * with computing everything else from real positions.
 */
private const val DASHA_YEAR_MILLIS = 365.2564 * 24 * 60 * 60 * 1000

/**
 * The sub-periods of [parent], in order.
 *
 * They run through the system's lord sequence starting from the parent's own lord, each taking a
 * share of the parent proportional to its own dasha years: `parentSpan × lordYears / totalYears`.
 * Those shares sum to the parent exactly, so the last is clamped to the parent's end rather than
 * left a few milliseconds short by integer truncation — the same clamping the Choghadiya windows use
 * so divisions tile without a visible gap.
 *
 * Identical at every depth, which is the whole point: mahadasha into antardasha and antardasha into
 * pratyantardasha are one operation, not two.
 */
internal fun subPeriodsOf(parent: DashaPeriod): List<DashaPeriod> {
    val lords = parent.system.lords
    val startOrdinal = lords.indexOfFirst { it.first == parent.lord }
    if (startOrdinal < 0) return emptyList()
    val parentSpan = parent.end.toEpochMilliseconds() - parent.start.toEpochMilliseconds()
    val total = parent.system.totalYears.toDouble()

    val periods = mutableListOf<DashaPeriod>()
    var start = parent.start.toEpochMilliseconds()
    val parentEnd = parent.end.toEpochMilliseconds()
    for (offset in lords.indices) {
        val (lord, lordYears) = lords[(startOrdinal + offset) % lords.size]
        val span = (parentSpan.toDouble() * lordYears / total).toLong()
        val end = if (offset == lords.lastIndex) parentEnd else start + span
        periods +=
            DashaPeriod(
                lord = lord,
                start = Instant.fromEpochMilliseconds(start),
                end = Instant.fromEpochMilliseconds(end),
                level = parent.level + 1,
                system = parent.system,
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
internal fun vimshottariDasha(epochMillis: Long): List<DashaPeriod> =
    vimshottariFromMoon(siderealLongitude(Graha.MOON, Ephemeris.julianCenturies(epochMillis)), epochMillis)

/** [vimshottariDasha] with the Moon's sidereal longitude supplied directly (so it can be tested). */
internal fun vimshottariFromMoon(
    moonLongitude: Double,
    epochMillis: Long,
): List<DashaPeriod> {
    val lords = DashaSystem.VIMSHOTTARI.lords
    val nakshatraIndex = AngularBuckets.nakshatraIndex(moonLongitude)
    val fractionElapsed = (moonLongitude - nakshatraIndex * NAKSHATRA_SPAN_DEGREES) / NAKSHATRA_SPAN_DEGREES
    val startOrdinal = nakshatraIndex % lords.size
    val firstLordYears = lords[startOrdinal].second
    val elapsedMillis = (fractionElapsed * firstLordYears * DASHA_YEAR_MILLIS).toLong()

    val periods = mutableListOf<DashaPeriod>()
    var periodStart = epochMillis - elapsedMillis
    for (offset in lords.indices) {
        val (lord, years) = lords[(startOrdinal + offset) % lords.size]
        val periodEnd = periodStart + (years * DASHA_YEAR_MILLIS).toLong()
        periods +=
            DashaPeriod(
                lord = lord,
                start = Instant.fromEpochMilliseconds(periodStart),
                end = Instant.fromEpochMilliseconds(periodEnd),
                level = 1,
                system = DashaSystem.VIMSHOTTARI,
            )
        periodStart = periodEnd
    }
    return periods
}

/*
 * Ashtottari and Yogini are deliberately absent, and this records how far they got so the work is
 * not repeated from scratch.
 *
 * Both had their lord order and year tables derived cleanly from an independent implementation's
 * period durations -- Ashtottari's eight lords over 108 years, with no Ketu; Yogini's eight over 36,
 * one to eight years each. Two independent sweeps across all 27 nakshatras agreed on every starting
 * lord, so the data is solid. What does not yet check out is the rest:
 *
 * - **Ashtottari** measures the elapsed fraction of its first period through the *lord's group of
 *   nakshatras* rather than through the single nakshatra, which 49 of 57 sampled births confirm. All
 *   eight failures are the Rahu group, and only that group -- the one wrapping the end of the zodiac
 *   back to the beginning, nakshatras 26, 27, 1 and 2. There the observed fraction comes out
 *   negative or above one, meaning the birth falls outside the very period it is supposed to sit in.
 *   Some different handling of the wrap is involved and guessing at it would be inventing a rule.
 *
 * - **Yogini's** starting lord advances by one per nakshatra, except for a single irregular step
 *   between nakshatras 6 and 7 where it moves back two. Both sweeps reproduce it, so it is not
 *   noise, but one unexplained discontinuity in a mapping is exactly the shape of somebody else's
 *   off-by-one, and encoding it would import their bug as our behaviour.
 *
 * Neither is hard to finish once its remaining question is answered. Shipping them now would mean
 * shipping a table nobody can account for, which is the one thing the validation work exists to
 * prevent.
 */
