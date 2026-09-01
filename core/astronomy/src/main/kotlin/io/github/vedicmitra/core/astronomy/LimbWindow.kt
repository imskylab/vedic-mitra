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

import kotlin.time.Duration
import kotlin.time.Instant

/**
 * The stretch of time over which one panchanga limb keeps its current value — when this tithi,
 * nakshatra, yoga or karana began and when it gives way to the next.
 *
 * @property start when the limb took its current value.
 * @property end when it next changes.
 * @property angularFraction how far through the division the driving angle has travelled, `[0, 1)`.
 *   Deliberately *angular* rather than temporal: the Moon's speed varies by about 30% between
 *   apogee and perigee, so this is not the same as the fraction of the window's duration that has
 *   elapsed. It is exact and free to compute, which makes it the right input for a progress arc.
 */
data class LimbWindow(
    val start: Instant,
    val end: Instant,
    val angularFraction: Double,
) {
    /** How long the limb holds this value in total. */
    val duration: Duration get() = end - start

    /** How long until it changes, measured from [at]. Never negative. */
    fun remainingFrom(at: Instant): Duration = (end - at).coerceAtLeast(Duration.ZERO)

    /** How long it has held this value as of [at]. Never negative. */
    fun elapsedAt(at: Instant): Duration = (at - start).coerceAtLeast(Duration.ZERO)
}

/**
 * Validity windows for every limb the day card shows.
 *
 * This once covered only the fast limbs, on the reasoning that a months-long countdown reads as
 * noise. That rule contradicted itself: the Sun's rashi was included *because the ingress date is
 * genuinely useful*, and the lunar month turns on the same ~30-day scale. The distinction that
 * actually matters is between a **neighbour** and a **boundary** — the neighbour is uninteresting
 * for ayana (there are only two, so it is always the other one) and for samvatsara (last year's
 * name is trivia), while the boundary is worth showing for all four. So the windows are computed
 * here for everything, and it is the UI that decides which limbs also deserve their neighbours.
 *
 * @property vara the weekday's window — sunrise to sunrise, the one limb here that is not angular.
 * @property ritu the season's window; its boundary is what ritucharya turns on.
 * @property ayana the half-year's window — Makara and Karka Sankranti.
 * @property samvatsara the lunar year's window, Ugadi to Ugadi.
 */
data class PanchangaLimbWindows(
    val tithi: LimbWindow,
    val nakshatra: LimbWindow,
    val yoga: LimbWindow,
    val karana: LimbWindow,
    val moonPada: LimbWindow,
    val moonRashi: LimbWindow,
    val moonPhase: LimbWindow,
    val sunRashi: LimbWindow,
    val vara: LimbWindow? = null,
    val ritu: LimbWindow? = null,
    val ayana: LimbWindow? = null,
    val samvatsara: LimbWindow? = null,
)

/**
 * Solves for the instants at which a bucketed angle enters and leaves its current division.
 *
 * The four panchanga angles are all **monotonically increasing**: the Moon's apparent longitude
 * never decreases, and neither does the Sun's, so neither their difference (tithi, karana) nor
 * their sum (yoga) ever runs backwards. That is what makes a plain bisection on "has the bucket
 * changed yet?" safe here — it would not be for a graha that can retrograde, which is why rashi
 * ingress for the other planets goes through [PlanetaryPositionsCalculator] instead.
 *
 * Bisection rather than extrapolation is not fussiness. Dividing the remaining arc by a mean daily
 * motion is wrong by up to about 30% near perigee and apogee, which for a tithi is well over an
 * hour.
 */
internal object LimbWindowSolver {
    /** Bisection stops once the bracket is this tight; the displayed value is whole minutes. */
    private const val TOLERANCE_MILLIS = 1_000L

    /**
     * The window containing [atMillis] for a limb whose current division is identified by
     * [bucketAt]. [maxSpanMillis] must comfortably exceed the longest the division can last, since
     * it bounds the search in both directions.
     */
    fun windowAt(
        atMillis: Long,
        maxSpanMillis: Long,
        angularFraction: Double,
        bucketAt: (Long) -> Int,
    ): LimbWindow {
        val current = bucketAt(atMillis)
        val end = firstChange(atMillis, atMillis + maxSpanMillis, current, bucketAt)
        val start = lastChange(atMillis - maxSpanMillis, atMillis, current, bucketAt)
        return LimbWindow(
            start = Instant.fromEpochMilliseconds(start),
            end = Instant.fromEpochMilliseconds(end),
            angularFraction = angularFraction,
        )
    }

