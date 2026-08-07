/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.model.LocationSource
import io.github.vedicmitra.core.common.model.SavedLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultLocationRepositoryTest {
    @get:Rule
    val tmpFolder = TemporaryFolder()

    @Test
    fun `upsert adds locations and replaces by id`() =
        runTest {
            val repository = DefaultLocationRepository(newDataStore())

            repository.upsert(location("a", label = "Delhi"))
            repository.upsert(location("b", label = "Chennai"))
            repository.upsert(location("a", label = "New Delhi")) // replaces the first

            val stored = repository.savedLocations.first()
            assertThat(stored.map { it.id }).containsExactly("a", "b")
            assertThat(stored.first { it.id == "a" }.label).isEqualTo("New Delhi")
        }

    @Test
    fun `remove drops a location by id`() =
        runTest {
            val repository = DefaultLocationRepository(newDataStore())
            repository.upsert(location("a", label = "Delhi"))
            repository.upsert(location("b", label = "Chennai"))

            repository.remove("a")

            assertThat(repository.savedLocations.first().map { it.id }).containsExactly("b")
        }

    @Test
    fun `no location is selected by default`() =
        runTest {
            val repository = DefaultLocationRepository(newDataStore())

            assertThat(repository.selectedLocationId.first()).isNull()
        }

    @Test
    fun `select marks a location as selected`() =
        runTest {
            val repository = DefaultLocationRepository(newDataStore())
            repository.upsert(location("a", label = "Delhi"))

            repository.select("a")

            assertThat(repository.selectedLocationId.first()).isEqualTo("a")
        }

    @Test
    fun `removing the selected location clears the selection`() =
        runTest {
            val repository = DefaultLocationRepository(newDataStore())
            repository.upsert(location("a", label = "Delhi"))
            repository.select("a")

            repository.remove("a")

            assertThat(repository.selectedLocationId.first()).isNull()
        }

    @Test
    fun `removing a non-selected location keeps the selection`() =
        runTest {
            val repository = DefaultLocationRepository(newDataStore())
            repository.upsert(location("a", label = "Delhi"))
            repository.upsert(location("b", label = "Chennai"))
            repository.select("a")

            repository.remove("b")

            assertThat(repository.selectedLocationId.first()).isEqualTo("a")
        }

    @Test
    fun `clearSelection removes the selection`() =
        runTest {
            val repository = DefaultLocationRepository(newDataStore())
            repository.upsert(location("a", label = "Delhi"))
            repository.select("a")

            repository.clearSelection()

            assertThat(repository.selectedLocationId.first()).isNull()
        }

    private fun location(
        id: String,
        label: String,
    ) = SavedLocation(
        id = id,
        label = label,
        coordinates = GeoCoordinates(latitude = 28.6139, longitude = 77.2090),
        zoneId = "Asia/Kolkata",
        source = LocationSource.CITY,
    )

    private fun kotlinx.coroutines.test.TestScope.newDataStore() =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job()),
        ) {
            tmpFolder.newFile("locations-${testScheduler.currentTime}.preferences_pb")
        }
}
