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
import io.github.vedicmitra.core.common.model.GeoCoordinates
import org.junit.Test
import kotlin.time.Instant

class RankedMuhurtaDayTest {
    @Test
    fun `ranks the more auspicious day ahead of a poor one`() {
        val poor =
            daySnapshot(
                dayMillis = SAMPLE_BASE_MILLIS,
                tithi = Tithi(number = 19, paksha = Paksha.KRISHNA, name = "Chaturthi"),
                nakshatra = Nakshatra(number = 2, name = "Bharani"),
                vara = Vara.MANGALAVARA,
                yoga = Yoga(number = 17, name = "Vyatipata"),
                karana = Karana(number = 7, name = "Vishti"),
            )
        val ideal =
            daySnapshot(
                dayMillis = SAMPLE_BASE_MILLIS + DAY_OFFSET_MILLIS,
                tithi = Tithi(number = 5, paksha = Paksha.SHUKLA, name = "Panchami"),
                nakshatra = Nakshatra(number = 4, name = "Rohini"),
                vara = Vara.GURUVARA,
                yoga = Yoga(number = 1, name = "Vishkambha"),
                karana = Karana(number = 2, name = "Bava"),
            )

        val ranked = rankMuhurtaDays(MuhurtaActivity.GRIHA_PRAVESH, listOf(poor, ideal))

        assertThat(ranked).hasSize(2)
        // Ideal ranks first despite falling on the later day.
        assertThat(ranked.first().atSunrise).isEqualTo(ideal.instant)
        assertThat(ranked.first().score.score).isGreaterThan(ranked.last().score.score)
    }

    @Test
    fun `an empty range yields no ranked days`() {
        assertThat(rankMuhurtaDays(MuhurtaActivity.VIVAH, emptyList())).isEmpty()
    }

    @Test
    fun `ties are broken by the earlier date`() {
        val earlier = idealDaySnapshot(SAMPLE_BASE_MILLIS)
        val later = idealDaySnapshot(SAMPLE_BASE_MILLIS + DAY_OFFSET_MILLIS)

        val ranked = rankMuhurtaDays(MuhurtaActivity.GRIHA_PRAVESH, listOf(later, earlier))

        assertThat(ranked.first().score.score).isEqualTo(ranked.last().score.score)
        assertThat(ranked.first().atSunrise).isEqualTo(earlier.instant)
    }
}

private const val SAMPLE_BASE_MILLIS = 1_705_320_000_000L
private const val DAY_OFFSET_MILLIS = 86_400_000L

private fun idealDaySnapshot(dayMillis: Long): AstronomySnapshot =
    daySnapshot(
        dayMillis = dayMillis,
        tithi = Tithi(number = 5, paksha = Paksha.SHUKLA, name = "Panchami"),
        nakshatra = Nakshatra(number = 4, name = "Rohini"),
        vara = Vara.GURUVARA,
        yoga = Yoga(number = 1, name = "Vishkambha"),
        karana = Karana(number = 2, name = "Bava"),
    )

private fun daySnapshot(
    dayMillis: Long,
    tithi: Tithi,
    nakshatra: Nakshatra,
    vara: Vara,
    yoga: Yoga,
    karana: Karana,
): AstronomySnapshot =
    AstronomySnapshot(
        instant = Instant.fromEpochMilliseconds(dayMillis),
        location = GeoCoordinates(latitude = 28.6139, longitude = 77.2090),
        sunTimes = SunTimes(sunrise = null, sunset = null),
        moonTimes = MoonTimes(moonrise = null, moonset = null),
        tithi = tithi,
        nakshatra = nakshatra,
        yoga = yoga,
        karana = karana,
        vara = vara,
        maasa = Maasa(number = 1, name = "Chaitra", adhika = false),
        samvatsara = Samvatsara(number = 1, name = "Prabhava", shakaYear = 1948),
        ayana = Ayana.UTTARAYANA,
        ritu = Ritu.SHISHIRA,
        moonPhase = MoonPhase.WAXING_GIBBOUS,
        goldenHour = GoldenHour(morningStart = null, morningEnd = null, eveningStart = null, eveningEnd = null),
        muhurtas = emptyList(),
    )