    /** The first instant after [from] whose bucket differs from [current], bisected in `[from, to]`. */
    private fun firstChange(
        from: Long,
        to: Long,
        current: Int,
        bucketAt: (Long) -> Int,
    ): Long {
        if (bucketAt(to) == current) return to
        var lo = from
        var hi = to
        while (hi - lo > TOLERANCE_MILLIS) {
            val mid = lo + (hi - lo) / 2
            if (bucketAt(mid) == current) lo = mid else hi = mid
        }
        return hi
    }

    /** The first instant in `[from, to]` whose bucket equals [current] — i.e. when it began. */
    private fun lastChange(
        from: Long,
        to: Long,
        current: Int,
        bucketAt: (Long) -> Int,
    ): Long {
        if (bucketAt(from) == current) return from
        var lo = from
        var hi = to
        while (hi - lo > TOLERANCE_MILLIS) {
            val mid = lo + (hi - lo) / 2
            if (bucketAt(mid) == current) hi = mid else lo = mid
        }
        return hi
    }
}

// Longest each division can last, with margin for the Moon's speed varying by ~30% between apogee
// and perigee. These only bound the bisection bracket, so generous is safe and too tight is not.
private const val HOUR_MILLIS = 3_600_000L
private const val TITHI_SPAN = 30 * HOUR_MILLIS
private const val NAKSHATRA_SPAN = 30 * HOUR_MILLIS
private const val YOGA_SPAN = 32 * HOUR_MILLIS
private const val KARANA_SPAN = 16 * HOUR_MILLIS
private const val PADA_SPAN = 10 * HOUR_MILLIS
private const val MOON_RASHI_SPAN = 70 * HOUR_MILLIS
private const val MOON_PHASE_SPAN = 96 * HOUR_MILLIS
private const val SUN_RASHI_SPAN = 33 * 24 * HOUR_MILLIS
private const val RITU_SPAN = 70L * 24 * HOUR_MILLIS
private const val AYANA_SPAN = 200L * 24 * HOUR_MILLIS
private const val RITU_ARCSEC = 216_000L // 60 degrees
private const val AYANA_ARCSEC = 648_000L // 180 degrees
private const val VASANTA_START_DEG = 330.0
private const val UTTARAYANA_START_DEG = 270.0
private const val MOON_PHASE_ARCSEC = 162_000L
private const val HALF_PHASE_DEG = 22.5

/**
 * Every limb window for [epochMillis], with the weekday's sunrise-to-sunrise window folded in when
 * the sun both rises today and tomorrow (it does not, inside the polar circles).
 *
 * Roughly 270 ephemeris evaluations for the angular limbs — two bisections per limb at about 17
 * iterations each — plus the lunar year's two Chaitra walks, which are syzygy searches rather than
 * boundary crossings and cost more. That is fine for the single-moment `snapshotAt` path, and is
 * exactly why these windows are **not** part of `daySummaryAt`, which the calendar grid calls once
 * per day of the month.
 */
