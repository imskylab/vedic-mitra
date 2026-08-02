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
import kotlin.math.abs
import kotlin.time.Instant

class DefaultAstronomyEngineTest {
    private val engine = DefaultAstronomyEngine(UnconfinedDispatcherProvider)

    // New Delhi.
    private val delhi = GeoCoordinates(latitude = 28.6139, longitude = 77.2090)

    private fun snapshot(result: AppResult<AstronomySnapshot>): AstronomySnapshot {
        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        check(result is AppResult.Success)
        return result.data
    }

    @Test
    fun `computes the panchanga for a known instant`() =
        runTest {
            // 2024-01-15 12:00 UTC — validated reference: Shukla Panchami (tithi 5), nakshatra 25,
            // yoga 18 (Variyana), karana Balava.
            val result = engine.snapshotAt(Instant.fromEpochMilliseconds(1_705_320_000_000L), delhi)

            val snap = snapshot(result)
            assertThat(snap.tithi.number).isEqualTo(5)
            assertThat(snap.tithi.paksha).isEqualTo(Paksha.SHUKLA)
            assertThat(snap.tithi.name).isEqualTo("Panchami")
            assertThat(snap.nakshatra.number).isEqualTo(25)
            assertThat(snap.yoga.number).isEqualTo(18)
            assertThat(snap.yoga.name).isEqualTo("Variyana")
            assertThat(snap.karana.name).isEqualTo("Balava")
        }

    @Test
    fun `computes the day's muhurta windows`() =
        runTest {
            val result = engine.snapshotAt(Instant.fromEpochMilliseconds(1_705_320_000_000L), delhi)
            val snap = snapshot(result)

            assertThat(snap.muhurtas.map { it.name }).containsExactly(
                "Brahma Muhurta",
                "Abhijit Muhurta",
                "Rahu Kalam",
                "Yamaganda",
                "Gulika Kalam",
            )
            assertThat(snap.muhurtas.first { it.name == "Abhijit Muhurta" }.quality)
                .isEqualTo(MuhurtaQuality.AUSPICIOUS)

            val rahu = snap.muhurtas.first { it.name == "Rahu Kalam" }
            assertThat(rahu.quality).isEqualTo(MuhurtaQuality.INAUSPICIOUS)
            // Rahu Kalam falls within the daytime.
            val sunrise = snap.sunTimes.sunrise!!.toEpochMilliseconds()
            val sunset = snap.sunTimes.sunset!!.toEpochMilliseconds()
            assertThat(rahu.start.toEpochMilliseconds()).isAtLeast(sunrise)
            assertThat(rahu.end.toEpochMilliseconds()).isAtMost(sunset)
        }

    @Test
    fun `sunrise for New Delhi matches reference within a few minutes`() =
        runTest {
            // 2024-01-01: reference sunrise 2024-01-01 01:44 UTC (07:14 IST).
            val result = engine.snapshotAt(Instant.fromEpochMilliseconds(1_704_067_200_000L), delhi)

            val sunrise = snapshot(result).sunTimes.sunrise
            assertThat(sunrise).isNotNull()
            val deltaMillis = abs(sunrise!!.toEpochMilliseconds() - 1_704_073_431_852L)
            assertThat(deltaMillis).isLessThan(180_000L) // within 3 minutes
        }

    @Test
    fun `rejects out-of-range coordinates`() =
        runTest {
            val result =
                engine.snapshotAt(
                    Instant.fromEpochMilliseconds(0L),
                    GeoCoordinates(latitude = 200.0, longitude = 0.0),
                )
            assertThat(result).isInstanceOf(AppResult.Failure::class.java)
        }
}

/** Runs everything on the unconfined dispatcher so `withContext` executes inline in tests. */
private object UnconfinedDispatcherProvider : DispatcherProvider {
    override val default: CoroutineDispatcher = Dispatchers.Unconfined
    override val io: CoroutineDispatcher = Dispatchers.Unconfined
    override val main: CoroutineDispatcher = Dispatchers.Unconfined
}
