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

class MuhurtaRulesTest {
    @Test
    fun `every activity resolves to rules with valid nakshatra and tithi numbers`() {
        MuhurtaActivity.entries.forEach { activity ->
            val rules = muhurtaRulesFor(activity)
            assertThat(rules.favorableNakshatras).isNotEmpty()
            assertThat(rules.favorableVaras).isNotEmpty()
            rules.favorableNakshatras.forEach { assertThat(it in 1..27).isTrue() }
            rules.favorableTithis.forEach { assertThat(it in 1..30).isTrue() }
        }
    }

    // The auspicious tithis moved to ActivityMuhurtaRulesTest, which asserts the whole set as a
    // derivation from the verses rather than only that two things are missing from it.

    @Test
    fun `benefic weekdays exclude Tuesday Saturday and Sunday`() {
        assertThat(BENEFIC_VARAS)
            .containsExactly(Vara.SOMAVARA, Vara.BUDHAVARA, Vara.GURUVARA, Vara.SHUKRAVARA)
    }

    @Test
    fun `every category has at least one activity`() {
        MuhurtaCategory.entries.forEach { category ->
            assertThat(MuhurtaActivity.inCategory(category)).isNotEmpty()
        }
    }
}