internal fun limbWindowsAt(
    epochMillis: Long,
    sunrise: Instant?,
    nextSunrise: Instant?,
): PanchangaLimbWindows {
    val elongationAt = { at: Long -> angles(at).elongation }
    val moonSiderealAt = { at: Long -> angles(at).moonSidereal }
    val sunSiderealAt = { at: Long -> angles(at).sunSidereal }
    val yogaSumAt = { at: Long -> angles(at).yogaSum }
    val now = angles(epochMillis)

    fun window(
        span: Long,
        arcsec: Long,
        angle: Double,
        angleAt: (Long) -> Double,
    ): LimbWindow =
        LimbWindowSolver.windowAt(
            atMillis = epochMillis,
            maxSpanMillis = span,
            angularFraction = AngularBuckets.fractionThrough(angle, arcsec),
            bucketAt = { at -> AngularBuckets.index(angleAt(at), arcsec) },
        )

    return PanchangaLimbWindows(
        tithi = window(TITHI_SPAN, AngularBuckets.TITHI_ARCSEC, now.elongation, elongationAt),
        nakshatra = window(NAKSHATRA_SPAN, AngularBuckets.NAKSHATRA_ARCSEC, now.moonSidereal, moonSiderealAt),
        yoga = window(YOGA_SPAN, AngularBuckets.NAKSHATRA_ARCSEC, now.yogaSum, yogaSumAt),
        karana = window(KARANA_SPAN, AngularBuckets.KARANA_ARCSEC, now.elongation, elongationAt),
        moonPada = window(PADA_SPAN, AngularBuckets.PADA_ARCSEC, now.moonSidereal, moonSiderealAt),
        moonRashi = window(MOON_RASHI_SPAN, AngularBuckets.RASHI_ARCSEC, now.moonSidereal, moonSiderealAt),
        moonPhase =
            window(MOON_PHASE_SPAN, MOON_PHASE_ARCSEC, now.elongation + HALF_PHASE_DEG) { at ->
                elongationAt(at) + HALF_PHASE_DEG
            },
        sunRashi = window(SUN_RASHI_SPAN, AngularBuckets.RASHI_ARCSEC, now.sunSidereal, sunSiderealAt),
        vara = if (sunrise != null && nextSunrise != null) varaWindow(epochMillis, sunrise, nextSunrise) else null,
        // Ritu and ayana are plain solar-longitude buckets, so they reuse the same monotonic
        // bisection as the fast limbs -- only the bucket width and the search bracket differ.
        ritu =
            window(RITU_SPAN, RITU_ARCSEC, now.sunSidereal - VASANTA_START_DEG) { at ->
                sunSiderealAt(at) - VASANTA_START_DEG
            },
        ayana =
            window(AYANA_SPAN, AYANA_ARCSEC, now.sunSidereal - UTTARAYANA_START_DEG) { at ->
                sunSiderealAt(at) - UTTARAYANA_START_DEG
            },
        samvatsara = samvatsaraWindow(epochMillis, elongationAt, sunSiderealAt),
    )
}

/**
 * The lunar year's window: the Chaitra that opened it, to the Chaitra that opens the next.
 *
 * Not an angle bucket. A lunar year is twelve *or thirteen* lunations, so its boundaries have to be
 * found by walking new moons the same way [samvatsaraOf] does — there is no fixed offset that lands
 * on Ugadi in both kinds of year.
 */
private fun samvatsaraWindow(
    epochMillis: Long,
    elongationAt: (Long) -> Double,
    sunSiderealAt: (Long) -> Double,
): LimbWindow {
    val start = chaitraStartAtOrBefore(epochMillis, elongationAt, sunSiderealAt)
    val end = chaitraStartAfter(start, elongationAt, sunSiderealAt)
    val span = (end - start).toDouble()
    return LimbWindow(
        start = Instant.fromEpochMilliseconds(start),
        end = Instant.fromEpochMilliseconds(end),
        angularFraction = if (span > 0) ((epochMillis - start) / span).coerceIn(0.0, 1.0) else 0.0,
    )
}

/** The weekday's window. Non-angular: the vedic day runs sunrise to sunrise. */
private fun varaWindow(
    epochMillis: Long,
    sunrise: Instant,
    nextSunrise: Instant,
): LimbWindow {
    val span = (nextSunrise - sunrise).inWholeMilliseconds
    val elapsed = epochMillis - sunrise.toEpochMilliseconds()
    return LimbWindow(
        start = sunrise,
        end = nextSunrise,
        angularFraction = if (span > 0) (elapsed.toDouble() / span).coerceIn(0.0, 1.0) else 0.0,
    )
}

/** The four driving angles at one instant, computed together so each limb shares the ephemeris. */
private data class SolarLunarAngles(
    val elongation: Double,
    val sunSidereal: Double,
    val moonSidereal: Double,
    val yogaSum: Double,
)

private fun angles(atMillis: Long): SolarLunarAngles {
    val t = Ephemeris.julianCenturies(atMillis)
    val sun = Ephemeris.sunApparentLongitude(t)
    val moon = Ephemeris.moonLongitude(t)
    val ayanamsa = Ephemeris.lahiriAyanamsa(t)
    val sunSidereal = Ephemeris.norm360(sun - ayanamsa)
    val moonSidereal = Ephemeris.norm360(moon - ayanamsa)
    return SolarLunarAngles(
        elongation = Ephemeris.norm360(moon - sun),
        sunSidereal = sunSidereal,
        moonSidereal = moonSidereal,
        yogaSum = Ephemeris.norm360(sunSidereal + moonSidereal),
    )
}
