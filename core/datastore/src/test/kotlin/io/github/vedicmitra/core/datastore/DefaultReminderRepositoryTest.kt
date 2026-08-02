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
    fun `upsert adds reminders and replaces by id`() =
        runTest {
            val repository = DefaultReminderRepository(newDataStore())

            repository.upsert(reminder("muhurta:a", triggerAt = 100L))
            repository.upsert(reminder("muhurta:b", triggerAt = 200L))
            repository.upsert(reminder("muhurta:a", triggerAt = 999L)) // replaces the first

            val stored = repository.reminders.first()
            assertThat(stored.map { it.id }).containsExactly("muhurta:a", "muhurta:b")
            assertThat(stored.first { it.id == "muhurta:a" }.triggerAtEpochMillis).isEqualTo(999L)
        }

    @Test
    fun `remove drops a reminder by id`() =
        runTest {
            val repository = DefaultReminderRepository(newDataStore())
            repository.upsert(reminder("muhurta:a", triggerAt = 100L))
            repository.upsert(reminder("muhurta:b", triggerAt = 200L))

            repository.remove("muhurta:a")

            assertThat(repository.reminders.first().map { it.id }).containsExactly("muhurta:b")
        }

    @Test
    fun `removePast keeps only reminders after now`() =
        runTest {
            val repository = DefaultReminderRepository(newDataStore())
            repository.upsert(reminder("past", triggerAt = 100L))
            repository.upsert(reminder("now", triggerAt = 500L))
            repository.upsert(reminder("future", triggerAt = 900L))

            repository.removePast(nowEpochMillis = 500L)

            assertThat(repository.reminders.first().map { it.id }).containsExactly("future")
        }

    @Test
    fun `lead time defaults to 10 minutes and is settable`() =
        runTest {
            val repository = DefaultReminderRepository(newDataStore())

            assertThat(repository.leadTimeMinutes.first()).isEqualTo(10)

            repository.setLeadTimeMinutes(30)

            assertThat(repository.leadTimeMinutes.first()).isEqualTo(30)
        }

    private fun reminder(
        id: String,
        triggerAt: Long,
    ) = PersistedReminder(id = id, triggerAtEpochMillis = triggerAt, title = "T", body = "B")

    private fun kotlinx.coroutines.test.TestScope.newDataStore() =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job()),
        ) {
            tmpFolder.newFile("reminders-${testScheduler.currentTime}.preferences_pb")
        }
}
