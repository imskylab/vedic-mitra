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

/** Whole-sign graha drishti, against the classical statement of it. */
class DrishtiTest {
    @Test
    fun `every graha aspects the seventh from itself`() {
        TRUE_GRAHAS.forEach { graha ->
            (0 until 12).forEach { from ->
                val seventh = (from + 6) % 12
                assertWithMessage("${graha.displayName} from sign $from")
                    .that(Drishti.aspects(graha, from, seventh))
                    .isTrue()
            }
        }
    }

    @Test
    fun `the special aspects belong to the grahas that have them`() {
        mapOf(
            Graha.MANGALA to setOf(4, 8),
            Graha.GURU to setOf(5, 9),
            Graha.SHANI to setOf(3, 10),
        ).forEach { (graha, special) ->
            (1..12).forEach { house ->
                val expected = house == 7 || house in special
                val to = (house - 1) % 12
                assertWithMessage("${graha.displayName} on the ${ordinal(house)} from itself")
                    .that(Drishti.aspects(graha, 0, to))
                    .isEqualTo(expected)
            }
        }
    }

    @Test
    fun `a graha without special aspects looks only at the seventh`() {
        listOf(Graha.SUN, Graha.MOON, Graha.BUDHA, Graha.SHUKRA).forEach { graha ->
            (1..12).forEach { house ->
                assertWithMessage("${graha.displayName} on the ${ordinal(house)} from itself")
                    .that(Drishti.aspects(graha, 0, (house - 1) % 12))
                    .isEqualTo(house == 7)
            }
        }
    }

    @Test
    fun `no graha aspects its own sign, but it does influence it`() {
        TRUE_GRAHAS.forEach { graha ->
            assertWithMessage("${graha.displayName} aspecting itself").that(Drishti.aspects(graha, 3, 3)).isFalse()
            assertWithMessage("${graha.displayName} influencing itself").that(Drishti.influences(graha, 3, 3)).isTrue()
        }
    }

    @Test
    fun `drishti is asymmetric where the special aspects are`() {
        // Saturn in Mesha looks upon Mithuna, the 3rd from it. Nothing in Mithuna looks back at Mesha,
        // which is the 11th from Mithuna and nobody's aspect. A Western trine would be mutual; this
        // is exactly the difference that makes the two sets non-interchangeable.
        assertThat(Drishti.aspects(Graha.SHANI, 0, 2)).isTrue()
        assertThat(Drishti.aspects(Graha.SHANI, 2, 0)).isFalse()
    }

    @Test
    fun `the nodes report no drishti because no convention is agreed`() {
        listOf(Graha.RAHU, Graha.KETU).forEach { node ->
            (0 until 12).forEach { to ->
                assertWithMessage("${node.displayName} onto sign $to")
                    .that(Drishti.aspects(node, 0, to))
                    .isFalse()
            }
            // Conjunction is a placement, not an aspect, so it still counts as influence.
            assertThat(Drishti.influences(node, 4, 4)).isTrue()
        }
    }

    @Test
    fun `aspects wrap around the zodiac`() {
        // Jupiter in Kumbha (10) aspects the 5th from it — Mithuna (2) — across the 0 degree wrap.
        assertThat(Drishti.aspects(Graha.GURU, 10, 2)).isTrue()
        // And the 9th from it, Tula (6).
        assertThat(Drishti.aspects(Graha.GURU, 10, 6)).isTrue()
    }
}

private val TRUE_GRAHAS = Graha.entries.filter { it != Graha.RAHU && it != Graha.KETU }
