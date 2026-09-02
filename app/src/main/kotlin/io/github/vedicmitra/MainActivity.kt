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
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.painterResource
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
import io.github.vedicmitra.core.designsystem.icon.VedicIcons
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme
import io.github.vedicmitra.feature.alarm.AlarmScreen
import io.github.vedicmitra.feature.calendar.CalendarScreen
import io.github.vedicmitra.feature.home.EventsScreen
import io.github.vedicmitra.feature.home.FestivalsScreen
import io.github.vedicmitra.feature.home.HomeScreen
import io.github.vedicmitra.feature.home.hub.DomainScreen
import io.github.vedicmitra.feature.home.hub.HubDomain
import io.github.vedicmitra.feature.home.hub.HubTarget
import io.github.vedicmitra.feature.home.PanchangScreen
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

/**
 * The app's top-level destinations, shown in the bottom navigation bar in this order.
 *
 * Most tabs are navigation chrome and use Material symbols, per the rule in [VedicIcons] that keeps
 * the ornate brand glyphs for signature features. Support is the exception: giving away money is a
 * cultural act (daana), so it carries the brand glyph.
 */
internal enum class TopDestination(
    val route: String,
    val label: String,
    val icon: ImageVector? = null,
    @param:DrawableRes val glyph: Int? = null,
) {
    HOME("home", "Home", icon = Icons.Filled.Home),
    SETTINGS("settings", "Settings", icon = Icons.Filled.Settings),
    PROFILE("profile", "Profile", icon = Icons.Filled.Person),
    SUPPORT("support", "Support", glyph = VedicIcons.support),
}

/**
 * The app bar's two-line title: the app's name, with the open destination beneath it.
 *
 * Every destination reads the same way — the brand on top, where you are underneath — rather than
 * some showing a tab label, some the app name, and some nothing at all.
 */
