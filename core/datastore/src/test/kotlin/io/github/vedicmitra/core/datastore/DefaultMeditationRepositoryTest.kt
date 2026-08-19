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

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("MagicNumber")
class DefaultMeditationRepositoryTest {
    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val dataStoreScopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        dataStoreScopes.forEach { it.cancel() }
    }

    @Test
    fun `defaults to no sits`() =
        runTest {
            assertThat(DefaultMeditationRepository(newDataStore()).sessions.first()).isEmpty()
        }

    @Test
    fun `added sits are stored newest-first`() =
        runTest {
            val repository = DefaultMeditationRepository(newDataStore())
            val older = session(completedAt = 1_000L, seconds = 600)
            val newer = session(completedAt = 2_000L, seconds = 900)

            repository.add(older)
            repository.add(newer)

            assertThat(repository.sessions.first()).containsExactly(newer, older).inOrder()
        }

    private fun session(
        completedAt: Long,
        seconds: Int,
    ) = MeditationSession(
        completedAtEpochMillis = completedAt,
        dateEpochDay = 20_301L,
        durationSeconds = seconds,
        nakshatraNumber = 5,
        tithiNumber = 11,
    )

    private fun TestScope.newDataStore(): DataStore<Preferences> {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
        dataStoreScopes += scope
        return PreferenceDataStoreFactory.create(scope = scope) {
            tmpFolder.newFile("prefs-${testScheduler.currentTime}.preferences_pb")
        }
    }
}
