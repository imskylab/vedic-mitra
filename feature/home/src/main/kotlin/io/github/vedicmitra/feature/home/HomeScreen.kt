/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.home

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vedicmitra.core.astronomy.AstronomySnapshot
import io.github.vedicmitra.core.astronomy.Ayana
import io.github.vedicmitra.core.astronomy.Festival
import io.github.vedicmitra.core.astronomy.FestivalType
import io.github.vedicmitra.core.astronomy.GoldenHour
import io.github.vedicmitra.core.astronomy.Graha
import io.github.vedicmitra.core.astronomy.GrahaPosition
import io.github.vedicmitra.core.astronomy.Karana
import io.github.vedicmitra.core.astronomy.Maasa
import io.github.vedicmitra.core.astronomy.MoonPhase
import io.github.vedicmitra.core.astronomy.MoonTimes
import io.github.vedicmitra.core.astronomy.Muhurta
import io.github.vedicmitra.core.astronomy.MuhurtaQuality
import io.github.vedicmitra.core.astronomy.Nakshatra
import io.github.vedicmitra.core.astronomy.Paksha
import io.github.vedicmitra.core.astronomy.PanchangaGlossary
import io.github.vedicmitra.core.astronomy.PanchangaNow
import io.github.vedicmitra.core.astronomy.Rasi
import io.github.vedicmitra.core.astronomy.Ritu
import io.github.vedicmitra.core.astronomy.Samvatsara
import io.github.vedicmitra.core.astronomy.SunTimes
import io.github.vedicmitra.core.astronomy.Tithi
import io.github.vedicmitra.core.astronomy.Vara
import io.github.vedicmitra.core.astronomy.Yoga
import io.github.vedicmitra.core.astronomy.observanceTithis
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.designsystem.icon.VedicIcons
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * Home screen entry point — the hub landing. Resolves the location permission, drives
 * [HomeViewModel.load], and delegates to the stateless, previewable [HomeContent]. Tiles either
 * navigate out (calendar, reminders), open the in-screen panchang detail, or report "coming soon".
 */
