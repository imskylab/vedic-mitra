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

/** Odd signs begin at themselves, even signs at the ninth from them. */
private val DASAMSA_STARTS = listOf(0, 9, 2, 11, 4, 1, 6, 3, 8, 5, 10, 7)

/** Odd signs begin at Simha, even signs at Karka. */
private val CHATURVIMSHAMSA_STARTS = listOf(4, 3, 4, 3, 4, 3, 4, 3, 4, 3, 4, 3)

/** Odd signs begin at Mesha, even signs at Tula. */
private val KHAVEDAMSA_STARTS = listOf(0, 6, 0, 6, 0, 6, 0, 6, 0, 6, 0, 6)

/** Movable signs begin at Mesha, fixed at Simha, dual at Dhanu. */
private val AKSHAVEDAMSA_STARTS = listOf(0, 4, 8, 0, 4, 8, 0, 4, 8, 0, 4, 8)

/**
 * A varga — a divisional chart, made by cutting each rashi into [divisions] equal parts and reading
 * which sign each part belongs to.
 *
 * **Seventeen charts, one expression.** Every varga here obeys
 *
 * ```
 * sign = start(rashi) + step × divisionIndex   (mod 12)
 * ```
 *
 * and they differ only in where each rashi's first division starts and how far each step moves. That
 * was not assumed: the rule was fitted to an independent implementation's output and then checked
 * against **every** observation — 520 placements per chart, 8,320 in all, no disagreements. Some
 * vargas need no table at all, because their start is just the continuous count from 0° Mesha; the
 * rest need a twelve-entry table, and those tables were read off the same data rather than recalled.
 *
 * ## Precision, which is the real limit here
 *
 * These are exact given a longitude. The longitudes are good to about an arcminute, so what matters
 * is how wide a division is. Measured over the same sample, the share of placements sitting within an
 * arcminute of a division edge — where the sign reported is effectively a coin toss:
 *
 * | Varga | Division | At risk | | Varga | Division | At risk |
 * |---|---|---|---|---|---|---|
 * | D-3 | 10°00' | 0.0% | | D-16 | 1°52' | 2.5% |
 * | D-6 | 5°00' | 0.4% | | D-27 | 1°07' | 1.9% |
 * | D-9 | 3°20' | 0.4% | | D-24 | 1°15' | 2.1% |
 * | D-4 | 7°30' | 1.0% | | D-20 | 1°30' | 3.1% |
 * | D-12 | 2°30' | 1.0% | | D-40 | 0°45' | 4.4% |
 * | D-7 | 4°17' | 1.0% | | D-45 | 0°40' | 5.0% |
 * | D-11 | 2°44' | 1.0% | | **D-60** | **0°30'** | **8.3%** |
 * | D-10 | 3°00' | 1.2% | | | | |
 * | D-8 | 3°45' | 1.3% | | | | |
 *
 * D-81 remains absent: its divisions are 22 arcminutes and 17% of placements fall at risk, which is
 * a sixth of the chart decided by noise. D-60 at 8.3% is the finest chart kept, and it is kept
 * because it is one of the most heavily weighted in classical practice — but a reader should know
 * that roughly one placement in twelve could fall either way, and that **below about D-24 the birth
 * time matters more than the ephemeris does**: the ascendant moves a degree in four minutes, which is
 * two whole D-60 divisions, so a birth time known only to the nearest five minutes makes the D-60
 * ascendant meaningless however exact the arithmetic.
 *
 * ## What is still missing, and why
 *
 * D-2 (hora), D-5, D-30 (trimsamsa, whose divisions are unequal), D-108 and D-144 do not fit the
 * expression at all — the fit found them taking two or more different steps within a single sign.
 * They are different rules, not variations, and approximating them here would be inventing answers.
 *
 * @property divisions how many parts each rashi is cut into.
 * @property displayName the traditional name.
 * @property step how many signs each successive division advances — one for most, but four
 *   for the drekkana and three for the chaturthamsa.
 * @property start where each rashi's first division begins.
 */
