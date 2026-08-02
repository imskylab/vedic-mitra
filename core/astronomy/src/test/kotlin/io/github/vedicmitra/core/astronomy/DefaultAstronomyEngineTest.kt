/*
 * Copyright (c) 2026 Vedic Mitra contributors
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
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
    fun `computes tithi and nakshatra for a known instant`() =
        runTest {
            // 2024-01-15 12:00 UTC — validated reference: Shukla Panchami (tithi 5), nakshatra 25.
            val result = engine.snapshotAt(Instant.fromEpochMilliseconds(1_705_320_000_000L), delhi)

            val snap = snapshot(result)
            assertThat(snap.tithi.number).isEqualTo(5)
            assertThat(snap.tithi.paksha).isEqualTo(Paksha.SHUKLA)
            assertThat(snap.tithi.name).isEqualTo("Panchami")
            assertThat(snap.nakshatra.number).isEqualTo(25)
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
