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
class DefaultJapaRepositoryTest {
    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val dataStoreScopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        dataStoreScopes.forEach { it.cancel() }
    }

    @Test
    fun `defaults to no sessions and no progress`() =
        runTest {
            val repository = DefaultJapaRepository(newDataStore())

            assertThat(repository.sessions.first()).isEmpty()
            assertThat(repository.inProgress.first()).isNull()
        }

    @Test
    fun `saved progress round-trips and clears`() =
        runTest {
            val repository = DefaultJapaRepository(newDataStore())

            repository.saveProgress(JapaProgress(mantraId = "gayatri", beads = 42))
            assertThat(repository.inProgress.first()).isEqualTo(JapaProgress("gayatri", 42))

            repository.clearProgress()
            assertThat(repository.inProgress.first()).isNull()
        }

    @Test
    fun `completing a session appends it newest-first and clears progress`() =
        runTest {
            val repository = DefaultJapaRepository(newDataStore())
            repository.saveProgress(JapaProgress(mantraId = "gayatri", beads = 108))

            val older = session(completedAt = 1_000L, beads = 108, rounds = 1)
            val newer = session(completedAt = 2_000L, beads = 216, rounds = 2)
            repository.completeSession(older)
            repository.completeSession(newer)

            assertThat(repository.sessions.first()).containsExactly(newer, older).inOrder()
            assertThat(repository.inProgress.first()).isNull()
        }

    private fun session(
        completedAt: Long,
        beads: Int,
        rounds: Int,
    ) = JapaSession(
        completedAtEpochMillis = completedAt,
        dateEpochDay = 20_301L,
        mantraId = "gayatri",
        beads = beads,
        rounds = rounds,
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
