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
 * The vashya classification, including the two signs that split at their midpoint.
 *
 * The half-sign rule is the part worth pinning down: Dhanu is human in its first half and equine in
 * its second, Makara land-dwelling then water-dwelling, so the same sign yields two answers
 * depending on the degree.
 */
class VashyaTest {
    @Test
    fun `whole signs give the same class at every degree`() {
        val expected =
            mapOf(
                0 to Vashya.CHATUSHPADA,
                1 to Vashya.CHATUSHPADA,
                2 to Vashya.MANAVA,
                3 to Vashya.JALACHARA,
                4 to Vashya.VANACHARA,
                5 to Vashya.MANAVA,
                6 to Vashya.MANAVA,
                7 to Vashya.KEETA,
                10 to Vashya.MANAVA,
                11 to Vashya.JALACHARA,
            )
        expected.forEach { (sign, vashya) ->
            (0..29).forEach { degree ->
                assertWithMessage("sign $sign at $degree deg")
                    .that(vashyaOf(sign, degree))
                    .isEqualTo(vashya)
            }
        }
    }

    @Test
    fun `Dhanu turns from human to quadruped at its midpoint`() {
        assertThat(vashyaOf(8, 0)).isEqualTo(Vashya.MANAVA)
        assertThat(vashyaOf(8, 14)).isEqualTo(Vashya.MANAVA)
        assertThat(vashyaOf(8, 15)).isEqualTo(Vashya.CHATUSHPADA)
        assertThat(vashyaOf(8, 29)).isEqualTo(Vashya.CHATUSHPADA)
    }

    @Test
    fun `Makara turns from quadruped to water-dweller at its midpoint`() {
        assertThat(vashyaOf(9, 0)).isEqualTo(Vashya.CHATUSHPADA)
        assertThat(vashyaOf(9, 14)).isEqualTo(Vashya.CHATUSHPADA)
        assertThat(vashyaOf(9, 15)).isEqualTo(Vashya.JALACHARA)
        assertThat(vashyaOf(9, 29)).isEqualTo(Vashya.JALACHARA)
    }

    @Test
    fun `every sign and degree yields a class`() {
        (0..11).forEach { sign ->
            (0..29).forEach { degree ->
                assertWithMessage("sign $sign at $degree deg").that(vashyaOf(sign, degree)).isNotNull()
            }
        }
    }
}
