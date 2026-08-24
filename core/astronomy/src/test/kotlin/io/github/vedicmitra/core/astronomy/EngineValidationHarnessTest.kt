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
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.time.Instant

/**
 * Reference-grade validation of the engine against publicly-fixed astronomy — the automated half of
 * the panchanga validation pass (see `docs/validation/panchanga-validation.md` for the on-device,
 * against-a-published-almanac half).
 *
 * These anchors are chosen because they don't need a third-party panchanga to know the answer: the
 * Sun's sidereal sign on a mid-month day is fixed by the Sankranti calendar, and the Lahiri ayanamsa
 * for the app's era is a published value. Fast-moving lunar limbs (tithi/nakshatra/yoga/karana) are
 * verified on-device against a published almanac via the checklist doc instead, since pinning exact
 * reference values here would just hard-code one source's output.
 */
class EngineValidationHarnessTest {
    private val engine = DefaultAstronomyEngine(HarnessDispatcherProvider)

    // New Delhi — the app's built-in default location.
    private val delhi = GeoCoordinates(latitude = 28.6139, longitude = 77.2090)

    @Test
    fun `the sidereal Sun sign on a mid-month day matches the Sankranti calendar`() {
        // The Sun enters each sidereal rashi around the 14th-17th (Lahiri). A day near the 25th sits
        // well inside the sign, clear of the ingress boundary, so the sign is unambiguous and
        // year-independent. Rashi index: 0 = Mesha .. 11 = Meena.
        assertThat(sunRasiIndexOn(2026, 1, 25)).isEqualTo(9) // Makara  (after Makar Sankranti ~Jan 14)
        assertThat(sunRasiIndexOn(2026, 4, 25)).isEqualTo(0) // Mesha   (after Mesha Sankranti ~Apr 14)
        assertThat(sunRasiIndexOn(2026, 7, 25)).isEqualTo(3) // Karka   (after Karka Sankranti ~Jul 16)
        assertThat(sunRasiIndexOn(2026, 10, 25)).isEqualTo(6) // Tula   (after Tula Sankranti ~Oct 17)
    }

    @Test
    fun `the Lahiri ayanamsa is about 24 degrees for the app's era`() {
        // Published Lahiri ayanamsa is ~24.19 deg at the start of 2026 (it grows ~50.3 arc-seconds a
        // year). The band catches a wrong constant or slope while tolerating the linear-fit drift.
        val t = Ephemeris.julianCenturies(utcNoonMillis(2026, 1, 1))
        val ayanamsa = Ephemeris.lahiriAyanamsa(t)

        assertThat(ayanamsa).isGreaterThan(23.9)
        assertThat(ayanamsa).isLessThan(24.5)
    }

    @Test
    fun `the sidereal Sun advances one sign across a solar month`() {
        // Consecutive mid-month samples must step to the next sign, never stall or jump two — a guard
        // on the Sun-longitude rate and the sidereal conversion.
        val jan = sunRasiIndexOn(2026, 2, 25) // Kumbha (10)
        val feb = sunRasiIndexOn(2026, 3, 25) // Meena  (11)
        assertThat(jan).isEqualTo(10)
        assertThat(feb).isEqualTo(11)
    }

    @Test
    fun `a full snapshot and natal chart compute cleanly for the checklist cases`() {
        // A liveness guard: the checklist's reproducible cases must actually produce a snapshot and a
        // chart, so on-device values exist to compare. Exact limb values are checked on-device.
        runTest {
            val snapshot = engine.snapshotAt(utcNoon(2026, 1, 25), delhi)
            assertThat(snapshot).isInstanceOf(AppResult.Success::class.java)

            // Reproducible synthetic birth: 2000-01-01 00:30 UTC (06:00 IST), New Delhi.
            val chart = engine.natalChartAt(Instant.fromEpochMilliseconds(946_686_600_000L), delhi)
            assertThat(chart).isInstanceOf(AppResult.Success::class.java)
            check(chart is AppResult.Success)
            assertThat(chart.data?.grahas).isNotEmpty()
        }
    }

    private fun sunRasiIndexOn(
        year: Int,
        month: Int,
        day: Int,
    ): Int {
        val t = Ephemeris.julianCenturies(utcNoonMillis(year, month, day))
        val siderealDeg = Ephemeris.norm360(Ephemeris.sunApparentLongitude(t) - Ephemeris.lahiriAyanamsa(t))
        return rasiOf(siderealDeg).index
    }

    private fun utcNoon(
        year: Int,
        month: Int,
        day: Int,
    ): Instant = Instant.fromEpochMilliseconds(utcNoonMillis(year, month, day))

    private fun utcNoonMillis(
        year: Int,
        month: Int,
        day: Int,
    ): Long =
        LocalDate
            .of(year, month, day)
            .atTime(12, 0)
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()
}

/** Runs the engine's `withContext(default)` inline so the harness stays synchronous. */
private object HarnessDispatcherProvider : DispatcherProvider {
    override val default: CoroutineDispatcher = Dispatchers.Unconfined
    override val io: CoroutineDispatcher = Dispatchers.Unconfined
    override val main: CoroutineDispatcher = Dispatchers.Unconfined
}
