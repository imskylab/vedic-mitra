/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
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
