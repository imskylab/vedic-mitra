/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.common.result

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Sanity tests for [AppResult]. These exist mainly to prove the unit-test source set, JUnit, and
 * Truth are wired correctly across the project; more meaningful tests accompany real logic.
 */
class AppResultTest {
    @Test
    fun `map transforms success value`() {
        val result: AppResult<Int> = AppResult.Success(2)

        val mapped = result.map { it * 3 }

        assertThat(mapped).isEqualTo(AppResult.Success(6))
    }

    @Test
    fun `map preserves failure`() {
        val cause = IllegalStateException("boom")
        val result: AppResult<Int> = AppResult.Failure(cause)

        val mapped = result.map { it * 3 }

        assertThat(mapped).isEqualTo(AppResult.Failure(cause))
    }

    @Test
    fun `getOrNull returns null for failure`() {
        val result: AppResult<Int> = AppResult.Failure(RuntimeException())

        assertThat(result.getOrNull()).isNull()
    }
}
