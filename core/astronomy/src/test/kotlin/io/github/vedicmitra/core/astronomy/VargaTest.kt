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
 * The divisional charts, against the classical rules and against an independent implementation.
 *
 * The point at issue is that one continuous count replaces a per-varga rule. These check both that
 * the collapsed form reproduces the classical statement case by case, and that it agrees with
 * Jagannatha Hora on real charts — 315 placements across five charts and seven vargas, drawn from a
 * wider sample of 7,500 that had no disagreements.
 */
class VargaTest {
    @Test
    fun `a movable sign's first navamsha is the sign itself`() {
        listOf(0, 3, 6, 9).forEach { sign ->
            assertWithMessage("movable sign $sign")
                .that(vargaSign(Varga.D9, sign * 30.0).index)
                .isEqualTo(sign)
        }
    }

    @Test
    fun `a fixed sign's first navamsha is the ninth from it`() {
        listOf(1, 4, 7, 10).forEach { sign ->
            assertWithMessage("fixed sign $sign")
                .that(vargaSign(Varga.D9, sign * 30.0).index)
                .isEqualTo((sign + 8) % 12)
        }
    }

    @Test
    fun `a dual sign's first navamsha is the fifth from it`() {
        listOf(2, 5, 8, 11).forEach { sign ->
            assertWithMessage("dual sign $sign")
                .that(vargaSign(Varga.D9, sign * 30.0).index)
                .isEqualTo((sign + 4) % 12)
        }
    }

    @Test
    fun `D1 is the rashi itself`() {
        var degrees = 0.0
        while (degrees < 360.0) {
            assertThat(vargaSign(Varga.D1, degrees).index).isEqualTo(AngularBuckets.rashiIndex(degrees))
            degrees += 0.37
        }
    }

    @Test
    fun `each varga cuts the zodiac into twelve times its divisions`() {
        Varga.entries.forEach { varga ->
            val last = divisionIndex(varga, 359.999999)
            assertWithMessage("${varga.displayName} highest division index")
                .that(last)
                .isEqualTo(12 * varga.divisions - 1)
            assertWithMessage("${varga.displayName} first division index")
                .that(divisionIndex(varga, 0.0))
                .isEqualTo(0)
        }
    }

    @Test
    fun `every varga maps the whole zodiac to a valid sign`() {
        Varga.entries.forEach { varga ->
            var degrees = 0.0
            while (degrees < 360.0) {
                assertWithMessage("${varga.displayName} at $degrees")
                    .that(vargaSign(varga, degrees).index)
                    .isIn(0..11)
                degrees += 0.29
            }
        }
    }

    @Test
    fun `consecutive divisions advance the sign by one`() {
        // The count is continuous, so crossing a division boundary always steps one sign on.
        Varga.entries.filter { it != Varga.D1 }.forEach { varga ->
            val span = 30.0 / varga.divisions
            repeat(11) { d ->
                val here = vargaSign(varga, d * span + span / 2).index
                val next = vargaSign(varga, (d + 1) * span + span / 2).index
                assertWithMessage("${varga.displayName} division $d to ${d + 1}")
                    .that(next)
                    .isEqualTo((here + 1) % 12)
            }
        }
    }

    @Test
    fun `divisions whose span is not a whole arcsecond still map cleanly`() {
        // D-7 and D-11 span 15428.57 and 9818.18 arcseconds. Multiplying before dividing keeps the
        // index exact; the assertion is on division *midpoints* rather than edges, because an edge
        // like `d * 30.0 / 7` is not representable and lands a hair below the boundary it names —
        // reporting the earlier division there is correct, not a bug.
        listOf(Varga.D7, Varga.D11).forEach { varga ->
            val span = 30.0 / varga.divisions
            repeat(12 * varga.divisions) { d ->
                assertWithMessage("${varga.displayName} division $d")
                    .that(divisionIndex(varga, d * span + span / 2))
                    .isEqualTo(d)
            }
        }
    }

    @Test
    fun `the division index never goes backwards across the zodiac`() {
        Varga.entries.forEach { varga ->
            var previous = 0
            var degrees = 0.0
            while (degrees < 360.0) {
                val index = divisionIndex(varga, degrees)
                assertWithMessage("${varga.displayName} at $degrees")
                    .that(index)
                    .isAtLeast(previous)
                previous = index
                degrees += 0.11
            }
        }
    }

