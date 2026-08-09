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
import kotlin.time.Instant

private const val DAY_MILLIS = 86_400_000L

class FestivalCalculatorTest {
    @Test
    fun `detects recurring observances by tithi`() {
        // Tithi 9..15 over seven days: Ekadashi (11) on day 2, Pradosh (13) on day 4, Purnima (15) day 6.
        val source = source(tithis = listOf(9, 10, 11, 12, 13, 14, 15))

        val festivals = upcomingFestivals(fromEpochMillis = 0L, windowDays = 7, limit = 10, source = source)

        assertThat(festivals.map { it.name }).containsExactly("Ekadashi", "Pradosh", "Purnima").inOrder()
        assertThat(festivals.first { it.name == "Ekadashi" }.atSunrise).isEqualTo(dayInstant(2))
        assertThat(festivals.first { it.name == "Ekadashi" }.type).isEqualTo(FestivalType.OBSERVANCE)
    }

    @Test
    fun `recognises the added monthly observances`() {
        // Krishna Chaturthi (19) = Sankashti; Krishna Chaturdashi (29) = Masik Shivaratri.
        val source = source(tithis = listOf(4, 19, 29))

        val festivals = upcomingFestivals(fromEpochMillis = 0L, windowDays = 3, limit = 10, source = source)

        assertThat(festivals.map { it.name })
            .containsExactly("Vinayaka Chaturthi", "Sankashti Chaturthi", "Masik Shivaratri")
            .inOrder()
    }

    @Test
    fun `a named festival overrides the generic observance on the same tithi`() {
        // Tithi 15 in Ashadha is Guru Purnima, not a plain "Purnima".
        val source = source(tithis = listOf(14, 15), maasas = listOf(maasa("Ashadha"), maasa("Ashadha")))

        val festivals = upcomingFestivals(fromEpochMillis = 0L, windowDays = 2, limit = 10, source = source)

        assertThat(festivals.map { it.name }).containsExactly("Guru Purnima")
        assertThat(festivals.single().type).isEqualTo(FestivalType.FESTIVAL)
    }

    @Test
    fun `a festival tithi in the wrong month is not matched`() {
        // Tithi 3 is Akshaya Tritiya only in Vaishakha; in Chaitra it is neither festival nor observance.
        val source = source(tithis = listOf(3), maasas = listOf(maasa("Chaitra")))

        val festivals = upcomingFestivals(fromEpochMillis = 0L, windowDays = 1, limit = 10, source = source)

        assertThat(festivals).isEmpty()
    }

    @Test
    fun `an adhika month suppresses its festivals`() {
        // Ugadi is Chaitra Shukla Pratipada, but not in an Adhika Chaitra.
        val source = source(tithis = listOf(1), maasas = listOf(maasa("Chaitra", adhika = true)))

        val festivals = upcomingFestivals(fromEpochMillis = 0L, windowDays = 1, limit = 10, source = source)

        assertThat(festivals).isEmpty()
    }

    @Test
    fun `emits a Sankranti when the Sun enters a new rashi`() {
        // Rashi 8 (Dhanu) then 9 (Makara) on day 1 -> Makara Sankranti.
        val source = source(tithis = listOf(5, 6), rashis = listOf(8, 9))

        val festivals = upcomingFestivals(fromEpochMillis = 0L, windowDays = 2, limit = 10, source = source)

        assertThat(festivals.map { it.name }).contains("Makara Sankranti")
        val sankranti = festivals.first { it.name == "Makara Sankranti" }
        assertThat(sankranti.type).isEqualTo(FestivalType.SANKRANTI)
        assertThat(sankranti.atSunrise).isEqualTo(dayInstant(1))
    }

    @Test
    fun `each name appears once and the limit is respected`() {
        // Two Ekadashis (tithi 11 on day 0 and day 14) -> only the first; limit caps the list.
        val tithis = (0 until 20).map { if (it == 0 || it == 14) 11 else 5 }
        val source = source(tithis = tithis)

        val festivals = upcomingFestivals(fromEpochMillis = 0L, windowDays = 20, limit = 1, source = source)

        assertThat(festivals).hasSize(1)
        assertThat(festivals.single().name).isEqualTo("Ekadashi")
        assertThat(festivals.single().atSunrise).isEqualTo(dayInstant(0))
    }

    private fun source(
        tithis: List<Int>,
        rashis: List<Int> = List(tithis.size) { 0 },
        // Pausha hosts none of the festivals, so festival-tithi days fall through to observances.
        maasas: List<Maasa> = List(tithis.size) { maasa("Pausha") },
    ): FestivalPanchangaSource =
        object : FestivalPanchangaSource {
            override fun sunrise(dayEpochMillis: Long): Long = dayEpochMillis

            override fun tithiNumber(epochMillis: Long): Int = tithis[index(epochMillis)]

            override fun sunRashi(epochMillis: Long): Int = rashis[index(epochMillis)]

            override fun maasa(epochMillis: Long): Maasa = maasas[index(epochMillis)]

            private fun index(epochMillis: Long): Int = (epochMillis / DAY_MILLIS).toInt()
        }

    private fun maasa(
        name: String,
        adhika: Boolean = false,
    ) = Maasa(number = 1, name = name, adhika = adhika)

    private fun dayInstant(day: Int) = Instant.fromEpochMilliseconds(day * DAY_MILLIS)
}
