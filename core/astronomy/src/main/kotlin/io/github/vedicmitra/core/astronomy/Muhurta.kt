/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
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
