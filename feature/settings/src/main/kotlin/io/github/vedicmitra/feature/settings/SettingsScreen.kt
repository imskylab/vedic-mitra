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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import io.github.vedicmitra.core.common.model.MaasaReckoning
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
                state = state,
                onDarkThemeConfigChange = viewModel::setDarkThemeConfig,
                onDynamicColorChange = viewModel::setDynamicColor,
                onMaasaReckoningChange = viewModel::setMaasaReckoning,
                onNavigate = { destination ->
                    when (destination) {
                        SettingsDestination.LOCATION -> onNavigateToLocation()
                        SettingsDestination.PROFILE -> onNavigateToProfile()
                        SettingsDestination.ABOUT -> onNavigateToAbout()
                    }
                },
                modifier = modifier,
            )
    }
}

/**
 * Where a settings row can send the reader.
 *
 * The three navigation callbacks are grouped behind one lambda so that adding a preference does not
 * push this screen's parameter list past what detekt allows — and because they were three
 * pass-throughs doing the same job.
 */
private enum class SettingsDestination { LOCATION, PROFILE, ABOUT }

@Composable
private fun SettingsContent(
    state: SettingsUiState.Loaded,
    onDarkThemeConfigChange: (DarkThemeConfig) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onMaasaReckoningChange: (MaasaReckoning) -> Unit,
    onNavigate: (SettingsDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        VedicSelectField(
            label = "Theme",
            options = DarkThemeConfig.entries,
            selected = state.settings.darkThemeConfig,
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
            Switch(checked = state.settings.useDynamicColor, onCheckedChange = onDynamicColorChange)
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSectionHeader(text = "Panchanga")
        VedicSelectField(
            label = "Month scheme",
            options = MaasaReckoning.entries,
            selected = state.maasaReckoning,
            optionLabel = { it.label },
            onSelect = onMaasaReckoningChange,
        )
        Text(
            text =
                "Both schemes describe the same days. They differ only in what the dark fortnight " +
                    "is called — under purnimanta it carries the next month's name.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSectionHeader(text = "Location")
        SettingsActionRow(
            label = "Panchanga location",
            action = "Change",
            onClick = { onNavigate(SettingsDestination.LOCATION) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSectionHeader(text = "Profile")
        SettingsActionRow(
            label = "Your birth profile",
            action = "Set up",
            onClick = { onNavigate(SettingsDestination.PROFILE) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSectionHeader(text = "About")
        SettingsActionRow(
            label = "About Vedic Mitra",
            action = "View",
            onClick = { onNavigate(SettingsDestination.ABOUT) },
        )
    }
}

private val DarkThemeConfig.label: String
    get() =
        when (this) {
            DarkThemeConfig.FOLLOW_SYSTEM -> "System default"
            DarkThemeConfig.LIGHT -> "Light"
            DarkThemeConfig.DARK -> "Dark"
        }

// Named by where the month ends, because that is the difference and it is what a reader recognises
// from their own almanac -- "amanta" and "purnimanta" alone mean nothing to someone meeting them here.
private val MaasaReckoning.label: String
    get() =
        when (this) {
            MaasaReckoning.AMANTA -> "Amanta — ends at the new moon"
            MaasaReckoning.PURNIMANTA -> "Purnimanta — ends at the full moon"
        }

@Preview
@Composable
private fun SettingsContentPreview() {
    VedicMitraTheme {
        SettingsContent(
            state =
                SettingsUiState.Loaded(
                    settings = ThemeSettings(darkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM, useDynamicColor = true),
                    maasaReckoning = MaasaReckoning.AMANTA,
                ),
            onDarkThemeConfigChange = {},
            onDynamicColorChange = {},
            onMaasaReckoningChange = {},
            onNavigate = {},
        )
    }
}
