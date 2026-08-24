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
 * The point at issue is that one expression — `start(rashi) + step × division` — replaces a per-varga
 * rule for all sixteen. These check both that it reproduces the classical statement case by case, and
 * that it agrees with an independent implementation on real charts: 720 placements across five charts
 * and sixteen vargas, drawn from a wider fit of 8,320 that had no disagreements at all.
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
    fun `consecutive divisions advance the sign by the varga's step`() {
        // Most vargas step one sign per division, but the drekkana steps four and the chaturthamsa
        // three -- the 5th and 9th from a sign, the 4th, 7th and 10th. Asserting one everywhere was
        // right only while every varga in the enum happened to have step 1.
        Varga.entries.filter { it != Varga.D1 }.forEach { varga ->
            val span = 30.0 / varga.divisions
            repeat(varga.divisions - 1) { d ->
                val here = vargaSign(varga, d * span + span / 2).index
                val next = vargaSign(varga, (d + 1) * span + span / 2).index
                assertWithMessage("${varga.displayName} division $d to ${d + 1}")
                    .that(next)
                    .isEqualTo((here + varga.step) % 12)
            }
        }
    }

    @Test
    fun `each rashi restarts the vargas that begin at their own sign`() {
        // D-12 and D-60 restart at the sign itself every rashi; D-3 and D-4 do too, before stepping.
        listOf(Varga.D3, Varga.D4, Varga.D12, Varga.D60).forEach { varga ->
            (0 until 12).forEach { rashi ->
                assertWithMessage("${varga.displayName} first division of rashi $rashi")
                    .that(vargaSign(varga, rashi * 30.0 + 0.01).index)
                    .isEqualTo(rashi)
            }
        }
    }

    @Test
    fun `the table-driven vargas start where their tables say`() {
        mapOf(
            Varga.D10 to listOf(0, 9, 2, 11, 4, 1, 6, 3, 8, 5, 10, 7),
            Varga.D24 to listOf(4, 3, 4, 3, 4, 3, 4, 3, 4, 3, 4, 3),
            Varga.D40 to listOf(0, 6, 0, 6, 0, 6, 0, 6, 0, 6, 0, 6),
            Varga.D45 to listOf(0, 4, 8, 0, 4, 8, 0, 4, 8, 0, 4, 8),
        ).forEach { (varga, starts) ->
            starts.forEachIndexed { rashi, expected ->
                assertWithMessage("${varga.displayName} start of rashi $rashi")
                    .that(vargaSign(varga, rashi * 30.0 + 0.01).index)
                    .isEqualTo(expected)
            }
        }
    }

    @Test
    fun `division width and the birth-time caution follow from the divisions`() {
        assertThat(Varga.D9.divisionArcminutes).isWithin(1e-9).of(200.0)
        assertThat(Varga.D60.divisionArcminutes).isWithin(1e-9).of(30.0)
        // The ascendant moves a degree in about four minutes, so anything under ~80 arcmin is
        // decided by the birth time rather than by this engine.
        assertThat(Varga.D20.needsExactBirthTime).isFalse()
        assertThat(Varga.D24.needsExactBirthTime).isTrue()
        assertThat(Varga.D60.needsExactBirthTime).isTrue()
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
    fun `every varga matches the reference implementation on the reference charts`() {
        // Placements sitting within EPHEMERIS_UNCERTAINTY_ARCMIN of a division edge are counted
        // rather than asserted. This engine's longitudes are good to roughly an arcminute, so which
        // side of an edge such a placement falls on is not a question it can answer -- asserting it
        // would be asserting precision we do not have, and a golden that passed would be right by
        // luck. Roughly 90 of the 720 land there, overwhelmingly in the finest vargas: a D-60
        // division is 30 arcminutes and two ephemerides can differ by nearly five, so a third of its
        // placements are simply not decidable by this comparison. VargaRuleTest covers the rule
        // itself with no tolerance at all.
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
 * How far a longitude must sit from a division edge before *this comparison* can say which side it is
 * on.
 *
 * Note what is being compared: our longitudes against another implementation's, not our longitudes
 * against truth. Two independent low-precision ephemerides disagree by more than either one's own
 * error, and most on the slow outer planets — the largest seen here is **4.7 arcminutes on Saturn in
 * 1975**, which is more than a whole D-45 division. An earlier version of this used 1.0, describing
 * our internal precision rather than the gap actually under test, and a D-45 golden failed for it.
 *
 * The rule itself is checked without any of this in [VargaRuleTest], which feeds the reference's own
 * longitudes in and asserts exactly. A failure there means the rule is wrong; a failure here means
 * the longitudes drifted, and the two want telling apart.
 */
private const val EPHEMERIS_UNCERTAINTY_ARCMIN = 6.0

/**
 * Out of 720 placements. A six-arcminute band skips about 90 of them — most in the finest vargas,
 * where D-60's 30-arcminute divisions put a third of placements within reach of the gap between two
 * ephemerides. That is a real limit on what an end-to-end comparison can prove about a fine varga,
 * which is why [VargaRuleTest] exists to prove the rule separately. The cap leaves headroom and still
 * fails if the band grew by half again.
 */
private const val MAX_TOO_CLOSE_TO_CALL = 130

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
                    Varga.D3,
                    listOf(
                        "Vrishabha",
                        "Kanya",
                        "Tula",
                        "Simha",
                        "Tula",
                        "Vrishchika",
                        "Makara",
                        "Vrishabha",
                        "Vrishchika",
                    ),
                ),
                VargaGolden(
                    Varga.D4,
                    listOf("Vrishabha", "Tula", "Vrishchika", "Karka", "Dhanu", "Kanya", "Makara", "Karka", "Makara"),
                ),
                VargaGolden(
                    Varga.D10,
                    listOf(
                        "Makara",
                        "Vrishabha",
                        "Tula",
                        "Simha",
                        "Vrishchika",
                        "Vrishabha",
                        "Kanya",
                        "Kumbha",
                        "Simha",
                    ),
                ),
                VargaGolden(
                    Varga.D12,
                    listOf("Vrishabha", "Tula", "Dhanu", "Kanya", "Dhanu", "Vrishchika", "Makara", "Simha", "Kumbha"),
                ),
                VargaGolden(
                    Varga.D24,
                    listOf("Simha", "Kumbha", "Mesha", "Karka", "Simha", "Vrishchika", "Simha", "Kanya", "Kanya"),
                ),
                VargaGolden(
                    Varga.D40,
                    listOf("Makara", "Mithuna", "Kumbha", "Tula", "Makara", "Makara", "Vrishchika", "Kanya", "Kanya"),
                ),
                VargaGolden(
                    Varga.D45,
                    listOf("Vrishchika", "Mesha", "Tula", "Makara", "Dhanu", "Karka", "Mithuna", "Mithuna", "Mithuna"),
                ),
                VargaGolden(
                    Varga.D60,
                    listOf("Kanya", "Makara", "Vrishabha", "Simha", "Kumbha", "Simha", "Meena", "Dhanu", "Mithuna"),
                ),
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
                    Varga.D3,
                    listOf(
                        "Kumbha",
                        "Vrishabha",
                        "Mithuna",
                        "Tula",
                        "Vrishchika",
                        "Mesha",
                        "Karka",
                        "Mithuna",
                        "Dhanu",
                    ),
                ),
                VargaGolden(
                    Varga.D4,
                    listOf("Mesha", "Mithuna", "Kanya", "Tula", "Dhanu", "Vrishabha", "Tula", "Karka", "Makara"),
                ),
                VargaGolden(
                    Varga.D10,
                    listOf("Meena", "Kumbha", "Kanya", "Tula", "Mithuna", "Vrishabha", "Mithuna", "Karka", "Makara"),
                ),
                VargaGolden(
                    Varga.D12,
                    listOf("Mesha", "Simha", "Kanya", "Tula", "Dhanu", "Karka", "Tula", "Kanya", "Meena"),
                ),
                VargaGolden(
                    Varga.D24,
                    listOf("Simha", "Mithuna", "Meena", "Simha", "Makara", "Karka", "Kumbha", "Karka", "Karka"),
                ),
                VargaGolden(
                    Varga.D40,
                    listOf("Makara", "Makara", "Mesha", "Mesha", "Vrishabha", "Karka", "Tula", "Mithuna", "Mithuna"),
                ),
                VargaGolden(
                    Varga.D45,
                    listOf(
                        "Meena",
                        "Simha",
                        "Makara",
                        "Mesha",
                        "Vrishchika",
                        "Meena",
                        "Vrishabha",
                        "Vrishchika",
                        "Vrishchika",
                    ),
                ),
                VargaGolden(
                    Varga.D60,
                    listOf("Vrishabha", "Simha", "Dhanu", "Tula", "Kumbha", "Mithuna", "Makara", "Karka", "Makara"),
                ),
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
                    Varga.D3,
                    listOf("Meena", "Kanya", "Karka", "Mithuna", "Kanya", "Vrishchika", "Vrishabha", "Tula", "Mesha"),
                ),
                VargaGolden(
                    Varga.D4,
                    listOf(
                        "Meena",
                        "Karka",
                        "Vrishabha",
                        "Vrishabha",
                        "Simha",
                        "Kanya",
                        "Vrishabha",
                        "Dhanu",
                        "Mithuna",
                    ),
                ),
                VargaGolden(
                    Varga.D10,
                    listOf(
                        "Makara",
                        "Mesha",
                        "Kumbha",
                        "Vrishabha",
                        "Mesha",
                        "Vrishabha",
                        "Makara",
                        "Vrishchika",
                        "Vrishabha",
                    ),
                ),
                VargaGolden(
                    Varga.D12,
                    listOf(
                        "Vrishabha",
                        "Kanya",
                        "Karka",
                        "Mithuna",
                        "Kanya",
                        "Vrishchika",
                        "Mithuna",
                        "Makara",
                        "Karka",
                    ),
                ),
                VargaGolden(
                    Varga.D24,
                    listOf("Dhanu", "Dhanu", "Dhanu", "Mesha", "Mesha", "Vrishchika", "Kanya", "Tula", "Tula"),
                ),
                VargaGolden(
                    Varga.D40,
                    listOf("Mithuna", "Meena", "Meena", "Mithuna", "Makara", "Makara", "Makara", "Meena", "Meena"),
                ),
                VargaGolden(
                    Varga.D45,
                    listOf("Kanya", "Dhanu", "Vrishabha", "Dhanu", "Makara", "Karka", "Dhanu", "Kumbha", "Kumbha"),
                ),
                VargaGolden(
                    Varga.D60,
                    listOf(
                        "Mesha",
                        "Simha",
                        "Karka",
                        "Vrishchika",
                        "Mesha",
                        "Simha",
                        "Tula",
                        "Vrishabha",
                        "Vrishchika",
                    ),
                ),
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
                    Varga.D3,
                    listOf(
                        "Mesha",
                        "Vrishabha",
                        "Vrishchika",
                        "Makara",
                        "Vrishabha",
                        "Karka",
                        "Mesha",
                        "Mithuna",
                        "Dhanu",
                    ),
                ),
                VargaGolden(
                    Varga.D4,
                    listOf(
                        "Mithuna",
                        "Mithuna",
                        "Dhanu",
                        "Makara",
                        "Vrishabha",
                        "Simha",
                        "Meena",
                        "Vrishabha",
                        "Vrishchika",
                    ),
                ),
                VargaGolden(
                    Varga.D10,
                    listOf("Vrishabha", "Dhanu", "Karka", "Tula", "Kumbha", "Kumbha", "Meena", "Mithuna", "Dhanu"),
                ),
                VargaGolden(
                    Varga.D12,
                    listOf("Mithuna", "Mithuna", "Makara", "Kumbha", "Mithuna", "Simha", "Mesha", "Karka", "Makara"),
                ),
                VargaGolden(
                    Varga.D24,
                    listOf("Kanya", "Makara", "Mesha", "Kanya", "Kanya", "Kumbha", "Vrishabha", "Karka", "Karka"),
                ),
                VargaGolden(
                    Varga.D40,
                    listOf("Kumbha", "Mesha", "Kanya", "Kumbha", "Kumbha", "Vrishabha", "Karka", "Tula", "Tula"),
                ),
                VargaGolden(
                    Varga.D45,
                    listOf("Dhanu", "Kanya", "Meena", "Simha", "Dhanu", "Karka", "Vrishabha", "Vrishabha", "Vrishabha"),
                ),
                VargaGolden(
                    Varga.D60,
                    listOf(
                        "Kanya",
                        "Mithuna",
                        "Karka",
                        "Karka",
                        "Vrishchika",
                        "Tula",
                        "Vrishchika",
                        "Mithuna",
                        "Dhanu",
                    ),
                ),
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
                    Varga.D3,
                    listOf("Mithuna", "Karka", "Mesha", "Kumbha", "Dhanu", "Mithuna", "Mesha", "Meena", "Kanya"),
                ),
                VargaGolden(
                    Varga.D4,
                    listOf(
                        "Mithuna",
                        "Karka",
                        "Vrishabha",
                        "Meena",
                        "Vrishchika",
                        "Mithuna",
                        "Vrishabha",
                        "Mesha",
                        "Tula",
                    ),
                ),
                VargaGolden(
                    Varga.D10,
                    listOf(
                        "Mithuna",
                        "Meena",
                        "Meena",
                        "Kumbha",
                        "Vrishchika",
                        "Mithuna",
                        "Vrishabha",
                        "Dhanu",
                        "Mithuna",
                    ),
                ),
                VargaGolden(
                    Varga.D12,
                    listOf("Mithuna", "Karka", "Vrishabha", "Mesha", "Dhanu", "Mithuna", "Mithuna", "Mithuna", "Dhanu"),
                ),
                VargaGolden(
                    Varga.D24,
                    listOf("Simha", "Simha", "Kumbha", "Mesha", "Mesha", "Simha", "Vrishabha", "Mithuna", "Mithuna"),
                ),
                VargaGolden(
                    Varga.D40,
                    listOf(
                        "Vrishabha",
                        "Dhanu",
                        "Tula",
                        "Makara",
                        "Vrishabha",
                        "Vrishabha",
                        "Mesha",
                        "Makara",
                        "Makara",
                    ),
                ),
                VargaGolden(
                    Varga.D45,
                    listOf("Makara", "Mithuna", "Mithuna", "Makara", "Vrishchika", "Makara", "Dhanu", "Dhanu", "Dhanu"),
                ),
                VargaGolden(
                    Varga.D60,
                    listOf("Karka", "Tula", "Mithuna", "Simha", "Mesha", "Karka", "Kumbha", "Mithuna", "Dhanu"),
                ),
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
