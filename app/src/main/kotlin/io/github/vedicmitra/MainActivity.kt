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
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import io.github.vedicmitra.R
import io.github.vedicmitra.core.datastore.DarkThemeConfig
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme
import io.github.vedicmitra.feature.alarm.AlarmScreen
import io.github.vedicmitra.feature.calendar.CalendarScreen
import io.github.vedicmitra.feature.home.HomeScreen
import io.github.vedicmitra.feature.japa.JapaScreen
import io.github.vedicmitra.feature.kundali.KundaliScreen
import io.github.vedicmitra.feature.location.AddCityScreen
import io.github.vedicmitra.feature.location.AddCoordinatesScreen
import io.github.vedicmitra.feature.location.LocationScreen
import io.github.vedicmitra.feature.matchmaking.MatchmakingScreen
import io.github.vedicmitra.feature.meditation.MeditationScreen
import io.github.vedicmitra.feature.muhurat.MuhuratActivitiesScreen
import io.github.vedicmitra.feature.muhurat.MuhuratCategoriesScreen
import io.github.vedicmitra.feature.muhurat.MuhuratDayScreen
import io.github.vedicmitra.feature.muhurat.MuhuratResultsScreen
import io.github.vedicmitra.feature.profile.ProfileEditScreen
import io.github.vedicmitra.feature.profile.ProfileListScreen
import io.github.vedicmitra.feature.rashifal.RashifalScreen
import io.github.vedicmitra.feature.settings.AboutScreen
import io.github.vedicmitra.feature.settings.SettingsScreen
import io.github.vedicmitra.feature.settings.SupportScreen
import io.github.vedicmitra.feature.stotra.StotraScreen

/** The app's top-level destinations, shown in the bottom navigation bar in this order. */
private enum class TopDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME("home", "Home", Icons.Filled.Home),
    SETTINGS("settings", "Settings", Icons.Filled.Settings),
    PROFILE("profile", "Profile", Icons.Filled.Person),
}

// Sub-routes pushed on top of a tab; reached from the Home hub tiles or Settings. They are not
// tabs (no bottom-bar entry), so they're returned from via the top-bar back button or system back.
private const val CALENDAR_ROUTE = "calendar"
private const val ALARM_ROUTE = "alarm"
private const val KUNDALI_ROUTE = "kundali"
private const val MATCHMAKING_ROUTE = "matchmaking"
private const val RASHIFAL_ROUTE = "rashifal"
private const val JAPA_ROUTE = "japa"
private const val MEDITATION_ROUTE = "meditation"
private const val STOTRA_ROUTE = "stotra"
private const val LOCATION_ROUTE = "settings/location"
private const val ADD_CITY_ROUTE = "settings/location/add-city"
private const val ADD_COORDINATES_ROUTE = "settings/location/add-coordinates"
private const val PROFILE_EDIT_ROUTE = "profile/edit"
private const val PROFILE_ID_ARG = "profileId"
private const val MUHURAT_ROUTE = "muhurat"
private const val MUHURAT_ACTIVITIES_ROUTE = "muhurat/activities"
private const val MUHURAT_RESULTS_ROUTE = "muhurat/results"
private const val MUHURAT_DAY_ROUTE = "muhurat/day"
private const val MUHURAT_CATEGORY_ARG = "category"
private const val MUHURAT_ACTIVITY_ARG = "activity"
private const val MUHURAT_DAY_ARG = "day"
private const val ABOUT_ROUTE = "settings/about"
private const val SUPPORT_ROUTE = "settings/support"

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
                AppRoot()
            }
        }
    }
}

/** Plays the intro splash on each launch, then shows the app; a tap anywhere skips the splash. */
@Composable
private fun AppRoot() {
    var splashDone by rememberSaveable { mutableStateOf(false) }
    if (splashDone) {
        VedicMitraApp()
    } else {
        SplashScreen(onFinished = { splashDone = true })
    }
}

