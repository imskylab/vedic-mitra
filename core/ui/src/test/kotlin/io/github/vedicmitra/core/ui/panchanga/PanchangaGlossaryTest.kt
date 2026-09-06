/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.ui.panchanga

import com.google.common.truth.Truth.assertThat
import io.github.vedicmitra.core.ui.R
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
    fun `every surfaced panchanga item has a significance blurb`() {
        surfacedNames.forEach { name ->
            assertThat(PanchangaGlossary.significanceOf(name)).isNotNull()
        }
    }

    @Test
    fun `any Sankranti resolves to a blurb, with Makara called out specially`() {
        assertThat(PanchangaGlossary.significanceOf("Simha Sankranti"))
            .isEqualTo(R.string.glossary_sankranti_generic)
        assertThat(PanchangaGlossary.significanceOf("Makara Sankranti"))
            .isEqualTo(R.string.glossary_sankranti_makara)
        assertThat(PanchangaGlossary.significanceOf("Simha Sankranti"))
            .isNotEqualTo(PanchangaGlossary.significanceOf("Makara Sankranti"))
    }

    @Test
    fun `an unknown name has no blurb`() {
        assertThat(PanchangaGlossary.significanceOf("Not A Panchanga Thing")).isNull()
    }

    @Test
    fun `a numbered window falls back to its unnumbered entry`() {
        // MuhurtaCalculator names a window "Dur Muhurta 1" / "Dur Muhurta 2" when a weekday has two
        // of them. Saturday is the only such weekday, and on Saturdays both rows were showing the
        // caller's "no significance known" fallback rather than the blurb, because the display name
        // no longer matched the key. [surfacedNames] could not catch it: it lists the unnumbered
        // name, which always resolved.
        val plain = PanchangaGlossary.significanceOf("Dur Muhurta")

        assertThat(plain).isNotNull()
        assertThat(PanchangaGlossary.significanceOf("Dur Muhurta 1")).isEqualTo(plain)
        assertThat(PanchangaGlossary.significanceOf("Dur Muhurta 2")).isEqualTo(plain)
    }

    @Test
    fun `stripping a trailing number does not invent entries`() {
        // The fallback must rescue a real entry, never manufacture one for a name that has none.
        assertThat(PanchangaGlossary.significanceOf("Not A Panchanga Thing 2")).isNull()
        assertThat(PanchangaGlossary.significanceOf("12")).isNull()
    }

    @Test
    fun `no two items share a blurb`() {
        // Each of these explains one named thing. Two names resolving to the same resource means a
        // copy-paste in the map -- one of them is now showing the other's explanation. The two
        // Sankranti blurbs are deliberately shared and are not in this list.
        val ids = surfacedNames.map { PanchangaGlossary.significanceOf(it) }

        assertThat(ids).containsNoDuplicates()
    }
}
