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
 * Sunrise/sunset via the NOAA solar-position equations. Uses local mean solar time (from longitude)
 * to pick the civil day, which matches the Vedic convention of local — not zone — time.
 */
internal object SolarDay {
    private const val DEG2RAD = PI / 180.0
    private const val MILLIS_PER_DAY = 86_400_000L
    private const val MILLIS_PER_MINUTE = 60_000.0
    private const val MILLIS_PER_HOUR = 3_600_000.0

    // Sun-centre zenith at rise/set: 90° + 0.833° for refraction and the solar semi-diameter.
    private const val RISE_SET_ZENITH = 90.833

    /**
     * Computes sunrise and sunset for the civil day (in local mean solar time) containing
     * [epochMillis], at latitude [latDeg] and east-positive longitude [lonEastDeg].
     */
    fun sunTimes(
        epochMillis: Long,
        latDeg: Double,
        lonEastDeg: Double,
    ): SunTimes {
        val localMillis = epochMillis + (lonEastDeg / 15.0 * MILLIS_PER_HOUR).toLong()
        val localDay = floor(localMillis.toDouble() / MILLIS_PER_DAY)
        val noonMillisUtc = ((localDay + 0.5) * MILLIS_PER_DAY).toLong()

        val t = Ephemeris.julianCenturies(noonMillisUtc)
        val dec = Ephemeris.sunDeclination(t) * DEG2RAD
        val eot = Ephemeris.equationOfTimeMinutes(t)
        val lat = latDeg * DEG2RAD

        val cosH = cos(RISE_SET_ZENITH * DEG2RAD) / (cos(lat) * cos(dec)) - tan(lat) * tan(dec)
        if (cosH > 1.0 || cosH < -1.0) return SunTimes(sunrise = null, sunset = null)

        val hourAngle = acos(cosH) / DEG2RAD
        val noonMinutes = 720.0 - 4.0 * lonEastDeg - eot
        val dayBaseMillis = localDay.toLong() * MILLIS_PER_DAY

        fun instantAt(minutes: Double) =
            Instant.fromEpochMilliseconds(dayBaseMillis + (minutes * MILLIS_PER_MINUTE).toLong())

        return SunTimes(
            sunrise = instantAt(noonMinutes - 4.0 * hourAngle),
            sunset = instantAt(noonMinutes + 4.0 * hourAngle),
        )
    }
}
