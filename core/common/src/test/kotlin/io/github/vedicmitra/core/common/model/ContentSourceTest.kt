/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.common.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ContentSourceTest {
    @Test
    fun `a citation names the work, where in it, and the recension`() {
        val source = ContentSource.Text(work = "Rigveda", locus = "3.62.10", recension = "Shakala")

        assertThat(source.label).isEqualTo("Rigveda 3.62.10 Shakala recension")
    }

    @Test
    fun `the optional parts are left out rather than shown empty`() {
        assertThat(ContentSource.Text(work = "Rigveda").label).isEqualTo("Rigveda")
        assertThat(ContentSource.Text(work = "Rigveda", locus = "3.62.10").label)
            .isEqualTo("Rigveda 3.62.10")
    }

    @Test
    fun `an unrecorded source says so rather than showing nothing`() {
        // A blank label would read as "no source needed". The point of the state is that a reader
        // can see the app has not identified one -- so it has to produce words.
        assertThat(ContentSource.NotRecorded.label).isEqualTo("Source not recorded")
    }
}