@Composable
private fun AppBarTitle(subtitle: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Vedic Mitra", style = MaterialTheme.typography.titleMedium)
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * What to show beneath the app name for [route].
 *
 * Routes carrying arguments arrive here as their declared pattern — `profile/edit?profileId={…}`,
 * `muhurat/day/{activity}/{day}` — so the argument part is trimmed before the lookup.
 */
internal fun subtitleOf(route: String?): String? {
    val base = route?.substringBefore('?')?.substringBefore("/{") ?: return null
    return DESTINATION_LABELS[base]
}

internal val DESTINATION_LABELS: Map<String, String> =
    mapOf(
        TopDestination.HOME.route to TopDestination.HOME.label,
        TopDestination.SETTINGS.route to TopDestination.SETTINGS.label,
        TopDestination.PROFILE.route to TopDestination.PROFILE.label,
        TopDestination.SUPPORT.route to TopDestination.SUPPORT.label,
        ABOUT_ROUTE to "About",
        LOCATION_ROUTE to "Locations",
        ADD_CITY_ROUTE to "Add a city",
        ADD_COORDINATES_ROUTE to "Add coordinates",
        PROFILE_EDIT_ROUTE to "Birth profile",
        PANCHANG_ROUTE to "Today's Panchang",
        FESTIVALS_ROUTE to "Festivals",
        EVENTS_ROUTE to "Events",
        CALENDAR_ROUTE to "Panchang calendar",
        ALARM_ROUTE to "Reminders",
        KUNDALI_ROUTE to "Kundali",
        MATCHMAKING_ROUTE to "Kundali matching",
        RASHIFAL_ROUTE to "Rashifal",
        JAPA_ROUTE to "Japa",
        MEDITATION_ROUTE to "Meditation",
        STOTRA_ROUTE to "Stotra",
        DOMAIN_ROUTE to "Shastras",
        MUHURAT_ROUTE to "Muhurat",
        MUHURAT_ACTIVITIES_ROUTE to "Muhurat",
        MUHURAT_RESULTS_ROUTE to "Muhurat",
        MUHURAT_DAY_ROUTE to "Muhurat",
    )

/**
 * A tab's icon, from either a Material symbol or a brand glyph.
 *
 * The glyph is an **alpha stencil**, so it tints exactly like the Material symbols beside it: the
 * lid, slot and lettering are holes rather than white paint, and the tint shows through them. That
 * keeps Support consistent with the other tabs — it takes the selected colour, and it stays legible
 * on a dark theme, where an opaque near-black illustration would have half-disappeared.
 *
 * The size is set explicitly because [Icon] only falls back to its own 24dp default when the painter
 * has **no intrinsic size**. A vector declares one of 24dp and so needs nothing; a 240px bitmap
 * declares 240px, which Compose honours — 80dp on a 3x screen, three times its neighbours.
 */
@Composable
private fun DestinationIcon(destination: TopDestination) {
    val glyph = destination.glyph
    if (glyph != null) {
        Icon(
            painter = painterResource(glyph),
            contentDescription = destination.label,
            modifier = Modifier.size(NAV_ICON_SIZE),
        )
    } else {
        Icon(imageVector = checkNotNull(destination.icon), contentDescription = destination.label)
    }
}

/** What Material 3 sizes a navigation-bar icon at, and what [Icon] defaults vector painters to. */
private val NAV_ICON_SIZE = 24.dp

// Sub-routes pushed on top of a tab; reached from the Home hub tiles or Settings. They are not
// tabs (no bottom-bar entry), so they're returned from via the top-bar back button or system back.
internal const val PANCHANG_ROUTE = "panchang"
internal const val FESTIVALS_ROUTE = "festivals"
internal const val EVENTS_ROUTE = "events"
internal const val CALENDAR_ROUTE = "calendar"
internal const val ALARM_ROUTE = "alarm"
internal const val KUNDALI_ROUTE = "kundali"
internal const val MATCHMAKING_ROUTE = "matchmaking"
internal const val RASHIFAL_ROUTE = "rashifal"
internal const val JAPA_ROUTE = "japa"
internal const val MEDITATION_ROUTE = "meditation"
internal const val STOTRA_ROUTE = "stotra"
internal const val DOMAIN_ROUTE = "domain"
internal const val DOMAIN_ID_ARG = "domainId"
internal const val LOCATION_ROUTE = "settings/location"
internal const val ADD_CITY_ROUTE = "settings/location/add-city"
internal const val ADD_COORDINATES_ROUTE = "settings/location/add-coordinates"
internal const val PROFILE_EDIT_ROUTE = "profile/edit"
internal const val PROFILE_ID_ARG = "profileId"
internal const val MUHURAT_ROUTE = "muhurat"
internal const val MUHURAT_ACTIVITIES_ROUTE = "muhurat/activities"
internal const val MUHURAT_RESULTS_ROUTE = "muhurat/results"
internal const val MUHURAT_DAY_ROUTE = "muhurat/day"
internal const val MUHURAT_CATEGORY_ARG = "category"
internal const val MUHURAT_ACTIVITY_ARG = "activity"
internal const val MUHURAT_DAY_ARG = "day"
internal const val ABOUT_ROUTE = "settings/about"

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
                title = { AppBarTitle(subtitle = subtitleOf(currentRoute)) },
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
                        icon = { DestinationIcon(destination) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(padding),
        )
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
        homeHubDestination(navController)
        homeDetailDestinations()
        composable(TopDestination.SETTINGS.route) {
            SettingsScreen(
                onNavigateToLocation = { navController.navigate(LOCATION_ROUTE) },
                onNavigateToProfile = { navController.navigateToTab(TopDestination.PROFILE.route) },
                onNavigateToAbout = { navController.navigate(ABOUT_ROUTE) },
            )
        }
        composable(ABOUT_ROUTE) {
            AboutScreen(
                onNavigateToSupport = { navController.navigateToTab(TopDestination.SUPPORT.route) },
            )
        }
        composable(TopDestination.SUPPORT.route) { SupportScreen() }
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
        domainDestinations(navController)
    muhuratDestinations(navController)
    }
}

