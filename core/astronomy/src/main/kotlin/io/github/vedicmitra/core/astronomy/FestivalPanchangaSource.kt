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
 * Source of the per-day panchanga facts the festival rules need, injected so the rule engine can be
 * tested without the ephemeris (mirroring how [maasaOf] takes longitude lambdas). Instants are Unix
 * epoch milliseconds (UTC); [DefaultAstronomyEngine] provides the real, ephemeris-backed source.
 */
internal interface FestivalPanchangaSource {
    /** Sunrise for the civil day containing [dayEpochMillis], or `null` at extreme latitudes. */
    fun sunrise(dayEpochMillis: Long): Long?

    /** The tithi number 1..30 at [epochMillis] (1..15 Shukla, 16..30 Krishna). */
    fun tithiNumber(epochMillis: Long): Int

    /** The Sun's sidereal rashi index 0..11 (0 = Mesha) at [epochMillis]. */
    fun sunRashi(epochMillis: Long): Int

    /** The amanta lunar month at [epochMillis]. */
    fun maasa(epochMillis: Long): Maasa
}
