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

/**
 * The arithmetic here is three additions, and that is exactly why the interesting cases are the
 * year boundary rather than the offsets. A suite that only checked mid-year dates would pass with
 * the Chaitradi convention wrong, or with the year turning on 1 January — so every case below
 * either straddles Ugadi or pins a published almanac value.
 *
 * Reference values are the Ugadi 2026 (Parabhava) almanac: Shaka 1948, Vikrama 2083, Kali 5128.
 */
class EraYearsTest {
    private fun elongationAt(epochMillis: Long): Double {
        val t = Ephemeris.julianCenturies(epochMillis)
        return Ephemeris.norm360(Ephemeris.moonLongitude(t) - Ephemeris.sunApparentLongitude(t))
    }

    private fun sunSiderealAt(epochMillis: Long): Double {
        val t = Ephemeris.julianCenturies(epochMillis)
        return Ephemeris.norm360(Ephemeris.sunApparentLongitude(t) - Ephemeris.lahiriAyanamsa(t))
    }

    private fun erasAt(epochMillis: Long) = samvatsaraOf(epochMillis, ::elongationAt, ::sunSiderealAt).eras

    @Test
    fun `matches the published era years for the lunar year opened by ugadi 2026`() {
        val eras = eraYearsOf(shakaYear = 1948)

        assertThat(eras.shaka).isEqualTo(1948)
        assertThat(eras.vikrama).isEqualTo(2083)
        assertThat(eras.kali).isEqualTo(5128)
    }

    @Test
    fun `every era turns at ugadi, not at the gregorian new year`() {
        // 15 January 2026 is Gregorian 2026 but still the lunar year that opened at Ugadi 2025 --
        // this is the case that fails if the boundary is taken from the calendar year.
        val before = erasAt(EPOCH_2026_01_15)
        assertThat(before.shaka).isEqualTo(1947)
        assertThat(before.vikrama).isEqualTo(2082)
        assertThat(before.kali).isEqualTo(5127)

        // 25 March 2026 is after Ugadi (19 March), so every era has advanced by one.
        val after = erasAt(EPOCH_2026_03_25)
        assertThat(after.shaka).isEqualTo(1948)
        assertThat(after.vikrama).isEqualTo(2083)
        assertThat(after.kali).isEqualTo(5128)
    }

    @Test
    fun `the same gregorian year holds two different era years, on either side of ugadi`() {
        // Both instants are in Gregorian 2026. Stating it this way pins the property the previous
        // test demonstrates: the Gregorian year alone cannot determine an era year.
        val before = erasAt(EPOCH_2026_01_15)
        val after = erasAt(EPOCH_2026_03_25)

        assertWithMessage("two dates in Gregorian 2026 must not share a Vikrama year")
            .that(before.vikrama)
            .isNotEqualTo(after.vikrama)
        assertThat(after.vikrama - before.vikrama).isEqualTo(1)
        assertThat(after.shaka - before.shaka).isEqualTo(1)
        assertThat(after.kali - before.kali).isEqualTo(1)
    }

    @Test
    fun `the eras stay anchored to the samvatsara they were derived from`() {
        // Samvatsara.eras exists so the two cannot drift apart; assert that rather than trust it.
        val samvatsara = samvatsaraOf(EPOCH_2026_03_25, ::elongationAt, ::sunSiderealAt)

        assertThat(samvatsara.eras.shaka).isEqualTo(samvatsara.shakaYear)
    }

    @Test
    fun `the gaps between eras are epoch differences, not year-dependent adjustments`() {
        // If someone ever makes an offset depend on the year, this fails rather than quietly
        // shifting one era against another.
        listOf(1, 1000, 1947, 1948, 2500).forEach { shaka ->
            val eras = eraYearsOf(shaka)
            assertWithMessage("Vikrama minus Shaka at Shaka $shaka")
                .that(eras.vikrama - eras.shaka)
                .isEqualTo(135)
            assertWithMessage("Kali minus Shaka at Shaka $shaka")
                .that(eras.kali - eras.shaka)
                .isEqualTo(3180)
        }
    }

    private companion object {
        const val EPOCH_2026_01_15 = 1_768_458_600_000L // 2026-01-15 12:00 IST, before Ugadi
        const val EPOCH_2026_03_25 = 1_774_420_200_000L // 2026-03-25 12:00 IST, after Ugadi
    }
}
