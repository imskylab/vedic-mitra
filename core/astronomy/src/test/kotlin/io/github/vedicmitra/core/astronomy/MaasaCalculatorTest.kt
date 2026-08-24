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
import org.junit.Test
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