@Composable
fun HomeScreen(
    onSubViewChange: (String?) -> Unit,
    onNavigateToLocation: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenKundali: () -> Unit,
    onOpenMuhurat: () -> Unit,
    onOpenMatch: () -> Unit,
    onOpenRashifal: () -> Unit,
    onOpenJapa: () -> Unit,
    onOpenMeditate: () -> Unit,
    onOpenStotra: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.load()
        }

    // Ask once, on first composition only. Asking again on every resume would re-prompt a user who
    // has already declined, which Android answers by refusing outright after the second refusal.
    LaunchedEffect(Unit) {
        val granted =
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (!granted) permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    // Reload whenever the screen comes back to the foreground, rather than once per composition.
    // Someone who opens the app with location switched off, turns it on and comes back was being
    // shown the fallback city indefinitely, because nothing re-resolved. The same staleness applies
    // to time: leave the app open overnight and the day, tithi and muhurta windows are all wrong
    // until something else happens to trigger a load.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.load()
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    HomeContent(
        uiState = uiState,
        onSubViewChange = onSubViewChange,
        onNavigateToLocation = onNavigateToLocation,
        onOpenCalendar = onOpenCalendar,
        onOpenReminders = onOpenReminders,
        onOpenKundali = onOpenKundali,
        onOpenMuhurat = onOpenMuhurat,
        onOpenMatch = onOpenMatch,
        onOpenRashifal = onOpenRashifal,
        onOpenJapa = onOpenJapa,
        onOpenMeditate = onOpenMeditate,
        onOpenStotra = onOpenStotra,
        onSetReminder = viewModel::setReminder,
        modifier = modifier,
    )
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onSubViewChange: (String?) -> Unit,
    onNavigateToLocation: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenKundali: () -> Unit,
    onOpenMuhurat: () -> Unit,
    onOpenMatch: () -> Unit,
    onOpenRashifal: () -> Unit,
    onOpenJapa: () -> Unit,
    onOpenMeditate: () -> Unit,
    onOpenStotra: () -> Unit,
    onSetReminder: (ReminderTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snapshot = uiState.snapshot
    when {
        // Only the first load takes over the screen. Now that the content reloads on every resume,
        // showing a spinner whenever isLoading is set would blank the hub every time the user came
        // back from another app; a refresh with content already on screen happens silently.
        uiState.isLoading && snapshot == null -> CenteredBox(modifier) { CircularProgressIndicator() }
        uiState.errorMessage != null ->
            CenteredBox(modifier) { Text(text = uiState.errorMessage, style = MaterialTheme.typography.bodyLarge) }

        snapshot != null -> {
            var view by rememberSaveable { mutableStateOf(HomeView.HUB) }
            val toHub = { view = HomeView.HUB }
            // The sub-views live inside the Home tab, so the app-level back handler (which only acts on
            // pushed routes) can't return from them — handle it here so system back goes to the hub.
            BackHandler(enabled = view != HomeView.HUB) { view = HomeView.HUB }
            LaunchedEffect(view) { onSubViewChange(view.label) }
            when (view) {
                HomeView.HUB ->
                    HubView(
                        uiState = uiState,
                        snapshot = snapshot,
                        onNavigateToLocation = onNavigateToLocation,
                        onOpenPanchang = { view = HomeView.PANCHANG },
                        onOpenCalendar = onOpenCalendar,
                        onOpenReminders = onOpenReminders,
                        onOpenKundali = onOpenKundali,
                        onOpenMuhurat = onOpenMuhurat,
                        onOpenMatch = onOpenMatch,
                        onOpenRashifal = onOpenRashifal,
                        onOpenJapa = onOpenJapa,
                        onOpenMeditate = onOpenMeditate,
                        onOpenStotra = onOpenStotra,
                        onOpenFestivals = { view = HomeView.FESTIVALS },
                        onOpenEvents = { view = HomeView.EVENTS },
                        onSetReminder = onSetReminder,
                        modifier = modifier,
                    )

                HomeView.PANCHANG ->
                    PanchangaDetailView(
                        uiState = uiState,
                        onBack = toHub,
                        onSetReminder = onSetReminder,
                        modifier = modifier,
                    )

                HomeView.FESTIVALS ->
                    EventListView(
                        title = "Festivals",
                        rows = uiState.festivals.map { SectionRow(it.name, formatDate(it.atSunrise)) },
                        onBack = toHub,
                        onSetReminder = onSetReminder,
                        modifier = modifier,
                    )

                HomeView.EVENTS ->
                    EventListView(
                        title = "Events",
                        rows = uiState.events.map { it.toEventRow() },
                        onBack = toHub,
                        onSetReminder = onSetReminder,
                        modifier = modifier,
                    )
            }
        }
    }
}

/**
 * Which sub-view the Home tab is showing: the hub, the full daily panchang, or a full list.
 *
 * The Home tab swaps between these in place rather than navigating, so each carries the label the
 * app bar shows beneath the app name. The hub has none — the bar falls back to "Home".
 */
private enum class HomeView(
    val label: String?,
) {
    HUB(null),
    PANCHANG("Today's Panchang"),
    FESTIVALS("Festivals"),
    EVENTS("Events"),
}

/** The hub landing: header, a tappable today's-panchang hero, the auspicious-now strip, the full
 *  shortcut grid (all shortcuts on one screen, each tinted by its category), and a peek at the next
 *  upcoming festivals (tap a row for its significance). */
@Composable
private fun HubView(
    uiState: HomeUiState,
    snapshot: AstronomySnapshot,
    onNavigateToLocation: () -> Unit,
    onOpenPanchang: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenKundali: () -> Unit,
    onOpenMuhurat: () -> Unit,
    onOpenMatch: () -> Unit,
    onOpenRashifal: () -> Unit,
    onOpenJapa: () -> Unit,
    onOpenMeditate: () -> Unit,
    onOpenStotra: () -> Unit,
    onOpenFestivals: () -> Unit,
    onOpenEvents: () -> Unit,
    onSetReminder: (ReminderTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedRow by remember { mutableStateOf<SectionRow?>(null) }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Header(uiState.locationLabel, onNavigateToLocation)
        HeroCard(snapshot, uiState.nowPanchanga, onClick = onOpenPanchang)
        uiState.auspicious?.let { AuspiciousCard(it) }
        if (uiState.festivals.isNotEmpty()) {
            ExpandableSection(
                title = "UPCOMING FESTIVALS",
                accent = MaterialTheme.colorScheme.primary,
                rows = uiState.festivals.map { SectionRow(it.name, formatDate(it.atSunrise)) },
                onRowClick = { selectedRow = it },
            )
        }
        ShortcutGrid(
            onOpenPanchang = onOpenPanchang,
            onOpenCalendar = onOpenCalendar,
            onOpenReminders = onOpenReminders,
            onOpenKundali = onOpenKundali,
            onOpenMuhurat = onOpenMuhurat,
            onOpenMatch = onOpenMatch,
            onOpenRashifal = onOpenRashifal,
            onOpenJapa = onOpenJapa,
            onOpenMeditate = onOpenMeditate,
            onOpenStotra = onOpenStotra,
            onOpenFestivals = onOpenFestivals,
            onOpenEvents = onOpenEvents,
        )
        if (uiState.usingDefaultLocation) {
            Text(
                text = "Showing New Delhi — grant location access for your area.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        CopyrightFooter()
    }
    selectedRow?.let { row -> RowDetailSheet(row, onSetReminder = onSetReminder) { selectedRow = null } }
}

/** A subtle copyright + licence line at the foot of the landing. */
@Composable
private fun CopyrightFooter() {
    Text(
        text = "© 2026 Jayvardhan Potabatti · GNU AGPL-3.0-or-later",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
}

/** The full shortcut grid — every shortcut on one screen, [GRID_COLUMNS] tiles per row. */
@Composable
private fun ShortcutGrid(
    onOpenPanchang: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenKundali: () -> Unit,
    onOpenMuhurat: () -> Unit,
    onOpenMatch: () -> Unit,
    onOpenRashifal: () -> Unit,
    onOpenJapa: () -> Unit,
    onOpenMeditate: () -> Unit,
    onOpenStotra: () -> Unit,
    onOpenFestivals: () -> Unit,
    onOpenEvents: () -> Unit,
) {
    val tiles: List<@Composable () -> Unit> =
        listOf(
            { GlyphTile("Today's Panchang", VedicIcons.panchang, HubCategory.DAILY, onClick = onOpenPanchang) },
            { GlyphTile("Calendar", VedicIcons.calendar, HubCategory.DAILY, onClick = onOpenCalendar) },
            { GlyphTile("Muhurat", VedicIcons.muhurat, HubCategory.DAILY, onClick = onOpenMuhurat) },
            { GlyphTile("Festivals", VedicIcons.festivals, HubCategory.DAILY, onClick = onOpenFestivals) },
            { GlyphTile("Events", VedicIcons.events, HubCategory.DAILY, onClick = onOpenEvents) },
            { VectorTile("Reminders", Icons.Filled.Notifications, HubCategory.DAILY, onClick = onOpenReminders) },
            { GlyphTile("Kundali", VedicIcons.kundali, HubCategory.ASTROLOGY, onClick = onOpenKundali) },
            { GlyphTile("Rashifal", VedicIcons.rashifal, HubCategory.ASTROLOGY, onClick = onOpenRashifal) },
            { GlyphTile("Match", VedicIcons.matchmaking, HubCategory.ASTROLOGY, onClick = onOpenMatch) },
            { OmTile("Stotra", onClick = onOpenStotra) },
            { GlyphTile("Japa", VedicIcons.japa, HubCategory.DEVOTION, onClick = onOpenJapa) },
            { GlyphTile("Meditate", VedicIcons.meditate, HubCategory.DEVOTION, onClick = onOpenMeditate) },
        )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(GRID_COLUMNS).forEach { rowTiles ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowTiles.forEach { tile -> Box(modifier = Modifier.weight(1f)) { tile() } }
                repeat(GRID_COLUMNS - rowTiles.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

/** The detailed daily panchanga (limbs, sun/moon, periods, festivals, events), behind a back button. */
@Composable
private fun PanchangaDetailView(
    uiState: HomeUiState,
    onBack: () -> Unit,
    onSetReminder: (ReminderTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snapshot = uiState.snapshot ?: return
    var selectedRow by remember { mutableStateOf<SectionRow?>(null) }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(text = "Today's Panchang", style = MaterialTheme.typography.titleMedium)
        }
        PanchangaLimbsStrip(snapshot)
        SunMoonStrip(snapshot)
        SeasonAyanaStrip(snapshot)
        if (uiState.planets.isNotEmpty()) {
            ExpandableSection(
                title = "PLANETARY POSITIONS",
                accent = MaterialTheme.colorScheme.primary,
                rows = uiState.planets.map { it.toSectionRow() },
            )
        }
        snapshot.periodRows(MuhurtaQuality.AUSPICIOUS).takeIf { it.isNotEmpty() }?.let { rows ->
            ExpandableSection(
                title = "AUSPICIOUS PERIODS",
                accent = MaterialTheme.colorScheme.primary,
                rows = rows,
                onRowClick = { selectedRow = it },
            )
        }
        snapshot.periodRows(MuhurtaQuality.INAUSPICIOUS).takeIf { it.isNotEmpty() }?.let { rows ->
            ExpandableSection(
                title = "INAUSPICIOUS PERIODS",
                accent = MaterialTheme.colorScheme.error,
                rows = rows,
                onRowClick = { selectedRow = it },
            )
        }
    }
    selectedRow?.let { row -> RowDetailSheet(row, onSetReminder = onSetReminder) { selectedRow = null } }
}

/**
 * A full, scrollable list of [rows] under a [title] with a back button — used for the Festivals and
 * Events shortcuts. Each row is tappable, opening the significance sheet (and a Set-reminder button
 * for recurring observances).
 */
@Composable
private fun EventListView(
    title: String,
    rows: List<SectionRow>,
    onBack: () -> Unit,
    onSetReminder: (ReminderTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedRow by remember { mutableStateOf<SectionRow?>(null) }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(text = title, style = MaterialTheme.typography.titleMedium)
        }
        if (rows.isEmpty()) {
            Text(
                text = "Nothing upcoming.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            rows.forEach { row -> SectionRowLine(row, onClick = { selectedRow = row }) }
        }
    }
    selectedRow?.let { row -> RowDetailSheet(row, onSetReminder = onSetReminder) { selectedRow = null } }
}

/** A shortcut tile: a tinted rounded chip holding [icon], with a [label] below. */
@Composable
private fun TileButton(
    label: String,
    category: HubCategory,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(15.dp)).background(category.container()),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A tile whose icon is one of the brand's duotone cultural glyphs (rendered in its own colours). */
@Composable
private fun GlyphTile(
    label: String,
    @androidx.annotation.DrawableRes glyph: Int,
    category: HubCategory,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    TileButton(label, category, enabled, onClick) {
        Icon(
            painter = painterResource(glyph),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(38.dp),
        )
    }
}

/** A tile whose icon is a Material symbol, tinted to the category's on-container colour. */
@Composable
private fun VectorTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    category: HubCategory,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val tint = category.onContainer()
    TileButton(label, category, enabled, onClick) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(42.dp))
    }
}

/** The Stotra tile: Om (ॐ) as a Devanagari text glyph rather than a drawable. */
@Composable
private fun OmTile(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val tint = HubCategory.DEVOTION.onContainer()
    TileButton(label, HubCategory.DEVOTION, enabled, onClick) {
        Text(text = "ॐ", style = MaterialTheme.typography.displayMedium, color = tint)
    }
}

@Composable
private fun HubCategory.container(): Color =
    when (this) {
        HubCategory.DAILY -> MaterialTheme.colorScheme.primaryContainer
        HubCategory.ASTROLOGY -> MaterialTheme.colorScheme.secondaryContainer
        HubCategory.DEVOTION -> MaterialTheme.colorScheme.tertiaryContainer
    }

@Composable
private fun HubCategory.onContainer(): Color =
    when (this) {
        HubCategory.DAILY -> MaterialTheme.colorScheme.onPrimaryContainer
        HubCategory.ASTROLOGY -> MaterialTheme.colorScheme.onSecondaryContainer
        HubCategory.DEVOTION -> MaterialTheme.colorScheme.onTertiaryContainer
    }

/** The hub's shortcut categories — each tints its tiles with its own container colour. */
private enum class HubCategory {
    DAILY,
    ASTROLOGY,
    DEVOTION,
}

@Composable
private fun CenteredBox(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { content() }
}

@Composable
private fun Header(
    locationLabel: String?,
    onNavigateToLocation: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = LocalDate.now().format(dateFormatter), style = MaterialTheme.typography.titleMedium)
        }
        Row(
            modifier = Modifier.clickable(onClick = onNavigateToLocation).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = Icons.Filled.LocationOn, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = locationLabel ?: "Location", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun HeroCard(
    snapshot: AstronomySnapshot,
    nowPanchanga: PanchangaNow?,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Today's Panchang",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = snapshot.vara.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    if (nowPanchanga != null) {
                        RunningTithi(now = nowPanchanga, sunriseTithi = snapshot.tithi)
                    } else {
                        Text(
                            text = "${snapshot.tithi.paksha.title} ${snapshot.tithi.name}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Text(
                        text = "${snapshot.maasa.displayName} · ${snapshot.samvatsara.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Text(
                    text = snapshot.moonPhase.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            // Outside the row, so it gets the whole card width and stays on one line. Inside it, the
            // moon-phase label on the right squeezed the column and this wrapped mid-phrase.
            Text(
                text = "Tap for full panchang ›",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/**
 * The tithi that is actually running, and — when the day has already moved on — when the one that
 * named the day gave way.
 *
 * The day is named by its **sunrise** tithi, which is the convention every published panchanga
 * follows, and that name still appears on the Panchang screen. But a card read at nine in the
 * evening was leading with a tithi that ended before breakfast, with the one genuinely in force
 * demoted to small print underneath. The prominence is now the other way round: what is running, and
 * how long it has left, with the handover recorded below it.
 *
 * The countdown re-reads the clock once a minute rather than every frame: the Moon opens the
 * elongation by about half an arcsecond a second, so a faster tick would redraw the same minute over
 * and over.
 */
@Composable
private fun RunningTithi(
    now: PanchangaNow,
    sunriseTithi: Tithi,
) {
    val remaining by produceState(initialValue = now.limbs.tithi.remainingFrom(systemNow()), now) {
        while (true) {
            value = now.limbs.tithi.remainingFrom(systemNow())
            delay(MINUTE_MILLIS)
        }
    }
    Text(
        text = "${now.tithi.paksha.title} ${now.tithi.name} · ends in ${formatRemaining(remaining)}",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.padding(top = 4.dp),
    )
    // The current tithi's window opens exactly where the previous one closed, so its start is the
    // handover instant -- no second solve needed. Shown only once the day has actually rolled over;
    // before that the sunrise tithi and the running one are the same thing said twice.
    if (now.tithi.number != sunriseTithi.number) {
        Text(
            text = "${sunriseTithi.paksha.title} ${sunriseTithi.name} ended ${formatTime(now.limbs.tithi.start)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

private fun systemNow(): Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())

/** A coarse "4h 12m" / "12m" countdown; seconds would churn without telling the reader anything. */
private fun formatRemaining(remaining: Duration): String {
    val hours = remaining.inWholeHours
    val minutes = remaining.inWholeMinutes % MINUTES_PER_HOUR
    return when {
        remaining <= Duration.ZERO -> "moments"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

private const val MINUTE_MILLIS = 60_000L
private const val MINUTES_PER_HOUR = 60L

@Composable
private fun AuspiciousCard(window: AuspiciousWindow) {
    val auspicious = window.quality == MuhurtaQuality.AUSPICIOUS
    val container =
        if (auspicious) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
    val onContainer =
        if (auspicious) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
    val heading =
        when {
            window.isActive && auspicious -> "Auspicious now"
            window.isActive -> "Caution now"
            else -> "Next auspicious"
        }
    val boundaryLabel =
        if (window.isActive) "ends ${formatTime(window.boundary)}" else "from ${formatTime(window.boundary)}"
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = container)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = heading, style = MaterialTheme.typography.labelMedium, color = onContainer)
                Text(text = window.name, style = MaterialTheme.typography.titleMedium, color = onContainer)
            }
            Text(text = boundaryLabel, style = MaterialTheme.typography.bodyMedium, color = onContainer)
        }
    }
}

@Composable
private fun SunMoonStrip(snapshot: AstronomySnapshot) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard("Sunrise", formatTime(snapshot.sunTimes.sunrise), Modifier.weight(1f))
        StatCard("Sunset", formatTime(snapshot.sunTimes.sunset), Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard("Moonrise", formatTime(snapshot.moonTimes.moonrise), Modifier.weight(1f))
        StatCard("Moonset", formatTime(snapshot.moonTimes.moonset), Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun PanchangaLimbsStrip(snapshot: AstronomySnapshot) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard("Nakshatra", snapshot.nakshatra.name, Modifier.weight(1f))
        StatCard("Yoga", snapshot.yoga.name, Modifier.weight(1f))
        StatCard("Karana", snapshot.karana.name, Modifier.weight(1f))
    }
}

@Composable
private fun SeasonAyanaStrip(snapshot: AstronomySnapshot) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard("Season", snapshot.ritu.displayName, Modifier.weight(1f))
        StatCard("Ayana", snapshot.ayana.displayName, Modifier.weight(1f))
    }
}

/**
 * A collapsible card for a titled list. Collapsed (the default) it shows the section title and a
 * one-line peek of the soonest row; tapping anywhere on the card expands it to the full list.
 */
@Composable
private fun ExpandableSection(
    title: String,
    accent: Color,
    rows: List<SectionRow>,
    onRowClick: ((SectionRow) -> Unit)? = null,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.labelMedium, color = accent)
                    if (!expanded) SectionRowLine(rows.first())
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                )
            }
            if (expanded) {
                rows.forEach { row ->
                    SectionRowLine(row, onClick = onRowClick?.let { handler -> { handler(row) } })
                }
            }
        }
    }
}

