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

class MuhurtaCalculatorTest {
    // A clean 12-hour day (00:00 to 12:00 UTC) so segment boundaries land on round numbers.
    private val sunrise = Instant.fromEpochMilliseconds(0L)
    private val sunset = Instant.fromEpochMilliseconds(DAY_MILLIS)
    private val sunTimes = SunTimes(sunrise = sunrise, sunset = sunset)
    private val fifteenth = DAY_MILLIS / 15

    // Verified against published almanacs for Delhi, 2026-08-02..08 — see
    // [[vedic-mitra-panchang-reference-data]] in project memory for the derivation.
    private val expectedDurMuhurtaSegments =
        mapOf(
            0 to listOf(14), // Sunday
            1 to listOf(9), // Monday
            2 to listOf(4), // Tuesday
            3 to listOf(8), // Wednesday
            4 to listOf(6), // Thursday
            5 to listOf(4), // Friday
            6 to listOf(1, 2), // Saturday
        )

    @Test
    fun `each weekday's Dur Muhurta lands on the verified segment or segments`() {
        for ((dayOfWeek, segments) in expectedDurMuhurtaSegments) {
            val durMuhurtas = muhurtasOf(sunTimes, dayOfWeek).filter { it.name.startsWith("Dur Muhurta") }

            assertThat(durMuhurtas).hasSize(segments.size)
            durMuhurtas.zip(segments).forEach { (muhurta, segment) ->
                assertThat(muhurta.start.toEpochMilliseconds()).isEqualTo((segment - 1) * fifteenth)
                assertThat(muhurta.end.toEpochMilliseconds()).isEqualTo(segment * fifteenth)
                assertThat(muhurta.quality).isEqualTo(MuhurtaQuality.INAUSPICIOUS)
            }
        }
    }

    @Test
    fun `a single Dur Muhurta is named plainly, multiple ones are numbered`() {
        assertThat(muhurtasOf(sunTimes, dayOfWeek = 1).map { it.name }).contains("Dur Muhurta")

        val saturday = muhurtasOf(sunTimes, dayOfWeek = 6).map { it.name }
        assertThat(saturday).containsAtLeast("Dur Muhurta 1", "Dur Muhurta 2")
        assertThat(saturday).doesNotContain("Dur Muhurta")
    }

    @Test
    fun `saturday's two Dur Muhurta windows are consecutive`() {
        val durMuhurtas = muhurtasOf(sunTimes, dayOfWeek = 6).filter { it.name.startsWith("Dur Muhurta") }

        assertThat(durMuhurtas).hasSize(2)
        assertThat(durMuhurtas[0].end).isEqualTo(durMuhurtas[1].start)
    }

    @Test
    fun `abhijit muhurta is present every day except wednesday`() {
        for (dayOfWeek in 0..6) {
            val hasAbhijit = muhurtasOf(sunTimes, dayOfWeek).any { it.name == "Abhijit Muhurta" }
            assertThat(hasAbhijit).isEqualTo(dayOfWeek != Vara.BUDHAVARA.ordinal)
        }
    }

    @Test
    fun `returns an empty list when the sun does not rise or set`() {
        val polarDay = SunTimes(sunrise = null, sunset = null)

        assertThat(muhurtasOf(polarDay, dayOfWeek = 0)).isEmpty()
    }

    private companion object {
        const val DAY_MILLIS = 43_200_000L // 12 hours
    }
}
