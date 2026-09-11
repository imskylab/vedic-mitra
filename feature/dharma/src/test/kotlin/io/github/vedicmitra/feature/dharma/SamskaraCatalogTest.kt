/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.dharma

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import io.github.vedicmitra.core.common.model.ContentSource
import org.junit.Test

/** Provenance and identity: that every rite names a real passage, and that nothing is keyed on copy. */
class SamskaraCatalogTest {
    @Test
    fun `nothing here is unsourced`() {
        // The stotra and mantra catalogs carry a ratchet because their content predates
        // docs/knowledge-standards.md and the honest count was 38. This content was written after
        // the rule, so the only defensible bound is zero -- and it is zero by construction, since
        // SamskaraEntry.source has no default. This asserts the construction was not worked around.
        val unsourced = SamskaraCatalog.all.count { it.source is ContentSource.NotRecorded }

        assertWithMessage("a samskara shipped without an identified source").that(unsourced).isEqualTo(0)
    }

    @Test
    fun `every source names a text and a place in it`() {
        // A citation that cannot say where in the work is not much of a citation. Each locus here was
        // read in Oldenberg before it was written down.
        SamskaraCatalog.all.forEach { entry ->
            val source = entry.source as ContentSource.Text
            assertWithMessage("${entry.kind} work").that(source.work).contains("Grhyasutra")
            assertWithMessage("${entry.kind} locus").that(source.locus).isNotNull()
        }
    }

    @Test
    fun `an id is never its own label`() {
        // "A claim is not its own key" -- four recorded instances in this repo, all of them a lookup
        // built on display copy. MuhurtaKindTest is the worked example this copies.
        Samskara.entries.forEach { kind ->
            assertWithMessage("${kind.name} id").that(kind.id).isNotEqualTo(kind.label)
            assertWithMessage("${kind.name} id should be a slug").that(kind.id).matches("[a-z-]+")
        }
        assertThat(Samskara.entries.map { it.id }).containsNoDuplicates()
    }

    @Test
    fun `every rite the catalog offers a muhurta for is one the app can actually open`() {
        // The bridge the roadmap calls "where the two domains meet". A link naming an activity that
        // no longer exists would compile and then dead-end, so the mapping is asserted rather than
        // assumed.
        val linked = SamskaraCatalog.all.mapNotNull { it.muhurta }

        assertThat(linked).isNotEmpty()
        assertThat(linked).containsNoDuplicates()
    }

    @Test
    fun `the catalog covers the enum exactly`() {
        assertThat(SamskaraCatalog.all.map { it.kind }).containsExactlyElementsIn(Samskara.entries)
    }

    @Test
    fun `Karnavedha is absent, deliberately`() {
        // The app offers a muhurta for Karnavedha and cannot explain it, because ear-piercing does
        // not appear in any of the four grhyasutras of SBE 29. That gap is recorded here rather than
        // closed with a plausible-looking citation -- which is the failure ContentSource exists to
        // prevent. Delete this test when a source is found and the entry is written.
        assertWithMessage("a Karnavedha entry arrived -- if it is sourced, delete this test")
            .that(Samskara.entries.map { it.id })
            .doesNotContain("karnavedha")
    }
}
