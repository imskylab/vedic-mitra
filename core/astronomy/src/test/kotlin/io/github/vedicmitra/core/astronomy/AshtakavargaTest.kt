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
 * Ashtakavarga against the implementation its tables were read from.
 *
 * The goldens carry **positions, not birth details**, which is what makes this a test of the tables
 * rather than of the ephemeris. Ashtakavarga needs nothing but which rashi each body occupies, so
 * feeding those in directly removes every other variable — unlike the vargas and the dashas, where
 * that separation had to be engineered, here it is simply what the calculation takes.
 *
 * 14 charts across four cities and 1968 to 2083: 98 binnashtakavarga rows and 14 sarvashtakavarga
 * rows, none of them the chart the tables were derived from.
 */
class AshtakavargaTest {
    @Test
    fun `binnashtakavarga matches the reference on every chart`() {
        val mismatches = mutableListOf<String>()
        AV_GOLDENS.forEach { golden ->
            Ashtakavarga.CONTRIBUTORS.forEachIndexed { index, graha ->
                val actual = Ashtakavarga.binna(graha) { golden.signs.getValue(it) }
                if (actual != golden.binna[index]) {
                    mismatches += "${golden.label} ${graha.displayName}: expected ${golden.binna[index]}, got $actual"
                }
            }
        }
        assertWithMessage(mismatches.joinToString("\n")).that(mismatches).isEmpty()
    }

    @Test
    fun `sarvashtakavarga matches the reference on every chart`() {
        val mismatches = mutableListOf<String>()
        AV_GOLDENS.forEach { golden ->
            val actual = Ashtakavarga.sarva { golden.signs.getValue(it) }
            if (actual != golden.sarva) {
                mismatches += "${golden.label}: expected ${golden.sarva}, got $actual"
            }
        }
        assertWithMessage(mismatches.joinToString("\n")).that(mismatches).isEmpty()
    }

    @Test
    fun `every sarvashtakavarga totals 337, whatever the chart`() {
        // The invariant every text quotes, and the one that catches a mistyped digit anywhere in the
        // 64 tables -- it cannot hold by accident once a single entry is wrong.
        AV_GOLDENS.forEach { golden ->
            val sarva = Ashtakavarga.sarva { golden.signs.getValue(it) }
            assertWithMessage(golden.label).that(sarva.sum()).isEqualTo(Ashtakavarga.SARVA_TOTAL)
        }
        // And for positions no real chart would produce, since the total is a property of the tables.
        (0 until 12).forEach { everywhere ->
            val sarva = Ashtakavarga.sarva { everywhere }
            assertWithMessage("all bodies in sign $everywhere")
                .that(sarva.sum())
                .isEqualTo(Ashtakavarga.SARVA_TOTAL)
        }
    }

    @Test
    fun `each graha's own bindus come to the classical figure`() {
        // Sun 48, Moon 49, Mars 39, Mercury 54, Jupiter 56, Venus 52, Saturn 39. These were never put
        // into the tables; they fall out of them, which is why they are worth asserting.
        val expected =
            mapOf(
                Graha.SUN to 48,
                Graha.MOON to 49,
                Graha.MANGALA to 39,
                Graha.BUDHA to 54,
                Graha.GURU to 56,
                Graha.SHUKRA to 52,
                Graha.SHANI to 39,
            )
        expected.forEach { (graha, total) ->
            assertWithMessage(graha.displayName)
                .that(Ashtakavarga.binna(graha) { 0 }.sum())
                .isEqualTo(total)
        }
        assertThat(expected.values.sum()).isEqualTo(Ashtakavarga.SARVA_TOTAL)
    }

    @Test
    fun `a sign never holds more than eight bindus`() {
        // Eight reference points, so eight is the ceiling by construction. A table entry outside
        // 1..12 would quietly break that.
        AV_GOLDENS.forEach { golden ->
            Ashtakavarga.CONTRIBUTORS.forEach { graha ->
                Ashtakavarga.binna(graha) { golden.signs.getValue(it) }.forEach {
                    assertWithMessage("${golden.label} ${graha.displayName}").that(it).isIn(0..8)
                }
            }
        }
    }

