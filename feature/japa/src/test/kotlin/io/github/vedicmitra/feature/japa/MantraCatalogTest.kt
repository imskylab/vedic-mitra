/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.japa

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import io.github.vedicmitra.core.common.model.ContentSource
import org.junit.Test

/**
 * Provenance only. `MantraCatalog` had no test of any kind, and this covers the part the content
 * standard requires — **not** the structural and lookup coverage it still wants (unique ids, every
 * graha mapped, the screen's suggestion resolving for every input). That is deliberately left for
 * the issue that asks for it, so it stays a real piece of work rather than a half-done one.
 */
class MantraCatalogTest {
    @Test
    fun `the unsourced backlog can shrink but never grow`() {
        // All twelve mantras predate docs/knowledge-standards.md and none has an identified source.
        // Pinning the count is what turns that from a habit into a debt: adding a thirteenth
        // without a source pushes this past twelve and fails here, while sourcing any existing one
        // brings it down. Lower the bound as they are sourced; never raise it.
        val unsourced = MantraCatalog.all.count { it.source is ContentSource.NotRecorded }

        assertWithMessage("mantras still lacking an identified source")
            .that(unsourced)
            .isAtMost(EXPECTED_UNSOURCED)
    }

    @Test
    fun `a sourced mantra says where it came from`() {
        // Guards the display path the reader actually sees, independent of what is populated today.
        val cited = ContentSource.Text(work = "Rigveda", locus = "3.62.10")

        assertThat(cited.label).contains("Rigveda")
        assertThat(ContentSource.NotRecorded.label).isNotEmpty()
    }

    private companion object {
        const val EXPECTED_UNSOURCED = 12
    }
}
