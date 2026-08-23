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
 * The classical rule is stated as three cases: a movable sign's first navamsha is the sign itself, a
 * fixed sign's is the ninth from it, and a dual sign's is the fifth. Counting navamshas from 0° Aries
 * across the whole zodiac collapses all three into one expression — the ninth sign from Taurus and
 * the fifth from Gemini are exactly where a continuous count lands — so the sign is simply the global
 * navamsha ordinal modulo twelve.
 *
 * A navamsha spans 3°20′, the same 12,000 arcseconds as a pada, so [AngularBuckets.PADA_ARCSEC] is
 * reused rather than restating the division. Doing the arithmetic in arcseconds also keeps the
 * boundaries exact: 3°20′ is not representable in degrees, and dividing by `3.3333…` is the
 * double-rounding that put pada boundaries in the wrong quarter before #129.
 */
internal fun navamshaOf(siderealDeg: Double): Rasi {
    val index = AngularBuckets.index(siderealDeg, AngularBuckets.PADA_ARCSEC) % RASHI_NAMES.size
    return Rasi(index = index, name = RASHI_NAMES[index])
}

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
