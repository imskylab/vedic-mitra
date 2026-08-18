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

import kotlin.time.Instant

/**
 * The overall verdict for a rashi's day, from its Moon-transit strength. Purely a function of the
 * classical Chandrabala (the day's Moon sign counted from the read sign) and — when the reading is
 * personalised to the person's own birth sign — their Tarabala.
 *
 * @property label a short human-readable verdict.
 */
enum class OutlookBand(
    val label: String,
) {
    AUSPICIOUS("Auspicious"),
    MIXED("Mixed"),
    CHALLENGING("Challenging"),
}

/**
 * One day of a rashi's outlook, anchored to that day's sunrise.
 *
 * @property atSunrise the sunrise instant of the day.
 * @property moonRasi the Moon's sign during the day (Chandra rashi).
 * @property nakshatra the Moon's nakshatra during the day.
 * @property vara the weekday.
 * @property chandraPosition the day's Moon sign counted from the read rashi (1..12).
 * @property chandrabala the strength of that Chandrabala position.
 * @property tara the day's Tarabala from the person's birth star, or `null` for a non-personalised read.
 * @property band the overall verdict for the day.
 */
data class RashiDay(
    val atSunrise: Instant,
    val moonRasi: Rasi,
    val nakshatra: Nakshatra,
    val vara: Vara,
    val chandraPosition: Int,
    val chandrabala: Bala,
    val tara: Tara?,
    val band: OutlookBand,
)

/**
 * A rashi's transit-based outlook for a run of days.
 *
 * @property rasi the sign the outlook is for.
 * @property personalized whether Tarabala was layered in (the read sign is the person's birth sign).
 * @property today the first (current) day.
 * @property week every computed day, today first.
 */
data class RashiOutlook(
    val rasi: Rasi,
    val personalized: Boolean,
    val today: RashiDay,
    val week: List<RashiDay>,
)

/**
 * The overall [OutlookBand] for a day, combining its [chandrabala] with the person's [tara] strength
 * (when personalised; `null` otherwise). Each grade scores +1/0/-1; a positive sum is auspicious, a
 * negative sum challenging, and a balanced sum mixed.
 */
internal fun outlookBand(
    chandrabala: Bala,
    tara: Bala?,
): OutlookBand {
    fun score(bala: Bala): Int =
        when (bala) {
            Bala.STRONG -> 1
            Bala.NEUTRAL -> 0
            Bala.WEAK -> -1
        }
    val total = score(chandrabala) + (tara?.let { score(it) } ?: 0)
    return when {
        total >= 1 -> OutlookBand.AUSPICIOUS
        total <= -1 -> OutlookBand.CHALLENGING
        else -> OutlookBand.MIXED
    }
}