enum class Varga(
    val divisions: Int,
    val displayName: String,
    val step: Int = 1,
    internal val start: VargaStart = VargaStart.Counted,
) {
    D1(1, "Rashi"),
    D3(3, "Drekkana", step = 4, start = VargaStart.OwnSign),
    D4(4, "Chaturthamsa", step = 3, start = VargaStart.OwnSign),
    D6(6, "Shashthamsa"),
    D7(7, "Saptamsa"),
    D8(8, "Ashtamsa"),
    D9(9, "Navamsha"),
    D10(10, "Dasamsa", start = VargaStart.Table(DASAMSA_STARTS)),
    D11(11, "Rudramsa"),
    D12(12, "Dwadasamsa", start = VargaStart.OwnSign),
    D16(16, "Shodashamsa"),
    D20(20, "Vimshamsa"),
    D24(24, "Chaturvimshamsa", start = VargaStart.Table(CHATURVIMSHAMSA_STARTS)),
    D27(27, "Nakshatramsa"),
    D40(40, "Khavedamsa", start = VargaStart.Table(KHAVEDAMSA_STARTS)),
    D45(45, "Akshavedamsa", start = VargaStart.Table(AKSHAVEDAMSA_STARTS)),
    D60(60, "Shashtiamsa", start = VargaStart.OwnSign),
    ;

    /** How wide one division is, in arcminutes — the number the precision caveat turns on. */
    val divisionArcminutes: Double get() = ARCMIN_PER_RASHI / divisions

    /**
     * Whether a division is narrow enough that an ordinary birth time, rather than this engine's
     * arithmetic, is the limiting factor. The ascendant covers a degree in about four minutes.
     */
    val needsExactBirthTime: Boolean get() = divisionArcminutes < FINE_DIVISION_ARCMIN

    /** Which sign [rashi]'s first division falls in. */
    internal fun startFor(rashi: Int): Int =
        when (start) {
            VargaStart.Counted -> (rashi * divisions) % RASHI_NAMES.size
            VargaStart.OwnSign -> rashi
            is VargaStart.Table -> start.starts[rashi]
        }
}

/**
 * Where a varga begins counting within each rashi.
 *
 * Three shapes cover every varga that fits the general expression, and naming them keeps the tables
 * to the four that genuinely need one.
 */
internal sealed interface VargaStart {
    /** Continue the count straight on from 0° Mesha — `(rashi × divisions) mod 12`. */
    data object Counted : VargaStart

    /** Begin at the rashi itself, restarting every sign. */
    data object OwnSign : VargaStart

    /** An explicit first sign per rashi, Mesha first. */
    data class Table(
        val starts: List<Int>,
    ) : VargaStart
}

private const val ARCMIN_PER_RASHI = 1800.0

/** Below this, a five-minute error in the birth time moves the ascendant more than one division. */
private const val FINE_DIVISION_ARCMIN = 80.0

/**
 * The sign a sidereal longitude occupies in [varga].
 *
 * Both the rashi and the division within it come from exact integer arcseconds. The division is
 * `(arcsecondsIntoSign × divisions) / RASHI_ARCSEC`, multiplying **before** dividing on purpose: a
 * division spans `108,000 / divisions` arcseconds, which is not a whole number for D-7 or D-11,
 * and dividing by it first would reintroduce exactly the rounding that #129 removed from the
 * pada. Multiplying first keeps every step exact, and the largest product any varga can produce is
 * well inside a `Long`.
 */
internal fun vargaSign(
    varga: Varga,
    siderealDeg: Double,
): Rasi {
    val arcsec = AngularBuckets.arcseconds(siderealDeg)
    val rashi = (arcsec / AngularBuckets.RASHI_ARCSEC).toInt()
    val division = ((arcsec % AngularBuckets.RASHI_ARCSEC) * varga.divisions / AngularBuckets.RASHI_ARCSEC).toInt()
    val index = (varga.startFor(rashi) + varga.step * division).mod(RASHI_NAMES.size)
    return Rasi(index = index, name = RASHI_NAMES[index])
}

/**
 * Which division of the whole zodiac [siderealDeg] falls in, counted from 0° Mesha, 0..12×divisions-1.
 *
 * Independent of where a varga starts counting — this is the raw position, used to check the
 * bucketing itself rather than the sign it maps to.
 */
internal fun divisionIndex(
    varga: Varga,
    siderealDeg: Double,
): Int = ((AngularBuckets.arcseconds(siderealDeg) * varga.divisions) / AngularBuckets.RASHI_ARCSEC).toInt()
