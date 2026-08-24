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
 * A birth chart (kundali): where the grahas and the ascendant fell at a person's exact birth moment
 * and place, plus the Vimshottari dasha timeline. Everything is sidereal (Lahiri) with whole-sign
 * houses (the ascendant's rashi is the first house).
 *
 * @property lagna the ascendant.
 * @property houses the rashi occupying each of the twelve whole-sign houses, house 1 first,
 *   counted from the lagna — the Lagna Kundali.
 * @property moonHouses the same twelve houses counted from the Moon's rashi instead — the Rashi
 *   (Chandra) Kundali. The placements are identical; only the frame moves.
 * @property grahas the nine grahas with their rashi, house and retrograde state.
 * @property moonNakshatra the nakshatra the Moon occupies (drives Vimshottari and muhurta).
 * @property moonPada the Moon's pada (quarter of the nakshatra), 1..4.
 * @property jataka the standing properties a panchanga lists beside the charts — gana, varna, yoni,
 *   nadi, the samvats and the ayanamsa at birth. `null` only for the lightweight/synthetic charts
 *   that test fixtures build; the real engine always populates it.
 * @property vimshottari the nine mahadasha periods over the 120-year cycle, birth inside the first.
 */
data class NatalChart(
    val lagna: Lagna,
    val houses: List<Rasi>,
    val moonHouses: List<Rasi>,
    val grahas: List<NatalGraha>,
    val moonNakshatra: Nakshatra,
    val moonPada: Int,
    val vimshottari: List<MahadashaPeriod>,
    val jataka: JatakaProfile? = null,
) {
    /**
     * The named combinations present in this chart.
     *
     * Derived on demand rather than stored, which also avoids the chart having to contain something
     * computed from itself.
     */
    val yogas: List<ChartYoga> get() = chartYogasOf(this)
}

/**
 * A graha's placement in a birth chart.
 *
 * @property graha which planet.
 * @property siderealLongitude its sidereal (Lahiri) ecliptic longitude, degrees 0..360.
 * @property rasi the rashi it occupies.
 * @property house the whole-sign house it falls in (1..12, counted from the ascendant).
 * @property houseFromMoon the same, counted from the Moon's rashi — its house in the Rashi Kundali.
 * @property retrograde whether it is moving retrograde (the nodes always are; Sun/Moon never).
 * @property combust whether it is astangata — within the Sun's glare. Needs the Sun's position as
 *   well as its own, so unlike the derivations below it cannot be computed from the longitude alone.
 *   Defaults to `false` for the lightweight charts test fixtures build.
 */
data class NatalGraha(
    val graha: Graha,
    val siderealLongitude: Double,
    val rasi: Rasi,
    val house: Int,
    val houseFromMoon: Int,
    val retrograde: Boolean,
    val combust: Boolean = false,
) {
    // The Spashta Graha columns are all pure functions of siderealLongitude, so they are derived
    // rather than stored. Passing them in would let a caller hand over a nakshatra that contradicts
    // the longitude beside it, and there is nothing a chart could do about that once built.

    /** How far into [rasi] it sits, to the arcminute. */
    val position: PositionInRashi get() = positionInRashi(siderealLongitude)

    /** The nakshatra it occupies. */
    val nakshatra: Nakshatra get() = nakshatraOf(siderealLongitude)

    /** The quarter of that nakshatra, 1..4. */
    val pada: Int get() = AngularBuckets.pada(siderealLongitude)

    /** Its sign in the navamsha (D9) division — the varga a reader asks for by name. */
    val navamsha: Rasi get() = varga(Varga.D9)

    /** Its sign in any supported divisional chart. */
    fun varga(varga: Varga): Rasi = vargaSign(varga, siderealLongitude)
}
