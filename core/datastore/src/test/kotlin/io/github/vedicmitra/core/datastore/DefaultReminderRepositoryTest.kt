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
import io.github.vedicmitra.core.common.model.AlertStyle
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
    fun `offsetMinutesByName has no overrides by default`() =
        runTest {
            val repository = DefaultReminderRepository(newDataStore())

            assertThat(repository.offsetMinutesByName.first()).isEmpty()
        }

    @Test
    fun `setOffsetMinutes stores and exposes an override`() =
        runTest {
            val repository = DefaultReminderRepository(newDataStore())

            repository.setOffsetMinutes("Brahma Muhurta", 30)

            assertThat(repository.offsetMinutesByName.first()).containsEntry("Brahma Muhurta", 30)
        }

    @Test
    fun `setOffsetMinutes replaces an existing override for the same name`() =
        runTest {
            val repository = DefaultReminderRepository(newDataStore())

            repository.setOffsetMinutes("Brahma Muhurta", 30)
            repository.setOffsetMinutes("Brahma Muhurta", 15)

            assertThat(repository.offsetMinutesByName.first()).containsEntry("Brahma Muhurta", 15)
        }

    @Test
    fun `setOffsetMinutes keeps overrides for other names independent`() =
        runTest {
            val repository = DefaultReminderRepository(newDataStore())

            repository.setOffsetMinutes("Brahma Muhurta", 30)
            repository.setOffsetMinutes("Rahu Kalam", 5)

            assertThat(repository.offsetMinutesByName.first())
                .containsExactly("Brahma Muhurta", 30, "Rahu Kalam", 5)
        }

    @Test
    fun `alertTypeByName has no overrides by default`() =
        runTest {
            val repository = DefaultReminderRepository(newDataStore())

            assertThat(repository.alertTypeByName.first()).isEmpty()
        }

    @Test
    fun `setAlertType stores and replaces the alert style for a name`() =
        runTest {
            val repository = DefaultReminderRepository(newDataStore())

            repository.setAlertType("Brahma Muhurta", AlertStyle.ALARM)
            repository.setAlertType("Rahu Kalam", AlertStyle.NOTIFICATION)
            repository.setAlertType("Brahma Muhurta", AlertStyle.NOTIFICATION) // replaces the first

            assertThat(repository.alertTypeByName.first())
                .containsExactly("Brahma Muhurta", AlertStyle.NOTIFICATION, "Rahu Kalam", AlertStyle.NOTIFICATION)
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
