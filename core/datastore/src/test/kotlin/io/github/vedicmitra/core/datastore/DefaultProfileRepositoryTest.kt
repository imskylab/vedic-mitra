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
class DefaultProfileRepositoryTest {
    @get:Rule
    val tmpFolder = TemporaryFolder()

    // DataStore keeps its backing file open on this scope; cancel it after each test so the temp
    // file is released promptly (an open handle intermittently trips AccessDeniedException on Windows).
    private val dataStoreScopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        dataStoreScopes.forEach { it.cancel() }
    }

    @Test
    fun `defaults to an empty, incomplete profile`() =
        runTest {
            val repository = DefaultProfileRepository(newDataStore())

            val profile = repository.profile.first()

            assertThat(profile).isEqualTo(UserProfile())
            assertThat(profile.isComplete).isFalse()
        }

    @Test
    @Suppress("MagicNumber")
    fun `persists and reads back the profile`() =
        runTest {
            val repository = DefaultProfileRepository(newDataStore())
            val saved =
                UserProfile(
                    name = "Leo",
                    dateOfBirth = LocalDate.of(1995, 3, 14),
                    timeOfBirth = LocalTime.of(9, 30),
                    placeOfBirth = "Hyderabad, India",
                )

            repository.setProfile(saved)

            val loaded = repository.profile.first()
            assertThat(loaded).isEqualTo(saved)
            assertThat(loaded.isComplete).isTrue()
        }

    private fun TestScope.newDataStore(): DataStore<Preferences> {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
        dataStoreScopes += scope
        return PreferenceDataStoreFactory.create(scope = scope) {
            tmpFolder.newFile("prefs-${testScheduler.currentTime}.preferences_pb")
        }
    }
}
