/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.astronomy

import kotlin.time.Instant

/**
 * Moonrise and moonset for a location and date, as absolute instants (UTC).
 *
 * Unlike [SunTimes], either field may be `null` even away from the poles: because the lunar day
 * (~24h50m) is longer than the civil day, roughly once a month a civil day has two moonrises (or
 * two moonsets) and the following day has none of that kind. This models the *first* occurrence
 * within the civil day, matching most consumer panchang displays; `null` means none occurred that
 * day, not that the Moon never rises/sets.
 *
 * @property moonrise the instant the Moon's upper limb rises, or `null` if it does not that day.
 * @property moonset the instant the Moon's upper limb sets, or `null` if it does not that day.
 */
data class MoonTimes(
    val moonrise: Instant?,
    val moonset: Instant?,
)
