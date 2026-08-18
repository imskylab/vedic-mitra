/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.meditation

import com.google.common.truth.Truth.assertThat
import io.github.vedicmitra.core.datastore.MeditationSession
import org.junit.Test

@Suppress("MagicNumber")
class MeditationLogicTest {
    @Test
    fun `secondsOn sums only the given day`() {
        val sessions =
            listOf(
                session(day = 100L, seconds = 600),
                session(day = 100L, seconds = 300),
                session(day = 99L, seconds = 900),
            )
        assertThat(MeditationLogic.secondsOn(sessions, 100L)).isEqualTo(900)
        assertThat(MeditationLogic.secondsOn(sessions, 98L)).isEqualTo(0)
    }

    @Test
    fun `streak counts consecutive days and survives a not-yet-sat today`() {
        assertThat(MeditationLogic.currentStreak(setOf(100L, 99L, 98L), 100L)).isEqualTo(3)
        assertThat(MeditationLogic.currentStreak(setOf(99L, 98L), 100L)).isEqualTo(2)
        assertThat(MeditationLogic.currentStreak(setOf(97L), 100L)).isEqualTo(0)
        assertThat(MeditationLogic.currentStreak(setOf(100L, 98L), 100L)).isEqualTo(1)
    }

    @Test
    fun `formatDuration reads naturally`() {
        assertThat(MeditationLogic.formatDuration(45)).isEqualTo("45 sec")
        assertThat(MeditationLogic.formatDuration(600)).isEqualTo("10 min")
        assertThat(MeditationLogic.formatDuration(3900)).isEqualTo("1 h 5 min")
    }

    @Test
    fun `formatClock is zero-padded`() {
        assertThat(MeditationLogic.formatClock(65)).isEqualTo("1:05")
        assertThat(MeditationLogic.formatClock(600)).isEqualTo("10:00")
    }

    private fun session(
        day: Long,
        seconds: Int,
    ) = MeditationSession(
        completedAtEpochMillis = day * 86_400_000L,
        dateEpochDay = day,
        durationSeconds = seconds,
    )
}
