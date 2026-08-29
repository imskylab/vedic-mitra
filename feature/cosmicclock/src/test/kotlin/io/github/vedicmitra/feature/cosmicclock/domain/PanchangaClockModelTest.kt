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
import io.github.vedicmitra.core.astronomy.AstronomySnapshot
import io.github.vedicmitra.core.astronomy.Ayana
import io.github.vedicmitra.core.astronomy.GoldenHour
import io.github.vedicmitra.core.astronomy.Karana
import io.github.vedicmitra.core.astronomy.LimbWindow
import io.github.vedicmitra.core.astronomy.Maasa
import io.github.vedicmitra.core.astronomy.MoonPhase
import io.github.vedicmitra.core.astronomy.MoonTimes
import io.github.vedicmitra.core.astronomy.Nakshatra
import io.github.vedicmitra.core.astronomy.Paksha
import io.github.vedicmitra.core.astronomy.PanchangaConcept
import io.github.vedicmitra.core.astronomy.PanchangaLimbWindows
import io.github.vedicmitra.core.astronomy.Ritu
import io.github.vedicmitra.core.astronomy.Samvatsara
import io.github.vedicmitra.core.astronomy.SunTimes
import io.github.vedicmitra.core.astronomy.Tithi
import io.github.vedicmitra.core.astronomy.Vara
import io.github.vedicmitra.core.astronomy.Yoga
import io.github.vedicmitra.core.common.model.GeoCoordinates
import org.junit.Test
import kotlin.time.Instant

/**
 * The factory's whole job is turning the engine's **1-based** limb numbers into **0-based** drawing
 * indices, and one off-by-one there would rotate a ring by a division without looking broken. Every
 * boundary case is asserted rather than the happy path alone.
 */
class PanchangaClockModelTest {
    @Test
    fun `the rings are ordered by segment count, widest cycle outermost`() {
        // Not recitation order. Arc per segment is 2*pi*r / n, so tick spacing only stays comparable
        // across rings if radius grows with count — recitation order would put 60 karana ticks on
        // the smallest ring and 7 vara ticks on the largest.
        val model = checkNotNull(buildPanchangaClock(snapshot()))
        assertThat(model.rings.map { it.concept })
            .containsExactly(
                PanchangaConcept.KARANA,
                PanchangaConcept.TITHI,
                PanchangaConcept.NAKSHATRA,
                PanchangaConcept.YOGA,
                PanchangaConcept.VARA,
            ).inOrder()
        assertThat(model.rings.map { it.segmentCount }).containsExactly(60, 30, 27, 27, 7).inOrder()
        val counts = model.rings.map { it.segmentCount }
        assertWithMessage("counts must never increase inwards")
            .that(counts.zipWithNext().all { (outer, inner) -> outer >= inner })
            .isTrue()
    }

    @Test
    fun `one-based limb numbers become zero-based indices`() {
        val model =
            checkNotNull(
                buildPanchangaClock(
                    snapshot(
                        tithi = Tithi(number = 1, paksha = Paksha.SHUKLA, name = "Pratipada"),
                        nakshatra = Nakshatra(number = 1, name = "Ashwini"),
                        yoga = Yoga(number = 1, name = "Vishkambha"),
                        karana = Karana(number = 1, name = "Kimstughna"),
                        vara = Vara.RAVIVARA,
                    ),
                ),
            )
        model.rings.forEach { ring ->
            assertWithMessage("${ring.label} first division").that(ring.activeIndex).isEqualTo(0)
        }
    }

    @Test
    fun `the last division of each cycle maps to the last index, not past it`() {
        // Karana is the one that bites: its number runs 1..60 (position in the lunar month), not
        // 1..11 (the names, which repeat). Treating it as 11 would overflow every ring past a third
        // of the month.
        val model =
            checkNotNull(
                buildPanchangaClock(
                    snapshot(
                        tithi = Tithi(number = 30, paksha = Paksha.KRISHNA, name = "Amavasya"),
                        nakshatra = Nakshatra(number = 27, name = "Revati"),
                        yoga = Yoga(number = 27, name = "Vaidhriti"),
                        karana = Karana(number = 60, name = "Naga"),
                        vara = Vara.SHANIVARA,
                    ),
                ),
            )
        assertThat(model.ring(PanchangaConcept.KARANA)?.activeIndex).isEqualTo(59)
        assertThat(model.ring(PanchangaConcept.TITHI)?.activeIndex).isEqualTo(29)
        assertThat(model.ring(PanchangaConcept.NAKSHATRA)?.activeIndex).isEqualTo(26)
        assertThat(model.ring(PanchangaConcept.YOGA)?.activeIndex).isEqualTo(26)
        assertThat(model.ring(PanchangaConcept.VARA)?.activeIndex).isEqualTo(6)
    }

