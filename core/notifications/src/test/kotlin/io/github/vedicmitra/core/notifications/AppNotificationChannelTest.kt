/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.notifications

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppNotificationChannelTest {
    @Test
    fun `channel ids are unique`() {
        val ids = AppNotificationChannel.entries.map { it.id }

        assertThat(ids).containsNoDuplicates()
    }

    @Test
    fun `every channel has a non-blank id name and description`() {
        AppNotificationChannel.entries.forEach { channel ->
            assertThat(channel.id).isNotEmpty()
            assertThat(channel.channelName).isNotEmpty()
            assertThat(channel.description).isNotEmpty()
        }
    }
}
