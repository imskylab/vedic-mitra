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
class DefaultReminderRepositoryTest {
    @get:Rule
    val tmpFolder = TemporaryFolder()

    @Test
    fun `enables and disables reminder ids`() =
        runTest {
            val repository = DefaultReminderRepository(newDataStore())

            repository.setEnabled("muhurta:abhijit", enabled = true)
            repository.setEnabled("muhurta:brahma", enabled = true)
            repository.setEnabled("muhurta:abhijit", enabled = false)

            assertThat(repository.enabledReminderIds.first()).containsExactly("muhurta:brahma")
        }

    @Test
    fun `disabling an id that was never enabled is a no-op`() =
        runTest {
            val repository = DefaultReminderRepository(newDataStore())

            repository.setEnabled("muhurta:abhijit", enabled = false)

            assertThat(repository.enabledReminderIds.first()).isEmpty()
        }

    private fun kotlinx.coroutines.test.TestScope.newDataStore() =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job()),
        ) {
            tmpFolder.newFile("reminders-${testScheduler.currentTime}.preferences_pb")
        }
}
