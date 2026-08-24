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

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Casting a chart into a divisional frame.
 *
 * The signs themselves are [VargaTest]'s business. What is checked here is the framing — that the
 * lagna is divided too and becomes house 1, and that the houses count from it — since that is the
 * step a per-graha accessor cannot do and the step a reader depends on.
 *
 * D-1 gives a free oracle: cast into D-1 the divisional chart must be the natal chart itself, houses
 * and all. Any error in the framing that is not also an error in the natal chart shows up there.
 */
class VargaChartTest {
    @Test
    fun `cast into D1 a chart is its own rashi chart`() {
        REFERENCE_BIRTHS.forEach { birth ->
            val chart = referenceChartFor(birth.label)
            val d1 = chart.vargaChart(Varga.D1)
            assertWithMessage("${birth.label} D1 lagna").that(d1.lagna).isEqualTo(chart.lagna.rasi)
            assertWithMessage("${birth.label} D1 houses").that(d1.houses).isEqualTo(chart.houses)
            chart.grahas.forEach { graha ->
                assertWithMessage("${birth.label} ${graha.graha.displayName} D1 house")
                    .that(d1.houseOf(graha))
                    .isEqualTo(graha.house)
            }
        }
    }

    @Test
    fun `the lagna always occupies the first house`() {
        val chart = referenceChartFor("Hyderabad 1990")
        Varga.entries.forEach { varga ->
            val cast = chart.vargaChart(varga)
            assertWithMessage("${varga.displayName} first house")
                .that(cast.houses.first())
                .isEqualTo(cast.lagna)
        }
    }

    @Test
    fun `the twelve houses are the twelve signs in order from the lagna`() {
        val chart = referenceChartFor("London 1980")
        Varga.entries.forEach { varga ->
            val cast = chart.vargaChart(varga)
            assertWithMessage("${varga.displayName} house count").that(cast.houses).hasSize(12)
            assertWithMessage("${varga.displayName} distinct signs")
                .that(cast.houses.map { it.index }.toSet())
                .hasSize(12)
            cast.houses.forEachIndexed { index, rasi ->
                assertWithMessage("${varga.displayName} house ${index + 1}")
                    .that(rasi.index)
                    .isEqualTo((cast.lagna.index + index) % 12)
            }
        }
    }

    @Test
    fun `a graha's house is the count from the varga lagna to its varga sign`() {
        REFERENCE_BIRTHS.forEach { birth ->
            val chart = referenceChartFor(birth.label)
            Varga.entries.forEach { varga ->
                val cast = chart.vargaChart(varga)
                chart.grahas.forEach { graha ->
                    val expected = ((graha.varga(varga).index - cast.lagna.index + 12) % 12) + 1
                    assertWithMessage("${birth.label} ${graha.graha.displayName} in ${varga.displayName}")
                        .that(cast.houseOf(graha))
                        .isEqualTo(expected)
                }
            }
        }
    }

    @Test
    fun `every house is between one and twelve`() {
        REFERENCE_BIRTHS.forEach { birth ->
            val chart = referenceChartFor(birth.label)
            Varga.entries.forEach { varga ->
                val cast = chart.vargaChart(varga)
                chart.grahas.forEach { graha ->
                    assertThat(cast.houseOf(graha)).isIn(1..12)
                }
            }
        }
    }

    @Test
    fun `a graha in the same sign as the lagna is in the first house`() {
        // The strongest single statement about the framing, and the one that breaks if the lagna is
        // read in the rashi frame while the grahas are read in the divisional one.
        REFERENCE_BIRTHS.forEach { birth ->
            val chart = referenceChartFor(birth.label)
            Varga.entries.forEach { varga ->
                val cast = chart.vargaChart(varga)
                chart.grahas
                    .filter { it.varga(varga).index == cast.lagna.index }
                    .forEach { graha ->
                        assertWithMessage(
                            "${birth.label} ${graha.graha.displayName} shares the " +
                                "${varga.displayName} lagna sign",
                        ).that(cast.houseOf(graha)).isEqualTo(1)
                    }
            }
        }
    }
}
