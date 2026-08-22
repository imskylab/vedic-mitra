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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class LimbWindowTest {
    // An arbitrary instant; every assertion below holds for any instant, not just this one.
    private val at = Instant.parse("2026-08-22T09:30:00Z")
    private val atMillis = at.toEpochMilliseconds()
    private val windows = limbWindowsAt(atMillis, sunrise = null, nextSunrise = null)

    @Test
    fun `every window brackets the instant it was computed for`() {
        namedWindows().forEach { (_, window) ->
            assertThat(window.start.toEpochMilliseconds()).isAtMost(atMillis)
            assertThat(window.end.toEpochMilliseconds()).isGreaterThan(atMillis)
            assertThat(window.duration.isPositive()).isTrue()
        }
    }

    @Test
    fun `the tithi changes just after its window ends`() {
        val before = tithiOf(elongationAt(atMillis)).number
        val after = tithiOf(elongationAt((windows.tithi.end + 2.seconds).toEpochMilliseconds())).number
        assertThat(after).isNotEqualTo(before)
    }

    @Test
    fun `the nakshatra is unchanged just before its window ends`() {
        val now = nakshatraOf(moonSiderealAt(atMillis)).number
        val soon = nakshatraOf(moonSiderealAt((windows.nakshatra.end - 2.seconds).toEpochMilliseconds())).number
        assertThat(soon).isEqualTo(now)
    }

    @Test
    fun `the nakshatra changes just after its window ends`() {
        val now = nakshatraOf(moonSiderealAt(atMillis)).number
        val next = nakshatraOf(moonSiderealAt((windows.nakshatra.end + 2.seconds).toEpochMilliseconds())).number
        assertThat(next).isNotEqualTo(now)
    }

    @Test
    fun `the tithi is unchanged just after its window starts`() {
        val now = tithiOf(elongationAt(atMillis)).number
        val justStarted = tithiOf(elongationAt((windows.tithi.start + 2.seconds).toEpochMilliseconds())).number
        assertThat(justStarted).isEqualTo(now)
    }

    @Test
    fun `window durations sit in their classical ranges`() {
        // A tithi runs roughly 19-26h and a nakshatra roughly 19-27h, both varying with the Moon's
        // speed. A linear estimate from a mean rate would fall outside these bounds.
        assertThat(windows.tithi.duration.inWholeMinutes).isGreaterThan(19 * 60L)
        assertThat(windows.tithi.duration.inWholeMinutes).isLessThan(27 * 60L)
        assertThat(windows.nakshatra.duration.inWholeMinutes).isGreaterThan(19 * 60L)
        assertThat(windows.nakshatra.duration.inWholeMinutes).isLessThan(28 * 60L)
    }

    @Test
    fun `finer divisions have shorter windows than the ones that contain them`() {
        assertThat(windows.karana.duration.inWholeMilliseconds)
            .isLessThan(windows.tithi.duration.inWholeMilliseconds)
        assertThat(windows.moonPada.duration.inWholeMilliseconds)
            .isLessThan(windows.nakshatra.duration.inWholeMilliseconds)
        assertThat(windows.nakshatra.duration.inWholeMilliseconds)
            .isLessThan(windows.moonRashi.duration.inWholeMilliseconds)
        assertThat(windows.moonRashi.duration.inWholeMilliseconds)
            .isLessThan(windows.sunRashi.duration.inWholeMilliseconds)
    }

    @Test
    fun `a karana boundary coincides with the tithi boundary or its midpoint`() {
        // Two karanas per tithi, so the current karana ends either when the tithi does or halfway.
        val karanaEnd = windows.karana.end.toEpochMilliseconds()
        val tithiEnd = windows.tithi.end.toEpochMilliseconds()
        assertThat(karanaEnd).isAtMost(tithiEnd + 2_000L)
    }

    @Test
    fun `remaining and elapsed never go negative`() {
        assertThat(windows.tithi.remainingFrom(windows.tithi.end + 100.hours).isNegative()).isFalse()
        assertThat(windows.tithi.elapsedAt(windows.tithi.start - 100.hours).isNegative()).isFalse()
    }

    @Test
    fun `the angular fraction is consistent with the window it describes`() {
        namedWindows().forEach { (_, window) ->
            assertThat(window.angularFraction).isAtLeast(0.0)
            assertThat(window.angularFraction).isLessThan(1.0)
        }
    }

    @Test
    fun `the vara window is sunrise to sunrise when the sun rises`() {
        val sunrise = Instant.parse("2026-08-22T00:45:00Z")
        val nextSunrise = Instant.parse("2026-08-23T00:46:00Z")
        val vara = requireNotNull(limbWindowsAt(atMillis, sunrise, nextSunrise).vara)
        assertThat(vara.start.toEpochMilliseconds()).isEqualTo(sunrise.toEpochMilliseconds())
        assertThat(vara.end.toEpochMilliseconds()).isEqualTo(nextSunrise.toEpochMilliseconds())
        assertThat(vara.angularFraction).isAtLeast(0.0)
        assertThat(vara.angularFraction).isAtMost(1.0)
    }

    @Test
    fun `the vara window is absent where the sun does not rise`() {
        assertThat(limbWindowsAt(atMillis, sunrise = null, nextSunrise = null).vara).isNull()
    }

    private fun namedWindows(): List<Pair<String, LimbWindow>> =
        listOf(
            "tithi" to windows.tithi,
            "nakshatra" to windows.nakshatra,
            "yoga" to windows.yoga,
            "karana" to windows.karana,
            "moonPada" to windows.moonPada,
            "moonRashi" to windows.moonRashi,
            "moonPhase" to windows.moonPhase,
            "sunRashi" to windows.sunRashi,
        )

    private fun elongationAt(millis: Long): Double {
        val t = Ephemeris.julianCenturies(millis)
        return Ephemeris.norm360(Ephemeris.moonLongitude(t) - Ephemeris.sunApparentLongitude(t))
    }

    private fun moonSiderealAt(millis: Long): Double {
        val t = Ephemeris.julianCenturies(millis)
        return Ephemeris.norm360(Ephemeris.moonLongitude(t) - Ephemeris.lahiriAyanamsa(t))
    }
}
