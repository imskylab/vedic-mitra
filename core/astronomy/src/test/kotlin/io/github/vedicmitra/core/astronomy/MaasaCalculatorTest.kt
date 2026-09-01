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

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import kotlin.time.Instant
import kotlin.math.min

/**
 * Verifies the amanta-month and samvatsara logic against the real [Ephemeris]. Reference values
 * are cross-checked against published almanacs, including the Ugadi 2026 (Parabhava) almanac;
 * 2026 is a thirteen-month year (Adhika Jyeshtha), which the cases below deliberately exercise.
 */
class MaasaCalculatorTest {
    private fun elongationAt(epochMillis: Long): Double {
        val t = Ephemeris.julianCenturies(epochMillis)
        return Ephemeris.norm360(Ephemeris.moonLongitude(t) - Ephemeris.sunApparentLongitude(t))
    }

    private fun sunSiderealAt(epochMillis: Long): Double {
        val t = Ephemeris.julianCenturies(epochMillis)
        return Ephemeris.norm360(Ephemeris.sunApparentLongitude(t) - Ephemeris.lahiriAyanamsa(t))
    }

    private fun maasa(epochMillis: Long) = maasaOf(epochMillis, ::elongationAt, ::sunSiderealAt)

    private fun samvatsara(epochMillis: Long) = samvatsaraOf(epochMillis, ::elongationAt, ::sunSiderealAt)

    private fun cycle(epochMillis: Long) = maasaCycleOf(epochMillis, ::elongationAt, ::sunSiderealAt)

    @Test
    fun `names the months either side of the current one`() {
        val around = cycle(EPOCH_2026_08_05)

        assertThat(around.current.displayName).isEqualTo("Ashadha")
        // 2026 inserts an Adhika Jyeshtha, so the month before Ashadha is the *nija* Jyeshtha.
        assertThat(around.previous.displayName).isEqualTo("Jyeshtha")
        assertThat(around.next.displayName).isEqualTo("Shravana")
    }

    @Test
    fun `the month after a leap month is its nija counterpart, not the next name along`() {
        // This is the whole reason the neighbours come from real lunations. Stepping the number
        // would say Adhika Jyeshtha is followed by Ashadha; it is followed by the *nija* Jyeshtha,
        // and a wheel built on arithmetic would be a month wrong for the length of the leap month.
        val around = cycle(EPOCH_2026_05_25)

        assertThat(around.current.displayName).isEqualTo("Adhika Jyeshtha")
        assertThat(around.next.name).isEqualTo("Jyeshtha")
        assertWithMessage("the month following a leap month is not itself a leap month")
            .that(around.next.adhika)
            .isFalse()
        assertThat(around.previous.displayName).isEqualTo("Vaishakha")
    }

    @Test
    fun `the month window brackets the instant it was computed for`() {
        val around = cycle(EPOCH_2026_08_05)
        val at = Instant.fromEpochMilliseconds(EPOCH_2026_08_05)

        assertThat(around.window.start).isLessThan(at)
        assertThat(around.window.end).isGreaterThan(at)
        // A lunation is 29 to 30 days; anything outside that means the syzygy search drifted.
        assertThat(around.window.duration.inWholeHours).isAtLeast(29L * 24)
        assertThat(around.window.duration.inWholeHours).isAtMost(30L * 24)
    }

    @Test
    fun `the lunar year runs Chaitra to Chaitra`() {
        val start = chaitraStartAtOrBefore(EPOCH_2026_08_05, ::elongationAt, ::sunSiderealAt)
        val end = chaitraStartAfter(start, ::elongationAt, ::sunSiderealAt)

        assertThat(start).isLessThan(EPOCH_2026_08_05)
        assertThat(end).isGreaterThan(EPOCH_2026_08_05)
        // 2026 inserts an Adhika Jyeshtha, so this year holds thirteen lunations, not twelve --
        // which is exactly why the boundary is walked rather than estimated from a fixed offset.
        val days = (end - start) / 86_400_000L
        assertWithMessage("a thirteen-month lunar year is longer than twelve lunations")
            .that(days)
            .isAtLeast(380L)
        assertThat(days).isAtMost(390L)
    }

    @Test
    fun `names the amanta month by the sun's rashi at the starting new moon`() {
        // 2026-08-05 12:00 IST — amanta Ashadha (month 4). Because 2026 inserts an Adhika Jyeshtha,
        // early August is still Ashadha, not Shravana.
        val month = maasa(EPOCH_2026_08_05)
        assertThat(month.name).isEqualTo("Ashadha")
        assertThat(month.number).isEqualTo(4)
        assertThat(month.adhika).isFalse()
    }

    @Test
    fun `flags an adhika month when a lunation contains no sankranti`() {
        // 2026-05-25 falls in Adhika Jyeshtha — the intercalary month whose bounding new moons both
        // fall in Vrishabha (no solar ingress between them).
        val month = maasa(EPOCH_2026_05_25)
        assertThat(month.name).isEqualTo("Jyeshtha")
        assertThat(month.adhika).isTrue()
        assertThat(month.displayName).isEqualTo("Adhika Jyeshtha")
    }

    @Test
    fun `samvatsara advances at ugadi, not at the gregorian new year`() {
        // Before Ugadi 2026 (19 March): still Vishvavasu, Shaka 1947.
        val before = samvatsara(EPOCH_2026_01_15)
        assertThat(before.name).isEqualTo("Vishvavasu")
        assertThat(before.shakaYear).isEqualTo(1947)

        // After Ugadi 2026: Parabhava, Shaka 1948.
        val after = samvatsara(EPOCH_2026_03_25)
        assertThat(after.name).isEqualTo("Parabhava")
        assertThat(after.number).isEqualTo(40)
        assertThat(after.shakaYear).isEqualTo(1948)
    }

    @Test
    fun `newMoonAtOrBefore lands on a conjunction preceding the instant`() {
        val newMoon = newMoonAtOrBefore(EPOCH_2026_08_05, ::elongationAt)

        // It precedes the query instant...
        assertThat(newMoon).isLessThan(EPOCH_2026_08_05)
        // ...and satisfies the defining property of a new moon: the Moon is in conjunction with the
        // Sun (elongation within a tenth of a degree of 0/360).
        val elongation = elongationAt(newMoon)
        assertThat(min(elongation, 360.0 - elongation)).isLessThan(0.1)
    }

    private companion object {
        const val EPOCH_2026_08_05 = 1_785_911_400_000L // 2026-08-05 12:00 IST
        const val EPOCH_2026_05_25 = 1_779_690_600_000L // 2026-05-25 12:00 IST
        const val EPOCH_2026_01_15 = 1_768_458_600_000L // 2026-01-15 12:00 IST
        const val EPOCH_2026_03_25 = 1_774_420_200_000L // 2026-03-25 12:00 IST
    }
}
