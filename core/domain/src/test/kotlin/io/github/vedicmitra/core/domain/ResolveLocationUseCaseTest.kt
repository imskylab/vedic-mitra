/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.domain

import com.google.common.truth.Truth.assertThat
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.model.LocationSource
import io.github.vedicmitra.core.common.model.SavedLocation
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.datastore.LocationRepository
import io.github.vedicmitra.core.location.LocationProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.ZoneId

class ResolveLocationUseCaseTest {
    private val locationRepository = mockk<LocationRepository>()
    private val locationProvider = mockk<LocationProvider>()
    private val useCase = ResolveLocationUseCase(locationRepository, locationProvider)

    @Test
    fun `prefers the selected saved location`() =
        runTest {
            val saved =
                SavedLocation(
                    id = "loc-1",
                    label = "Varanasi",
                    coordinates = GeoCoordinates(latitude = 25.3176, longitude = 82.9739),
                    zoneId = "Asia/Kolkata",
                    source = LocationSource.CITY,
                )
            every { locationRepository.selectedLocationId } returns flowOf("loc-1")
            every { locationRepository.savedLocations } returns flowOf(listOf(saved))

            val resolved = useCase()

            assertThat(resolved.coordinates).isEqualTo(saved.coordinates)
            assertThat(resolved.zoneId).isEqualTo("Asia/Kolkata")
            assertThat(resolved.label).isEqualTo("Varanasi")
            assertThat(resolved.isDefault).isFalse()
        }

    @Test
    fun `falls back to the device location when nothing is selected`() =
        runTest {
            every { locationRepository.selectedLocationId } returns flowOf(null)
            every { locationRepository.savedLocations } returns flowOf(emptyList())
            coEvery { locationProvider.currentLocation() } returns
                AppResult.Success(GeoCoordinates(latitude = 12.9716, longitude = 77.5946))

            val resolved = useCase()

            assertThat(resolved.coordinates).isEqualTo(GeoCoordinates(latitude = 12.9716, longitude = 77.5946))
            assertThat(resolved.zoneId).isEqualTo(ZoneId.systemDefault().id)
            assertThat(resolved.isDefault).isFalse()
        }

    @Test
    fun `falls back to the default when nothing is selected and the device location is unavailable`() =
        runTest {
            every { locationRepository.selectedLocationId } returns flowOf(null)
            every { locationRepository.savedLocations } returns flowOf(emptyList())
            coEvery { locationProvider.currentLocation() } returns AppResult.Failure(IllegalStateException("no fix"))

            val resolved = useCase()

            assertThat(resolved.coordinates).isEqualTo(GeoCoordinates(latitude = 28.6139, longitude = 77.2090))
            assertThat(resolved.label).isEqualTo("New Delhi")
            assertThat(resolved.isDefault).isTrue()
        }

    @Test
    fun `ignores a stale selection whose location no longer exists`() =
        runTest {
            every { locationRepository.selectedLocationId } returns flowOf("missing")
            every { locationRepository.savedLocations } returns flowOf(emptyList())
            coEvery { locationProvider.currentLocation() } returns AppResult.Failure(IllegalStateException("no fix"))

            val resolved = useCase()

            assertThat(resolved.isDefault).isTrue()
        }
}
