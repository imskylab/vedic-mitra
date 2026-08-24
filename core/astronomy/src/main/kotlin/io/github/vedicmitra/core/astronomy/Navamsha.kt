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
 * The navamsha (D9) — each rashi divided into nine parts of 3°20′.
 *
 * Kept as a name of its own because the D9 is the varga a reader actually asks for, but the rule now
 * lives in [vargaSign] alongside every other divisional chart. The classical statement is three
 * cases — a movable sign's first navamsha is the sign itself, a fixed sign's is the ninth from it,
 * a dual sign's the fifth — and counting continuously from 0° Mesha collapses all three into one
 * expression. [VargaTest] checks the collapsed form against each case separately.
 */
internal fun navamshaOf(siderealDeg: Double): Rasi = vargaSign(Varga.D9, siderealDeg)

/**
 * How far into its rashi a longitude sits, as whole degrees and whole minutes.
 *
 * Integers, and taken from exact arcseconds rather than `longitude - rashiIndex * 30.0`: that
 * subtraction is the pattern #129 removed, and near a sign boundary it can land on the wrong side.
 *
 * Minutes rather than seconds is deliberate. The planetary positions come from Keplerian elements in
 * a J2000 frame with a linear precession term, which is good to arcminutes — `JplReferenceChartTest`
 * asserts rashi and nakshatra bins for exactly that reason and never raw degrees. A seconds column
 * would imply a precision the ephemeris does not have.
 */
internal fun positionInRashi(siderealDeg: Double): PositionInRashi {
    val intoSign = AngularBuckets.arcseconds(siderealDeg) % AngularBuckets.RASHI_ARCSEC
    return PositionInRashi(
        degrees = (intoSign / ARCSEC_PER_DEGREE).toInt(),
        minutes = ((intoSign % ARCSEC_PER_DEGREE) / ARCSEC_PER_MINUTE).toInt(),
    )
}

/**
 * A position within its rashi, to the arcminute.
 *
 * @property degrees whole degrees into the sign, 0..29.
 * @property minutes whole arcminutes past [degrees], 0..59.
 */
data class PositionInRashi(
    val degrees: Int,
    val minutes: Int,
)

private const val ARCSEC_PER_DEGREE = 3600L
private const val ARCSEC_PER_MINUTE = 60L
