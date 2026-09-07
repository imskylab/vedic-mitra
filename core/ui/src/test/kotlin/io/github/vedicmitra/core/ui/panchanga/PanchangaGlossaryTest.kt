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
import com.google.common.truth.Truth.assertWithMessage
import io.github.vedicmitra.core.astronomy.FestivalKind
import io.github.vedicmitra.core.astronomy.MuhurtaKind
import io.github.vedicmitra.core.ui.R
import org.junit.Test

/**
 * The glossary is now **total over two enums** rather than a map keyed on display names, so most of
 * what this file used to check has become impossible to get wrong: there is no key to mistype, no
 * name to normalise, and an unhandled kind does not compile.
 *
 * What is left worth asserting is that every kind has *its own* copy — a `when` compiles perfectly
 * with two branches pointing at the same string, and that reads as one festival wearing another's
 * explanation.
 */
class PanchangaGlossaryTest {
    @Test
    fun `every muhurta window has a blurb of its own`() {
        val ids = MuhurtaKind.entries.map { PanchangaGlossary.significanceOf(it) }

        MuhurtaKind.entries.forEach {
            assertWithMessage("${it.name} blurb").that(PanchangaGlossary.significanceOf(it)).isNotEqualTo(0)
        }
        assertThat(ids).containsNoDuplicates()
    }

    @Test
    fun `every festival and observance has a blurb`() {
        FestivalKind.entries.forEach {
            assertWithMessage("${it.name} blurb").that(PanchangaGlossary.significanceOf(it)).isNotEqualTo(0)
        }
    }

    @Test
    fun `only the two Sankranti kinds share their wording, and not with each other`() {
        // Eleven rashis share one blurb, which is why they share one kind. Everything else is
        // distinct: a duplicate here means one festival is showing another's explanation.
        val ids = FestivalKind.entries.map { PanchangaGlossary.significanceOf(it) }

        assertThat(ids).containsNoDuplicates()
        assertThat(PanchangaGlossary.significanceOf(FestivalKind.SANKRANTI))
            .isNotEqualTo(PanchangaGlossary.significanceOf(FestivalKind.MAKARA_SANKRANTI))
    }

    @Test
    fun `Makara Sankranti is the one called out separately`() {
        assertThat(PanchangaGlossary.significanceOf(FestivalKind.MAKARA_SANKRANTI))
            .isEqualTo(R.string.glossary_sankranti_makara)
        assertThat(PanchangaGlossary.significanceOf(FestivalKind.SANKRANTI))
            .isEqualTo(R.string.glossary_sankranti_generic)
    }

    @Test
    fun `the numbered Dur Muhurtas resolve like any other window`() {
        // Saturday's two windows are *displayed* "Dur Muhurta 1" and "Dur Muhurta 2". That numbering
        // used to break the lookup, and a regex stripped the suffix to paper over it. There is
        // nothing to strip now: both occurrences are one kind, and the kind is what is asked for.
        assertThat(PanchangaGlossary.significanceOf(MuhurtaKind.DUR_MUHURTA))
            .isEqualTo(R.string.glossary_dur_muhurta)
    }
}
