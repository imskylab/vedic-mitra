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
 * Sunrise and sunset for a location and date, as absolute instants (UTC). Either may be `null` at
 * high latitudes on days when the Sun does not cross the horizon (polar day/night).
 *
 * @property sunrise the instant the Sun's upper limb rises, or `null` if it does not rise.
 * @property sunset the instant the Sun's upper limb sets, or `null` if it does not set.
 */
data class SunTimes(
    val sunrise: Instant?,
    val sunset: Instant?,
)
