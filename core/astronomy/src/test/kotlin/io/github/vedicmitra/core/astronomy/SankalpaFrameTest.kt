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
import io.github.vedicmitra.core.common.model.GeoCoordinates
import org.junit.Test
import kotlin.time.Instant

/**
 * The frame adds no astronomy — every value is already on the snapshot — so what is worth pinning is
 * the **order**, which is the part carrying a claim, and that nothing is silently dropped or
 * invented on the way through.
 */
class SankalpaFrameTest {
    @Test
    fun `names the ten measures in recitation order`() {
        val frame = snapshot().sankalpaFrame()

        assertThat(frame.coordinates.map { it.label })
            .containsExactly(
                "Samvatsara",
                "Ayana",
                "Ritu",
                "Maasa",
                "Paksha",
                "Tithi",
                "Vara",
                "Nakshatra",
                "Yoga",
                "Karana",
            ).inOrder()
    }

    @Test
    fun `takes every value from the snapshot rather than restating a default`() {
        val frame = snapshot().sankalpaFrame()
        val byLabel = frame.coordinates.associate { it.label to it.value }

        assertThat(byLabel["Samvatsara"]).isEqualTo("Parabhava")
        assertThat(byLabel["Ayana"]).isEqualTo("Uttarayana")
        assertThat(byLabel["Ritu"]).isEqualTo("Vasanta")
        assertThat(byLabel["Maasa"]).isEqualTo("Chaitra")
        assertThat(byLabel["Paksha"]).isEqualTo("Shukla")
        assertThat(byLabel["Tithi"]).isEqualTo("Chaturdashi")
        assertThat(byLabel["Vara"]).isEqualTo("Budhavara")
        assertThat(byLabel["Nakshatra"]).isEqualTo("Rohini")
        assertThat(byLabel["Yoga"]).isEqualTo("Shubha")
        assertThat(byLabel["Karana"]).isEqualTo("Bava")
    }

    @Test
    fun `an adhika month is named as one`() {
        // The frame must not quietly drop the Adhika prefix: in a leap year the intercalary month
        // carries the following month's name, and the two are a fortnight of tithis apart.
        val frame = snapshot(maasa = Maasa(number = 3, name = "Jyeshtha", adhika = true)).sankalpaFrame()

        assertThat(frame.coordinates.single { it.label == "Maasa" }.value).isEqualTo("Adhika Jyeshtha")
    }

    @Test
    fun `the place is copied out first, because desha precedes kala`() {
        val text = snapshot().sankalpaFrame(place = "New Delhi").asText

        assertThat(text.lineSequence().first()).isEqualTo("Place: New Delhi")
        assertWithMessage("the time measures follow the place")
            .that(text.lineSequence().elementAt(1))
            .isEqualTo("Samvatsara: Parabhava")
        assertThat(text.lineSequence().count()).isEqualTo(11)
    }

    @Test
    fun `an unknown place leaves no empty line behind`() {
        // Rendering a blank "Place:" line would be worse than omitting it -- someone copying this
        // out would paste a field the app could not fill.
        val text = snapshot().sankalpaFrame(place = null).asText

        assertThat(text).doesNotContain("Place")
        assertThat(text.lineSequence().count()).isEqualTo(10)
        assertThat(text.lineSequence().first()).isEqualTo("Samvatsara: Parabhava")
    }

    private fun snapshot(maasa: Maasa = Maasa(number = 1, name = "Chaitra", adhika = false)): AstronomySnapshot =
        AstronomySnapshot(
            instant = Instant.fromEpochMilliseconds(1_774_420_200_000L),
            location = GeoCoordinates(latitude = 28.6139, longitude = 77.2090),
            sunTimes = SunTimes(sunrise = null, sunset = null),
            moonTimes = MoonTimes(moonrise = null, moonset = null),
            tithi = Tithi(number = 14, paksha = Paksha.SHUKLA, name = "Chaturdashi"),
            nakshatra = Nakshatra(number = 4, name = "Rohini"),
            yoga = Yoga(number = 23, name = "Shubha"),
            karana = Karana(number = 27, name = "Bava"),
            vara = Vara.BUDHAVARA,
            maasa = maasa,
            samvatsara = Samvatsara(number = 40, name = "Parabhava", shakaYear = 1948),
            ayana = Ayana.UTTARAYANA,
            ritu = Ritu.VASANTA,
            moonPhase = MoonPhase.WAXING_GIBBOUS,
            goldenHour = GoldenHour(morningStart = null, morningEnd = null, eveningStart = null, eveningEnd = null),
            muhurtas = emptyList(),
        )
}
