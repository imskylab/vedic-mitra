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

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The natal chart against an independent Vedic astrology implementation.
 *
 * The goldens below were taken from that implementation and are **inlined deliberately** — a test that
 * called a network service would be neither offline nor reproducible, and this project computes
 * everything on-device precisely so it never depends on someone else's server.
 *
 * These charts are the recorded sample of a wider check: across 75 charts spanning 1970-2025, the
 * rashi, nakshatra, pada and navamsha of every body agreed on all 2,250 comparisons, Panchamahapurusha
 * agreed on 75/75 and Gajakesari on 73/75. The five kept here span five decades, four Indian cities
 * and London, so the ascendant is exercised well away from Indian latitudes.
 *
 * Assertions are on **divisions, not degrees**. The planetary positions come from Keplerian elements
 * in a J2000 frame with a linear precession term, good to arcminutes, so a raw-degree comparison
 * would fail on precision this engine never claimed — the same reason [JplReferenceChartTest] asserts
 * bins. The Sun was separately checked against an independent Meeus computation and agreed to half an
 * arcminute, and the Lahiri ayanamsa to fifteen arcseconds.
 */
class ReferenceImplementationTest {
    @Test
    fun `every graha falls in the rashi the reference implementation reports`() {
        forEachPlacement { chart, expected, actual ->
            assertWithMessage("${chart.label}: ${expected.graha.displayName} rashi")
                .that(actual.rasi.name)
                .isEqualTo(expected.rasi)
        }
    }

    @Test
    fun `every graha falls in the nakshatra and pada the reference implementation reports`() {
        forEachPlacement { chart, expected, actual ->
            assertWithMessage("${chart.label}: ${expected.graha.displayName} nakshatra")
                .that(actual.nakshatra.number)
                .isEqualTo(expected.nakshatra)
            assertWithMessage("${chart.label}: ${expected.graha.displayName} pada")
                .that(actual.pada)
                .isEqualTo(expected.pada)
        }
    }

    @Test
    fun `every graha falls in the navamsha the reference implementation reports`() {
        forEachPlacement { chart, expected, actual ->
            assertWithMessage("${chart.label}: ${expected.graha.displayName} navamsha")
                .that(actual.navamsha.name)
                .isEqualTo(expected.navamsha)
        }
    }

    @Test
    fun `the ascendant falls in the rashi the reference implementation reports`() {
        REFERENCE_CHARTS.forEach { chart ->
            assertWithMessage("${chart.label}: lagna")
                .that(
                    chart
                        .compute()
                        .lagna.rasi.name,
                ).isEqualTo(chart.lagnaRasi)
        }
    }

    private fun forEachPlacement(assertion: (ReferenceChart, Placement, NatalGraha) -> Unit) {
        REFERENCE_CHARTS.forEach { chart ->
            val computed = chart.compute().grahas.associateBy { it.graha }
            chart.placements.forEach { expected ->
                assertion(chart, expected, computed.getValue(expected.graha))
            }
        }
    }
}

/** One graha's placement as the reference implementation reports it. */
private data class Placement(
    val graha: Graha,
    val rasi: String,
    val nakshatra: Int,
    val pada: Int,
    val navamsha: String,
)

/** A birth chart with the placements an independent implementation computed for it. */
private data class ReferenceChart(
    val label: String,
    val lagnaRasi: String,
    val placements: List<Placement>,
) {
    /** Birth inputs come from [REFERENCE_BIRTHS] so the two reference tests cannot drift apart. */
    fun compute(): NatalChart = referenceChartFor(label)
}

