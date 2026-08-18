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
import io.github.vedicmitra.core.common.coroutines.DispatcherProvider
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Instant

@Suppress("MagicNumber")
class RashiOutlookTest {
    // --- Pure Chandrabala counting ---------------------------------------------------------------

    @Test
    fun `chandra position counts the day Moon sign from the read sign, one-based`() {
        // Same sign is position 1; Kanya (5) from Mesha (0) is the 6th; it wraps mod 12.
        assertThat(chandraPosition(dayMoonRasi = 0, fromMoonRasi = 0)).isEqualTo(1)
        assertThat(chandraPosition(dayMoonRasi = 5, fromMoonRasi = 0)).isEqualTo(6)
        assertThat(chandraPosition(dayMoonRasi = 0, fromMoonRasi = 2)).isEqualTo(11)
    }

    @Test
    fun `chandra strength grades the classical strong, weak and neutral positions`() {
        listOf(1, 3, 6, 7, 10, 11).forEach { assertThat(chandraStrength(it)).isEqualTo(Bala.STRONG) }
        listOf(4, 8, 12).forEach { assertThat(chandraStrength(it)).isEqualTo(Bala.WEAK) }
        listOf(2, 5, 9).forEach { assertThat(chandraStrength(it)).isEqualTo(Bala.NEUTRAL) }
    }

    // --- Pure Tarabala counting ------------------------------------------------------------------

    @Test
    fun `tara from the birth star grades favourable, weak and neutral taras`() {
        // Birth star Ashwini (1): Rohini (4) is the 4th tara (Kshema — favourable).
        val favourable = taraBetween(dayNakshatra = 4, birthNakshatra = 1)
        assertThat(favourable.number).isEqualTo(4)
        assertThat(favourable.name).isEqualTo("Kshema")
        assertThat(favourable.strength).isEqualTo(Bala.STRONG)

        // Krittika (3) is the 3rd tara (Vipat — unfavourable).
        assertThat(taraBetween(dayNakshatra = 3, birthNakshatra = 1).strength).isEqualTo(Bala.WEAK)

        // The birth star itself is Janma — neutral.
        assertThat(taraBetween(dayNakshatra = 1, birthNakshatra = 1).strength).isEqualTo(Bala.NEUTRAL)
    }

    // --- Pure band combination -------------------------------------------------------------------

    @Test
    fun `sign-only band follows Chandrabala when there is no Tarabala`() {
        assertThat(outlookBand(Bala.STRONG, tara = null)).isEqualTo(OutlookBand.AUSPICIOUS)
        assertThat(outlookBand(Bala.NEUTRAL, tara = null)).isEqualTo(OutlookBand.MIXED)
        assertThat(outlookBand(Bala.WEAK, tara = null)).isEqualTo(OutlookBand.CHALLENGING)
    }

    @Test
    fun `personalised band combines Chandrabala and Tarabala`() {
        assertThat(outlookBand(Bala.STRONG, tara = Bala.STRONG)).isEqualTo(OutlookBand.AUSPICIOUS)
        assertThat(outlookBand(Bala.WEAK, tara = Bala.WEAK)).isEqualTo(OutlookBand.CHALLENGING)
        // One strong, one weak cancels to Mixed.
        assertThat(outlookBand(Bala.STRONG, tara = Bala.WEAK)).isEqualTo(OutlookBand.MIXED)
        // A strong Chandrabala with a neutral tara still leans auspicious.
        assertThat(outlookBand(Bala.STRONG, tara = Bala.NEUTRAL)).isEqualTo(OutlookBand.AUSPICIOUS)
    }

    // --- Engine wiring smoke test ----------------------------------------------------------------

    @Test
    fun `engine returns a full week of graded days, personalised only when a person is given`() =
        runTest {
            val engine = DefaultAstronomyEngine(OutlookDispatcherProvider)
            val delhi = GeoCoordinates(latitude = 28.6139, longitude = 77.2090)
            val instant = Instant.fromEpochMilliseconds(946_684_800_000L) // 2000-01-01

            val general = engine.rashiOutlook(rasiIndex = 0, instant = instant, location = delhi, days = 7)
            assertThat(general).isInstanceOf(AppResult.Success::class.java)
            check(general is AppResult.Success)
            val outlook = requireNotNull(general.data)
            assertThat(outlook.rasi.name).isEqualTo("Mesha")
            assertThat(outlook.personalized).isFalse()
            assertThat(outlook.week).hasSize(7)
            assertThat(outlook.today).isEqualTo(outlook.week.first())
            outlook.week.forEach {
                assertThat(it.chandraPosition).isIn(1..12)
                assertThat(it.tara).isNull()
            }

            val personalised =
                engine.rashiOutlook(
                    rasiIndex = 0,
                    instant = instant,
                    location = delhi,
                    person = PersonalMuhurtaContext(birthNakshatraNumber = 1, birthMoonRasiIndex = 0),
                    days = 3,
                )
            check(personalised is AppResult.Success)
            val personalOutlook = requireNotNull(personalised.data)
            assertThat(personalOutlook.personalized).isTrue()
            assertThat(personalOutlook.week).hasSize(3)
            personalOutlook.week.forEach { assertThat(it.tara).isNotNull() }
        }

    @Test
    fun `an out-of-range sign index fails cleanly`() =
        runTest {
            val engine = DefaultAstronomyEngine(OutlookDispatcherProvider)
            val delhi = GeoCoordinates(latitude = 28.6139, longitude = 77.2090)
            val result =
                engine.rashiOutlook(rasiIndex = 12, instant = Instant.fromEpochMilliseconds(0L), location = delhi)
            assertThat(result).isInstanceOf(AppResult.Failure::class.java)
        }
}

/** Runs the engine's `withContext(default)` inline so the test stays synchronous. */
private object OutlookDispatcherProvider : DispatcherProvider {
    override val default: CoroutineDispatcher = Dispatchers.Unconfined
    override val io: CoroutineDispatcher = Dispatchers.Unconfined
    override val main: CoroutineDispatcher = Dispatchers.Unconfined
}
