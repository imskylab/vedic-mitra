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

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.tan
import kotlin.time.Instant

/**
 * Sunrise/sunset and golden-hour times via the NOAA solar-position equations. Uses local mean solar
 * time (from longitude) to pick the civil day, which matches the Vedic convention of local — not
 * zone — time.
 */
internal object SolarDay {
    private const val DEG2RAD = PI / 180.0
    private const val MILLIS_PER_DAY = 86_400_000L
    private const val MILLIS_PER_MINUTE = 60_000.0
    private const val MILLIS_PER_HOUR = 3_600_000.0

    // Sun-centre zenith at rise/set: 90° + 0.833° for refraction and the solar semi-diameter.
    private const val RISE_SET_ZENITH = 90.833

    // Golden hour: Sun elevation between -4° and +6° (zenith = 90° - elevation), the conventional
    // photography definition of soft, warm light shortly after sunrise and before sunset.
    private const val GOLDEN_HOUR_OUTER_ZENITH = 94.0
    private const val GOLDEN_HOUR_INNER_ZENITH = 84.0

    /** Per-day solar geometry, computed once and shared by [sunTimes] and [goldenHour]. */
    private class Context(
        val lat: Double,
        val dec: Double,
        val noonMinutes: Double,
        val dayBaseMillis: Long,
    ) {
        fun instantAt(minutes: Double): Instant =
            Instant.fromEpochMilliseconds(dayBaseMillis + (minutes * MILLIS_PER_MINUTE).toLong())

        /** The hour angle (degrees) at which the Sun reaches [zenithDeg], or `null` if it never does. */
        fun hourAngleDeg(zenithDeg: Double): Double? {
            val cosH = cos(zenithDeg * DEG2RAD) / (cos(lat) * cos(dec)) - tan(lat) * tan(dec)
            if (cosH > 1.0 || cosH < -1.0) return null
            return acos(cosH) / DEG2RAD
        }
    }

    private fun contextFor(
        epochMillis: Long,
        latDeg: Double,
        lonEastDeg: Double,
    ): Context {
        val localMillis = epochMillis + (lonEastDeg / 15.0 * MILLIS_PER_HOUR).toLong()
        val localDay = floor(localMillis.toDouble() / MILLIS_PER_DAY)
        val noonMillisUtc = ((localDay + 0.5) * MILLIS_PER_DAY).toLong()
        val t = Ephemeris.julianCenturies(noonMillisUtc)

        return Context(
            lat = latDeg * DEG2RAD,
            dec = Ephemeris.sunDeclination(t) * DEG2RAD,
            noonMinutes = 720.0 - 4.0 * lonEastDeg - Ephemeris.equationOfTimeMinutes(t),
            dayBaseMillis = localDay.toLong() * MILLIS_PER_DAY,
        )
    }

    /**
     * Computes sunrise and sunset for the civil day (in local mean solar time) containing
     * [epochMillis], at latitude [latDeg] and east-positive longitude [lonEastDeg].
     */
    fun sunTimes(
        epochMillis: Long,
        latDeg: Double,
        lonEastDeg: Double,
    ): SunTimes {
        val ctx = contextFor(epochMillis, latDeg, lonEastDeg)
        val hourAngle = ctx.hourAngleDeg(RISE_SET_ZENITH) ?: return SunTimes(sunrise = null, sunset = null)
        return SunTimes(
            sunrise = ctx.instantAt(ctx.noonMinutes - 4.0 * hourAngle),
            sunset = ctx.instantAt(ctx.noonMinutes + 4.0 * hourAngle),
        )
    }

    /**
     * Computes the day's golden-hour windows (Sun elevation between -4° and +6°) for the civil day
     * (in local mean solar time) containing [epochMillis], at latitude [latDeg] and east-positive
     * longitude [lonEastDeg].
     */
    fun goldenHour(
        epochMillis: Long,
        latDeg: Double,
        lonEastDeg: Double,
    ): GoldenHour {
        val ctx = contextFor(epochMillis, latDeg, lonEastDeg)
        val outer = ctx.hourAngleDeg(GOLDEN_HOUR_OUTER_ZENITH)
        val inner = ctx.hourAngleDeg(GOLDEN_HOUR_INNER_ZENITH)

        return GoldenHour(
            morningStart = outer?.let { ctx.instantAt(ctx.noonMinutes - 4.0 * it) },
            morningEnd = inner?.let { ctx.instantAt(ctx.noonMinutes - 4.0 * it) },
            eveningStart = inner?.let { ctx.instantAt(ctx.noonMinutes + 4.0 * it) },
            eveningEnd = outer?.let { ctx.instantAt(ctx.noonMinutes + 4.0 * it) },
        )
    }
}
