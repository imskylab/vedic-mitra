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
import kotlin.time.Instant

class ChoghadiyaCalculatorTest {
    // A clean synthetic day: 12h day + 12h night, so each of the eight windows is exactly 90 min.
    private val sunriseMs = 1_700_000_000_000L
    private val halfMillis = 43_200_000L
    private val sunsetMs = sunriseMs + halfMillis
    private val nextSunriseMs = sunsetMs + halfMillis
    private val sunTimes =
        SunTimes(
            sunrise = Instant.fromEpochMilliseconds(sunriseMs),
            sunset = Instant.fromEpochMilliseconds(sunsetMs),
        )

    private fun compute(dayOfWeek: Int) =
        choghadiyaOf(sunTimes, Instant.fromEpochMilliseconds(nextSunriseMs), dayOfWeek)

    @Test
    fun `produces eight day and eight night windows that tile each half`() {
        val list = compute(dayOfWeek = 0)
        val day = list.filter { it.isDay }
        val night = list.filterNot { it.isDay }

        assertThat(list).hasSize(16)
        assertThat(day).hasSize(8)
        assertThat(night).hasSize(8)
        assertThat(day.first().start.toEpochMilliseconds()).isEqualTo(sunriseMs)
        assertThat(day.last().end.toEpochMilliseconds()).isEqualTo(sunsetMs)
        assertThat(night.first().start.toEpochMilliseconds()).isEqualTo(sunsetMs)
        assertThat(night.last().end.toEpochMilliseconds()).isEqualTo(nextSunriseMs)
        day.forEach {
            assertThat(it.end.toEpochMilliseconds() - it.start.toEpochMilliseconds()).isEqualTo(5_400_000L)
        }
    }

    @Test
    fun `day sequence begins on the traditional weekday choghadiya`() {
        val expected =
            mapOf(
                0 to ChoghadiyaName.UDVEG, // Sunday
                1 to ChoghadiyaName.AMRIT, // Monday
                2 to ChoghadiyaName.ROG, // Tuesday
                3 to ChoghadiyaName.LABH, // Wednesday
                4 to ChoghadiyaName.SHUBH, // Thursday
                5 to ChoghadiyaName.CHAR, // Friday
                6 to ChoghadiyaName.KAAL, // Saturday
            )
        expected.forEach { (dayOfWeek, name) ->
            assertThat(compute(dayOfWeek).first { it.isDay }.name).isEqualTo(name)
        }
    }

    @Test
    fun `night sequence begins on the traditional weekday choghadiya`() {
        val expected =
            mapOf(
                0 to ChoghadiyaName.SHUBH, // Sunday
                1 to ChoghadiyaName.CHAR, // Monday
                2 to ChoghadiyaName.KAAL, // Tuesday
                3 to ChoghadiyaName.UDVEG, // Wednesday
                4 to ChoghadiyaName.AMRIT, // Thursday
                5 to ChoghadiyaName.ROG, // Friday
                6 to ChoghadiyaName.LABH, // Saturday
            )
        expected.forEach { (dayOfWeek, name) ->
            assertThat(compute(dayOfWeek).first { !it.isDay }.name).isEqualTo(name)
        }
    }

    @Test
    fun `steps through the fixed cyclic order within a half`() {
        // Sunday day: Udveg, Char, Labh, Amrit, Kaal, Shubh, Rog, then wraps to Udveg.
        val dayNames = compute(dayOfWeek = 0).filter { it.isDay }.map { it.name }
        assertThat(dayNames)
            .containsExactly(
                ChoghadiyaName.UDVEG,
                ChoghadiyaName.CHAR,
                ChoghadiyaName.LABH,
                ChoghadiyaName.AMRIT,
                ChoghadiyaName.KAAL,
                ChoghadiyaName.SHUBH,
                ChoghadiyaName.ROG,
                ChoghadiyaName.UDVEG,
            ).inOrder()
    }

    @Test
    fun `window quality follows its name`() {
        val amrit = compute(dayOfWeek = 1).first { it.name == ChoghadiyaName.AMRIT }
        val rog = compute(dayOfWeek = 2).first { it.name == ChoghadiyaName.ROG }
        assertThat(amrit.quality).isEqualTo(MuhurtaQuality.AUSPICIOUS)
        assertThat(rog.quality).isEqualTo(MuhurtaQuality.INAUSPICIOUS)
    }

    @Test
    fun `returns empty when the sun does not rise or set or the next sunrise is unknown`() {
        val noSunset = SunTimes(sunrise = Instant.fromEpochMilliseconds(sunriseMs), sunset = null)
        assertThat(choghadiyaOf(noSunset, Instant.fromEpochMilliseconds(nextSunriseMs), 0)).isEmpty()
        assertThat(choghadiyaOf(sunTimes, null, 0)).isEmpty()
    }
}