    @Test
    fun `every varga matches Jagannatha Hora on the reference charts`() {
        // Placements sitting within EPHEMERIS_UNCERTAINTY_ARCMIN of a division edge are counted
        // rather than asserted. This engine's longitudes are good to roughly an arcminute, so which
        // side of an edge such a placement falls on is not a question it can answer -- asserting it
        // would be asserting precision we do not have, and a golden that passed would be right by
        // luck. Three of the 315 land there, at 0.04, 0.17 and 0.75 arcmin.
        //
        // Everything further from an edge *is* asserted, which is what keeps this a test of the rule
        // rather than of the ephemeris: a disagreement mid-division would mean the count is wrong,
        // and no tolerance here would hide it. The skipped count is asserted too, so widening the
        // band -- by a precision regression, or by adding a varga too fine for these longitudes --
        // fails the test instead of quietly shrinking what it covers.
        val mismatches = mutableListOf<String>()
        var tooCloseToCall = 0
        VARGA_GOLDENS.forEach { (label, goldens) ->
            val chart = referenceChartFor(label)
            goldens.forEach { golden ->
                golden.signs.forEachIndexed { index, expected ->
                    val graha = GOLDEN_ORDER[index]
                    val natal = chart.grahas.first { it.graha == graha }
                    val fromEdge = arcminutesFromEdge(golden.varga, natal.siderealLongitude)
                    if (fromEdge < EPHEMERIS_UNCERTAINTY_ARCMIN) {
                        tooCloseToCall++
                        return@forEachIndexed
                    }
                    val actual = natal.varga(golden.varga)
                    if (actual.name != expected) {
                        mismatches +=
                            "$label ${graha.displayName} in ${golden.varga.displayName}: " +
                            "expected $expected, got ${actual.name} " +
                            "(at ${"%.4f".format(natal.siderealLongitude)} deg, " +
                            "${"%.2f".format(fromEdge)} arcmin from the nearest division edge)"
                    }
                }
            }
        }
        assertWithMessage(mismatches.joinToString("\n")).that(mismatches).isEmpty()
        assertWithMessage("placements too close to a division edge to call")
            .that(tooCloseToCall)
            .isAtMost(MAX_TOO_CLOSE_TO_CALL)
    }
}

/** How far [siderealDeg] sits from the nearest edge of its [varga] division, in arcminutes. */
private fun arcminutesFromEdge(
    varga: Varga,
    siderealDeg: Double,
): Double {
    val span = AngularBuckets.RASHI_ARCSEC.toDouble() / varga.divisions
    val into = (AngularBuckets.arcseconds(siderealDeg).toDouble()) % span
    return minOf(into, span - into) / ARCSEC_PER_ARCMIN
}

private const val ARCSEC_PER_ARCMIN = 60.0

/**
 * How far a longitude must sit from a division edge before this engine can say which side it is on.
 *
 * The planetary positions here are Meeus truncated series over Keplerian elements, accurate to about
 * an arcminute -- ample for every panchanga limb and for the rashi, whose divisions are 30 degrees
 * wide, but comparable to the whole question when a division edge is a fraction of a degree away.
 */
private const val EPHEMERIS_UNCERTAINTY_ARCMIN = 1.0

/** Out of 315 placements. Around 2% is what the division widths of D-11 through D-27 predict. */
private const val MAX_TOO_CLOSE_TO_CALL = 15

/** The grahas the goldens are listed in, in order. */
private val GOLDEN_ORDER =
    listOf(
        Graha.SUN,
        Graha.MOON,
        Graha.MANGALA,
        Graha.BUDHA,
        Graha.GURU,
        Graha.SHUKRA,
        Graha.SHANI,
        Graha.RAHU,
        Graha.KETU,
    )

/** One varga's signs for the nine grahas, in [GOLDEN_ORDER]. */
private data class VargaGolden(
    val varga: Varga,
    val signs: List<String>,
)

