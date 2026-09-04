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
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * [MuhurtaKind.id] is persisted — a reminder is stored under it — so these are data-format tests
 * rather than style ones. The point of the enum is that display copy can change freely without
 * moving a key, and each test below is one way that could quietly stop being true.
 */
class MuhurtaKindTest {
    @Test
    fun `the ids are exactly these, and changing one is a migration`() {
        // Pinned deliberately, not derived. If this fails because an id was edited, the edit needs a
        // LegacyReminderKeys entry -- otherwise every reminder stored under the old id is orphaned,
        // which is the bug this enum exists to fix.
        val ids = MuhurtaKind.entries.associate { it.name to it.id }

        assertThat(ids).containsExactly(
            "BRAHMA", "brahma",
            "ABHIJIT", "abhijit",
            "RAHU_KALAM", "rahu-kalam",
            "YAMAGANDA", "yamaganda",
            "GULIKA_KALAM", "gulika-kalam",
            "DUR_MUHURTA", "dur-muhurta",
            "VARJYAM", "varjyam",
        )
    }

    @Test
    fun `no id is a display name`() {
        // The whole failure mode in one assertion: an id that equals its label is a label, and will
        // be "corrected" by the next person who improves the wording.
        MuhurtaKind.entries.forEach { kind ->
            assertWithMessage("${kind.name} id").that(kind.id).isNotEqualTo(kind.label)
            assertWithMessage("${kind.name} id should be a lowercase slug")
                .that(kind.id)
                .matches("[a-z][a-z0-9-]*")
        }
    }

    @Test
    fun `ids and labels are each unique`() {
        // Two kinds sharing an id would silently merge two users' reminders into one.
        assertThat(MuhurtaKind.entries.map { it.id }).containsNoDuplicates()
        assertThat(MuhurtaKind.entries.map { it.label }).containsNoDuplicates()
    }
}
