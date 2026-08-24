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
 * A divisional chart drawn as a chart in its own right, rather than as a column of signs.
 *
 * A varga is usually read the way the rashi chart is read: the lagna's own divisional sign becomes
 * the first house, and every graha is placed by counting whole signs from there. So the D-9 of a
 * chart is not simply "each graha's navamsha sign" — it is a twelve-house figure whose houses mean
 * what houses mean, and the seventh house of the D-9 is the one a reader is looking for.
 *
 * That framing is the whole reason this type exists. [NatalGraha.varga] already gives a graha's
 * divisional sign, and the Spashta Graha table shows it; what it cannot give is which *house* that
 * sign occupies, because a house only exists relative to an ascendant. The lagna's own longitude has
 * to be divided too, and it is the one longitude a per-graha accessor never sees.
 *
 * The same caution as [Varga] applies: these are exact given the longitudes, and the longitudes are
 * good to about an arcminute, so a graha sitting within an arcminute of a division edge may be shown
 * one sign — and therefore one house — either way. The finer the varga, the likelier that is.
 *
 * @property varga which divisional chart this is.
 * @property lagna the ascendant's sign in that division — the first house.
 * @property houses the rashi occupying each of the twelve whole-sign houses, house 1 first.
 */
data class VargaChart(
    val varga: Varga,
    val lagna: Rasi,
    val houses: List<Rasi>,
) {
    /** Which house of this divisional chart [graha] falls in, 1..12. */
    fun houseOf(graha: NatalGraha): Int = houseFrom(lagna.index, graha.varga(varga).index)
}

/** This chart cast into [varga] — the divisional figure, not just the divisional signs. */
fun NatalChart.vargaChart(varga: Varga): VargaChart {
    val vargaLagna = vargaSign(varga, lagna.siderealLongitude)
    return VargaChart(
        varga = varga,
        lagna = vargaLagna,
        houses = wholeSignHouses(vargaLagna.index),
    )
}
