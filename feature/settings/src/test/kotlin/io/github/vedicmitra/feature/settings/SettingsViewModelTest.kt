/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.settings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.vedicmitra.core.common.model.MaasaReckoning
import io.github.vedicmitra.core.datastore.DarkThemeConfig
import io.github.vedicmitra.core.datastore.ThemeSettings
import io.github.vedicmitra.core.datastore.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `exposes the loaded theme settings`() =
        runTest {
            val settings = ThemeSettings(darkThemeConfig = DarkThemeConfig.DARK, useDynamicColor = false)
            val viewModel = SettingsViewModel(FakeUserPreferencesRepository(settings))

            viewModel.uiState.test {
                var item = awaitItem()
                while (item is SettingsUiState.Loading) item = awaitItem()
                assertThat(item).isEqualTo(SettingsUiState.Loaded(settings, MaasaReckoning.AMANTA))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `setDarkThemeConfig persists via the repository`() =
        runTest {
            val repository =
                FakeUserPreferencesRepository(
                    ThemeSettings(darkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM, useDynamicColor = true),
                )
            val viewModel = SettingsViewModel(repository)

            viewModel.setDarkThemeConfig(DarkThemeConfig.LIGHT)

            assertThat(repository.themeSettings.first().darkThemeConfig).isEqualTo(DarkThemeConfig.LIGHT)
        }

    @Test
    fun `setDynamicColor persists via the repository`() =
        runTest {
            val repository =
                FakeUserPreferencesRepository(
                    ThemeSettings(darkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM, useDynamicColor = true),
                )
            val viewModel = SettingsViewModel(repository)

            viewModel.setDynamicColor(false)

            assertThat(repository.themeSettings.first().useDynamicColor).isFalse()
        }

    @Test
    fun `setMaasaReckoning persists via the repository`() =
        runTest {
            val repository =
                FakeUserPreferencesRepository(
                    ThemeSettings(darkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM, useDynamicColor = true),
                )
            val viewModel = SettingsViewModel(repository)

            viewModel.setMaasaReckoning(MaasaReckoning.PURNIMANTA)

            assertThat(repository.maasaReckoning.first()).isEqualTo(MaasaReckoning.PURNIMANTA)
        }

    @Test
    fun `defaults to amanta, so an existing reading never changes without being asked`() =
        runTest {
            val repository =
                FakeUserPreferencesRepository(
                    ThemeSettings(darkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM, useDynamicColor = true),
                )

            assertThat(repository.maasaReckoning.first()).isEqualTo(MaasaReckoning.AMANTA)
        }
}

private class FakeUserPreferencesRepository(
    initial: ThemeSettings,
) : UserPreferencesRepository {
    private val settings = MutableStateFlow(initial)
    private val reckoningState = MutableStateFlow(MaasaReckoning.AMANTA)

    override val themeSettings: Flow<ThemeSettings> = settings.asStateFlow()
    override val maasaReckoning: Flow<MaasaReckoning> = reckoningState.asStateFlow()

    override suspend fun setDarkThemeConfig(config: DarkThemeConfig) {
        settings.update { it.copy(darkThemeConfig = config) }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        settings.update { it.copy(useDynamicColor = enabled) }
    }

    override suspend fun setMaasaReckoning(reckoning: MaasaReckoning) {
        reckoningState.value = reckoning
    }
}
