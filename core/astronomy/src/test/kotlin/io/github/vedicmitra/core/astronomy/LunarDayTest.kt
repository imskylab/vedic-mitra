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
import kotlin.math.abs

/**
 * Cross-checked against drikpanchang.com for Delhi over 2026-08-02..08 (see
 * [[vedic-mitra-panchang-reference-data]] in project memory) — every reference instant below
 * matches within ~5 minutes, well inside the 10-minute margin these tests allow.
 */
class LunarDayTest {
    private val delhi = GeoCoordinates(latitude = 28.6139, longitude = 77.2090)

    @Test
    fun `moonrise and moonset are within 10 minutes of the drikpanchang reference`() {
        // 2026-08-02 12:00 IST (06:30 UTC): reference moonrise 21:24 IST, moonset 08:52 IST.
        val result = LunarDay.moonTimes(1_785_652_200_000L, delhi.latitude, delhi.longitude)

        assertThat(result.moonrise).isNotNull()
        assertThat(result.moonset).isNotNull()
        assertThat(abs(result.moonrise!!.toEpochMilliseconds() - 1_785_686_040_000L)).isLessThan(600_000L)
        assertThat(abs(result.moonset!!.toEpochMilliseconds() - 1_785_640_920_000L)).isLessThan(600_000L)
    }

    @Test
    fun `returns no moonrise for a civil day the Moon doesn't rise in`() {
        // 2026-08-07 12:00 IST: the Moon doesn't rise again until 00:37 IST on 2026-08-08 — just
        // outside 2026-08-07's civil day — because the lunar day (~24h50m) is longer than the
        // civil day. Confirmed against drikpanchang.com, which attributes that same rise to its
        // Aug-07 page as the "next" one even though it technically falls on Aug 8.
        val result = LunarDay.moonTimes(1_786_084_200_000L, delhi.latitude, delhi.longitude)

        assertThat(result.moonrise).isNull()
        assertThat(result.moonset).isNotNull()
    }

    @Test
    fun `moonrise precedes moonset when both occur the same civil day`() {
        val result = LunarDay.moonTimes(1_785_652_200_000L, delhi.latitude, delhi.longitude)

        assertThat(result.moonset!!.toEpochMilliseconds()).isGreaterThan(result.moonrise!!.toEpochMilliseconds())
    }
}
