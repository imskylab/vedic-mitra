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

import io.github.vedicmitra.core.datastore.JapaSession

/** Pure counting and streak helpers for japa — a traditional mala is 108 beads. */
object JapaLogic {
    /** Beads in one mala (round). */
    const val BEADS_PER_MALA = 108

    /** Completed malas in [beads] total beads. */
    fun rounds(beads: Int): Int = beads / BEADS_PER_MALA

    /** The bead within the current, partial mala (0..107). */
    fun beadInMala(beads: Int): Int = beads % BEADS_PER_MALA

    /** True exactly when [beads] just crossed a multiple of 108 (a mala was completed on this bead). */
    fun completesMala(beads: Int): Boolean = beads > 0 && beads % BEADS_PER_MALA == 0

    /** Total beads logged on the day [epochDay] across completed sittings. */
    fun beadsOn(
        sessions: List<JapaSession>,
        epochDay: Long,
    ): Int = sessions.filter { it.dateEpochDay == epochDay }.sumOf { it.beads }

    /**
     * The current daily streak: consecutive days with at least one sitting, ending today (if a sitting
     * was logged today) or yesterday (the streak is still alive until a full day passes without one).
     * [sessionDays] are the distinct `dateEpochDay`s that have a sitting.
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
}
