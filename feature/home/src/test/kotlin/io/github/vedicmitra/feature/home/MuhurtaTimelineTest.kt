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

import com.google.common.truth.Truth.assertThat
import io.github.vedicmitra.core.astronomy.Muhurta
import io.github.vedicmitra.core.astronomy.MuhurtaKind
import io.github.vedicmitra.core.astronomy.MuhurtaQuality
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * The layout behind the Home bar.
 *
 * The band sweep gets most of the attention: it is the only new arithmetic here, it decides whether a
 * stretch draws darker, and an off-by-one in it would be invisible on screen — a band a shade wrong
 * still looks like a band.
 */
class MuhurtaTimelineTest {
    private val now = Instant.fromEpochMilliseconds(1_700_000_000_000L)

    // --- the sweep ----------------------------------------------------------

    @Test
    fun `a single range is one band at depth one`() {
        val bands = bandsOf(listOf(0.2f to 0.6f))

        assertThat(bands).hasSize(1)
        assertThat(bands.single().start).isEqualTo(0.2f)
        assertThat(bands.single().end).isEqualTo(0.6f)
        assertThat(bands.single().depth).isEqualTo(1)
    }

    @Test
    fun `two overlapping ranges give a deeper band between them`() {
        // Gulika 07:44-09:15 with Dur Muhurta 08:38-09:27 is this shape, and the 37-minute overlap
        // is what the reader most needs to see.
        val bands = bandsOf(listOf(0.0f to 0.6f, 0.4f to 1.0f))

        assertThat(bands.map { it.depth }).containsExactly(1, 2, 1).inOrder()
        assertThat(bands[1].start).isEqualTo(0.4f)
        assertThat(bands[1].end).isEqualTo(0.6f)
    }

    @Test
    fun `ranges that only touch do not deepen`() {
        // Monday's Abhijit ends at exactly the fifteenth where Dur Muhurta begins. Counting a shared
        // edge as an overlap would invent a conflict the day does not have -- that is the assertion
        // that matters. They join into one band, which is the same picture two abutting bands of the
        // same colour would make, minus a seam.
        val bands = bandsOf(listOf(0.0f to 0.5f, 0.5f to 1.0f))

        assertThat(bands.map { it.depth }).containsExactly(1)
        assertThat(bands.single().start).isEqualTo(0.0f)
        assertThat(bands.single().end).isEqualTo(1.0f)
    }

    @Test
    fun `a wholly contained range deepens only the stretch it covers`() {
        // Saturday's first Dur Muhurta sits entirely inside Gulika Kalam.
        val bands = bandsOf(listOf(0.0f to 1.0f, 0.25f to 0.5f))

        assertThat(bands.map { it.depth }).containsExactly(1, 2, 1).inOrder()
        assertThat(bands[1].start).isEqualTo(0.25f)
        assertThat(bands[1].end).isEqualTo(0.5f)
    }

    @Test
    fun `identical ranges are one band at depth two`() {
        val bands = bandsOf(listOf(0.3f to 0.7f, 0.3f to 0.7f))

        assertThat(bands).hasSize(1)
        assertThat(bands.single().depth).isEqualTo(2)
    }

    @Test
    fun `a gap between ranges draws nothing`() {
        val bands = bandsOf(listOf(0.0f to 0.2f, 0.8f to 1.0f))

        assertThat(bands.map { it.start to it.end }).containsExactly(0.0f to 0.2f, 0.8f to 1.0f).inOrder()
    }

    @Test
    fun `nothing to lay out is no bands`() {
        assertThat(bandsOf(emptyList())).isEmpty()
    }

    // --- the span -----------------------------------------------------------

    @Test
    fun `the span covers a pre-sunrise window and a night one`() {
        // Brahma Muhurta runs before sunrise and Varjyam can fall at any hour. A sunrise-to-sunset
        // span would clip the first and could lose the second entirely.
        val timeline =
            checkNotNull(
                muhurtaTimeline(
                    listOf(
                        muhurta("Brahma Muhurta", (-2).hours, (-1).hours, MuhurtaQuality.AUSPICIOUS),
                        muhurta("Varjyam", 9.hours, 11.hours, MuhurtaQuality.INAUSPICIOUS),
                    ),
                    now,
                ),
            )

        assertThat(timeline.spanStart).isEqualTo(now - 2.hours)
        assertThat(timeline.spanEnd).isEqualTo(now + 11.hours)
    }

    @Test
    fun `the edges of the span map to zero and one`() {
        val timeline =
            checkNotNull(
                muhurtaTimeline(listOf(muhurta("Rahu Kalam", 0.hours, 4.hours, MuhurtaQuality.INAUSPICIOUS)), now),
            )

        assertThat(timeline.caution.single().start).isEqualTo(0f)
        assertThat(timeline.caution.single().end).isEqualTo(1f)
        assertThat(timeline.nowFraction).isEqualTo(0f)
    }

