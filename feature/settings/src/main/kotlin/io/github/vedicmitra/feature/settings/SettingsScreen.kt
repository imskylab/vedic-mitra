/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

package io.github.vedicmitra.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme

/**
 * Settings screen. Collects [SettingsViewModel] state and lets the user choose the theme mode and
 * toggle dynamic colour. The stateless [SettingsContent] is previewable and testable.
 */
@Composable
fun SettingsScreen(
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
                modifier = modifier,
            )
    }
}

@Composable
private fun SettingsContent(
    settings: ThemeSettings,
    onDarkThemeConfigChange: (DarkThemeConfig) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        Text(text = "Theme", style = MaterialTheme.typography.titleMedium)
        DarkThemeConfig.entries.forEach { config ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = settings.darkThemeConfig == config,
                            onClick = { onDarkThemeConfigChange(config) },
                        ).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = settings.darkThemeConfig == config, onClick = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = config.label)
            }
        }

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
        )
    }
}
