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

/** The karana identities, and that the sixty positions map onto them the way the month runs. */
class KaranaKindTest {
    @Test
    fun `an id is never its own label`() {
        // "A claim is not its own key" -- MuhurtaKindTest is the worked example. This enum exists
        // because the scorer used to ask `karana.name == "Vishti"`, which is the same failure.
        KaranaKind.entries.forEach { kind ->
            assertWithMessage("${kind.name} id").that(kind.id).isNotEqualTo(kind.label)
            assertWithMessage("${kind.name} id should be a slug").that(kind.id).matches("[a-z]+")
        }
        assertThat(KaranaKind.entries.map { it.id }).containsNoDuplicates()
    }

    @Test
    fun `the month is seven movable karanas and four fixed ones`() {
        assertThat(KaranaKind.movable).hasSize(7)
        assertThat(KaranaKind.entries.filter { it.fixed }).hasSize(4)
    }

    @Test
    fun `the sixty positions run Kimstughna, then the cycle, then the three fixed ones`() {
        assertThat(karanaKindAt(1)).isEqualTo(KaranaKind.KIMSTUGHNA)
        assertThat(karanaKindAt(2)).isEqualTo(KaranaKind.BAVA)
        assertThat(karanaKindAt(58)).isEqualTo(KaranaKind.SHAKUNI)
        assertThat(karanaKindAt(59)).isEqualTo(KaranaKind.CHATUSHPADA)
        assertThat(karanaKindAt(60)).isEqualTo(KaranaKind.NAGA)
    }

    @Test
    fun `Vishti falls at eight and every seventh position after it, never at seven`() {
        // The fixture bug this enum found. Three tests in this module described a day with Vishti at
        // position 7, which cannot happen -- position 7 is Vanija. They passed only because the
        // scorer matched on the name it was handed rather than on where in the month it was.
        assertThat(karanaKindAt(7)).isEqualTo(KaranaKind.VANIJA)

        val vishtiPositions = (1..60).filter { karanaKindAt(it) == KaranaKind.VISHTI }

        assertThat(vishtiPositions).containsExactly(8, 15, 22, 29, 36, 43, 50, 57).inOrder()
    }

    @Test
    fun `every movable karana comes round eight times`() {
        KaranaKind.movable.forEach { kind ->
            assertWithMessage("${kind.label} occurrences")
                .that((1..60).count { karanaKindAt(it) == kind })
                .isEqualTo(8)
        }
    }

    @Test
    fun `the printed name is the identity's label`() {
        // The single source of truth the enum exists to create: there is no second table of names
        // that could drift out of step with it.
        (1..60).forEach {
            assertWithMessage("position $it").that(karanaNameAt(it)).isEqualTo(karanaKindAt(it).label)
        }
    }
}