@Composable
private fun SectionRowLine(
    row: SectionRow,
    onClick: (() -> Unit)? = null,
) {
    val rowModifier =
        Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick).padding(vertical = 6.dp) else it }
    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = row.label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = row.trailing,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A bottom sheet with the significance of a tapped list item (muhurta, festival, or observance). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RowDetailSheet(
    row: SectionRow,
    onSetReminder: (ReminderTarget) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = row.label, style = MaterialTheme.typography.headlineSmall)
            if (row.trailing.isNotBlank()) {
                Text(
                    text = row.trailing,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = PanchangaGlossary.significanceOf(row.label) ?: "More details coming soon.",
                style = MaterialTheme.typography.bodyLarge,
            )
            row.reminderTarget?.let { target ->
                Button(
                    onClick = {
                        onSetReminder(target)
                        onDismiss()
                    },
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text("Set reminder")
                }
            }
        }
    }
}

/**
 * One row of an [ExpandableSection]: a label, a right-aligned trailing value (time or date), and —
 * when the item can be reminded for — the [reminderTarget] the detail sheet's "Set reminder" uses.
 */
private data class SectionRow(
    val label: String,
    val trailing: String,
    val reminderTarget: ReminderTarget? = null,
)

/** A graha's rashi as a row: "Guru · Karka" with its next pravesh date, or an em dash if none. */
private fun GrahaPosition.toSectionRow(): SectionRow =
    SectionRow(
        label = "${graha.displayName} · ${rasi.name}",
        trailing = pravesh?.let { "→ ${formatDate(it)}" } ?: "—",
    )

