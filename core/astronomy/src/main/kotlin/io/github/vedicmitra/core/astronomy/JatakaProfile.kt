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
 * The jataka's standing properties — the summary block a printed panchanga puts beside the charts.
 *
 * Values are display strings rather than the engine's enums on purpose: Gana, Varna, Yoni and Nadi
 * come from the tables in `GunaMilan.kt`, which exist for Ashtakoota matching and whose enum names
 * are scoring tokens, not labels. Reusing those tables and formatting here keeps one source of truth
 * for the classifications — a second copy that drifted from the matchmaking copy would be worse than
 * not showing them at all.
 *
 * @property janmaRashi the Moon's rashi — the rashi a person is normally identified by.
 * @property nakshatra the Moon's nakshatra with its pada.
 * @property gana Deva, Manushya or Rakshasa, from the birth nakshatra.
 * @property varna Brahmin, Kshatriya, Vaishya or Shudra, from the Moon's rashi.
 * @property yoni the animal symbol of the birth nakshatra.
 * @property nadi Aadi, Madhya or Antya, from the birth nakshatra.
 * @property lagna the ascendant's rashi.
 * @property rashiLord the lord of the Moon's rashi.
 * @property sunRashi the Sun's *sidereal* rashi, as everything else in this app is sidereal.
 * @property sunSign the Sun's **tropical** (Western) sign. The only tropical value in the app, shown
 *   because it is what most people mean by "sun sign" — labelled so it cannot be mistaken for the
 *   sidereal one above it.
 * @property ayanamsa the Lahiri ayanamsa at birth, in degrees — the offset between the two zodiacs,
 *   and the reason those two rows usually disagree by one sign.
 * @property shakaSamvat the Shaka year.
 * @property vikramSamvat the Vikram year, 135 ahead of Shaka.
 * @property samvatsara the year's name in the sixty-year Jovian cycle.
 */
data class JatakaProfile(
    val janmaRashi: Rasi,
    val nakshatra: Nakshatra,
    val pada: Int,
    val gana: String,
    val varna: String,
    val yoni: String,
    val nadi: String,
    val lagna: Rasi,
    val rashiLord: Graha,
    val sunRashi: Rasi,
    val sunSign: String,
    val ayanamsa: Double,
    val shakaSamvat: Int,
    val vikramSamvat: Int,
    val samvatsara: String,
)

/** Vikram Samvat runs 135 years ahead of Shaka. */
private const val VIKRAM_MINUS_SHAKA = 135

/** The tropical zodiac, used only for the Western sun-sign row. */
private val TROPICAL_SIGNS =
    listOf(
        "Aries",
        "Taurus",
        "Gemini",
        "Cancer",
        "Leo",
        "Virgo",
        "Libra",
        "Scorpio",
        "Sagittarius",
        "Capricorn",
        "Aquarius",
        "Pisces",
    )

/**
 * Derives the [JatakaProfile] from a chart's parts at [epochMillis].
 *
 * Takes the pieces rather than the assembled [NatalChart] because the chart carries this profile as a
 * field — passing the whole chart in would require it to exist before it could be built.
 *
 * The samvatsara is resolved through the same [samvatsaraOf] the daily panchanga uses, fed the same
 * closures, so a birth year named here and a date named on the Panchang screen cannot disagree.
 */
internal fun jatakaProfileOf(
    grahas: List<NatalGraha>,
    lagna: Lagna,
    moonNakshatra: Nakshatra,
    moonPada: Int,
    epochMillis: Long,
): JatakaProfile {
    val t = Ephemeris.julianCenturies(epochMillis)
    val ayanamsa = Ephemeris.lahiriAyanamsa(t)
    val moon = grahas.first { it.graha == Graha.MOON }
    val sun = grahas.first { it.graha == Graha.SUN }
    val nakshatraIndex = moonNakshatra.number - 1
    val samvatsara = samvatsaraOf(epochMillis, elongationAt(), sunSiderealAt())

    return JatakaProfile(
        janmaRashi = moon.rasi,
        nakshatra = moonNakshatra,
        pada = moonPada,
        gana = GANA_BY_NAKSHATRA[nakshatraIndex].label(),
        varna = VARNA_BY_RASI[moon.rasi.index].label(),
        yoni = YONI_BY_NAKSHATRA[nakshatraIndex].label(),
        nadi = NADI_BY_NAKSHATRA[nakshatraIndex].label(),
        lagna = lagna.rasi,
        rashiLord = RASI_LORD[moon.rasi.index],
        sunRashi = sun.rasi,
        sunSign = TROPICAL_SIGNS[AngularBuckets.rashiIndex(sun.siderealLongitude + ayanamsa)],
        ayanamsa = ayanamsa,
        shakaSamvat = samvatsara.shakaYear,
        vikramSamvat = samvatsara.shakaYear + VIKRAM_MINUS_SHAKA,
        samvatsara = samvatsara.name,
    )
}

private fun elongationAt(): (Long) -> Double =
    { at ->
        val atT = Ephemeris.julianCenturies(at)
        Ephemeris.norm360(Ephemeris.moonLongitude(atT) - Ephemeris.sunApparentLongitude(atT))
    }

private fun sunSiderealAt(): (Long) -> Double =
    { at ->
        val atT = Ephemeris.julianCenturies(at)
        Ephemeris.norm360(Ephemeris.sunApparentLongitude(atT) - Ephemeris.lahiriAyanamsa(atT))
    }

/** Enum constants are scoring tokens; these render them as words. */
private fun Gana.label(): String = name.titleCase()

private fun Varna.label(): String = name.titleCase()

private fun Yoni.label(): String = name.titleCase()

private fun Nadi.label(): String = name.titleCase()

private fun String.titleCase(): String = first() + lowercase().drop(1)