/** A full-screen intro video (muted). It finishes on its own, or a tap/back skips it. */
@Composable
private fun SplashScreen(onFinished: () -> Unit) {
    BackHandler(onBack = onFinished)
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().clipToBounds().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        // Size the player to *cover* the screen at the video's aspect ratio so it fills edge-to-edge
        // (the overflow is clipped) rather than letterboxing on tall phones.
        val coverModifier =
            if (maxWidth / maxHeight > SPLASH_VIDEO_ASPECT) {
                Modifier.width(maxWidth).height(maxWidth / SPLASH_VIDEO_ASPECT)
            } else {
                Modifier.height(maxHeight).width(maxHeight * SPLASH_VIDEO_ASPECT)
            }
        AndroidView(
            modifier = coverModifier,
            factory = { context ->
                VideoView(context).apply {
                    setVideoURI("android.resource://${context.packageName}/${R.raw.splash_intro}".toUri())
                    setOnPreparedListener { player ->
                        player.setVolume(0f, 0f)
                        start()
                    }
                    setOnCompletionListener { onFinished() }
                    setOnErrorListener { _, _, _ ->
                        onFinished()
                        true
                    }
                }
            },
        )
        Text(
            text = "Tap to skip",
            color = Color.White.copy(alpha = SKIP_HINT_ALPHA),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
        )
        // A transparent overlay on top of the video surface so a tap anywhere is reliably caught.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { detectTapGestures { onFinished() } },
        )
    }
}

// The bundled splash clip is 720x1280 (portrait 9:16); width / height.
private const val SPLASH_VIDEO_ASPECT = 0.5625f
private const val SKIP_HINT_ALPHA = 0.7f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VedicMitraApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val current = TopDestination.entries.firstOrNull { it.route == currentRoute }

    // System back should retrace the in-app journey and only leave the app from the Home landing.
    // Handled explicitly so it is reliable regardless of the platform's predictive-back path.
    BackHandler(enabled = currentRoute != TopDestination.HOME.route) {
        if (!navController.popBackStack()) navController.navigateToTab(TopDestination.HOME.route)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(current?.label ?: "Vedic Mitra") },
                navigationIcon = {
                    // Sub-routes (calendar, reminders, kundali, …) aren't tabs, so offer a back arrow.
                    if (current == null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                TopDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = { navController.navigateToTab(destination.route) },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        AppNavHost(navController = navController, modifier = Modifier.padding(padding))
    }
}

/**
 * Navigates to a bottom-bar [route], clearing any pushed sub-route (Panchang, Reminders, Kundali, …)
 * back to the tab's root. Used by the bottom bar and by in-app shortcuts that jump to a tab.
 *
 * Deliberately does **not** save/restore per-tab state: the tabs are single screens and the
 * sub-routes are pushed on top of Home at the graph root, so restoring Home's saved stack would bring
 * a sub-route back instead of showing the Home landing — i.e. the Home tab would appear to do nothing.
 * Popping to the start destination and launching single-top always lands on the tab's root.
 */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = false }
        launchSingleTop = true
        restoreState = false
    }
}

