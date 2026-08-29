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

package io.github.vedicmitra.feature.cosmicclock.domain

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import io.github.vedicmitra.core.astronomy.LimbWindow
import io.github.vedicmitra.core.astronomy.PanchangaConcept
import org.junit.Test
import kotlin.time.Instant

/**
 * A screen reader is the only way some people will ever encounter this face, and unlike the drawing
 * it can be asserted exactly — so it is, rather than left to a manual TalkBack sweep that nobody
 * repeats.
 */
class SpokenSummaryTest {
    @Test
    fun `the whole face is one sentence per limb, in drawing order`() {
        // Outside in, matching the rings, so someone who listens first and looks later can map what
        // they heard onto what they see.
        val spoken = model().spokenSummary(AT, ::fakeTime)

        assertThat(spoken).startsWith("Panchanga clock.")
        assertThat(spoken).contains("Karana Bava")
        assertThat(spoken).contains("Tithi Chaturdashi")
        assertThat(spoken).contains("Nakshatra Rohini")
        assertThat(spoken).contains("Yoga Dhriti")
        assertThat(spoken).contains("Vara Shukravara")
        assertWithMessage("karana is drawn outermost, so it is spoken first")
            .that(spoken.indexOf("Karana"))
            .isLessThan(spoken.indexOf("Vara"))
    }

    @Test
    fun `pada is spoken with the nakshatra, the only place it appears`() {
        // The face cannot draw pada -- a quarter of a nakshatra arc is about five pixels -- so for a
        // listener this and the limb list are the only ways to reach it at all.
        val spoken = model().spokenSummary(AT, ::fakeTime)
        assertThat(spoken).contains("Nakshatra Rohini, pada 3")
        assertWithMessage("pada belongs to the nakshatra, not to every limb")
            .that(spoken.split("pada").size - 1)
            .isEqualTo(1)
    }

    @Test
    fun `each limb says when it changes`() {
        val spoken = model().spokenSummary(AT, ::fakeTime)
        assertThat(spoken).contains("ends 06:00")
    }

    @Test
    fun `a limb with no known boundary is still named`() {
        // Vara at a latitude where the Sun does not rise. Saying nothing about it would be worse
        // than saying the weekday without a time.
        val spoken = model(varaWindow = null).spokenSummary(AT, ::fakeTime)
        assertThat(spoken).contains("Vara Shukravara")
        assertWithMessage("no dangling 'ends' for the limb without a window")
            .that(spoken)
            .doesNotContain("Shukravara, ends")
    }

    @Test
    fun `the sentence is closed`() {
        // It is read aloud; a trailing fragment sounds like the app was interrupted.
        assertThat(model().spokenSummary(AT, ::fakeTime)).endsWith(".")
    }

    @Test
    fun `time formatting is the caller's business`() {
        // The domain has no zone and no locale. Passing the formatter in is what keeps it that way,
        // and this asserts the seam holds rather than a zone leaking in through a default.
        val shouted = model().spokenSummary(AT) { "SIX O'CLOCK" }
        assertThat(shouted).contains("ends SIX O'CLOCK")
    }

    private fun model(varaWindow: LimbWindow? = window()) =
        PanchangaClockModel(
            at = AT,
            rings =
                listOf(
                    ring(PanchangaConcept.KARANA, "Karana", 60, 26, "Bava", window()),
                    ring(PanchangaConcept.TITHI, "Tithi", 30, 13, "Chaturdashi", window()),
                    ring(PanchangaConcept.NAKSHATRA, "Nakshatra", 27, 3, "Rohini", window()),
                    ring(PanchangaConcept.YOGA, "Yoga", 27, 11, "Dhriti", window()),
                    ring(PanchangaConcept.VARA, "Vara", 7, 5, "Shukravara", varaWindow),
                ),
            pada = PadaMarker(index = 2, window = window()),
        )

    @Suppress("LongParameterList")
    private fun ring(
        concept: PanchangaConcept,
        label: String,
        segments: Int,
        active: Int,
        name: String,
        window: LimbWindow?,
    ) = ClockRing(
        concept = concept,
        label = label,
        segmentCount = segments,
        activeIndex = active,
        activeName = name,
        window = window,
    )

    private companion object {
        val AT = Instant.fromEpochMilliseconds(1_787_000_000_000L)

        fun window() =
            LimbWindow(
                start = AT,
                end = Instant.fromEpochMilliseconds(AT.toEpochMilliseconds() + 86_400_000L),
                angularFraction = 0.4,
            )

        /** A fixed rendering, so the assertions are about the sentence rather than about a clock. */
        fun fakeTime(instant: Instant): String = "06:00"
    }
}
