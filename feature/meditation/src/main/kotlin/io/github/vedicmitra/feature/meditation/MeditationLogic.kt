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

import io.github.vedicmitra.core.datastore.MeditationSession

/** Pure totals, streak, and formatting helpers for meditation. */
object MeditationLogic {
    private const val SECONDS_PER_MINUTE = 60
    private const val MINUTES_PER_HOUR = 60

    /** Total seconds meditated on the day [epochDay] across logged sits. */
    fun secondsOn(
        sessions: List<MeditationSession>,
        epochDay: Long,
    ): Int = sessions.filter { it.dateEpochDay == epochDay }.sumOf { it.durationSeconds }

    /**
     * The current daily streak: consecutive days with at least one sit, ending today (if one was
     * logged today) or yesterday (the streak stays alive until a full day passes without one).
     * [sessionDays] are the distinct `dateEpochDay`s that have a sit.
     */
    fun currentStreak(
        sessionDays: Set<Long>,
        todayEpochDay: Long,
    ): Int {
        var day =
            when {
                todayEpochDay in sessionDays -> todayEpochDay
                (todayEpochDay - 1) in sessionDays -> todayEpochDay - 1
                else -> return 0
            }
        var count = 0
        while (day in sessionDays) {
            count++
            day--
        }
        return count
    }

    /** A compact human label for a duration in [seconds], e.g. "45 sec", "12 min", "1 h 5 min". */
    fun formatDuration(seconds: Int): String {
        if (seconds < SECONDS_PER_MINUTE) return "$seconds sec"
        val totalMinutes = seconds / SECONDS_PER_MINUTE
        val hours = totalMinutes / MINUTES_PER_HOUR
        val minutes = totalMinutes % MINUTES_PER_HOUR
        return if (hours > 0) "$hours h $minutes min" else "$minutes min"
    }

    /** Clock label "M:SS" for a remaining/elapsed [seconds] count. */
    fun formatClock(seconds: Int): String {
        val minutes = seconds / SECONDS_PER_MINUTE
        val secs = seconds % SECONDS_PER_MINUTE
        return "%d:%02d".format(minutes, secs)
    }
}
