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

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.model.LocationSource
import io.github.vedicmitra.core.common.model.SavedLocation
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
class LocationViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `select delegates to the repository`() =
        runTest {
            val repository = FakeLocationRepository(initial = listOf(location("a")))
            val viewModel = LocationViewModel(repository)

            viewModel.select("a")

            assertThat(repository.selectedLocationId.value).isEqualTo("a")
        }

    @Test
    fun `useCurrentLocation clears the selection`() =
        runTest {
            val repository = FakeLocationRepository(initial = listOf(location("a")), selected = "a")
            val viewModel = LocationViewModel(repository)

            viewModel.useCurrentLocation()

            assertThat(repository.selectedLocationId.value).isNull()
        }

    @Test
    fun `delete removes the location`() =
        runTest {
            val repository = FakeLocationRepository(initial = listOf(location("a"), location("b")))
            val viewModel = LocationViewModel(repository)

            viewModel.delete("a")

            assertThat(repository.savedLocations.value.map { it.id }).containsExactly("b")
        }

    @Test
    fun `uiState reflects the saved locations and selection`() =
        runTest {
            val repository = FakeLocationRepository(initial = listOf(location("a"), location("b")), selected = "b")
            val viewModel = LocationViewModel(repository)

            viewModel.uiState.test {
                // Skip the initial empty state emitted before the repository flows are combined.
                var state = awaitItem()
                if (state.locations.isEmpty()) state = awaitItem()

                assertThat(state.locations.map { it.id }).containsExactly("a", "b")
                assertThat(state.selectedId).isEqualTo("b")
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun location(id: String) =
        SavedLocation(
            id = id,
            label = "Label $id",
            coordinates = GeoCoordinates(latitude = 1.0, longitude = 2.0),
            zoneId = "UTC",
            source = LocationSource.CITY,
        )
}
