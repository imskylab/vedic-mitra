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
 * 120-year cycle, in order, the first of which contains the birth.
 */
internal fun vimshottariDasha(epochMillis: Long): List<DashaPeriod> =
    dashaTimeline(DashaSystem.VIMSHOTTARI, epochMillis)

/** Any system's mahadasha timeline for a birth at [epochMillis], from the Moon's own longitude. */
internal fun dashaTimeline(
    system: DashaSystem,
    epochMillis: Long,
): List<DashaPeriod> =
    dashaFromMoon(system, siderealLongitude(Graha.MOON, Ephemeris.julianCenturies(epochMillis)), epochMillis)

/** [vimshottariDasha] with the Moon's sidereal longitude supplied directly (so it can be tested). */
internal fun vimshottariFromMoon(
    moonLongitude: Double,
    epochMillis: Long,
): List<DashaPeriod> = dashaFromMoon(DashaSystem.VIMSHOTTARI, moonLongitude, epochMillis)

/**
 * A system's timeline with the Moon's sidereal longitude supplied directly.
 *
 * The birth sits partway through the first period, and how far is the only thing that differs
 * between systems here: the [DashaStart] table says which lord the birth begins on and where its
 * nakshatra sits in that lord's run, so the elapsed share is `(position + fractionThroughNakshatra)
 * / runLength`. Vimshottari and Yogini give every nakshatra its own lord, making that just the
 * fraction through the nakshatra; Ashtottari's runs are three or four long.
 */
internal fun dashaFromMoon(
    system: DashaSystem,
    moonLongitude: Double,
    epochMillis: Long,
): List<DashaPeriod> {
    val lords = system.lords
    val nakshatraIndex = AngularBuckets.nakshatraIndex(moonLongitude)
    val start = system.starts[nakshatraIndex]
    val throughNakshatra =
        (moonLongitude - nakshatraIndex * NAKSHATRA_SPAN_DEGREES) / NAKSHATRA_SPAN_DEGREES
    val elapsedShare = (start.position + throughNakshatra) / start.runLength

    val startOrdinal = lords.indexOfFirst { it.first == start.lord }
    val firstLordYears = lords[startOrdinal].second
    val elapsedMillis = (elapsedShare * firstLordYears * DASHA_YEAR_MILLIS).toLong()

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
                system = system,
            )
        periodStart = periodEnd
    }
    return periods
}

/**
 * Where a birth in nakshatras 26, 27, 1 or 2 parts company with the reference implementation.
 *
 * Ashtottari's lords cover runs of three or four nakshatras and the first period's elapsed share
 * runs across the whole run, which 49 of 57 sampled births confirm. The eight that do not are
 * exactly Rahu's run — the one wrapping the end of the zodiac back to its start.
 *
 * A controlled sweep pinned the behaviour without explaining it. Holding the birth date fixed and
 * moving only the time of day, the reference's Rahu mahadasha start shifts about **66 days later per
 * eight hours** of birth time, where the run model calls for three years per nakshatra — and the
 * start it reports places the birth *outside* the very period it is supposed to sit in, by roughly
 * seven months before it for nakshatras 26 and 27 and seven months after it for 1 and 2. The
 * relationship is consistent and reproducible across four separate months, so it is a rule of some
 * kind rather than noise, but not one that could be recovered from the data available.
 *
 * So this engine applies the run model uniformly. For the 23 nakshatras outside Rahu's run that
 * matches the reference exactly. For the four inside it the **lord is still right** — that comes
 * from the table and was verified twice over — and only the period boundaries differ.
 */
internal val ashtottariWrapCaveat: String
    get() =
        "Ashtottari period boundaries for a Moon in the last two or first two nakshatras follow " +
            "this engine's own uniform rule; the lord is unaffected."
