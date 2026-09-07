/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.home

import io.github.vedicmitra.core.astronomy.Muhurta
import io.github.vedicmitra.core.astronomy.MuhurtaQuality
import kotlin.time.Instant

/**
 * The muhurta the home strip is showing: the one running now (its end), or the next one to open
 * (its start).
 *
 * @property name the muhurta's name (e.g. "Abhijit Muhurta", "Rahu Kalam").
 * @property quality whether it is auspicious or inauspicious.
 * @property boundary the end (when [isActive]) or the start (when upcoming).
 * @property isActive whether the window is in effect right now.
 */
data class AuspiciousWindow(
    val name: String,
    val quality: MuhurtaQuality,
    val boundary: Instant,
    val isActive: Boolean,
)

/**
 * The muhurta running at [now], else the next one to open. `null` when the day holds neither.
 *
 * A free function rather than a method on the ViewModel because it is re-evaluated **on a timer while
 * the screen is open**, not only when the screen loads. The card outlived its own window otherwise:
 * `load()` runs on `ON_RESUME`, so Home left open across a boundary kept showing a window that had
 * already ended.
 *
 * Two rules decide what is shown, and both are deliberate:
 *
 * **An active inauspicious window beats an overlapping auspicious one.** Varjyam can run through
 * Brahma Muhurta, and a caution not shown is a worse failure than a favourable window not shown.
 * This reverses the earlier preference, which led with whichever was auspicious.
 *
 * **When nothing is running, the next window is whichever comes first, either quality.** Previously
 * only an auspicious window could be previewed, so an approaching Rahu Kalam was invisible until it
 * began — and the card vanished entirely once the day's last auspicious window had passed.
 *
 * Choghadiya is deliberately not considered. Those periods tile the whole day, so one is always
 * running; including them would mean the "next window" case could never occur at all.
 */
internal fun activeOrNextMuhurta(
    muhurtas: List<Muhurta>,
    now: Instant,
): AuspiciousWindow? {
    // Half-open: a window whose end is exactly now has finished, and its successor is what matters.
    val active = muhurtas.filter { now >= it.start && now < it.end }
    if (active.isNotEmpty()) {
        val cautions = active.filter { it.quality == MuhurtaQuality.INAUSPICIOUS }
        // Soonest-ending within the winning quality, so the countdown belongs to the boundary the
        // reader will actually meet first. Picking by list order would depend on emission order.
        val current = (cautions.ifEmpty { active }).minByOrNull { it.end } ?: return null
        return AuspiciousWindow(current.name, current.quality, boundary = current.end, isActive = true)
    }

    val next = muhurtas.filter { it.start > now }.minByOrNull { it.start } ?: return null
    return AuspiciousWindow(next.name, next.quality, boundary = next.start, isActive = false)
}
