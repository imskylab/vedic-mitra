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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vedicmitra.core.datastore.DarkThemeConfig
import io.github.vedicmitra.core.datastore.ThemeSettings
import io.github.vedicmitra.core.designsystem.component.VedicSelectField
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme

/**
 * Settings screen. Collects [SettingsViewModel] state and lets the user choose the theme mode and
 * toggle dynamic colour. The stateless [SettingsContent] is previewable and testable.
 */
@Composable
fun SettingsScreen(
    onNavigateToLocation: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (val state = uiState) {
        SettingsUiState.Loading -> Unit
        is SettingsUiState.Loaded ->
            SettingsContent(
                settings = state.settings,
                onDarkThemeConfigChange = viewModel::setDarkThemeConfig,
                onDynamicColorChange = viewModel::setDynamicColor,
                onNavigateToLocation = onNavigateToLocation,
                onNavigateToProfile = onNavigateToProfile,
                onNavigateToAbout = onNavigateToAbout,
                modifier = modifier,
            )
    }
}

@Composable
private fun SettingsContent(
    settings: ThemeSettings,
    onDarkThemeConfigChange: (DarkThemeConfig) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onNavigateToLocation: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        VedicSelectField(
            label = "Theme",
            options = DarkThemeConfig.entries,
            selected = settings.darkThemeConfig,
            optionLabel = { it.label },
            onSelect = onDarkThemeConfigChange,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Dynamic colour", modifier = Modifier.weight(1f))
            Switch(checked = settings.useDynamicColor, onCheckedChange = onDynamicColorChange)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Location", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToLocation)
                    .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Panchanga location", modifier = Modifier.weight(1f))
            Text(text = "Change", color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Profile", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToProfile)
                    .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Your birth profile", modifier = Modifier.weight(1f))
            Text(text = "Set up", color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "About", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToAbout)
                    .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "About Vedic Mitra", modifier = Modifier.weight(1f))
            Text(text = "View", color = MaterialTheme.colorScheme.primary)
        }
    }
}

private val DarkThemeConfig.label: String
    get() =
        when (this) {
            DarkThemeConfig.FOLLOW_SYSTEM -> "System default"
            DarkThemeConfig.LIGHT -> "Light"
            DarkThemeConfig.DARK -> "Dark"
        }

@Preview
@Composable
private fun SettingsContentPreview() {
    VedicMitraTheme {
        SettingsContent(
            settings = ThemeSettings(darkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM, useDynamicColor = true),
            onDarkThemeConfigChange = {},
            onDynamicColorChange = {},
            onNavigateToLocation = {},
            onNavigateToProfile = {},
            onNavigateToAbout = {},
        )
    }
}
