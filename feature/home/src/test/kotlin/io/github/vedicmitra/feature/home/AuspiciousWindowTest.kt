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
 * The home strip's one decision, tested directly rather than through the ViewModel: what the reader
 * is shown is a choice between overlapping windows, and it is worth being able to state that choice
 * in isolation.
 */
class AuspiciousWindowTest {
    private val now = Instant.fromEpochMilliseconds(1_757_000_000_000L)

    @Test
    fun `a caution running now beats an auspicious window running alongside it`() {
        // Varjyam can run straight through Brahma Muhurta. A warning not shown is a worse failure
        // than a favourable window not shown, which is why this reverses the earlier preference.
        val window = activeOrNextMuhurta(listOf(brahma(-1.hours, 2.hours), varjyam(-10.minutes, 20.minutes)), now)

        assertThat(window?.name).isEqualTo("Varjyam")
        assertThat(window?.quality).isEqualTo(MuhurtaQuality.INAUSPICIOUS)
        assertThat(window?.isActive).isTrue()
    }

    @Test
    fun `an active window reports its end, so the countdown is time remaining`() {
        val end = now + 40.minutes

        val window = activeOrNextMuhurta(listOf(abhijit(-20.minutes, 60.minutes)), now)

        assertThat(window?.isActive).isTrue()
        assertThat(window?.boundary).isEqualTo(end)
    }

    @Test
    fun `among several running, the one ending soonest wins`() {
        // The boundary shown has to be the one the reader meets first, or the countdown describes a
        // change that is not the next change. List order would decide this otherwise.
        val windows =
            listOf(
                rahuKalam(-1.hours, 2.hours),
                varjyam(-30.minutes, 45.minutes),
                gulika(-10.minutes, 45.minutes),
            )

        val window = activeOrNextMuhurta(windows, now)

        assertThat(window?.name).isEqualTo("Varjyam")
        assertThat(window?.boundary).isEqualTo(now + 15.minutes)
    }

    @Test
    fun `with nothing running, an approaching caution is shown`() {
        // The gap this closes. Only an auspicious window could be previewed before, so a Rahu Kalam
        // about to begin was invisible until it began.
        val windows = listOf(rahuKalam(30.minutes, 2.hours), abhijit(90.minutes, 2.hours))

        val window = activeOrNextMuhurta(windows, now)

        assertThat(window?.name).isEqualTo("Rahu Kalam")
        assertThat(window?.quality).isEqualTo(MuhurtaQuality.INAUSPICIOUS)
        assertThat(window?.isActive).isFalse()
        assertThat(window?.boundary).isEqualTo(now + 30.minutes)
    }

    @Test
    fun `with nothing running, the nearest window wins whichever quality it is`() {
        val windows = listOf(rahuKalam(90.minutes, 2.hours), abhijit(30.minutes, 1.hours))

        assertThat(activeOrNextMuhurta(windows, now)?.name).isEqualTo("Abhijit Muhurta")
    }

    @Test
    fun `a window that has just ended is not running`() {
        // Half-open: at exactly its end the window is over, and its successor is what matters.
        val windows = listOf(abhijit(-1.hours, 1.hours), rahuKalam(2.hours, 1.hours))

        val window = activeOrNextMuhurta(windows, now)

        assertThat(window?.name).isEqualTo("Rahu Kalam")
        assertThat(window?.isActive).isFalse()
    }

    @Test
    fun `nothing running and nothing ahead shows no strip at all`() {
        // Late evening, after the day's last window. The card is hidden rather than rolled to
        // tomorrow, which would cost a second snapshot on the home load path.
        assertThat(activeOrNextMuhurta(listOf(abhijit(-3.hours, 1.hours)), now)).isNull()
        assertThat(activeOrNextMuhurta(emptyList(), now)).isNull()
    }

    private fun muhurta(
        kind: MuhurtaKind,
        quality: MuhurtaQuality,
        startOffset: Duration,
        length: Duration,
    ) = Muhurta(kind, kind.label, now + startOffset, now + startOffset + length, quality)

    private fun brahma(
        at: Duration,
        length: Duration,
    ) = muhurta(MuhurtaKind.BRAHMA, MuhurtaQuality.AUSPICIOUS, at, length)

    private fun abhijit(
        at: Duration,
        length: Duration,
    ) = muhurta(MuhurtaKind.ABHIJIT, MuhurtaQuality.AUSPICIOUS, at, length)

    private fun rahuKalam(
        at: Duration,
        length: Duration,
    ) = muhurta(MuhurtaKind.RAHU_KALAM, MuhurtaQuality.INAUSPICIOUS, at, length)

    private fun gulika(
        at: Duration,
        length: Duration,
    ) = muhurta(MuhurtaKind.GULIKA_KALAM, MuhurtaQuality.INAUSPICIOUS, at, length)

    private fun varjyam(
        at: Duration,
        length: Duration,
    ) = muhurta(MuhurtaKind.VARJYAM, MuhurtaQuality.INAUSPICIOUS, at, length)
}
