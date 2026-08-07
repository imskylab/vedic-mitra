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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.vedicmitra.core.datastore.DarkThemeConfig
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme
import io.github.vedicmitra.feature.alarm.AlarmScreen
import io.github.vedicmitra.feature.calendar.CalendarScreen
import io.github.vedicmitra.feature.home.HomeScreen
import io.github.vedicmitra.feature.location.AddCityScreen
import io.github.vedicmitra.feature.location.AddCoordinatesScreen
import io.github.vedicmitra.feature.location.LocationScreen
import io.github.vedicmitra.feature.settings.SettingsScreen

/** The app's top-level destinations, shown in the bottom navigation bar in this order. */
private enum class TopDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME("home", "Home", Icons.Filled.Home),
    CALENDAR("calendar", "Calendar", Icons.Filled.DateRange),
    ALARM("alarm", "Reminders", Icons.Filled.Notifications),
    SETTINGS("settings", "Settings", Icons.Filled.Settings),
}

// Non-tab routes reached from Settings, for managing the panchanga location.
private const val LOCATION_ROUTE = "settings/location"
private const val ADD_CITY_ROUTE = "settings/location/add-city"
private const val ADD_COORDINATES_ROUTE = "settings/location/add-coordinates"

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VedicMitraApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val current = TopDestination.entries.firstOrNull { it.route == currentRoute }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text(current?.label ?: "Vedic Mitra") })
        },
        bottomBar = {
            NavigationBar {
                TopDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                // Single instance per tab; preserve/restore each tab's own state.
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TopDestination.HOME.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(TopDestination.HOME.route) {
                HomeScreen(
                    onNavigateToLocation = { navController.navigate(LOCATION_ROUTE) },
                )
            }
            composable(TopDestination.CALENDAR.route) { CalendarScreen() }
            composable(TopDestination.ALARM.route) { AlarmScreen() }
            composable(TopDestination.SETTINGS.route) {
                SettingsScreen(onNavigateToLocation = { navController.navigate(LOCATION_ROUTE) })
            }
            composable(LOCATION_ROUTE) {
                LocationScreen(
                    onAddCity = { navController.navigate(ADD_CITY_ROUTE) },
                    onAddCoordinates = { navController.navigate(ADD_COORDINATES_ROUTE) },
                )
            }
            composable(ADD_CITY_ROUTE) { AddCityScreen(onDone = { navController.popBackStack() }) }
            composable(ADD_COORDINATES_ROUTE) { AddCoordinatesScreen(onDone = { navController.popBackStack() }) }
        }
    }
}
