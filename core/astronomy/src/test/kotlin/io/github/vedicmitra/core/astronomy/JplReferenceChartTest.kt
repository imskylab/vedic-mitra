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
import io.github.vedicmitra.core.common.coroutines.DispatcherProvider
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Instant

/**
 * Validates the natal-chart engine against **NASA JPL HORIZONS** (DE441) — the authoritative,
 * free ephemeris that the paid panchanga sites use under the hood. HORIZONS geocentric
 * apparent ecliptic longitudes were pulled for each instant and converted to Lahiri-sidereal here
 * (`sidereal = tropical − Lahiri ayanamsa`, standard Chitrapaksha ayanamsa); the resulting rashi
 * and (for the Moon) nakshatra are the golden values below.
 *
 * Famous birthdays are used as recognisable, memorable reference instants. Because both the app and
 * HORIZONS are evaluated at the *same* instant, the comparison is exact and does not depend on the
 * person's true (uncertain) birth time — this validates the engine, it is not a natal reading. Only
 * bodies safely inside a rashi (and nakshatra) are asserted; a Moon sitting on a nakshatra edge is
 * left out of that one check rather than made brittle. Lagna is asserted only for the fully-specified
 * synthetic case (ascendant needs an exact time and place).
 */
@Suppress("MagicNumber")
class JplReferenceChartTest {
    private val engine = DefaultAstronomyEngine(JplDispatcherProvider)
    private val delhi = GeoCoordinates(latitude = 28.6139, longitude = 77.2090)

    @Test
    fun `graha rashis, moon nakshatra and lagna match JPL HORIZONS references`() =
        runTest {
            for (case in REFERENCE_CASES) {
                val result = engine.natalChartAt(Instant.fromEpochMilliseconds(case.epochMillis), delhi)
                assertWithMessage("chart for ${case.label}").that(result).isInstanceOf(AppResult.Success::class.java)
                check(result is AppResult.Success)
                val chart = requireNotNull(result.data) { "no chart for ${case.label}" }

                case.expectedRasi.forEach { (graha, rasiIndex) ->
                    val actual = chart.grahas.first { it.graha == graha }.rasi.index
                    assertWithMessage("${case.label}: ${graha.displayName} rashi").that(actual).isEqualTo(rasiIndex)
                }
                case.expectedMoonNakshatra?.let {
                    assertWithMessage("${case.label}: Moon nakshatra").that(chart.moonNakshatra.number).isEqualTo(it)
                }
                case.expectedLagnaRasi?.let {
                    assertWithMessage("${case.label}: Lagna rashi").that(chart.lagna.rasi.index).isEqualTo(it)
                }
            }
        }
}

/**
 * One reference instant with its JPL-derived goldens.
 *
 * @property expectedRasi each graha's sidereal rashi (0 = Mesha .. 11 = Meena).
 * @property expectedMoonNakshatra the Moon's nakshatra (1..27), or `null` to skip (edge case).
 * @property expectedLagnaRasi the ascendant's rashi, or `null` when the instant isn't fully specified.
 */
private class ReferenceCase(
    val label: String,
    val epochMillis: Long,
    val expectedRasi: Map<Graha, Int>,
    val expectedMoonNakshatra: Int?,
    val expectedLagnaRasi: Int?,
)

// Goldens: JPL HORIZONS (DE441) geocentric apparent ecliptic longitude of date, minus the standard
// Lahiri ayanamsa at each instant, binned to rashi (30 deg) and nakshatra (13deg20'). Mars = Mangala,
// Mercury = Budha, Jupiter = Guru, Venus = Shukra, Saturn = Shani.
@Suppress("MagicNumber")
private val REFERENCE_CASES: List<ReferenceCase> =
    listOf(
        ReferenceCase(
            label = "Synthetic 2000-01-01 00:30 UTC, New Delhi",
            epochMillis = 946_686_600_000L,
            expectedRasi =
                mapOf(
                    Graha.SUN to 8, // Dhanu
                    Graha.MOON to 6, // Tula
                    Graha.MANGALA to 10, // Kumbha
                    Graha.BUDHA to 8, // Dhanu
                    Graha.GURU to 0, // Mesha
                    Graha.SHUKRA to 7, // Vrishchika
                    Graha.SHANI to 0, // Mesha
                ),
            expectedMoonNakshatra = 15, // Swati
            expectedLagnaRasi = 7, // Vrishchika
        ),
        ReferenceCase(
            label = "Gandhi 1869-10-02 12:00 UTC",
            epochMillis = -3_163_492_800_000L,
            expectedRasi =
                mapOf(
                    Graha.SUN to 5, // Kanya
                    Graha.MOON to 4, // Simha
                    Graha.MANGALA to 6, // Tula
                    Graha.BUDHA to 6, // Tula
                    Graha.GURU to 0, // Mesha
                    Graha.SHUKRA to 6, // Tula
                    Graha.SHANI to 7, // Vrishchika
                ),
            expectedMoonNakshatra = 10, // Magha
            expectedLagnaRasi = null,
        ),
        ReferenceCase(
            label = "Einstein 1879-03-14 12:00 UTC",
            epochMillis = -2_865_412_800_000L,
            expectedRasi =
                mapOf(
                    Graha.SUN to 11, // Meena
                    Graha.MOON to 7, // Vrishchika
                    Graha.MANGALA to 9, // Makara
                    Graha.BUDHA to 11, // Meena
                    Graha.GURU to 10, // Kumbha
                    Graha.SHUKRA to 11, // Meena
                    Graha.SHANI to 11, // Meena
                ),
            expectedMoonNakshatra = 18, // Jyeshtha
            expectedLagnaRasi = null,
        ),
        ReferenceCase(
            label = "Sachin 1973-04-24 12:00 UTC",
            epochMillis = 104_500_800_000L,
            expectedRasi =
                mapOf(
                    Graha.SUN to 0, // Mesha
                    Graha.MOON to 8, // Dhanu
                    Graha.MANGALA to 9, // Makara
                    Graha.BUDHA to 11, // Meena
                    Graha.GURU to 9, // Makara
                    Graha.SHUKRA to 0, // Mesha
                    Graha.SHANI to 1, // Vrishabha
                ),
            expectedMoonNakshatra = null, // sits on a nakshatra boundary — rashi only
            expectedLagnaRasi = null,
        ),
    )

/** Runs the engine's `withContext(default)` inline so the test stays synchronous. */
private object JplDispatcherProvider : DispatcherProvider {
    override val default: CoroutineDispatcher = Dispatchers.Unconfined
    override val io: CoroutineDispatcher = Dispatchers.Unconfined
    override val main: CoroutineDispatcher = Dispatchers.Unconfined
}
