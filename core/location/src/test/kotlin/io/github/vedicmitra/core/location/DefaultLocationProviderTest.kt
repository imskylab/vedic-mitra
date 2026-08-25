/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.location

import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import io.github.vedicmitra.core.common.coroutines.DispatcherProvider
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.result.AppResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultLocationProviderTest {
    private val fusedClient = mockk<FusedLocationProviderClient>()
    private val provider = DefaultLocationProvider(fusedClient, UnconfinedDispatcherProvider)

    @Test
    fun `returns coordinates from the last known location`() =
        runTest {
            val location =
                mockk<Location> {
                    every { latitude } returns 28.6139
                    every { longitude } returns 77.2090
                }
            every { fusedClient.lastLocation } returns Tasks.forResult(location)

            val result = provider.currentLocation()

            check(result is AppResult.Success)
            assertThat(result.data).isEqualTo(GeoCoordinates(latitude = 28.6139, longitude = 77.2090))
        }

    @Test
    fun `falls back to a fresh fix when nothing is cached`() =
        runTest {
            // What happens after the user turns the device's location setting back on: the cache is
            // empty, because switching it on does not by itself produce a fix. Reading only the cache
            // left the app on its fallback location for good.
            val location =
                mockk<Location> {
                    every { latitude } returns 17.3850
                    every { longitude } returns 78.4867
                }
            every { fusedClient.lastLocation } returns Tasks.forResult(null)
            every { fusedClient.getCurrentLocation(any<Int>(), any<CancellationToken>()) } returns
                Tasks.forResult(location)

            val result = provider.currentLocation()

            check(result is AppResult.Success)
            assertThat(result.data).isEqualTo(GeoCoordinates(latitude = 17.3850, longitude = 78.4867))
        }

    @Test
    fun `fails when neither the cache nor a fresh fix has anything`() =
        runTest {
            every { fusedClient.lastLocation } returns Tasks.forResult(null)
            every { fusedClient.getCurrentLocation(any<Int>(), any<CancellationToken>()) } returns
                Tasks.forResult(null)

            assertThat(provider.currentLocation()).isInstanceOf(AppResult.Failure::class.java)
        }

    @Test
    fun `the cache is preferred, so no fresh fix is requested when it is populated`() =
        runTest {
            // The fresh-fix path costs a scan; it must stay the exception rather than the rule.
            val location =
                mockk<Location> {
                    every { latitude } returns 28.6139
                    every { longitude } returns 77.2090
                }
            every { fusedClient.lastLocation } returns Tasks.forResult(location)

            provider.currentLocation()

            verify(exactly = 0) { fusedClient.getCurrentLocation(any<Int>(), any<CancellationToken>()) }
        }

    @Test
    fun `fails when the location permission is missing`() =
        runTest {
            every { fusedClient.lastLocation } throws SecurityException("permission denied")

            assertThat(provider.currentLocation()).isInstanceOf(AppResult.Failure::class.java)
        }
}

private object UnconfinedDispatcherProvider : DispatcherProvider {
    override val default: CoroutineDispatcher = Dispatchers.Unconfined
    override val io: CoroutineDispatcher = Dispatchers.Unconfined
    override val main: CoroutineDispatcher = Dispatchers.Unconfined
}
