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

/** Whether a muhurta window is favourable or to be avoided. */
enum class MuhurtaQuality { AUSPICIOUS, INAUSPICIOUS }

/**
 * A named time window of the day with an astrological quality — an auspicious muhurta (e.g. Brahma,
 * Abhijit) or an inauspicious kalam (e.g. Rahu Kalam).
 *
 * @property name the traditional name of the window.
 * @property start when the window begins.
 * @property end when the window ends.
 * @property quality whether the window is auspicious or inauspicious.
 */
data class Muhurta(
    val name: String,
    val start: Instant,
    val end: Instant,
    val quality: MuhurtaQuality,
)