    @Test
    fun `the nodes have no binnashtakavarga`() {
        listOf(Graha.RAHU, Graha.KETU).forEach { node ->
            assertWithMessage(node.displayName)
                .that(Ashtakavarga.binna(node) { 0 })
                .containsExactlyElementsIn(List(12) { 0 })
        }
        assertThat(Ashtakavarga.CONTRIBUTORS).containsNoneOf(Graha.RAHU, Graha.KETU)
    }
}

/** One reference chart, as positions rather than birth details. */
private data class AvGolden(
    val label: String,
    val signs: Map<AshtakavargaReference, Int>,
    val binna: List<List<Int>>,
    val sarva: List<Int>,
)

private val AV_GOLDENS =
    listOf(
        AvGolden(
            "1968-03-11 Hyderabad",
            mapOf(
                AshtakavargaReference.SUN to 10,
                AshtakavargaReference.MOON to 3,
                AshtakavargaReference.MANGALA to 11,
                AshtakavargaReference.BUDHA to 9,
                AshtakavargaReference.GURU to 4,
                AshtakavargaReference.SHUKRA to 10,
                AshtakavargaReference.SHANI to 11,
                AshtakavargaReference.LAGNA to 7,
            ),
            listOf(
                listOf(5, 3, 4, 1, 3, 6, 5, 4, 6, 5, 2, 4),
                listOf(6, 6, 2, 5, 7, 4, 2, 4, 4, 5, 1, 3),
                listOf(3, 3, 5, 3, 1, 5, 2, 4, 5, 5, 0, 3),
                listOf(5, 3, 7, 2, 2, 5, 6, 4, 7, 5, 3, 5),
                listOf(4, 6, 4, 3, 5, 5, 5, 6, 4, 3, 5, 6),
                listOf(2, 6, 7, 3, 2, 5, 3, 6, 5, 4, 4, 5),
                listOf(1, 4, 2, 4, 5, 4, 1, 3, 6, 5, 3, 1),
            ),
            listOf(26, 31, 31, 21, 25, 34, 24, 31, 37, 32, 18, 27),
        ),
        AvGolden(
            "1969-05-06 London",
            mapOf(
                AshtakavargaReference.SUN to 0,
                AshtakavargaReference.MOON to 8,
                AshtakavargaReference.MANGALA to 7,
                AshtakavargaReference.BUDHA to 1,
                AshtakavargaReference.GURU to 5,
                AshtakavargaReference.SHUKRA to 11,
                AshtakavargaReference.SHANI to 0,
                AshtakavargaReference.LAGNA to 0,
            ),
            listOf(
                listOf(3, 5, 2, 6, 2, 5, 4, 3, 3, 5, 8, 2),
                listOf(2, 3, 6, 3, 4, 8, 3, 3, 5, 4, 5, 3),
                listOf(2, 2, 4, 3, 4, 4, 4, 2, 2, 4, 7, 1),
                listOf(5, 6, 2, 7, 3, 5, 4, 4, 3, 5, 6, 4),
                listOf(5, 4, 6, 4, 6, 5, 5, 4, 5, 5, 4, 3),
                listOf(5, 3, 4, 7, 3, 2, 4, 5, 4, 6, 5, 4),
                listOf(4, 2, 2, 3, 4, 3, 4, 1, 1, 6, 7, 2),
            ),
            listOf(26, 25, 26, 33, 26, 32, 28, 22, 23, 35, 42, 19),
        ),
        AvGolden(
            "1970-07-01 Delhi",
            mapOf(
                AshtakavargaReference.SUN to 2,
                AshtakavargaReference.MOON to 1,
                AshtakavargaReference.MANGALA to 2,
                AshtakavargaReference.BUDHA to 2,
                AshtakavargaReference.GURU to 6,
                AshtakavargaReference.SHUKRA to 3,
                AshtakavargaReference.SHANI to 0,
                AshtakavargaReference.LAGNA to 4,
            ),
            listOf(
                listOf(4, 3, 5, 5, 2, 2, 4, 3, 4, 5, 6, 5),
                listOf(5, 4, 3, 3, 5, 3, 6, 5, 2, 6, 2, 5),
                listOf(4, 2, 3, 4, 4, 2, 5, 3, 3, 3, 2, 4),
                listOf(4, 6, 4, 3, 5, 4, 5, 5, 3, 3, 6, 6),
                listOf(6, 3, 6, 4, 5, 6, 2, 5, 5, 5, 3, 6),
                listOf(6, 5, 4, 4, 7, 4, 3, 5, 3, 3, 5, 3),
                listOf(3, 4, 4, 2, 4, 3, 3, 3, 2, 3, 3, 5),
            ),
            listOf(32, 27, 29, 25, 32, 24, 28, 29, 22, 28, 27, 34),
        ),
        AvGolden(
            "1971-08-26 Chennai",
            mapOf(
                AshtakavargaReference.SUN to 4,
                AshtakavargaReference.MOON to 6,
                AshtakavargaReference.MANGALA to 9,
                AshtakavargaReference.BUDHA to 4,
                AshtakavargaReference.GURU to 7,
                AshtakavargaReference.SHUKRA to 4,
                AshtakavargaReference.SHANI to 1,
                AshtakavargaReference.LAGNA to 8,
            ),
            listOf(
                listOf(4, 4, 3, 5, 4, 4, 3, 4, 3, 4, 5, 5),
                listOf(2, 6, 6, 2, 3, 3, 7, 4, 4, 1, 6, 5),
                listOf(2, 3, 3, 2, 4, 2, 5, 2, 5, 5, 3, 3),
                listOf(5, 4, 5, 5, 5, 4, 5, 4, 5, 6, 2, 4),
                listOf(7, 5, 6, 2, 6, 6, 4, 5, 4, 5, 4, 2),
                listOf(4, 2, 6, 4, 5, 5, 4, 3, 6, 4, 3, 6),
                listOf(2, 4, 4, 3, 2, 4, 4, 2, 3, 2, 2, 7),
            ),
            listOf(26, 28, 33, 23, 29, 28, 32, 24, 30, 27, 25, 32),
        ),
        AvGolden(
            "1972-10-20 Hyderabad",
            mapOf(
                AshtakavargaReference.SUN to 6,
                AshtakavargaReference.MOON to 11,
                AshtakavargaReference.MANGALA to 5,
                AshtakavargaReference.BUDHA to 6,
                AshtakavargaReference.GURU to 8,
                AshtakavargaReference.SHUKRA to 4,
                AshtakavargaReference.SHANI to 1,
                AshtakavargaReference.LAGNA to 1,
            ),
            listOf(
                listOf(4, 5, 4, 5, 6, 2, 4, 2, 4, 4, 4, 4),
                listOf(3, 4, 3, 6, 3, 3, 6, 3, 5, 4, 4, 5),
                listOf(1, 4, 2, 4, 4, 2, 3, 2, 4, 3, 4, 6),
                listOf(3, 4, 7, 3, 6, 4, 6, 3, 6, 2, 4, 6),
                listOf(5, 3, 6, 6, 3, 6, 6, 4, 4, 6, 3, 4),
                listOf(3, 5, 4, 5, 7, 5, 3, 3, 5, 3, 4, 5),
                listOf(2, 5, 3, 6, 5, 2, 4, 3, 0, 4, 2, 3),
            ),
            listOf(21, 30, 29, 35, 34, 24, 32, 20, 28, 26, 25, 33),
        ),
        AvGolden(
            "1973-12-15 London",
            mapOf(
                AshtakavargaReference.SUN to 7,
                AshtakavargaReference.MOON to 4,
                AshtakavargaReference.MANGALA to 0,
                AshtakavargaReference.BUDHA to 7,
                AshtakavargaReference.GURU to 9,
                AshtakavargaReference.SHUKRA to 9,
                AshtakavargaReference.SHANI to 2,
                AshtakavargaReference.LAGNA to 5,
            ),
            listOf(
                listOf(3, 4, 6, 6, 3, 4, 3, 4, 5, 4, 4, 2),
                listOf(5, 5, 5, 3, 6, 4, 4, 5, 0, 5, 5, 2),
                listOf(4, 1, 5, 2, 2, 4, 3, 4, 3, 5, 3, 3),
                listOf(6, 3, 4, 5, 3, 6, 4, 5, 4, 5, 4, 5),
                listOf(4, 5, 4, 5, 4, 5, 5, 6, 4, 4, 7, 3),
                listOf(5, 3, 3, 4, 4, 8, 6, 4, 3, 4, 3, 5),
                listOf(2, 2, 7, 2, 4, 4, 3, 5, 4, 2, 3, 1),
            ),
            listOf(29, 23, 34, 27, 26, 35, 28, 33, 23, 29, 29, 21),
        ),
        AvGolden(
            "1975-02-09 Delhi",
            mapOf(
                AshtakavargaReference.SUN to 9,
                AshtakavargaReference.MOON to 9,
                AshtakavargaReference.MANGALA to 8,
                AshtakavargaReference.BUDHA to 9,
                AshtakavargaReference.GURU to 10,
                AshtakavargaReference.SHUKRA to 10,
                AshtakavargaReference.SHANI to 2,
                AshtakavargaReference.LAGNA to 9,
            ),
            listOf(
                listOf(3, 1, 6, 5, 3, 4, 6, 4, 5, 4, 2, 5),
                listOf(4, 4, 4, 3, 5, 3, 7, 7, 2, 3, 2, 5),
                listOf(1, 2, 6, 3, 0, 3, 3, 5, 4, 5, 1, 6),
                listOf(4, 3, 7, 3, 3, 6, 5, 4, 6, 5, 4, 4),
                listOf(4, 5, 4, 5, 3, 6, 6, 7, 3, 4, 5, 4),
                listOf(4, 5, 3, 0, 5, 6, 4, 7, 4, 3, 5, 6),
                listOf(4, 1, 4, 3, 3, 2, 5, 6, 3, 4, 2, 2),
            ),
            listOf(24, 21, 34, 22, 22, 30, 36, 40, 27, 28, 21, 32),
        ),
        AvGolden(
            "1976-04-05 Chennai",
            mapOf(
                AshtakavargaReference.SUN to 11,
                AshtakavargaReference.MOON to 1,
                AshtakavargaReference.MANGALA to 2,
                AshtakavargaReference.BUDHA to 11,
                AshtakavargaReference.GURU to 0,
                AshtakavargaReference.SHUKRA to 11,
                AshtakavargaReference.SHANI to 3,
                AshtakavargaReference.LAGNA to 2,
            ),
            listOf(
                listOf(4, 3, 2, 4, 5, 5, 3, 3, 4, 4, 6, 5),
                listOf(3, 6, 2, 5, 3, 4, 5, 6, 4, 5, 2, 4),
                listOf(3, 3, 2, 5, 4, 2, 3, 1, 2, 6, 3, 5),
                listOf(4, 3, 4, 6, 4, 3, 3, 5, 3, 6, 6, 7),
                listOf(6, 2, 7, 5, 2, 5, 3, 7, 6, 6, 2, 5),
                listOf(5, 5, 3, 4, 5, 4, 4, 5, 3, 6, 5, 3),
                listOf(3, 2, 2, 1, 5, 4, 4, 4, 3, 3, 3, 5),
            ),
            listOf(28, 24, 22, 30, 28, 27, 25, 31, 25, 36, 27, 34),
        ),
        AvGolden(
            "1977-05-31 Hyderabad",
            mapOf(
                AshtakavargaReference.SUN to 1,
                AshtakavargaReference.MOON to 6,
                AshtakavargaReference.MANGALA to 0,
                AshtakavargaReference.BUDHA to 0,
                AshtakavargaReference.GURU to 1,
                AshtakavargaReference.SHUKRA to 0,
                AshtakavargaReference.SHANI to 3,
                AshtakavargaReference.LAGNA to 6,
            ),
            listOf(
                listOf(2, 3, 2, 4, 5, 4, 4, 2, 5, 6, 4, 7),
                listOf(2, 3, 5, 5, 6, 2, 4, 4, 6, 3, 5, 4),
                listOf(3, 2, 1, 4, 3, 3, 5, 2, 2, 2, 6, 6),
                listOf(6, 5, 2, 5, 5, 2, 5, 4, 4, 6, 4, 6),
                listOf(4, 5, 5, 5, 6, 3, 2, 6, 5, 5, 7, 3),
                listOf(3, 4, 5, 2, 4, 5, 3, 4, 7, 4, 7, 4),
                listOf(1, 2, 2, 1, 4, 5, 2, 3, 5, 3, 4, 7),
            ),
            listOf(21, 24, 22, 26, 33, 24, 25, 25, 34, 29, 37, 37),
        ),
        AvGolden(
            "1978-07-26 London",
            mapOf(
                AshtakavargaReference.SUN to 3,
                AshtakavargaReference.MOON to 0,
                AshtakavargaReference.MANGALA to 5,
                AshtakavargaReference.BUDHA to 4,
                AshtakavargaReference.GURU to 2,
                AshtakavargaReference.SHUKRA to 4,
                AshtakavargaReference.SHANI to 4,
                AshtakavargaReference.LAGNA to 10,
            ),
            listOf(
                listOf(6, 5, 4, 5, 2, 3, 4, 3, 3, 5, 5, 3),
                listOf(5, 3, 6, 3, 1, 3, 5, 4, 7, 5, 5, 2),
                listOf(5, 3, 5, 3, 1, 3, 2, 4, 4, 2, 3, 4),
                listOf(5, 7, 5, 4, 3, 5, 3, 6, 5, 3, 3, 5),
                listOf(5, 5, 5, 5, 5, 5, 5, 2, 7, 5, 3, 4),
                listOf(6, 6, 6, 2, 3, 2, 5, 4, 6, 2, 5, 5),
                listOf(4, 4, 5, 5, 2, 1, 3, 3, 2, 5, 4, 1),
            ),
            listOf(36, 33, 36, 27, 17, 22, 27, 26, 34, 27, 28, 24),
        ),
        AvGolden(
            "1979-09-20 Delhi",
            mapOf(
                AshtakavargaReference.SUN to 5,
                AshtakavargaReference.MOON to 4,
                AshtakavargaReference.MANGALA to 3,
                AshtakavargaReference.BUDHA to 5,
                AshtakavargaReference.GURU to 4,
                AshtakavargaReference.SHUKRA to 5,
                AshtakavargaReference.SHANI to 4,
                AshtakavargaReference.LAGNA to 3,
            ),
            listOf(
                listOf(5, 6, 6, 3, 4, 3, 4, 2, 3, 4, 4, 4),
                listOf(5, 5, 6, 3, 3, 4, 2, 5, 5, 4, 3, 4),
                listOf(4, 4, 4, 6, 3, 1, 2, 3, 1, 5, 5, 1),
                listOf(4, 7, 4, 6, 5, 4, 3, 4, 2, 6, 5, 4),
                listOf(4, 6, 5, 6, 3, 4, 7, 3, 5, 5, 5, 3),
                listOf(5, 6, 5, 5, 3, 4, 5, 5, 5, 2, 2, 5),
                listOf(4, 3, 6, 5, 2, 3, 4, 1, 5, 3, 2, 1),
            ),
            listOf(31, 37, 36, 34, 23, 23, 27, 23, 26, 29, 26, 22),
        ),
        AvGolden(
            "1980-11-14 Chennai",
            mapOf(
                AshtakavargaReference.SUN to 6,
                AshtakavargaReference.MOON to 9,
                AshtakavargaReference.MANGALA to 8,
                AshtakavargaReference.BUDHA to 6,
                AshtakavargaReference.GURU to 5,
                AshtakavargaReference.SHUKRA to 5,
                AshtakavargaReference.SHANI to 5,
                AshtakavargaReference.LAGNA to 7,
            ),
            listOf(
                listOf(3, 3, 5, 5, 5, 4, 5, 2, 3, 4, 4, 5),
                listOf(5, 4, 3, 6, 3, 4, 4, 3, 4, 6, 3, 4),
                listOf(3, 1, 4, 5, 5, 3, 1, 2, 4, 2, 4, 5),
                listOf(5, 2, 6, 5, 6, 6, 5, 3, 5, 2, 5, 4),
                listOf(3, 4, 5, 7, 4, 4, 5, 6, 4, 5, 5, 4),
                listOf(4, 6, 5, 4, 4, 4, 2, 5, 5, 5, 4, 4),
                listOf(3, 3, 2, 5, 5, 3, 2, 5, 0, 4, 5, 2),
            ),
            listOf(26, 23, 30, 37, 32, 28, 24, 26, 25, 28, 30, 28),
        ),
        AvGolden(
            "1982-01-09 Hyderabad",
            mapOf(
                AshtakavargaReference.SUN to 8,
                AshtakavargaReference.MOON to 2,
                AshtakavargaReference.MANGALA to 5,
                AshtakavargaReference.BUDHA to 9,
                AshtakavargaReference.GURU to 6,
                AshtakavargaReference.SHUKRA to 9,
                AshtakavargaReference.SHANI to 5,
                AshtakavargaReference.LAGNA to 0,
            ),
            listOf(
                listOf(3, 3, 7, 5, 3, 5, 4, 2, 5, 2, 2, 7),
                listOf(4, 4, 4, 6, 3, 3, 5, 6, 1, 5, 5, 3),
                listOf(5, 3, 5, 3, 3, 5, 2, 3, 3, 1, 2, 4),
                listOf(6, 7, 3, 4, 3, 7, 4, 5, 3, 4, 2, 6),
                listOf(5, 4, 4, 5, 4, 5, 7, 4, 5, 5, 6, 2),
                listOf(4, 7, 5, 6, 5, 3, 3, 6, 3, 3, 5, 2),
                listOf(2, 0, 5, 4, 4, 4, 2, 5, 3, 4, 4, 2),
            ),
            listOf(29, 28, 33, 33, 25, 32, 27, 31, 23, 24, 26, 26),
        ),
        AvGolden(
            "1983-03-06 London",
            mapOf(
                AshtakavargaReference.SUN to 10,
                AshtakavargaReference.MOON to 7,
                AshtakavargaReference.MANGALA to 11,
                AshtakavargaReference.BUDHA to 10,
                AshtakavargaReference.GURU to 7,
                AshtakavargaReference.SHUKRA to 11,
                AshtakavargaReference.SHANI to 6,
                AshtakavargaReference.LAGNA to 4,
            ),
            listOf(
                listOf(5, 3, 4, 4, 4, 5, 5, 5, 3, 5, 2, 3),
                listOf(4, 6, 4, 4, 6, 5, 1, 5, 6, 4, 3, 1),
                listOf(6, 2, 5, 3, 4, 3, 5, 1, 3, 5, 1, 1),
                listOf(6, 3, 8, 4, 3, 4, 6, 5, 4, 6, 2, 3),
                listOf(4, 5, 4, 3, 4, 6, 3, 5, 8, 4, 5, 5),
                listOf(3, 3, 7, 5, 4, 4, 4, 4, 6, 5, 3, 4),
                listOf(2, 3, 1, 2, 5, 4, 3, 3, 4, 5, 4, 3),
            ),
            listOf(30, 25, 33, 25, 30, 31, 27, 28, 34, 34, 20, 20),
        ),
    )
