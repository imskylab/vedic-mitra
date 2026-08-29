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

import io.github.vedicmitra.core.astronomy.PanchangaConcept
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * What the middle of the clock says, in words.
 *
 * The rings show *where* in each cycle we are; this says *what* that means. A reader who takes in
 * nothing else and taps nothing should still come away with one correct, specific sentence — that is
 * the whole test of whether this screen clarifies anything.
 *
 * Structured rather than pre-formatted so the times can be rendered in the reader's locale and the
 * same values can feed the spoken description for TalkBack.
 *
 * @property tithi the lunar day with its fortnight, e.g. "Shukla Chaturdashi".
 * @property tithiEndsAt when it gives way to the next, or `null` if not known.
 * @property tithiRemaining how long that is from now, or `null`.
 * @property nakshatra the Moon's nakshatra.
 * @property pada its quarter, 1-based, or `null` when the chart has none.
 */
data class ClockSummary(
    val tithi: String,
    val tithiEndsAt: Instant?,
    val tithiRemaining: Duration?,
    val nakshatra: String,
    val pada: Int?,
)

/** The hub's reading of [this] at [at]. */
fun PanchangaClockModel.summaryAt(at: Instant): ClockSummary? {
    val tithiRing = ring(PanchangaConcept.TITHI) ?: return null
    val nakshatraRing = ring(PanchangaConcept.NAKSHATRA) ?: return null
    return ClockSummary(
        tithi = "${pakshaOf(tithiRing.activeIndex)} ${tithiRing.activeName}",
        tithiEndsAt = tithiRing.endsAt,
        tithiRemaining = tithiRing.window?.remainingFrom(at),
        nakshatra = nakshatraRing.activeName,
        pada = pada?.let { it.index + 1 },
    )
}

/**
 * Which fortnight a tithi index falls in.
 *
 * Derived from the index rather than carried through the model, because it is exactly the same fact:
 * the engine numbers tithis 1..30 with 1..15 in the waxing fortnight and 16..30 in the waning one,
 * so the 0-based index splits at the same place. Naming the paksha matters because each tithi name
 * occurs twice a month — "Chaturdashi" alone is ambiguous, and a reader checking against an almanac
 * would have no way to tell which one this is.
 */
internal fun pakshaOf(tithiIndex: Int): String = if (tithiIndex < TITHIS_PER_PAKSHA) "Shukla" else "Krishna"

private const val TITHIS_PER_PAKSHA = 15