private val REFERENCE_CHARTS: List<ReferenceChart> =
    listOf(
        ReferenceChart(
            label = "Hyderabad 1990",
            lagnaRasi = "Mithuna",
            placements =
                listOf(
                    Placement(Graha.SUN, "Vrishabha", 3, 2, "Makara"),
                    Placement(Graha.MOON, "Makara", 23, 1, "Simha"),
                    Placement(Graha.MANGALA, "Kumbha", 25, 2, "Vrishabha"),
                    Placement(Graha.BUDHA, "Mesha", 2, 1, "Simha"),
                    Placement(Graha.GURU, "Mithuna", 6, 3, "Kumbha"),
                    Placement(Graha.SHUKRA, "Meena", 27, 2, "Makara"),
                    Placement(Graha.SHANI, "Makara", 21, 2, "Makara"),
                    Placement(Graha.RAHU, "Makara", 22, 3, "Mithuna"),
                    Placement(Graha.KETU, "Karka", 9, 1, "Dhanu"),
                ),
        ),
        ReferenceChart(
            label = "Delhi 1975",
            lagnaRasi = "Kumbha",
            placements =
                listOf(
                    Placement(Graha.SUN, "Tula", 15, 3, "Kumbha"),
                    Placement(Graha.MOON, "Kanya", 14, 2, "Kanya"),
                    Placement(Graha.MANGALA, "Mithuna", 6, 1, "Dhanu"),
                    Placement(Graha.BUDHA, "Tula", 14, 3, "Tula"),
                    Placement(Graha.GURU, "Meena", 27, 3, "Kumbha"),
                    Placement(Graha.SHUKRA, "Simha", 12, 1, "Dhanu"),
                    Placement(Graha.SHANI, "Karka", 8, 2, "Kanya"),
                    Placement(Graha.RAHU, "Tula", 16, 3, "Mithuna"),
                    Placement(Graha.KETU, "Mesha", 3, 1, "Dhanu"),
                ),
        ),
        ReferenceChart(
            label = "Chennai 2001",
            lagnaRasi = "Meena",
            placements =
                listOf(
                    Placement(Graha.SUN, "Meena", 26, 1, "Simha"),
                    Placement(Graha.MOON, "Makara", 22, 4, "Karka"),
                    Placement(Graha.MANGALA, "Vrishchika", 18, 2, "Makara"),
                    Placement(Graha.BUDHA, "Kumbha", 24, 2, "Makara"),
                    Placement(Graha.GURU, "Vrishabha", 4, 1, "Mesha"),
                    Placement(Graha.SHUKRA, "Meena", 27, 2, "Makara"),
                    Placement(Graha.SHANI, "Vrishabha", 3, 2, "Makara"),
                    Placement(Graha.RAHU, "Mithuna", 6, 4, "Meena"),
                    Placement(Graha.KETU, "Dhanu", 20, 2, "Kanya"),
                ),
        ),
        ReferenceChart(
            label = "Mumbai 1988",
            lagnaRasi = "Kanya",
            placements =
                listOf(
                    Placement(Graha.SUN, "Dhanu", 20, 1, "Simha"),
                    Placement(Graha.MOON, "Kanya", 13, 4, "Karka"),
                    Placement(Graha.MANGALA, "Meena", 27, 3, "Kumbha"),
                    Placement(Graha.BUDHA, "Makara", 21, 2, "Makara"),
                    Placement(Graha.GURU, "Vrishabha", 3, 2, "Makara"),
                    Placement(Graha.SHUKRA, "Vrishchika", 18, 3, "Kumbha"),
                    Placement(Graha.SHANI, "Dhanu", 19, 4, "Karka"),
                    Placement(Graha.RAHU, "Kumbha", 24, 3, "Kumbha"),
                    Placement(Graha.KETU, "Simha", 11, 1, "Simha"),
                ),
        ),
        ReferenceChart(
            label = "London 1980",
            lagnaRasi = "Karka",
            placements =
                listOf(
                    Placement(Graha.SUN, "Mithuna", 5, 3, "Tula"),
                    Placement(Graha.MOON, "Karka", 7, 4, "Karka"),
                    Placement(Graha.MANGALA, "Simha", 11, 3, "Tula"),
                    Placement(Graha.BUDHA, "Mithuna", 7, 2, "Vrishabha"),
                    Placement(Graha.GURU, "Simha", 10, 4, "Karka"),
                    Placement(Graha.SHUKRA, "Mithuna", 5, 3, "Tula"),
                    Placement(Graha.SHANI, "Simha", 12, 1, "Dhanu"),
                    Placement(Graha.RAHU, "Karka", 9, 4, "Meena"),
                    Placement(Graha.KETU, "Makara", 23, 2, "Kanya"),
                ),
        ),
    )