/** Builds collapsible rows for the muhurtas of a given [quality], in chronological order. */
private fun AstronomySnapshot.periodRows(quality: MuhurtaQuality): List<SectionRow> =
    muhurtas
        .filter { it.quality == quality }
        .sortedBy { it.start }
        .map { SectionRow(it.name, formatRange(it.start, it.end), ReminderTarget.Muhurta(it.name)) }

/** A festival/observance as a row; observances also carry a [ReminderTarget] so they can be reminded for. */
private fun Festival.toEventRow(): SectionRow =
    SectionRow(
        label = name,
        trailing = formatDate(atSunrise),
        reminderTarget = observanceTithis(name)?.let { ReminderTarget.Observance(name, it) },
    )

private val Paksha.title: String
    get() =
        when (this) {
            Paksha.SHUKLA -> "Shukla"
            Paksha.KRISHNA -> "Krishna"
        }

private const val AFTERNOON_FROM_HOUR = 12
private const val EVENING_FROM_HOUR = 17
private const val GRID_COLUMNS = 3

private fun greeting(): String =
    when (LocalTime.now().hour) {
        in 0 until AFTERNOON_FROM_HOUR -> "Good morning"
        in AFTERNOON_FROM_HOUR until EVENING_FROM_HOUR -> "Good afternoon"
        else -> "Good evening"
    }

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM")
private val festivalDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun zoned(instant: Instant) =
    java.time.Instant
        .ofEpochMilli(instant.toEpochMilliseconds())
        .atZone(ZoneId.systemDefault())

