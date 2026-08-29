/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.cosmicclock.domain

import io.github.vedicmitra.core.astronomy.AstronomySnapshot
import io.github.vedicmitra.core.astronomy.PanchangaConcept
import kotlin.time.Instant

/**
 * Everything the Panchanga clock draws, as plain data.
 *
 * Positions are indices and fractions, never instants: the drawing code should never do date
 * arithmetic, and keeping the conversion here means the geometry can be tested without a clock.
 *
 * @property at the instant this describes.
 * @property rings **outermost first.** See [buildPanchangaClock] for why that order.
 * @property pada the current quarter of the active nakshatra, nested inside its segment.
 */
data class PanchangaClockModel(
    val at: Instant,
    val rings: List<ClockRing>,
    val pada: PadaMarker?,
) {
    /** The nakshatra ring, which is the one the pada nests inside. */
    val nakshatraRing: ClockRing? get() = rings.firstOrNull { it.concept == PanchangaConcept.NAKSHATRA }

    /** The ring for [concept], if the clock is showing one. */
    fun ring(concept: PanchangaConcept): ClockRing? = rings.firstOrNull { it.concept == concept }
}

/**
 * Builds the clock from a snapshot, or `null` if the snapshot has no limb windows yet.
 *
 * ## Why the rings are ordered by segment count
 *
 * Outermost is karana (60), then tithi (30), nakshatra (27), yoga (27), and vara (7) innermost —
 * *not* the order a panchanga is recited in.
 *
 * At radius `r` a ring's arc per segment is `2πr / n`, so tick spacing stays comparable across all
 * five only if `r` grows with `n`. Any other ordering crowds one ring into mush while another sits
 * half empty: recitation order would put 60 karana ticks on the smallest ring and 7 vara ticks on
 * the largest. Ordering by count also happens to place the fastest-moving limb outermost, where
 * movement is most visible.
 *
 * Returns `null` rather than a partial clock when [AstronomySnapshot.limbs] is absent — a clock
 * missing its progress is worse than a spinner, because it looks finished.
 */
fun buildPanchangaClock(
    snapshot: AstronomySnapshot,
    at: Instant = snapshot.instant,
): PanchangaClockModel? {
    val limbs = snapshot.limbs ?: return null
    val rings =
        listOf(
            ClockRing(
                concept = PanchangaConcept.KARANA,
                label = "Karana",
                segmentCount = KARANAS_PER_MONTH,
                activeIndex = snapshot.karana.number - 1,
                activeName = snapshot.karana.name,
                window = limbs.karana,
            ),
            ClockRing(
                concept = PanchangaConcept.TITHI,
                label = "Tithi",
                segmentCount = TITHIS_PER_MONTH,
                activeIndex = snapshot.tithi.number - 1,
                activeName = snapshot.tithi.name,
                window = limbs.tithi,
            ),
            ClockRing(
                concept = PanchangaConcept.NAKSHATRA,
                label = "Nakshatra",
                segmentCount = NAKSHATRA_COUNT,
                activeIndex = snapshot.nakshatra.number - 1,
                activeName = snapshot.nakshatra.name,
                window = limbs.nakshatra,
            ),
            ClockRing(
                concept = PanchangaConcept.YOGA,
                label = "Yoga",
                segmentCount = YOGA_COUNT,
                activeIndex = snapshot.yoga.number - 1,
                activeName = snapshot.yoga.name,
                window = limbs.yoga,
            ),
            ClockRing(
                concept = PanchangaConcept.VARA,
                label = "Vara",
                // Vara alone keeps its ring when the window is unknown: the weekday is still known,
                // only its sunrise boundary is not. Omitting the ring would reshuffle every radius.
                segmentCount = DAYS_PER_WEEK,
                activeIndex = snapshot.vara.ordinal,
                activeName = snapshot.vara.displayName,
                window = limbs.vara,
            ),
        )
    return PanchangaClockModel(
        at = at,
        rings = rings,
        pada = snapshot.moonPada?.let { PadaMarker(index = it - 1, window = limbs.moonPada) },
    )
}

/** A lunar month holds sixty karanas — two per tithi. Not the eleven names, which repeat. */
private const val KARANAS_PER_MONTH = 60
private const val TITHIS_PER_MONTH = 30
private const val NAKSHATRA_COUNT = 27
private const val YOGA_COUNT = 27
private const val DAYS_PER_WEEK = 7
