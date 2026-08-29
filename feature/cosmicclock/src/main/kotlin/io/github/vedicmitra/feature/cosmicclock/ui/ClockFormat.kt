/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.cosmicclock.ui

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * Time formatting for the clock, matching what the Home and Panchang screens already show.
 *
 * Duplicated rather than shared because the originals are private to `:feature:home` and promoting
 * them to a core module for two small functions would be a wider change than this screen justifies.
 * If a third screen needs them, that is the moment to move them.
 */
private val clockTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** An instant as local wall-clock time in the device's zone, or an em dash if absent. */
internal fun formatClockTime(instant: Instant?): String =
    if (instant == null) {
        "—"
    } else {
        java.time.Instant
            .ofEpochMilli(instant.toEpochMilliseconds())
            .atZone(ZoneId.systemDefault())
            .format(clockTimeFormatter)
    }

/**
 * A coarse "4h 12m" / "12m" countdown.
 *
 * Seconds would churn every tick without telling the reader anything they can act on — and this
 * screen only recomposes once a minute anyway, so a seconds figure would be wrong most of the time.
 */
internal fun formatRemaining(remaining: Duration): String {
    val hours = remaining.inWholeHours
    val minutes = remaining.inWholeMinutes % MINUTES_PER_HOUR
    return when {
        remaining <= Duration.ZERO -> "moments"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

private const val MINUTES_PER_HOUR = 60L
