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

class PanchangaGlossaryTest {
    @Test
    fun `a numbered window falls back to its unnumbered entry`() {
        // The bug this pins: MuhurtaCalculator names a window "Dur Muhurta 1" / "Dur Muhurta 2" when
        // a weekday has two of them, but the glossary is keyed "Dur Muhurta". Saturday is the only
        // weekday with two, and on Saturdays both rows were showing the caller's "no significance
        // known" fallback instead of the blurb.
        val plain = PanchangaGlossary.significanceOf("Dur Muhurta")
        assertThat(plain).isNotNull()
        assertThat(PanchangaGlossary.significanceOf("Dur Muhurta 1")).isEqualTo(plain)
        assertThat(PanchangaGlossary.significanceOf("Dur Muhurta 2")).isEqualTo(plain)
    }

    @Test
    fun `every muhurta the calculator can emit has a blurb`() {
        // Names taken from MuhurtaCalculator, including the numbered forms. If a window is renamed
        // there, this fails rather than silently degrading in the UI.
        listOf(
            "Brahma Muhurta",
            "Abhijit Muhurta",
            "Rahu Kalam",
            "Yamaganda",
            "Gulika Kalam",
            "Dur Muhurta",
            "Dur Muhurta 1",
            "Dur Muhurta 2",
            "Varjyam",
        ).forEach { name ->
            assertWithMessage(name).that(PanchangaGlossary.significanceOf(name)).isNotNull()
        }
    }

    @Test
    fun `stripping the suffix does not invent entries`() {
        // The fallback must only rescue a real entry, never manufacture one for an unknown name.
        assertThat(PanchangaGlossary.significanceOf("Not A Window 2")).isNull()
        assertThat(PanchangaGlossary.significanceOf("Ekadashi 3")).isNotNull() // real entry, numbered
        assertThat(PanchangaGlossary.significanceOf("12")).isNull()
    }

    @Test
    fun `sankrantis resolve by suffix`() {
        assertThat(PanchangaGlossary.significanceOf("Makara Sankranti")).isNotNull()
        assertThat(PanchangaGlossary.significanceOf("Mesha Sankranti")).isNotNull()
        assertWithMessage("Makara has its own blurb, not the generic one")
            .that(PanchangaGlossary.significanceOf("Makara Sankranti"))
            .isNotEqualTo(PanchangaGlossary.significanceOf("Mesha Sankranti"))
    }
}
