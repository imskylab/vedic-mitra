/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.datastore

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MuhurtaOffsetCodecTest {
    @Test
    fun `round-trips a muhurta offset`() {
        val offset = MuhurtaOffset(name = "Brahma Muhurta", offsetMinutes = 30)

        assertThat(MuhurtaOffsetCodec.decode(MuhurtaOffsetCodec.encode(offset))).isEqualTo(offset)
    }

    @Test
    fun `decode returns null for a malformed value`() {
        assertThat(MuhurtaOffsetCodec.decode("not-an-offset")).isNull()
    }

    @Test
    fun `decode returns null when minutes is not a number`() {
        val offset = MuhurtaOffset(name = "Rahu Kalam", offsetMinutes = 1)
        // Corrupt the minutes field of a valid encoding.
        val corrupted = MuhurtaOffsetCodec.encode(offset).replace("1", "x")

        assertThat(MuhurtaOffsetCodec.decode(corrupted)).isNull()
    }
}
