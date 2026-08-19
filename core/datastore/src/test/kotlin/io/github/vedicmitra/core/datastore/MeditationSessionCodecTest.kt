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
class MeditationSessionCodecTest {
    @Test
    fun `a full sit round-trips`() {
        val session =
            MeditationSession(
                completedAtEpochMillis = 1_760_000_000_000L,
                dateEpochDay = 20_301L,
                durationSeconds = 600,
                nakshatraNumber = 5,
                tithiNumber = 11,
            )

        assertThat(MeditationSessionCodec.decode(MeditationSessionCodec.encode(session))).isEqualTo(session)
    }

    @Test
    fun `a sit without recorded astronomy round-trips with nulls`() {
        val session =
            MeditationSession(
                completedAtEpochMillis = 1_760_000_100_000L,
                dateEpochDay = 20_301L,
                durationSeconds = 900,
                nakshatraNumber = null,
                tithiNumber = null,
            )

        val decoded = MeditationSessionCodec.decode(MeditationSessionCodec.encode(session))
        assertThat(decoded).isEqualTo(session)
        assertThat(decoded?.nakshatraNumber).isNull()
    }

    @Test
    fun `malformed input decodes to null`() {
        assertThat(MeditationSessionCodec.decode("nope")).isNull()
        assertThat(MeditationSessionCodec.decode("")).isNull()
    }
}