/**
 * The Home hub itself.
 *
 * Extracted because every tile Home gains adds a line here, and [AppNavHost] sits against detekt's
 * eighty-line limit — the Cosmic Clock tile was the line that crossed it. Grouping the hub's wiring
 * means the next feature costs a line in a function that has room, rather than another refactor.
 */
private fun NavGraphBuilder.homeHubDestination(navController: NavHostController) {
    composable(TopDestination.HOME.route) {
        HomeScreen(
            onNavigateToLocation = { navController.navigate(LOCATION_ROUTE) },
            onOpen = { navController.navigate(routeOf(it)) },
            onOpenDomain = { navController.navigate("$DOMAIN_ROUTE/${it.name}") },
        )
    }
}

/**
 * The three screens reached from the Home hub's own content: the full daily panchanga, and the two
 * upcoming lists. Grouped here rather than inline so [AppNavHost] stays readable, the same reason
 * [muhuratDestinations] is separate.
 *
 * Each takes no arguments and navigates nowhere, so unlike the muhurat flow none of them needs the
 * NavHostController.
 */
private fun NavGraphBuilder.homeDetailDestinations() {
    composable(PANCHANG_ROUTE) { PanchangScreen() }
    composable(FESTIVALS_ROUTE) { FestivalsScreen() }
    composable(EVENTS_ROUTE) { EventsScreen() }
}

/**
 * The route a hub destination opens.
 *
 * `:feature:home` names an intent and `:app` chooses the route, so this `when` is the contract
 * between them. Exhaustive on purpose: a destination added without a route becomes a compile error
 * rather than a tile that silently goes nowhere. It also keeps the route strings in this file, which
 * is where `NavigationTitleTest` reflects for them.
 */
internal fun routeOf(target: HubTarget): String =
    when (target) {
        HubTarget.PANCHANG -> PANCHANG_ROUTE
        HubTarget.CALENDAR -> CALENDAR_ROUTE
        HubTarget.REMINDERS -> ALARM_ROUTE
        HubTarget.KUNDALI -> KUNDALI_ROUTE
        HubTarget.RASHIFAL -> RASHIFAL_ROUTE
        HubTarget.MATCH -> MATCHMAKING_ROUTE
        HubTarget.MUHURAT -> MUHURAT_ROUTE
        HubTarget.STOTRA -> STOTRA_ROUTE
        HubTarget.JAPA -> JAPA_ROUTE
        HubTarget.MEDITATE -> MEDITATION_ROUTE
        HubTarget.FESTIVALS -> FESTIVALS_ROUTE
        HubTarget.EVENTS -> EVENTS_ROUTE
    }

/**
 * The hub's second level — one screen per shastra, addressed by the domain's own enum name.
 *
 * One parameterised route rather than a constant per domain, so every level-two screen shares the
 * "Shastras" subtitle; the domain's own name is drawn on the screen. That is the trade the four
 * muhurat steps already make, and it keeps the argument name declared once, here, since the screen
 * reads it in this lambda rather than from a ViewModel.
 */
private fun NavGraphBuilder.domainDestinations(navController: NavHostController) {
    composable(
        route = "$DOMAIN_ROUTE/{$DOMAIN_ID_ARG}",
        arguments = listOf(navArgument(DOMAIN_ID_ARG) { type = NavType.StringType }),
    ) { entry ->
        val name = entry.arguments?.getString(DOMAIN_ID_ARG).orEmpty()
        DomainScreen(
            domain = runCatching { HubDomain.valueOf(name) }.getOrDefault(HubDomain.PANCHANGA),
            onOpen = { navController.navigate(routeOf(it)) },
        )
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