/** Formats an instant as local wall-clock time in the device's zone, or an em dash if absent. */
private fun formatTime(instant: Instant?): String = if (instant == null) "—" else zoned(instant).format(timeFormatter)

/** Formats a start–end muhurta window as local wall-clock times, e.g. "06:12–07:00". */
private fun formatRange(
    start: Instant,
    end: Instant,
): String = "${formatTime(start)}–${formatTime(end)}"

/** Formats a festival's sunrise instant as a local date (e.g. "Wed, 8 Nov"). */
private fun formatDate(instant: Instant): String = zoned(instant).format(festivalDateFormatter)

@Preview
@Composable
private fun HomeContentPreview() {
    VedicMitraTheme {
        HomeContent(
            uiState = sampleHomeState(),
            onSubViewChange = {},
            onNavigateToLocation = {},
            onOpenCalendar = {},
            onOpenReminders = {},
            onOpenKundali = {},
            onOpenMuhurat = {},
            onOpenMatch = {},
            onOpenRashifal = {},
            onOpenJapa = {},
            onOpenMeditate = {},
            onOpenStotra = {},
            onSetReminder = {},
        )
    }
}

// Fixture data for the preview. Kept out of the @Composable so it doesn't trip LongMethod, and
// suppressed for the sample epoch/coordinate literals it necessarily contains.
@Suppress("MagicNumber", "LongMethod")
private fun sampleHomeState(): HomeUiState {
    val sample =
        AstronomySnapshot(
            instant = Instant.fromEpochMilliseconds(1_705_320_000_000L),
            location = GeoCoordinates(latitude = 28.6139, longitude = 77.2090),
            sunTimes =
                SunTimes(
                    sunrise = Instant.fromEpochMilliseconds(1_705_282_440_000L),
                    sunset = Instant.fromEpochMilliseconds(1_705_320_180_000L),
                ),
            moonTimes =
                MoonTimes(
                    moonrise = Instant.fromEpochMilliseconds(1_705_286_640_000L),
                    moonset = Instant.fromEpochMilliseconds(1_705_331_520_000L),
                ),
            tithi = Tithi(number = 5, paksha = Paksha.SHUKLA, name = "Panchami"),
            nakshatra = Nakshatra(number = 25, name = "Purva Bhadrapada"),
            yoga = Yoga(number = 18, name = "Variyana"),
            karana = Karana(number = 10, name = "Balava"),
            vara = Vara.SOMAVARA,
            maasa = Maasa(number = 10, name = "Pausha", adhika = false),
            samvatsara = Samvatsara(number = 37, name = "Shobhakruth", shakaYear = 1945),
            ayana = Ayana.UTTARAYANA,
            ritu = Ritu.SHISHIRA,
            moonPhase = MoonPhase.WAXING_GIBBOUS,
            goldenHour = GoldenHour(morningStart = null, morningEnd = null, eveningStart = null, eveningEnd = null),
            muhurtas =
                listOf(
                    Muhurta(
                        name = "Brahma Muhurta",
                        start = Instant.fromEpochMilliseconds(1_705_280_400_000L),
                        end = Instant.fromEpochMilliseconds(1_705_282_200_000L),
                        quality = MuhurtaQuality.AUSPICIOUS,
                    ),
                    Muhurta(
                        name = "Abhijit Muhurta",
                        start = Instant.fromEpochMilliseconds(1_705_300_140_000L),
                        end = Instant.fromEpochMilliseconds(1_705_302_960_000L),
                        quality = MuhurtaQuality.AUSPICIOUS,
                    ),
                    Muhurta(
                        name = "Rahu Kalam",
                        start = Instant.fromEpochMilliseconds(1_705_287_540_000L),
                        end = Instant.fromEpochMilliseconds(1_705_292_265_000L),
                        quality = MuhurtaQuality.INAUSPICIOUS,
                    ),
                ),
        )
    return HomeUiState(
        isLoading = false,
        snapshot = sample,
        auspicious =
            AuspiciousWindow(
                name = "Abhijit Muhurta",
                quality = MuhurtaQuality.AUSPICIOUS,
                boundary = Instant.fromEpochMilliseconds(1_705_302_960_000L),
                isActive = true,
            ),
        festivals =
            listOf(
                Festival(
                    name = "Ganesh Chaturthi",
                    atSunrise = Instant.fromEpochMilliseconds(1_757_808_000_000L),
                    type = FestivalType.FESTIVAL,
                ),
                Festival(
                    name = "Makara Sankranti",
                    atSunrise = Instant.fromEpochMilliseconds(1_768_003_200_000L),
                    type = FestivalType.SANKRANTI,
                ),
            ),
        events =
            listOf(
                Festival(
                    name = "Purnima",
                    atSunrise = Instant.fromEpochMilliseconds(1_757_030_400_000L),
                    type = FestivalType.OBSERVANCE,
                ),
                Festival(
                    name = "Amavasya",
                    atSunrise = Instant.fromEpochMilliseconds(1_758_240_000_000L),
                    type = FestivalType.OBSERVANCE,
                ),
            ),
        planets =
            listOf(
                GrahaPosition(
                    graha = Graha.MOON,
                    rasi = Rasi(index = 11, name = "Meena"),
                    pravesh = Instant.fromEpochMilliseconds(1_705_449_600_000L),
                ),
                GrahaPosition(
                    graha = Graha.SUN,
                    rasi = Rasi(index = 9, name = "Makara"),
                    pravesh = Instant.fromEpochMilliseconds(1_707_566_400_000L),
                ),
                GrahaPosition(
                    graha = Graha.GURU,
                    rasi = Rasi(index = 1, name = "Vrishabha"),
                    pravesh = Instant.fromEpochMilliseconds(1_716_336_000_000L),
                ),
                GrahaPosition(
                    graha = Graha.SHUKRA,
                    rasi = Rasi(index = 9, name = "Makara"),
                    pravesh = Instant.fromEpochMilliseconds(1_706_140_800_000L),
                ),
            ),
        locationLabel = "New Delhi",
    )
}
