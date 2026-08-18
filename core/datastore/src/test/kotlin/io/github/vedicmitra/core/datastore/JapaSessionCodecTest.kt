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

@Suppress("MagicNumber")
class JapaSessionCodecTest {
    @Test
    fun `a full session round-trips`() {
        val session =
            JapaSession(
                completedAtEpochMillis = 1_760_000_000_000L,
                dateEpochDay = 20_301L,
                mantraId = "gayatri",
                beads = 216,
                rounds = 2,
                nakshatraNumber = 5,
                tithiNumber = 11,
            )

        assertThat(JapaSessionCodec.decode(JapaSessionCodec.encode(session))).isEqualTo(session)
    }

    @Test
    fun `a session without recorded astronomy round-trips with nulls`() {
        val session =
            JapaSession(
                completedAtEpochMillis = 1_760_000_100_000L,
                dateEpochDay = 20_301L,
                mantraId = "om_namah_shivaya",
                beads = 108,
                rounds = 1,
                nakshatraNumber = null,
                tithiNumber = null,
            )

        val decoded = JapaSessionCodec.decode(JapaSessionCodec.encode(session))
        assertThat(decoded).isEqualTo(session)
        assertThat(decoded?.nakshatraNumber).isNull()
        assertThat(decoded?.tithiNumber).isNull()
    }

    @Test
    fun `malformed input decodes to null`() {
        assertThat(JapaSessionCodec.decode("not a session")).isNull()
        assertThat(JapaSessionCodec.decode("")).isNull()
    }
}
