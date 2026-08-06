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
class DefaultUserPreferencesRepositoryTest {
    @get:Rule
    val tmpFolder = TemporaryFolder()

    @Test
    fun `defaults to follow-system and the brand palette (dynamic colour off)`() =
        runTest {
            val repository = DefaultUserPreferencesRepository(newDataStore())

            val settings = repository.themeSettings.first()

            assertThat(settings.darkThemeConfig).isEqualTo(DarkThemeConfig.FOLLOW_SYSTEM)
            // Dynamic colour is off by default so the golden brand palette shows out of the box.
            assertThat(settings.useDynamicColor).isFalse()
        }

    @Test
    fun `persists theme preference changes`() =
        runTest {
            val repository = DefaultUserPreferencesRepository(newDataStore())

            repository.setDarkThemeConfig(DarkThemeConfig.DARK)
            repository.setDynamicColor(true)

            val settings = repository.themeSettings.first()
            assertThat(settings.darkThemeConfig).isEqualTo(DarkThemeConfig.DARK)
            assertThat(settings.useDynamicColor).isTrue()
        }

    private fun kotlinx.coroutines.test.TestScope.newDataStore() =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job()),
        ) {
            tmpFolder.newFile("prefs-${testScheduler.currentTime}.preferences_pb")
        }
}
