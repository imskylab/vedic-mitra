/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

@file:Suppress("MagicNumber")

package io.github.vedicmitra.core.astronomy

import kotlin.math.abs

/**
 * Astangata — a graha "combust", swallowed by the Sun's glare.
 *
 * **The convention is fixed longitudinal orbs, from Brihat Parashara Hora Shastra**, and is stated
 * here because authorities differ and a reader deserves to know which one produced the answer. The
 * separation is the shortest angular distance in longitude only; latitude is ignored, as the
 * classical rule is stated in longitude.
 *
 * Mercury and Venus take a tighter orb when retrograde, being nearer the Earth and so harder for the
 * Sun to drown out.
 *
 * | Graha   | Direct | Retrograde |
 * |---------|--------|------------|
 * | Moon    | 12°    | —          |
 * | Mangala | 17°    | 17°        |
 * | Budha   | 14°    | 12°        |
 * | Guru    | 11°    | 11°        |
 * | Shukra  | 10°    | 8°         |
 * | Shani   | 15°    | 15°        |
 *
 * **This deliberately does not attempt true heliacal visibility.** Software that computes whether a
 * graha is *actually* invisible — from its magnitude, altitude and atmospheric extinction — gives
 * materially different answers: measured against one such implementation, Budha was found combust at
 * 7.62° in one chart and clear at 7.08° in another, which no fixed orb can reproduce. Visibility
 * needs planetary magnitudes and an extinction model, neither of which belongs in an engine that is
 * otherwise pure Meeus with no bundled data. The orbs above agree with that implementation on 171 of
 * 180 sampled placements, which is the honest ceiling for a table-based rule.
 *
 * The Sun is never combust, and Rahu and Ketu are shadows rather than bodies, so neither applies.
 */
internal object Astangata {
    private val ORBS: Map<Graha, Orb> =
        mapOf(
            Graha.MOON to Orb(direct = 12.0, retrograde = 12.0),
            Graha.MANGALA to Orb(direct = 17.0, retrograde = 17.0),
            Graha.BUDHA to Orb(direct = 14.0, retrograde = 12.0),
            Graha.GURU to Orb(direct = 11.0, retrograde = 11.0),
            Graha.SHUKRA to Orb(direct = 10.0, retrograde = 8.0),
            Graha.SHANI to Orb(direct = 15.0, retrograde = 15.0),
        )

    /** Whether [graha] at [siderealLongitude] is combust with the Sun at [sunSiderealLongitude]. */
    fun isCombust(
        graha: Graha,
        siderealLongitude: Double,
        sunSiderealLongitude: Double,
        retrograde: Boolean,
    ): Boolean {
        val orb = ORBS[graha] ?: return false
        return separation(siderealLongitude, sunSiderealLongitude) <
            if (retrograde) orb.retrograde else orb.direct
    }

    /** Shortest angular distance between two longitudes, 0..180 degrees. */
    fun separation(
        first: Double,
        second: Double,
    ): Double = abs(((first - second + 540.0) % 360.0) - 180.0)

    private data class Orb(
        val direct: Double,
        val retrograde: Double,
    )
}