    @Test
    fun `an empty day has no timeline`() {
        assertThat(muhurtaTimeline(emptyList(), now)).isNull()
    }

    @Test
    fun `the two qualities go to different lanes`() {
        val timeline = checkNotNull(muhurtaTimeline(friday(), now))

        assertThat(timeline.auspicious).isNotEmpty()
        assertThat(timeline.caution).isNotEmpty()
    }

    // --- what the heading is not saying --------------------------------------

    @Test
    fun `the overlapping auspicious window is reported alongside the caution`() {
        // The case the whole bar exists for. At this minute the heading reads "Caution now -- Rahu
        // Kalam" and Abhijit Muhurta is running too, which the card could not previously say.
        val friday = friday()
        val shown = checkNotNull(activeOrNextMuhurta(friday, now))

        assertThat(shown.name).isEqualTo("Rahu Kalam")
        assertThat(alsoRunning(friday, now).map { it.name }).containsExactly("Abhijit Muhurta")
    }

    @Test
    fun `nothing else running is an empty list, not the window itself`() {
        val alone = listOf(muhurta("Rahu Kalam", (-1).hours, 1.hours, MuhurtaQuality.INAUSPICIOUS))

        assertThat(alsoRunning(alone, now)).isEmpty()
    }

    @Test
    fun `two windows sharing a name both survive the exclusion`() {
        // Saturday has two Dur Muhurtas. Dropping "the one the heading names" by matching the string
        // would remove both, so the exclusion is by identity.
        val saturday =
            listOf(
                muhurta("Dur Muhurta 1", (-30).minutes, 30.minutes, MuhurtaQuality.INAUSPICIOUS),
                muhurta("Dur Muhurta 1", (-20).minutes, 40.minutes, MuhurtaQuality.INAUSPICIOUS),
            )

        assertThat(alsoRunning(saturday, now)).hasSize(1)
    }

    // --- the tap --------------------------------------------------------------

    @Test
    fun `a tap inside a window names it`() {
        val timeline = checkNotNull(muhurtaTimeline(friday(), now))
        val rahuMidpoint = timeline.fractionOfInstant(now)

        assertThat(timeline.windowAt(laneIndex = 1, fraction = rahuMidpoint)?.name).isEqualTo("Rahu Kalam")
        assertThat(timeline.windowAt(laneIndex = 0, fraction = rahuMidpoint)?.name).isEqualTo("Abhijit Muhurta")
    }

    @Test
    fun `a tap in a gap names nothing`() {
        val timeline =
            checkNotNull(
                muhurtaTimeline(
                    listOf(
                        muhurta("Rahu Kalam", 0.hours, 1.hours, MuhurtaQuality.INAUSPICIOUS),
                        muhurta("Yamaganda", 5.hours, 6.hours, MuhurtaQuality.INAUSPICIOUS),
                    ),
                    now,
                ),
            )

        assertThat(timeline.windowAt(laneIndex = 1, fraction = 0.5f)).isNull()
    }

    @Test
    fun `the spoken description names what is running and what is next`() {
        val spoken = checkNotNull(muhurtaTimeline(friday(), now)).spoken()

        assertThat(spoken).contains("Rahu Kalam")
        assertThat(spoken).contains("Abhijit Muhurta")
        assertThat(spoken).contains("Varjyam")
    }

    // --- fixtures --------------------------------------------------------------

    private fun MuhurtaTimeline.fractionOfInstant(instant: Instant): Float =
        ((instant - spanStart).inWholeMilliseconds.toFloat() / (spanEnd - spanStart).inWholeMilliseconds)

    /** The Friday minute where Rahu Kalam and Abhijit Muhurta overlap, plus a window still ahead. */
    private fun friday(): List<Muhurta> =
        listOf(
            muhurta("Rahu Kalam", (-11).minutes, 13.minutes, MuhurtaQuality.INAUSPICIOUS),
            muhurta("Abhijit Muhurta", (-11).minutes, 37.minutes, MuhurtaQuality.AUSPICIOUS),
            muhurta("Varjyam", 3.hours, 5.hours, MuhurtaQuality.INAUSPICIOUS),
        )

    private fun muhurta(
        name: String,
        from: Duration,
        to: Duration,
        quality: MuhurtaQuality,
    ): Muhurta =
        Muhurta(
            kind = MuhurtaKind.RAHU_KALAM,
            name = name,
            start = now + from,
            end = now + to,
            quality = quality,
        )
}
