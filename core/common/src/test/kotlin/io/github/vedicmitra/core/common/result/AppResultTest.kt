/*
 * Copyright (c) 2026 Vedic Mitra contributors
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
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
