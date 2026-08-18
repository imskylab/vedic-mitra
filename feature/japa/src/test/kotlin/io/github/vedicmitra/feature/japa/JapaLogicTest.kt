/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.japa

import com.google.common.truth.Truth.assertThat
import io.github.vedicmitra.core.datastore.JapaSession
import org.junit.Test

@Suppress("MagicNumber")
class JapaLogicTest {
    @Test
    fun `beads split into rounds and the current mala position`() {
        assertThat(JapaLogic.rounds(0)).isEqualTo(0)
        assertThat(JapaLogic.beadInMala(0)).isEqualTo(0)

        assertThat(JapaLogic.rounds(107)).isEqualTo(0)
        assertThat(JapaLogic.beadInMala(107)).isEqualTo(107)

        assertThat(JapaLogic.rounds(108)).isEqualTo(1)
        assertThat(JapaLogic.beadInMala(108)).isEqualTo(0)

        assertThat(JapaLogic.rounds(250)).isEqualTo(2)
        assertThat(JapaLogic.beadInMala(250)).isEqualTo(34)
    }

    @Test
    fun `completesMala is true only on a multiple of 108`() {
        assertThat(JapaLogic.completesMala(0)).isFalse()
        assertThat(JapaLogic.completesMala(107)).isFalse()
        assertThat(JapaLogic.completesMala(108)).isTrue()
        assertThat(JapaLogic.completesMala(216)).isTrue()
    }

    @Test
    fun `beadsOn sums only the given day`() {
        val sessions =
            listOf(
                session(day = 100L, beads = 108),
                session(day = 100L, beads = 54),
                session(day = 99L, beads = 108),
            )
        assertThat(JapaLogic.beadsOn(sessions, 100L)).isEqualTo(162)
        assertThat(JapaLogic.beadsOn(sessions, 99L)).isEqualTo(108)
        assertThat(JapaLogic.beadsOn(sessions, 98L)).isEqualTo(0)
    }

    @Test
    fun `streak counts consecutive days ending today`() {
        assertThat(JapaLogic.currentStreak(setOf(100L, 99L, 98L), todayEpochDay = 100L)).isEqualTo(3)
    }

    @Test
    fun `streak stays alive when today has no sitting but yesterday did`() {
        assertThat(JapaLogic.currentStreak(setOf(99L, 98L), todayEpochDay = 100L)).isEqualTo(2)
    }

    @Test
    fun `streak breaks after a missed day`() {
        // Nothing today or yesterday -> broken.
        assertThat(JapaLogic.currentStreak(setOf(97L, 96L), todayEpochDay = 100L)).isEqualTo(0)
        // A gap stops the count.
        assertThat(JapaLogic.currentStreak(setOf(100L, 98L, 97L), todayEpochDay = 100L)).isEqualTo(1)
    }

    private fun session(
        day: Long,
        beads: Int,
    ) = JapaSession(
        completedAtEpochMillis = day * 86_400_000L,
        dateEpochDay = day,
        mantraId = "gayatri",
        beads = beads,
        rounds = beads / 108,
    )
}
