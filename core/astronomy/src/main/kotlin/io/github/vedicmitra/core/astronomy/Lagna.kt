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

/**
 * The lagna (ascendant) — the point of the ecliptic rising on the eastern horizon at a given moment
 * and place. In whole-sign houses its [rasi] is the first house.
 *
 * @property siderealLongitude the ascendant's sidereal (Lahiri) ecliptic longitude, degrees 0..360.
 * @property rasi the rashi the ascendant falls in.
 */
data class Lagna(
    val siderealLongitude: Double,
    val rasi: Rasi,
)
