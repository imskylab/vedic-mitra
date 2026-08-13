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

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.google.common.truth.Truth.assertThat
import io.github.vedicmitra.core.common.model.GeoCoordinates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("MagicNumber")
class DefaultProfileRepositoryTest {
    @get:Rule
    val tmpFolder = TemporaryFolder()

    // Cancel each DataStore's scope after the test so its backing temp file is released promptly —
    // a lingering handle intermittently trips AccessDeniedException on Windows.
    private val dataStoreScopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        dataStoreScopes.forEach { it.cancel() }
    }

    private val leo =
        BirthProfile(
            id = "a",
            name = "Leo",
            relation = ProfileRelation.SELF,
            dateOfBirth = LocalDate.of(1995, 3, 14),
            timeOfBirth = LocalTime.of(9, 30),
            placeOfBirth = "Hyderabad, India",
            birthCoordinates = GeoCoordinates(latitude = 17.385, longitude = 78.4867),
            birthZoneId = "Asia/Kolkata",
        )
    private val mia = BirthProfile(id = "b", name = "Mia", relation = ProfileRelation.SPOUSE)

    @Test
    fun `defaults to no profiles and no primary`() =
        runTest {
            val repository = DefaultProfileRepository(newDataStore())

            assertThat(repository.profiles.first()).isEmpty()
            assertThat(repository.primaryProfileId.first()).isNull()
        }

    @Test
    fun `the first profile added becomes primary and round-trips its fields`() =
        runTest {
            val repository = DefaultProfileRepository(newDataStore())

            repository.upsert(leo)

            assertThat(repository.profiles.first()).containsExactly(leo)
            assertThat(repository.primaryProfileId.first()).isEqualTo("a")
        }

    @Test
    fun `adding more profiles keeps the primary and setPrimary switches it`() =
        runTest {
            val repository = DefaultProfileRepository(newDataStore())

            repository.upsert(leo)
            repository.upsert(mia)
            assertThat(repository.profiles.first()).containsExactly(leo, mia)
            assertThat(repository.primaryProfileId.first()).isEqualTo("a")

            repository.setPrimary("b")
            assertThat(repository.primaryProfileId.first()).isEqualTo("b")
        }

    @Test
    fun `removing the primary promotes another and removing the last clears it`() =
        runTest {
            val repository = DefaultProfileRepository(newDataStore())
            repository.upsert(leo)
            repository.upsert(mia)

            repository.remove("a")
            assertThat(repository.profiles.first()).containsExactly(mia)
            assertThat(repository.primaryProfileId.first()).isEqualTo("b")

            repository.remove("b")
            assertThat(repository.profiles.first()).isEmpty()
            assertThat(repository.primaryProfileId.first()).isNull()
        }

    private fun TestScope.newDataStore(): DataStore<Preferences> {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
        dataStoreScopes += scope
        return PreferenceDataStoreFactory.create(scope = scope) {
            tmpFolder.newFile("prefs-${testScheduler.currentTime}.preferences_pb")
        }
    }
}
