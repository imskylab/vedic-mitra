/*
 * Copyright (c) 2026 Vedic Mitra contributors
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

package io.github.vedicmitra.core.common.model

/**
 * A geographic point used across the location and astronomy contracts. A plain, framework-agnostic
 * value type — it holds coordinates but performs no calculation.
 *
 * @property latitude degrees north of the equator, in the range -90.0..90.0.
 * @property longitude degrees east of the prime meridian, in the range -180.0..180.0.
 */
data class GeoCoordinates(
    val latitude: Double,
    val longitude: Double,
)
