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

class ReminderCodecTest {
    @Test
    fun `round-trips a reminder`() {
        val reminder =
            PersistedReminder(
                id = "muhurta:Abhijit Muhurta",
                triggerAtEpochMillis = 1_705_300_140_000L,
                title = "Abhijit Muhurta",
                body = "This auspicious window is beginning now.",
            )

        assertThat(ReminderCodec.decode(ReminderCodec.encode(reminder))).isEqualTo(reminder)
    }

    @Test
    fun `decode returns null for a malformed value`() {
        assertThat(ReminderCodec.decode("not-a-reminder")).isNull()
    }

    @Test
    fun `decode returns null when the trigger time is not a number`() {
        val reminder =
            PersistedReminder(
                id = "id",
                triggerAtEpochMillis = 1L,
                title = "t",
                body = "b",
            )
        // Corrupt the trigger field of a valid encoding.
        val corrupted = ReminderCodec.encode(reminder).replace("1", "x")

        assertThat(ReminderCodec.decode(corrupted)).isNull()
    }
}
