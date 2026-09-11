/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.home

import io.github.vedicmitra.core.astronomy.Muhurta
import io.github.vedicmitra.core.astronomy.MuhurtaQuality
import io.github.vedicmitra.core.designsystem.component.TimelineBand
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/**
 * The day's muhurtas arranged for the two-lane bar on Home.
 *
 * @property auspicious bands for the favourable lane, left to right.
 * @property caution bands for the lane below it.
 * @property nowFraction where the present moment falls, 0..1.
 * @property spanStart the instant the bar begins at.
 * @property spanEnd the instant it ends at.
 * @property windows every muhurta that went into it, earliest first — the bar is drawn from the
 *   bands, but a tap has to name a window, and only this keeps the names.
 */
internal data class MuhurtaTimeline(
    val auspicious: List<TimelineBand>,
    val caution: List<TimelineBand>,
    val nowFraction: Float,
    val spanStart: Instant,
    val spanEnd: Instant,
    val windows: List<Muhurta>,
)

/**
 * Lays [muhurtas] out as a timeline around [now], or `null` when there is nothing to draw.
 *
 * **The span is the windows' own extent, not sunrise to sunset.** Brahma Muhurta ends 48 minutes
 * before sunrise and Varjyam is placed from a nakshatra ingress, so it can fall at any hour including
 * the night. A sunrise-to-sunset bar would clip the first and sometimes lose the second entirely.
 */
internal fun muhurtaTimeline(
    muhurtas: List<Muhurta>,
    now: Instant,
): MuhurtaTimeline? {
    if (muhurtas.isEmpty()) return null
    val spanStart = muhurtas.minOf { it.start }
    val spanEnd = muhurtas.maxOf { it.end }
    val total = (spanEnd - spanStart).inWholeMilliseconds
    // A zero-width span would divide by zero, and nothing sensible can be drawn from it anyway.
    if (total <= 0L) return null

    fun fractionOf(instant: Instant): Float =
        ((instant - spanStart).inWholeMilliseconds.toDouble() / total).toFloat().coerceIn(0f, 1f)

    fun lane(quality: MuhurtaQuality): List<TimelineBand> =
        bandsOf(muhurtas.filter { it.quality == quality }.map { fractionOf(it.start) to fractionOf(it.end) })

    return MuhurtaTimeline(
        auspicious = lane(MuhurtaQuality.AUSPICIOUS),
        caution = lane(MuhurtaQuality.INAUSPICIOUS),
        nowFraction = fractionOf(now),
        spanStart = spanStart,
        spanEnd = spanEnd,
        windows = muhurtas.sortedBy { it.start },
    )
}

/**
 * The window under [fraction] in lane [laneIndex] (0 = auspicious, 1 = caution), or null for a gap.
 *
 * The tap lands on a position, not on a band, so this asks which window covers that instant. A gap
 * returns null and the card falls back to its own summary line rather than showing a stale name.
 */
internal fun MuhurtaTimeline.windowAt(
    laneIndex: Int,
    fraction: Float,
): Muhurta? {
    val quality = if (laneIndex == 0) MuhurtaQuality.AUSPICIOUS else MuhurtaQuality.INAUSPICIOUS
    val total = (spanEnd - spanStart).inWholeMilliseconds
    val at = spanStart + (total * fraction.toDouble()).toLong().milliseconds
    return windows
        .filter { it.quality == quality && at >= it.start && at < it.end }
        // The shortest wins where two overlap: it is the more specific claim about that instant, and
        // the harder one to hit by tapping.
        .minByOrNull { (it.end - it.start).inWholeMilliseconds }
}

/**
 * The bar in one sentence, for a screen reader.
 *
 * A tap position cannot be aimed without sight, so everything the tap would reveal has to be here
 * instead. Without it the bar is a feature only sighted readers get.
 */
internal fun MuhurtaTimeline.spoken(): String {
    val now = nowInstant()
    val running = windows.filter { now >= it.start && now < it.end }
    val parts = mutableListOf<String>()
    parts +=
        if (running.isEmpty()) {
            "Nothing running now"
        } else {
            running.joinToString(", ") { "${it.name} until ${clock(it.end)}" } + " running now"
        }
    windows.filter { it.start > now }.minByOrNull { it.start }?.let {
        parts += "next, ${it.name} at ${clock(it.start)}"
    }
    return parts.joinToString(". ") + "."
}

private fun MuhurtaTimeline.nowInstant(): Instant =
    spanStart + ((spanEnd - spanStart).inWholeMilliseconds * nowFraction.toDouble()).toLong().milliseconds

/**
 * Flattens overlapping [ranges] into non-overlapping bands, each carrying how many ranges cover it.
 *
 * A sweep rather than a merge, because the depth is the point: two cautions covering the same stretch
 * draw darker than one, so the bar distinguishes "Gulika Kalam" from "Gulika Kalam *and* Dur Muhurta",
 * which on a Friday is a real 37-minute difference.
 *
 * Boundaries are half-open, matching `activeOrNextMuhurta`. Two windows that merely touch — Monday's
 * Abhijit ends at exactly the fifteenth where Dur Muhurta begins — stay at **depth 1**. Counting a
 * shared edge as an overlap would invent a conflict the day does not have.
 *
 * They do come out as a *single* band rather than two, because two abutting bands of the same colour
 * and depth are the same picture, and drawing them separately would put a seam where the day has no
 * boundary a reader can act on. What the two windows are called is the tap's job, and the card's.
 */
internal fun bandsOf(ranges: List<Pair<Float, Float>>): List<TimelineBand> {
    val edges = ranges.flatMap { listOf(it.first, it.second) }.distinct().sorted()
    if (edges.size < 2) return emptyList()
    val bands = mutableListOf<TimelineBand>()
    for (i in 0 until edges.size - 1) {
        val from = edges[i]
        val to = edges[i + 1]
        // Depth is sampled strictly inside the slice, so a range that only touches it does not count.
        val depth = ranges.count { it.first <= from && it.second >= to && it.first < it.second }
        if (depth == 0) continue
        val previous = bands.lastOrNull()
        // Slices split only by an unrelated window's edge are one band again, so a single muhurta
        // does not draw as two abutting rectangles with a seam between them.
        if (previous != null && previous.end == from && previous.depth == depth) {
            bands[bands.lastIndex] = previous.copy(end = to)
        } else {
            bands += TimelineBand(start = from, end = to, depth = depth)
        }
    }
    return bands
}

/**
 * The muhurtas running at [now] that the heading is not already naming.
 *
 * This is the whole reason the bar exists. `activeOrNextMuhurta` picks one window to lead with and
 * discards the rest, so on any Friday between 11:54 and 12:18 the card reads "Caution now — Rahu
 * Kalam" while Abhijit Muhurta, the day's most favourable window, runs unmentioned.
 *
 * Excluded by **identity**, not by name: [leadingActive] is asked which window the heading took and
 * that exact one is dropped. Two windows can share a name — Saturday has two Dur Muhurtas — and
 * filtering on the display string would silently hide the wrong one.
 */
internal fun alsoRunning(
    muhurtas: List<Muhurta>,
    now: Instant,
): List<Muhurta> {
    val active = activeAt(muhurtas, now)
    val leader = leadingActive(active)
    return active.filterNot { it === leader }.sortedBy { it.end }
}

private val spokenClock: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** A wall-clock time for the spoken description. The screen's own formatter is private to it. */
private fun clock(instant: Instant): String =
    java.time.Instant
        .ofEpochMilli(instant.toEpochMilliseconds())
        .atZone(ZoneId.systemDefault())
        .format(spokenClock)
