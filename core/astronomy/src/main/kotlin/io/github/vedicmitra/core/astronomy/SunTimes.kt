/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
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
