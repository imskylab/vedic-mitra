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

/** The activity catalogue, and which of its entries are chosen for two people rather than one. */
class MuhurtaActivityTest {
    @Test
    fun `exactly the betrothal and the marriage are a couple's`() {
        // Pinned by name. Making an activity a couple's changes what the screen asks for and how a day
        // is ranked, so it should be a decision someone takes here rather than a default they inherit
        // -- and the reverse matters more: quietly dropping one back to a single person would rank a
        // wedding for the groom alone without anything saying so.
        val couples =
            MuhurtaActivity.entries
                .filter { it.participants == MuhurtaParticipants.COUPLE }
                .map { it.name }

        assertThat(couples).containsExactly("VAAGDAAN", "VIVAH")
    }

    @Test
    fun `everything else is chosen for one person`() {
        val single = MuhurtaActivity.entries.filter { it.participants == MuhurtaParticipants.ONE }

        assertThat(single).hasSize(MuhurtaActivity.entries.size - 2)
    }

    @Test
    fun `every activity belongs to a category that offers it`() {
        // The picker lists activities by category, so an activity whose category does not return it
        // would exist and be unreachable.
        MuhurtaActivity.entries.forEach { activity ->
            assertThat(MuhurtaActivity.inCategory(activity.category)).contains(activity)
        }
    }

    @Test
    fun `every category offers at least one activity`() {
        MuhurtaCategory.entries.forEach { category ->
            assertThat(MuhurtaActivity.inCategory(category)).isNotEmpty()
        }
    }
}
