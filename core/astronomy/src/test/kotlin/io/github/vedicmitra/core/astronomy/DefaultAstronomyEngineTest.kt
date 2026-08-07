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

            // 2024-01-15 is a Monday: Dur Muhurta falls in a different segment than Abhijit, so
            // both appear (Wednesday is the only day that suppresses Abhijit — see
            // `muhurtasOf`'s KDoc).
            assertThat(snap.muhurtas.map { it.name }).containsExactly(
                "Brahma Muhurta",
                "Abhijit Muhurta",
                "Rahu Kalam",
                "Yamaganda",
                "Gulika Kalam",
                "Dur Muhurta",
                "Varjyam",
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
    fun `abhijit muhurta is suppressed on wednesdays, replaced by dur muhurta in the same segment`() =
        runTest {
            // 2026-08-05 is a Wednesday (already used and cross-checked for ayana/ritu above).
            val result = engine.snapshotAt(Instant.fromEpochMilliseconds(1_785_911_400_000L), delhi)
            val snap = snapshot(result)

            assertThat(snap.muhurtas.map { it.name }).doesNotContain("Abhijit Muhurta")
            assertThat(snap.muhurtas.map { it.name }).contains("Dur Muhurta")
        }

    @Test
    fun `varjyam is a 4-ghati inauspicious window`() =
        runTest {
            val result = engine.snapshotAt(Instant.fromEpochMilliseconds(1_705_320_000_000L), delhi)
            val snap = snapshot(result)

            val varjyam = snap.muhurtas.first { it.name == "Varjyam" }
            assertThat(varjyam.quality).isEqualTo(MuhurtaQuality.INAUSPICIOUS)
            // Every nakshatra's Varjyam window is exactly 4 ghatis (96 minutes) — see
            // VarjyamCalculatorTest for the nakshatra-start-finding math itself.
            assertThat(varjyam.end.toEpochMilliseconds() - varjyam.start.toEpochMilliseconds())
                .isEqualTo(96 * 60_000L)
        }

    @Test
    fun `ayana and ritu match drikpanchang and datepanchang reference for New Delhi`() =
        runTest {
            // 2026-08-05 12:00 IST (06:30 UTC) — cross-checked against datepanchang.com (Mumbai) and
            // drikpanchang.com (Delhi): both report Dakshinayana and Varsha (monsoon) Ritu.
            val result = engine.snapshotAt(Instant.fromEpochMilliseconds(1_785_911_400_000L), delhi)

            val snap = snapshot(result)
            assertThat(snap.ayana).isEqualTo(Ayana.DAKSHINAYANA)
            assertThat(snap.ritu).isEqualTo(Ritu.VARSHA)
        }

    @Test
    fun `maasa and samvatsara match drikpanchang reference for New Delhi`() =
        runTest {
            // 2026-08-05 12:00 IST — amanta Ashadha, Krishna paksha (2026 has an intercalary Adhika
            // Jyeshtha, so early August is Ashadha, not Shravana), in the Parabhava samvatsara
            // (Shaka 1948). Cross-checked against drikpanchang.com and the published Ugadi 2026
            // almanac (Parabhava Nama Samvatsara, new year 19 March 2026).
            val result = engine.snapshotAt(Instant.fromEpochMilliseconds(1_785_911_400_000L), delhi)
            val snap = snapshot(result)

            assertThat(snap.maasa.name).isEqualTo("Ashadha")
            assertThat(snap.maasa.number).isEqualTo(4)
            assertThat(snap.maasa.adhika).isFalse()
            assertThat(snap.samvatsara.name).isEqualTo("Parabhava")
            assertThat(snap.samvatsara.shakaYear).isEqualTo(1948)
        }

    @Test
    fun `moonrise and moonset match drikpanchang reference for New Delhi`() =
        runTest {
            // 2026-08-05 12:00 IST — cross-checked against drikpanchang.com for Delhi: moonrise
            // 23:04 IST, moonset 11:52 IST. The engine's low-precision ephemeris + bisection search
            // lands consistently within ~5 minutes of drikpanchang.com across a full week of
            // real reference data (see project memory) — allow 10 minutes of margin here.
            val result = engine.snapshotAt(Instant.fromEpochMilliseconds(1_785_911_400_000L), delhi)
            val snap = snapshot(result)

            assertThat(snap.moonTimes.moonrise).isNotNull()
            assertThat(snap.moonTimes.moonset).isNotNull()
            val moonriseDeltaMillis = abs(snap.moonTimes.moonrise!!.toEpochMilliseconds() - 1_785_951_240_000L)
            val moonsetDeltaMillis = abs(snap.moonTimes.moonset!!.toEpochMilliseconds() - 1_785_910_920_000L)
            assertThat(moonriseDeltaMillis).isLessThan(600_000L) // within 10 minutes
            assertThat(moonsetDeltaMillis).isLessThan(600_000L) // within 10 minutes
        }

    @Test
    fun `moon phase is consistent with the tithi`() =
        runTest {
            val result = engine.snapshotAt(Instant.fromEpochMilliseconds(1_705_320_000_000L), delhi)

            // Shukla Panchami (tithi 5, elongation 48-60°) falls in the Waxing Crescent phase (22.5-67.5°).
            assertThat(snapshot(result).moonPhase).isEqualTo(MoonPhase.WAXING_CRESCENT)
        }

    @Test
    fun `golden hour windows bracket sunrise and sunset`() =
        runTest {
            val result = engine.snapshotAt(Instant.fromEpochMilliseconds(1_705_320_000_000L), delhi)
            val snap = snapshot(result)
            val golden = snap.goldenHour
            val sunrise = snap.sunTimes.sunrise!!.toEpochMilliseconds()
            val sunset = snap.sunTimes.sunset!!.toEpochMilliseconds()

            assertThat(golden.morningStart).isNotNull()
            assertThat(golden.morningEnd).isNotNull()
            assertThat(golden.eveningStart).isNotNull()
            assertThat(golden.eveningEnd).isNotNull()

            val morningStart = golden.morningStart!!.toEpochMilliseconds()
            val morningEnd = golden.morningEnd!!.toEpochMilliseconds()
            val eveningStart = golden.eveningStart!!.toEpochMilliseconds()
            val eveningEnd = golden.eveningEnd!!.toEpochMilliseconds()

            // Morning golden hour brackets sunrise; evening golden hour brackets sunset.
            assertThat(morningStart).isLessThan(sunrise)
            assertThat(morningEnd).isGreaterThan(sunrise)
            assertThat(eveningStart).isLessThan(sunset)
            assertThat(eveningEnd).isGreaterThan(sunset)
            assertThat(morningStart).isLessThan(morningEnd)
            assertThat(eveningStart).isLessThan(eveningEnd)
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
