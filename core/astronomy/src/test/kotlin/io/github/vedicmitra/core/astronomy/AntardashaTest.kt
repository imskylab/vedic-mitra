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

package io.github.vedicmitra.core.astronomy

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import kotlin.time.Instant

class AntardashaTest {
    private val birth = Instant.parse("1990-05-17T09:20:00Z").toEpochMilliseconds()
    private val mahadashas = vimshottariDasha(birth)

    @Test
    fun `each mahadasha divides into nine antardashas`() {
        mahadashas.forEach { period ->
            assertWithMessage(period.lord.displayName).that(period.antardashas).hasSize(9)
        }
    }

    @Test
    fun `the first antardasha is ruled by the mahadasha's own lord`() {
        mahadashas.forEach { period ->
            assertWithMessage(period.lord.displayName)
                .that(period.antardashas.first().lord)
                .isEqualTo(period.lord)
        }
    }

    @Test
    fun `the nine sub-lords are the nine grahas, each once`() {
        mahadashas.forEach { period ->
            assertWithMessage(period.lord.displayName)
                .that(period.antardashas.map { it.lord })
                .containsNoDuplicates()
        }
    }

    @Test
    fun `antardashas tile the mahadasha without gap or overlap`() {
        mahadashas.forEach { period ->
            val subs = period.antardashas
            assertWithMessage("${period.lord.displayName} starts with its parent")
                .that(subs.first().start)
                .isEqualTo(period.start)
            assertWithMessage("${period.lord.displayName} ends with its parent")
                .that(subs.last().end)
                .isEqualTo(period.end)
            subs.zipWithNext().forEach { (earlier, later) ->
                assertWithMessage("${period.lord.displayName}: ${earlier.lord} into ${later.lord}")
                    .that(later.start)
                    .isEqualTo(earlier.end)
            }
        }
    }

    @Test
    fun `a sub-period's share is proportional to its own dasha years`() {
        // Within Ketu's 7-year mahadasha, Shukra (20 of the 120 years) takes 7 x 20 / 120 years.
        val ketu = mahadashas.first { it.lord == Graha.KETU }
        val shukra = ketu.antardashas.first { it.lord == Graha.SHUKRA }
        val expectedMillis = (7.0 * 20.0 / 120.0 * 365.2564 * 86_400_000.0).toLong()
        val actualMillis = shukra.end.toEpochMilliseconds() - shukra.start.toEpochMilliseconds()
        assertThat(actualMillis.toDouble()).isWithin(1000.0).of(expectedMillis.toDouble())
    }

    @Test
    fun `every antardasha runs forwards`() {
        mahadashas.flatMap { it.antardashas }.forEach { period ->
            assertWithMessage(period.lord.displayName)
                .that(period.end.toEpochMilliseconds())
                .isGreaterThan(period.start.toEpochMilliseconds())
        }
    }
}