/** The app's navigation graph — the top-level tabs plus the Settings sub-routes. */
@Composable
private fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TopDestination.HOME.route,
        modifier = modifier,
    ) {
        composable(TopDestination.HOME.route) {
            HomeScreen(
                onNavigateToLocation = { navController.navigate(LOCATION_ROUTE) },
                onOpenCalendar = { navController.navigate(CALENDAR_ROUTE) },
                onOpenReminders = { navController.navigate(ALARM_ROUTE) },
                onOpenKundali = { navController.navigate(KUNDALI_ROUTE) },
                onOpenMuhurat = { navController.navigate(MUHURAT_ROUTE) },
                onOpenMatch = { navController.navigate(MATCHMAKING_ROUTE) },
                onOpenRashifal = { navController.navigate(RASHIFAL_ROUTE) },
                onOpenJapa = { navController.navigate(JAPA_ROUTE) },
                onOpenMeditate = { navController.navigate(MEDITATION_ROUTE) },
                onOpenStotra = { navController.navigate(STOTRA_ROUTE) },
            )
        }
        composable(TopDestination.SETTINGS.route) {
            SettingsScreen(
                onNavigateToLocation = { navController.navigate(LOCATION_ROUTE) },
                onNavigateToProfile = { navController.navigateToTab(TopDestination.PROFILE.route) },
                onNavigateToSupport = { navController.navigate(SUPPORT_ROUTE) },
                onNavigateToAbout = { navController.navigate(ABOUT_ROUTE) },
            )
        }
        composable(ABOUT_ROUTE) {
            AboutScreen(onNavigateToSupport = { navController.navigate(SUPPORT_ROUTE) })
        }
        composable(SUPPORT_ROUTE) { SupportScreen() }
        composable(TopDestination.PROFILE.route) {
            ProfileListScreen(
                onAddProfile = { navController.navigate(PROFILE_EDIT_ROUTE) },
                onEditProfile = { id -> navController.navigate("$PROFILE_EDIT_ROUTE?$PROFILE_ID_ARG=$id") },
            )
        }
        composable(CALENDAR_ROUTE) { CalendarScreen() }
        composable(ALARM_ROUTE) { AlarmScreen() }
        composable(LOCATION_ROUTE) {
            LocationScreen(
                onAddCity = { navController.navigate(ADD_CITY_ROUTE) },
                onAddCoordinates = { navController.navigate(ADD_COORDINATES_ROUTE) },
            )
        }
        composable(ADD_CITY_ROUTE) { AddCityScreen(onDone = { navController.popBackStack() }) }
        composable(ADD_COORDINATES_ROUTE) { AddCoordinatesScreen(onDone = { navController.popBackStack() }) }
        composable(
            route = "$PROFILE_EDIT_ROUTE?$PROFILE_ID_ARG={$PROFILE_ID_ARG}",
            arguments =
                listOf(
                    navArgument(PROFILE_ID_ARG) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
        ) { ProfileEditScreen(onDone = { navController.popBackStack() }) }
        composable(KUNDALI_ROUTE) {
            KundaliScreen(onSetUpProfile = { navController.navigateToTab(TopDestination.PROFILE.route) })
        }
        composable(MATCHMAKING_ROUTE) {
            MatchmakingScreen(onSetUpProfiles = { navController.navigateToTab(TopDestination.PROFILE.route) })
        }
        composable(RASHIFAL_ROUTE) {
            RashifalScreen(onSetUpProfile = { navController.navigateToTab(TopDestination.PROFILE.route) })
        }
        composable(JAPA_ROUTE) { JapaScreen() }
        composable(MEDITATION_ROUTE) { MeditationScreen() }
        composable(STOTRA_ROUTE) { StotraScreen() }
        muhuratDestinations(navController)
    }
}

/** The muhurta "find best dates" flow: category grid → activity list → ranked results. */
private fun NavGraphBuilder.muhuratDestinations(navController: NavHostController) {
    composable(MUHURAT_ROUTE) {
        MuhuratCategoriesScreen(
            onOpenCategory = { categoryName ->
                navController.navigate("$MUHURAT_ACTIVITIES_ROUTE/$categoryName")
            },
        )
    }
    composable(
        route = "$MUHURAT_ACTIVITIES_ROUTE/{$MUHURAT_CATEGORY_ARG}",
        arguments = listOf(navArgument(MUHURAT_CATEGORY_ARG) { type = NavType.StringType }),
    ) { entry ->
        MuhuratActivitiesScreen(
            categoryName = entry.arguments?.getString(MUHURAT_CATEGORY_ARG).orEmpty(),
            onOpenActivity = { activityName ->
                navController.navigate("$MUHURAT_RESULTS_ROUTE/$activityName")
            },
        )
    }
    composable(
        route = "$MUHURAT_RESULTS_ROUTE/{$MUHURAT_ACTIVITY_ARG}",
        arguments = listOf(navArgument(MUHURAT_ACTIVITY_ARG) { type = NavType.StringType }),
    ) {
        MuhuratResultsScreen(
            onOpenDay = { activityName, millis ->
                navController.navigate("$MUHURAT_DAY_ROUTE/$activityName/$millis")
            },
        )
    }
    composable(
        route = "$MUHURAT_DAY_ROUTE/{$MUHURAT_ACTIVITY_ARG}/{$MUHURAT_DAY_ARG}",
        arguments =
            listOf(
                navArgument(MUHURAT_ACTIVITY_ARG) { type = NavType.StringType },
                navArgument(MUHURAT_DAY_ARG) { type = NavType.LongType },
            ),
    ) { MuhuratDayScreen() }
}
