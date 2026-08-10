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

class PanchangaGlossaryTest {
    // Every item the app surfaces in the Home lists must have a blurb. Keep this in sync with the
    // muhurta names, observanceAt(...), and the FESTIVAL_RULES names.
    private val surfacedNames =
        listOf(
            "Brahma Muhurta",
            "Abhijit Muhurta",
            "Rahu Kalam",
            "Yamaganda",
            "Gulika Kalam",
            "Dur Muhurta",
            "Varjyam",
            "Ekadashi",
            "Purnima",
            "Amavasya",
            "Sankashti Chaturthi",
            "Vinayaka Chaturthi",
            "Pradosh",
            "Masik Shivaratri",
            "Ugadi / Gudi Padwa",
            "Rama Navami",
            "Akshaya Tritiya",
            "Buddha Purnima",
            "Guru Purnima",
            "Raksha Bandhan",
            "Krishna Janmashtami",
            "Ganesh Chaturthi",
            "Navaratri begins",
            "Vijayadashami",
            "Diwali",
            "Maha Shivaratri",
            "Holi",
        )

    @Test
    fun `every surfaced panchanga item has a non-blank significance blurb`() {
        surfacedNames.forEach { name ->
            val blurb = PanchangaGlossary.significanceOf(name)
            assertThat(blurb).isNotNull()
            assertThat(blurb!!.isNotBlank()).isTrue()
        }
    }

    @Test
    fun `any Sankranti resolves to a blurb, with Makara called out specially`() {
        assertThat(PanchangaGlossary.significanceOf("Simha Sankranti")).isNotNull()
        assertThat(PanchangaGlossary.significanceOf("Makara Sankranti")).contains("Uttarayana")
        assertThat(PanchangaGlossary.significanceOf("Simha Sankranti"))
            .isNotEqualTo(PanchangaGlossary.significanceOf("Makara Sankranti"))
    }

    @Test
    fun `an unknown name has no blurb`() {
        assertThat(PanchangaGlossary.significanceOf("Not A Panchanga Thing")).isNull()
    }
}