    @Test
    fun `vara is an ordinal already, so it is not shifted`() {
        // The one limb whose engine value is 0-based. Subtracting one here would put Sunday at -1.
        Vara.entries.forEach { vara ->
            val model = checkNotNull(buildPanchangaClock(snapshot(vara = vara)))
            assertWithMessage(vara.name)
                .that(model.ring(PanchangaConcept.VARA)?.activeIndex)
                .isEqualTo(vara.ordinal)
        }
    }

    @Test
    fun `a snapshot without limb windows yields no clock at all`() {
        // Better a spinner than a clock with no progress: the latter looks finished and is not.
        assertThat(buildPanchangaClock(snapshot(limbs = null))).isNull()
    }

    @Test
    fun `vara keeps its ring when its sunrise boundary is unknown`() {
        // Polar latitudes: the weekday is still known, only its window is not. Dropping the ring
        // would reshuffle every other ring's radius as the reader crossed a latitude.
        val model = checkNotNull(buildPanchangaClock(snapshot(limbs = limbWindows(varaWindow = null))))
        val varaRing = checkNotNull(model.ring(PanchangaConcept.VARA))
        assertThat(model.rings).hasSize(5)
        assertThat(varaRing.window).isNull()
        assertThat(varaRing.fraction).isNull()
        assertThat(varaRing.endsAt).isNull()
        assertWithMessage("the weekday itself is still known").that(varaRing.activeName).isNotEmpty()
    }

    @Test
    fun `progress comes from the limb window`() {
        val model = checkNotNull(buildPanchangaClock(snapshot()))
        assertThat(model.ring(PanchangaConcept.TITHI)?.fraction).isEqualTo(0.25)
        assertThat(model.ring(PanchangaConcept.VARA)?.fraction).isEqualTo(0.5)
    }

    @Test
    fun `pada is nested rather than given a ring`() {
        val model = checkNotNull(buildPanchangaClock(snapshot()))
        assertWithMessage("108 divisions would be the least legible thing on the face")
            .that(model.rings.map { it.concept })
            .doesNotContain(PanchangaConcept.PADA)
        assertThat(model.pada?.index).isEqualTo(2) // moonPada 3 is 0-based 2
        assertThat(model.nakshatraRing).isNotNull()
    }

    @Test
    fun `a chart without a pada still builds`() {
        val model = checkNotNull(buildPanchangaClock(snapshot(moonPada = null)))
        assertThat(model.pada).isNull()
        assertThat(model.rings).hasSize(5)
    }

    @Test
    fun `an index outside its cycle is rejected rather than drawn`() {
        // A ring silently rotated by a bad index looks entirely plausible, so fail loudly instead.
        val error =
            runCatching {
                ClockRing(
                    concept = PanchangaConcept.TITHI,
                    label = "Tithi",
                    segmentCount = 30,
                    activeIndex = 30,
                    activeName = "impossible",
                    window = null,
                )
            }.exceptionOrNull()
        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun snapshot(
        tithi: Tithi = Tithi(number = 14, paksha = Paksha.SHUKLA, name = "Chaturdashi"),
        nakshatra: Nakshatra = Nakshatra(number = 4, name = "Rohini"),
        yoga: Yoga = Yoga(number = 12, name = "Dhriti"),
        karana: Karana = Karana(number = 27, name = "Bava"),
        vara: Vara = Vara.SHUKRAVARA,
        moonPada: Int? = 3,
        limbs: PanchangaLimbWindows? = limbWindows(),
    ): AstronomySnapshot =
        AstronomySnapshot(
            instant = AT,
            location = GeoCoordinates(latitude = 17.385, longitude = 78.4867),
            sunTimes = SunTimes(sunrise = AT, sunset = AT),
            moonTimes = MoonTimes(moonrise = AT, moonset = AT),
            tithi = tithi,
            nakshatra = nakshatra,
            moonPada = moonPada,
            yoga = yoga,
            karana = karana,
            vara = vara,
            maasa = Maasa(number = 5, name = "Shravana", adhika = false),
            samvatsara = Samvatsara(number = 39, name = "Vishvavasu", shakaYear = 1948),
            ayana = Ayana.DAKSHINAYANA,
            ritu = Ritu.VARSHA,
            moonPhase = MoonPhase.FULL_MOON,
            goldenHour = GoldenHour(null, null, null, null),
            muhurtas = emptyList(),
            limbs = limbs,
        )

    private fun limbWindows(varaWindow: LimbWindow? = window(0.5)) =
        PanchangaLimbWindows(
            tithi = window(0.25),
            nakshatra = window(0.6),
            yoga = window(0.4),
            karana = window(0.8),
            moonPada = window(0.1),
            moonRashi = window(0.3),
            moonPhase = window(0.9),
            sunRashi = window(0.7),
            vara = varaWindow,
        )

    private companion object {
        val AT = Instant.fromEpochMilliseconds(1_787_000_000_000L)

        fun window(fraction: Double) =
            LimbWindow(
                start = AT,
                end = Instant.fromEpochMilliseconds(AT.toEpochMilliseconds() + 86_400_000L),
                angularFraction = fraction,
            )
    }
}
