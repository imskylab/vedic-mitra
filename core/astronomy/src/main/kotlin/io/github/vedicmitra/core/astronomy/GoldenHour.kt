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
 * The day's golden-hour windows for a location and date — when the Sun's elevation is between -4°
 * and +6°, the conventional photography definition of soft, warm light shortly after sunrise and
 * before sunset. A field is `null` when the Sun never crosses that elevation that day (e.g. at high
 * latitudes), matching [SunTimes]'s convention.
 *
 * @property morningStart when the Sun rises through -4° elevation.
 * @property morningEnd when the Sun climbs through +6° elevation.
 * @property eveningStart when the Sun descends through +6° elevation.
 * @property eveningEnd when the Sun sets through -4° elevation.
 */
data class GoldenHour(
    val morningStart: Instant?,
    val morningEnd: Instant?,
    val eveningStart: Instant?,
    val eveningEnd: Instant?,
)
