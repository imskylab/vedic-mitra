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

/**
 * One completed meditation sitting.
 *
 * @property completedAtEpochMillis when the sit was logged (also its unique key and sort order).
 * @property dateEpochDay the local date it was logged on (`LocalDate.toEpochDay`), for day-grouping
 *   and streaks — kept separate from the millis so grouping never depends on a time zone at read time.
 * @property durationSeconds how long the sit lasted, in seconds.
 * @property nakshatraNumber the Moon's nakshatra (1..27) when logged, or `null` if it wasn't recorded.
 * @property tithiNumber the tithi (1..30) when logged, or `null` if it wasn't recorded.
 */
data class MeditationSession(
    val completedAtEpochMillis: Long,
    val dateEpochDay: Long,
    val durationSeconds: Int,
    val nakshatraNumber: Int? = null,
    val tithiNumber: Int? = null,
)
