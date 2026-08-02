/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.vedicmitra.core.datastore.DarkThemeConfig
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme
import io.github.vedicmitra.feature.alarm.AlarmScreen
import io.github.vedicmitra.feature.home.HomeScreen
import io.github.vedicmitra.feature.settings.SettingsScreen

private const val HOME_ROUTE = "home"
private const val SETTINGS_ROUTE = "settings"
private const val ALARM_ROUTE = "alarm"

/**
 * Single-activity host. Applies the user's persisted theme to the whole UI and hosts the
 * navigation graph. Annotated with [AndroidEntryPoint] so Hilt can inject the ViewModels it hosts.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val theme by viewModel.themeSettings.collectAsStateWithLifecycle()
            val darkTheme =
                when (theme.darkThemeConfig) {
                    DarkThemeConfig.FOLLOW_SYSTEM -> isSystemInDarkTheme()
                    DarkThemeConfig.LIGHT -> false
                    DarkThemeConfig.DARK -> true
                }
            VedicMitraTheme(darkTheme = darkTheme, dynamicColor = theme.useDynamicColor) {
                VedicMitraApp()
            }
        }
    }
}

@Composable
private fun VedicMitraApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HOME_ROUTE) {
        composable(HOME_ROUTE) {
            AppScaffold(
                title = "Vedic Mitra",
                action = {
                    TextButton(onClick = { navController.navigate(ALARM_ROUTE) }) { Text("Reminders") }
                    TextButton(onClick = { navController.navigate(SETTINGS_ROUTE) }) { Text("Settings") }
                },
            ) { padding -> HomeScreen(modifier = Modifier.padding(padding)) }
        }
        composable(ALARM_ROUTE) {
            AppScaffold(
                title = "Reminders",
                navigation = { TextButton(onClick = { navController.popBackStack() }) { Text("Back") } },
            ) { padding -> AlarmScreen(modifier = Modifier.padding(padding)) }
        }
        composable(SETTINGS_ROUTE) {
            AppScaffold(
                title = "Settings",
                navigation = { TextButton(onClick = { navController.popBackStack() }) { Text("Back") } },
            ) { padding -> SettingsScreen(modifier = Modifier.padding(padding)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppScaffold(
    title: String,
    modifier: Modifier = Modifier,
    navigation: @Composable () -> Unit = {},
    action: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = navigation,
                actions = { action() },
            )
        },
        content = content,
    )
}