private val VARGA_GOLDENS: Map<String, List<VargaGolden>> =
    mapOf(
        "Hyderabad 1990" to
            listOf(
                VargaGolden(
                    Varga.D6,
                    listOf("Tula", "Kumbha", "Kanya", "Mithuna", "Karka", "Kumbha", "Tula", "Makara", "Makara"),
                ),
                VargaGolden(
                    Varga.D7,
                    listOf(
                        "Vrishchika",
                        "Dhanu",
                        "Simha",
                        "Karka",
                        "Kanya",
                        "Makara",
                        "Karka",
                        "Vrishchika",
                        "Vrishabha",
                    ),
                ),
                VargaGolden(
                    Varga.D8,
                    listOf("Dhanu", "Tula", "Mithuna", "Karka", "Dhanu", "Makara", "Mesha", "Simha", "Simha"),
                ),
                VargaGolden(
                    Varga.D11,
                    listOf("Meena", "Meena", "Meena", "Kanya", "Karka", "Dhanu", "Karka", "Makara", "Karka"),
                ),
                VargaGolden(
                    Varga.D16,
                    listOf("Kanya", "Mesha", "Kanya", "Vrishchika", "Simha", "Vrishchika", "Mesha", "Makara", "Makara"),
                ),
                VargaGolden(
                    Varga.D20,
                    listOf("Makara", "Simha", "Vrishabha", "Makara", "Mithuna", "Kanya", "Mesha", "Meena", "Meena"),
                ),
                VargaGolden(
                    Varga.D27,
                    listOf("Kanya", "Mesha", "Kanya", "Mesha", "Dhanu", "Karka", "Simha", "Tula", "Mesha"),
                ),
            ),
        "Delhi 1975" to
            listOf(
                VargaGolden(
                    Varga.D6,
                    listOf("Karka", "Meena", "Vrishabha", "Mesha", "Kumbha", "Kanya", "Vrishchika", "Kanya", "Kanya"),
                ),
                VargaGolden(
                    Varga.D7,
                    listOf("Makara", "Kanya", "Simha", "Tula", "Kumbha", "Kumbha", "Meena", "Mesha", "Tula"),
                ),
                VargaGolden(
                    Varga.D8,
                    listOf("Simha", "Meena", "Tula", "Mesha", "Kumbha", "Karka", "Mithuna", "Vrishchika", "Vrishchika"),
                ),
                VargaGolden(
                    Varga.D11,
                    listOf("Meena", "Kanya", "Vrishabha", "Tula", "Makara", "Tula", "Mesha", "Simha", "Kumbha"),
                ),
                VargaGolden(
                    Varga.D16,
                    listOf("Dhanu", "Meena", "Mesha", "Mesha", "Dhanu", "Vrishchika", "Simha", "Karka", "Karka"),
                ),
                VargaGolden(
                    Varga.D20,
                    listOf(
                        "Kumbha",
                        "Meena",
                        "Kumbha",
                        "Mesha",
                        "Vrishchika",
                        "Karka",
                        "Tula",
                        "Vrishchika",
                        "Vrishchika",
                    ),
                ),
                VargaGolden(
                    Varga.D27,
                    listOf("Dhanu", "Kanya", "Mithuna", "Tula", "Tula", "Mithuna", "Kanya", "Dhanu", "Mithuna"),
                ),
            ),
        "Chennai 2001" to
            listOf(
                VargaGolden(
                    Varga.D6,
                    listOf("Vrishchika", "Kumbha", "Kumbha", "Mithuna", "Dhanu", "Kumbha", "Tula", "Karka", "Karka"),
                ),
                VargaGolden(
                    Varga.D7,
                    listOf("Tula", "Dhanu", "Tula", "Mesha", "Makara", "Makara", "Vrishchika", "Tula", "Mesha"),
                ),
                VargaGolden(
                    Varga.D8,
                    listOf("Kanya", "Kanya", "Vrishabha", "Kumbha", "Meena", "Makara", "Dhanu", "Dhanu", "Dhanu"),
                ),
                VargaGolden(
                    Varga.D11,
                    listOf("Karka", "Kumbha", "Vrishabha", "Tula", "Karka", "Dhanu", "Mesha", "Simha", "Kumbha"),
                ),
                VargaGolden(
                    Varga.D16,
                    listOf("Meena", "Meena", "Karka", "Makara", "Kumbha", "Vrishchika", "Kanya", "Kanya", "Kanya"),
                ),
                VargaGolden(
                    Varga.D20,
                    listOf("Dhanu", "Mithuna", "Kumbha", "Karka", "Karka", "Kanya", "Makara", "Karka", "Karka"),
                ),
                VargaGolden(
                    Varga.D27,
                    listOf("Mithuna", "Kumbha", "Kanya", "Karka", "Vrishabha", "Karka", "Kanya", "Makara", "Karka"),
                ),
            ),
        "Mumbai 1988" to
            listOf(
                VargaGolden(
                    Varga.D6,
                    listOf("Karka", "Kumbha", "Meena", "Tula", "Tula", "Kumbha", "Mithuna", "Mithuna", "Mithuna"),
                ),
                VargaGolden(
                    Varga.D7,
                    listOf(
                        "Meena",
                        "Simha",
                        "Meena",
                        "Karka",
                        "Vrishchika",
                        "Tula",
                        "Kumbha",
                        "Vrishabha",
                        "Vrishchika",
                    ),
                ),
                VargaGolden(
                    Varga.D8,
                    listOf("Dhanu", "Kumbha", "Meena", "Mesha", "Dhanu", "Mithuna", "Vrishchika", "Meena", "Meena"),
                ),
                VargaGolden(
                    Varga.D11,
                    listOf(
                        "Kumbha",
                        "Karka",
                        "Kumbha",
                        "Simha",
                        "Mesha",
                        "Vrishabha",
                        "Dhanu",
                        "Vrishchika",
                        "Vrishabha",
                    ),
                ),
                VargaGolden(
                    Varga.D16,
                    listOf("Simha", "Dhanu", "Kumbha", "Vrishabha", "Kanya", "Simha", "Mithuna", "Meena", "Meena"),
                ),
                VargaGolden(
                    Varga.D20,
                    listOf("Karka", "Vrishchika", "Makara", "Mithuna", "Kumbha", "Meena", "Meena", "Kanya", "Kanya"),
                ),
                VargaGolden(
                    Varga.D27,
                    listOf("Mithuna", "Meena", "Dhanu", "Kanya", "Kanya", "Tula", "Kumbha", "Tula", "Mesha"),
                ),
            ),
        "London 1980" to
            listOf(
                VargaGolden(
                    Varga.D6,
                    listOf("Mesha", "Tula", "Simha", "Kanya", "Mithuna", "Mesha", "Kanya", "Meena", "Meena"),
                ),
                VargaGolden(
                    Varga.D7,
                    listOf("Mithuna", "Makara", "Makara", "Vrishchika", "Tula", "Mithuna", "Kumbha", "Karka", "Makara"),
                ),
                VargaGolden(
                    Varga.D8,
                    listOf(
                        "Simha",
                        "Mesha",
                        "Mithuna",
                        "Kumbha",
                        "Kumbha",
                        "Simha",
                        "Karka",
                        "Vrishchika",
                        "Vrishchika",
                    ),
                ),
                VargaGolden(
                    Varga.D11,
                    listOf(
                        "Kumbha",
                        "Makara",
                        "Simha",
                        "Vrishchika",
                        "Meena",
                        "Kumbha",
                        "Kanya",
                        "Vrishchika",
                        "Vrishabha",
                    ),
                ),
                VargaGolden(
                    Varga.D16,
                    listOf("Dhanu", "Mesha", "Simha", "Makara", "Makara", "Dhanu", "Tula", "Karka", "Karka"),
                ),
                VargaGolden(
                    Varga.D20,
                    listOf(
                        "Simha",
                        "Vrishabha",
                        "Meena",
                        "Dhanu",
                        "Mithuna",
                        "Simha",
                        "Mithuna",
                        "Vrishchika",
                        "Vrishchika",
                    ),
                ),
                VargaGolden(
                    Varga.D27,
                    listOf("Tula", "Kumbha", "Dhanu", "Simha", "Makara", "Tula", "Mesha", "Meena", "Kanya"),
                ),
            ),
    )
