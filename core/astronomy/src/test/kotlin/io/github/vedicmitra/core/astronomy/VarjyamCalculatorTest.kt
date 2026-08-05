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
import kotlin.math.abs

class VarjyamCalculatorTest {
    // A synthetic Moon that advances at a realistic ~13.2°/day (not the real Ephemeris, so the
    // expected crossing time can be computed exactly by hand) rather than a huge synthetic speed
    // that would complete many full revolutions inside the 32-hour search window and break the
    // "at most one crossing in the window" assumption real lunar motion satisfies.
    private val degPerMs = MOON_DEG_PER_DAY / 86_400_000.0

    private fun moonAt(epochMillis: Long): Double = Ephemeris.norm360(epochMillis * degPerMs)

    @Test
    fun `finds a mid-range boundary crossing`() {
        val boundaryDeg = 200.0
        val beforeMs = (205.0 / degPerMs).toLong() // 5 degrees past the boundary
        val expectedCrossingMs = (boundaryDeg / degPerMs).toLong()

        val crossing = findNakshatraStart(beforeMs, boundaryDeg, ::moonAt)

        assertThat(abs(crossing - expectedCrossingMs)).isLessThan(5L)
    }

    @Test
    fun `finds a boundary crossing at the 0-360 wrap point`() {
        val boundaryDeg = 0.0
        // 3 degrees past a full revolution plus the boundary, so the search must recognise the
        // wrapped position (norm360(363) == 3) as "just past 0", not "far from 0".
        val beforeMs = (363.0 / degPerMs).toLong()
        val expectedCrossingMs = (360.0 / degPerMs).toLong()

        val crossing = findNakshatraStart(beforeMs, boundaryDeg, ::moonAt)

        assertThat(abs(crossing - expectedCrossingMs)).isLessThan(5L)
    }

    @Test
    fun `varjyam uses the ghati range for the current nakshatra`() {
        // 5 degrees into Ashwini (index 0, boundary 0 degrees, ghatis 51-54).
        val atEpochMillis = (5.0 / degPerMs).toLong()
        val nakshatraStartMs = 0L

        val varjyam = varjyamOf(atEpochMillis, moonSiderealDeg = 5.0, moonSiderealDegAt = ::moonAt)

        assertThat(varjyam.name).isEqualTo("Varjyam")
        assertThat(varjyam.quality).isEqualTo(MuhurtaQuality.INAUSPICIOUS)
        // Ghatis 51-54 → (51-1)*24min to 54*24min after the nakshatra's start.
        val expectedStart = nakshatraStartMs + 50 * 24 * 60_000L
        val expectedEnd = nakshatraStartMs + 54 * 24 * 60_000L
        assertThat(abs(varjyam.start.toEpochMilliseconds() - expectedStart)).isLessThan(5L)
        assertThat(abs(varjyam.end.toEpochMilliseconds() - expectedEnd)).isLessThan(5L)
    }

    private companion object {
        const val MOON_DEG_PER_DAY = 13.2
    }
}
