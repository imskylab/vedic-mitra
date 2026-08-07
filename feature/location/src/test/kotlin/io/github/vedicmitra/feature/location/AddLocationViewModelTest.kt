/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.location

import com.google.common.truth.Truth.assertThat
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.model.LocationSource
import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.location.GeocodeResult
import io.github.vedicmitra.core.location.GeocodingClient
import io.github.vedicmitra.core.location.TimeZoneResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddLocationViewModelTest {
    private val repository = FakeLocationRepository()
    private val geocoder = FakeGeocodingClient()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveFromResult resolves the time zone and selects the new location`() =
        runTest {
            val resolver = FakeTimeZoneResolver("Europe/Paris")
            val viewModel = AddLocationViewModel(repository, geocoder, resolver)
            var saved = false

            viewModel.saveFromResult(
                GeocodeResult(label = "Paris", coordinates = GeoCoordinates(latitude = 48.8566, longitude = 2.3522)),
            ) { saved = true }

            val location = repository.savedLocations.value.single()
            assertThat(location.label).isEqualTo("Paris")
            assertThat(location.zoneId).isEqualTo("Europe/Paris")
            assertThat(location.source).isEqualTo(LocationSource.CITY)
            assertThat(repository.selectedLocationId.value).isEqualTo(location.id)
            assertThat(saved).isTrue()
        }

    @Test
    fun `saveManual with a blank zone auto-detects it from the coordinates`() =
        runTest {
            val resolver = FakeTimeZoneResolver("America/New_York")
            val viewModel = AddLocationViewModel(repository, geocoder, resolver)

            viewModel.saveManual(label = "Office", latitude = 40.7128, longitude = -74.0060, zoneId = "") {}

            val location = repository.savedLocations.value.single()
            assertThat(location.zoneId).isEqualTo("America/New_York")
            assertThat(location.source).isEqualTo(LocationSource.MANUAL)
            assertThat(resolver.resolveCalls).isEqualTo(1)
        }

    @Test
    fun `saveManual with an explicit zone uses the override and does not auto-detect`() =
        runTest {
            val resolver = FakeTimeZoneResolver("America/New_York")
            val viewModel = AddLocationViewModel(repository, geocoder, resolver)

            viewModel.saveManual(label = "Home", latitude = 28.6139, longitude = 77.2090, zoneId = "Asia/Kolkata") {}

            val location = repository.savedLocations.value.single()
            assertThat(location.zoneId).isEqualTo("Asia/Kolkata")
            assertThat(resolver.resolveCalls).isEqualTo(0)
        }

    @Test
    fun `saveManual with a blank label falls back to the coordinates`() =
        runTest {
            val resolver = FakeTimeZoneResolver("Asia/Kolkata")
            val viewModel = AddLocationViewModel(repository, geocoder, resolver)

            viewModel.saveManual(label = "", latitude = 12.9716, longitude = 77.5946, zoneId = "Asia/Kolkata") {}

            assertThat(
                repository.savedLocations.value
                    .single()
                    .label,
            ).isEqualTo("12.9716, 77.5946")
        }
}

private class FakeTimeZoneResolver(
    private val zone: String,
) : TimeZoneResolver {
    var resolveCalls = 0
        private set

    override suspend fun resolve(coordinates: GeoCoordinates): String {
        resolveCalls++
        return zone
    }
}

private class FakeGeocodingClient(
    private val results: List<GeocodeResult> = emptyList(),
) : GeocodingClient {
    override suspend fun search(
        query: String,
        maxResults: Int,
    ): AppResult<List<GeocodeResult>> = AppResult.Success(results)
}
