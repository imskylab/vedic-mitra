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

@Suppress("MagicNumber")
class GunaMilanTest {
    private fun points(
        result: GunaMilanResult,
        koota: Koota,
    ): Double = result.scores.first { it.koota == koota }.points

    @Test
    fun `identical charts score the same-sign kootas but trip the Nadi dosha`() {
        // Rohini (nakshatra 4) in Vrishabha (sign 1) for both.
        val same = GunaMilanProfile(nakshatraNumber = 4, moonRasiIndex = 1)

        val result = gunaMilan(groom = same, bride = same)

        assertThat(result.total).isEqualTo(25.0)
        assertThat(result.verdict).isEqualTo(GunaMilanVerdict.GOOD)
        assertThat(points(result, Koota.YONI)).isEqualTo(4.0)
        assertThat(points(result, Koota.GANA)).isEqualTo(6.0)
        assertThat(points(result, Koota.TARA)).isEqualTo(0.0) // Janma tara — not favourable
        assertThat(points(result, Koota.NADI)).isEqualTo(0.0)
        assertThat(result.doshas).containsExactly("Nadi dosha")
    }

    @Test
    fun `a 2-12 sign axis is the Bhakoot dosha`() {
        // Ashwini (1) in Mesha (0) with Rohini (4) in Vrishabha (1): signs are 2/12 apart.
        val result =
            gunaMilan(
                groom = GunaMilanProfile(nakshatraNumber = 1, moonRasiIndex = 0),
                bride = GunaMilanProfile(nakshatraNumber = 4, moonRasiIndex = 1),
            )

        assertThat(points(result, Koota.BHAKOOT)).isEqualTo(0.0)
        assertThat(points(result, Koota.NADI)).isEqualTo(8.0)
        assertThat(result.total).isEqualTo(20.5)
        assertThat(result.verdict).isEqualTo(GunaMilanVerdict.AVERAGE)
        assertThat(result.doshas).containsExactly("Bhakoot dosha")
    }

    @Test
    fun `a well-matched pair scores in the good band with no doshas`() {
        // Pushya (8) in Karka (3) with Hasta (13) in Kanya (5).
        val result =
            gunaMilan(
                groom = GunaMilanProfile(nakshatraNumber = 8, moonRasiIndex = 3),
                bride = GunaMilanProfile(nakshatraNumber = 13, moonRasiIndex = 5),
            )

        assertThat(result.total).isEqualTo(26.5)
        assertThat(result.verdict).isEqualTo(GunaMilanVerdict.GOOD)
        assertThat(result.doshas).isEmpty()
    }

    @Test
    fun `a poor pair can carry both doshas`() {
        // U.Ashadha (21) in Dhanu (8) with Rohini (4) in Vrishabha (1): same nadi and a 6/8 axis.
        val result =
            gunaMilan(
                groom = GunaMilanProfile(nakshatraNumber = 21, moonRasiIndex = 8),
                bride = GunaMilanProfile(nakshatraNumber = 4, moonRasiIndex = 1),
            )

        assertThat(result.total).isEqualTo(10.5)
        assertThat(result.verdict).isEqualTo(GunaMilanVerdict.POOR)
        assertThat(result.doshas).containsExactly("Nadi dosha", "Bhakoot dosha")
    }

    @Test
    fun `every koota stays within its weight and the eight are returned in order`() {
        for (gN in 1..27) {
            for (bN in listOf(1, 7, 14, 21, 27)) {
                val result =
                    gunaMilan(
                        groom = GunaMilanProfile(nakshatraNumber = gN, moonRasiIndex = (gN - 1) % 12),
                        bride = GunaMilanProfile(nakshatraNumber = bN, moonRasiIndex = (bN - 1) % 12),
                    )
                assertThat(result.total).isAtLeast(0.0)
                assertThat(result.total).isAtMost(36.0)
                assertThat(result.total).isEqualTo(result.scores.sumOf { it.points })
                result.scores.forEach { assertThat(it.points).isAtMost(it.koota.maxPoints) }
            }
        }
        val ordered = gunaMilan(GunaMilanProfile(1, 0), GunaMilanProfile(1, 0)).scores.map { it.koota }
        assertThat(ordered).containsExactlyElementsIn(Koota.entries).inOrder()
    }
}
