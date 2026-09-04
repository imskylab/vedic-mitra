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
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * These assert against **data already on users' devices**, so the expected values here are not a
 * matter of taste. Every string on the left was written by a shipped build; if one of these tests is
 * ever failing, the fix is on the other side.
 */
class LegacyReminderKeysTest {
    @Test
    fun `every label a shipped build could have written maps to its kind`() {
        // The full set of Muhurta.name values that reached a release, and what each is now.
        val expected =
            mapOf(
                "muhurta:Brahma Muhurta" to "muhurta:brahma",
                "muhurta:Abhijit Muhurta" to "muhurta:abhijit",
                "muhurta:Rahu Kalam" to "muhurta:rahu-kalam",
                "muhurta:Yamaganda" to "muhurta:yamaganda",
                "muhurta:Gulika Kalam" to "muhurta:gulika-kalam",
                "muhurta:Dur Muhurta" to "muhurta:dur-muhurta",
                "muhurta:Varjyam" to "muhurta:varjyam",
            )

        expected.forEach { (legacy, current) ->
            assertWithMessage(legacy).that(LegacyReminderKeys.canonical(legacy)).isEqualTo(current)
        }
    }

    @Test
    fun `both numbered Dur Muhurtas land on the one kind`() {
        // The reason this mapping is not one-to-one. Saturday's two occurrences were numbered and so
        // keyed apart from every other weekday's plain "Dur Muhurta", which meant a reminder set on
        // a Sunday silently did not match on a Saturday.
        val keys =
            listOf("muhurta:Dur Muhurta", "muhurta:Dur Muhurta 1", "muhurta:Dur Muhurta 2")
                .map(LegacyReminderKeys::canonical)

        assertThat(keys.toSet()).containsExactly("muhurta:dur-muhurta")
    }

    @Test
    fun `a key that is already current is left alone`() {
        // Translation runs on every read, so it has to be safe to apply to its own output.
        val current = "muhurta:brahma"

        assertThat(LegacyReminderKeys.canonical(current)).isEqualTo(current)
        assertThat(LegacyReminderKeys.canonical(LegacyReminderKeys.canonical(current))).isEqualTo(current)
    }

    @Test
    fun `keys from the other spaces are untouched`() {
        // Neither was ever built from a display name -- choghadiya carries an enum name and tithi
        // carries tithi numbers -- so neither needs translating, and translating one would be a bug.
        listOf("choghadiya:AMRIT", "tithi:*:30", "tithi:Kartika:11,26", "muhurat:something")
            .forEach { assertWithMessage(it).that(LegacyReminderKeys.canonical(it)).isEqualTo(it) }
    }

    @Test
    fun `an unrecognised muhurta key is passed through rather than dropped`() {
        // A key this table has never heard of is more likely a future id than corruption. Passing it
        // through leaves a reminder that cannot resolve; mapping it to a guess would fire the wrong
        // alarm, and returning null would delete it.
        val unknown = "muhurta:Something Nobody Shipped"

        assertThat(LegacyReminderKeys.canonical(unknown)).isEqualTo(unknown)
    }
}
