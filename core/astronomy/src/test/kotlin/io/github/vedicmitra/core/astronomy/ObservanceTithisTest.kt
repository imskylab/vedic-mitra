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

class ObservanceTithisTest {
    @Test
    fun `maps each recurring observance to its global tithis`() {
        assertThat(observanceTithis("Ekadashi")).containsExactly(11, 26)
        assertThat(observanceTithis("Pradosh")).containsExactly(13, 28)
        assertThat(observanceTithis("Purnima")).containsExactly(15)
        assertThat(observanceTithis("Amavasya")).containsExactly(30)
        assertThat(observanceTithis("Sankashti Chaturthi")).containsExactly(19)
        assertThat(observanceTithis("Masik Shivaratri")).containsExactly(29)
        assertThat(observanceTithis("Vinayaka Chaturthi")).containsExactly(4)
    }

    @Test
    fun `returns null for a name that is not a recurring observance`() {
        assertThat(observanceTithis("Diwali")).isNull()
        assertThat(observanceTithis("Makara Sankranti")).isNull()
    }
}
