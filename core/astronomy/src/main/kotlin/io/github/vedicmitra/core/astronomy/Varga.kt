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

/**
 * A varga — a divisional chart, made by cutting each rashi into [divisions] equal parts and reading
 * which sign each part belongs to.
 *
 * Only the vargas whose sign follows directly from a continuous count are here. For these, counting
 * divisions from 0° Mesha and taking the result modulo twelve reproduces the classical rule exactly:
 * the ninth sign from a fixed sign, the fifth from a dual one, and so on all fall out of the count
 * rather than needing a table. Checked against an independent implementation across 75 charts —
 * 7,500 placements, no disagreements.
 *
 * The vargas left out need more than a count. D-3, D-4, D-10, D-12, D-24, D-40, D-45 and D-60 keep
 * the same shape but start each rashi somewhere the count does not reach, so each needs a twelve-entry
 * table; D-2 (hora), D-5, D-30 (unequal divisions) and D-108/D-144 (compositions of other vargas) are
 * different rules altogether. They belong in a later change with their own reference goldens, not
 * approximated here.
 *
 * @property divisions how many parts each rashi is cut into.
 * @property displayName the traditional name.
 */
enum class Varga(
    val divisions: Int,
    val displayName: String,
) {
    D1(1, "Rashi"),
    D6(6, "Shashthamsa"),
    D7(7, "Saptamsa"),
    D8(8, "Ashtamsa"),
    D9(9, "Navamsha"),
    D11(11, "Rudramsa"),
    D16(16, "Shodashamsa"),
    D20(20, "Vimshamsa"),
    D27(27, "Nakshatramsa"),
    D81(81, "Nava-navamsa"),
}

/**
 * The sign a sidereal longitude occupies in [varga].
 *
 * The division index is `(arcseconds × divisions) / RASHI_ARCSEC`, multiplying **before** dividing
 * on purpose. A varga's division spans `108,000 / divisions` arcseconds, which is not a whole number
 * for D-7, D-11 or D-81 — dividing by it first would reintroduce exactly the rounding that #129
 * removed from the pada. Multiplying first keeps every step in exact integer arithmetic; the largest
 * product any varga can produce is well inside a `Long`.
 */
internal fun vargaSign(
    varga: Varga,
    siderealDeg: Double,
): Rasi {
    val index = divisionIndex(varga, siderealDeg) % RASHI_NAMES.size
    return Rasi(index = index, name = RASHI_NAMES[index])
}

/** Which division of the whole zodiac [siderealDeg] falls in, counted from 0° Mesha. */
internal fun divisionIndex(
    varga: Varga,
    siderealDeg: Double,
): Int = ((AngularBuckets.arcseconds(siderealDeg) * varga.divisions) / AngularBuckets.RASHI_ARCSEC).toInt()
