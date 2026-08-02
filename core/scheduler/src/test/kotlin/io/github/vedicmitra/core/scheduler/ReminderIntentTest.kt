/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.scheduler

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import io.github.vedicmitra.core.notifications.AppNotification
import io.github.vedicmitra.core.notifications.AppNotificationChannel
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ReminderIntentTest {
    @Test
    fun `round-trips the notification through intent extras`() {
        val original =
            AppNotification(
                id = 99,
                channel = AppNotificationChannel.MUHURTA_REMINDERS,
                title = "Abhijit Muhurta",
                body = "Begins in 10 minutes.",
            )

        val restored = ReminderIntent.readNotification(ReminderIntent.putNotification(Intent(), original))

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `readNotification returns null for an intent without a reminder payload`() {
        assertThat(ReminderIntent.readNotification(Intent())).isNull()
    }
}
