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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vedicmitra.core.astronomy.AstronomySnapshot
import io.github.vedicmitra.core.astronomy.Ayana
import io.github.vedicmitra.core.astronomy.DayRule
import io.github.vedicmitra.core.astronomy.Festival
import io.github.vedicmitra.core.astronomy.FestivalKind
import io.github.vedicmitra.core.astronomy.FestivalType
import io.github.vedicmitra.core.astronomy.GoldenHour
import io.github.vedicmitra.core.astronomy.Graha
import io.github.vedicmitra.core.astronomy.GrahaPosition
import io.github.vedicmitra.core.astronomy.Karana
import io.github.vedicmitra.core.astronomy.Maasa
import io.github.vedicmitra.core.astronomy.MoonPhase
import io.github.vedicmitra.core.astronomy.MoonTimes
import io.github.vedicmitra.core.astronomy.Muhurta
import io.github.vedicmitra.core.astronomy.MuhurtaKind
import io.github.vedicmitra.core.astronomy.MuhurtaQuality
import io.github.vedicmitra.core.astronomy.Nakshatra
import io.github.vedicmitra.core.astronomy.Paksha
import io.github.vedicmitra.core.astronomy.PanchangaNow
import io.github.vedicmitra.core.astronomy.Rasi
import io.github.vedicmitra.core.astronomy.Ritu
import io.github.vedicmitra.core.astronomy.Samvatsara
import io.github.vedicmitra.core.astronomy.SunTimes
import io.github.vedicmitra.core.astronomy.Tithi
import io.github.vedicmitra.core.astronomy.Vara
import io.github.vedicmitra.core.astronomy.Yoga
import io.github.vedicmitra.core.astronomy.nameIn
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.model.MaasaReckoning
import io.github.vedicmitra.core.designsystem.component.TimelineLane
import io.github.vedicmitra.core.designsystem.component.VedicTimelineBar
import io.github.vedicmitra.core.designsystem.theme.LocalVedicAccents
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme
import io.github.vedicmitra.core.ui.panchanga.PanchangaGlossary
import io.github.vedicmitra.core.ui.panchanga.explanationRes
import io.github.vedicmitra.core.ui.panchanga.festivalDatesNoteRes
import io.github.vedicmitra.core.ui.preview.ThemePreviews
import io.github.vedicmitra.feature.home.hub.HubCatalog
import io.github.vedicmitra.feature.home.hub.HubDomain
import io.github.vedicmitra.feature.home.hub.HubTarget
import io.github.vedicmitra.feature.home.hub.HubTile
import io.github.vedicmitra.feature.home.hub.SectionLabel
import io.github.vedicmitra.feature.home.hub.TileAction
import io.github.vedicmitra.feature.home.hub.TileGrid
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * Home screen entry point — the hub landing. Resolves the location permission, drives
 * [HomeViewModel.load], and delegates to the stateless, previewable [HomeContent]. Every tile
 * navigates; nothing is swapped in place.
 */
@Composable
fun HomeScreen(
    onNavigateToLocation: () -> Unit,
    onOpen: (HubTarget) -> Unit,
    onOpenDomain: (HubDomain) -> Unit,
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

    HomeDataEffects(viewModel)

    HomeContent(
        uiState = uiState,
        onNavigateToLocation = onNavigateToLocation,
        onOpen = onOpen,
        onTile = tileHandler(onOpen, onOpenDomain),
        onSetReminder = viewModel::setReminder,
        modifier = modifier,
    )
}

/**
 * Turns a tap on a tile into navigation, or into a word about where that domain stands.
 *
 * The toast is raised here rather than through [HomeViewModel]'s message channel: that channel
 * carries the result of something the reader did, while a roadmap note is presentation copy that
 * never leaves the UI layer. It also keeps the domain screen free of a ViewModel it would otherwise
 * have to resolve — and resolving Home's would recompute a whole panchanga to show a sentence.
 */
@Composable
private fun tileHandler(
    onOpen: (HubTarget) -> Unit,
    onOpenDomain: (HubDomain) -> Unit,
): (HubTile) -> Unit {
    val context = LocalContext.current
    return { tile ->
        when (val action = tile.action) {
            is TileAction.Open -> onOpen(action.target)
            is TileAction.Drill -> onOpenDomain(action.domain)
            // LENGTH_LONG: these are sentences, not "Copied".
            is TileAction.NotYet -> Toast.makeText(context, action.note, Toast.LENGTH_LONG).show()
        }
    }
}

/**
 * The load-on-resume and message wiring every screen in the Home family needs.
 *
 * Reloading on resume rather than once per composition is what keeps a stale view from persisting:
 * someone who opens the app with location switched off, turns it on and comes back was being shown
 * the fallback city indefinitely, because nothing re-resolved. The same staleness applies to time —
 * leave the app open overnight and the day, tithi and muhurta windows are all wrong until something
 * else happens to trigger a load.
 */
@Composable
private fun HomeDataEffects(viewModel: HomeViewModel) {
    val context = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.load()
    }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * The loading / error / ready gate the Home family shares.
 *
 * Only the first load takes over the screen. Now that the content reloads on every resume, showing a
 * spinner whenever `isLoading` is set would blank the screen every time the user came back from
 * another app; a refresh with content already on screen happens silently.
 */
@Composable
private fun HomeDataGate(
    uiState: HomeUiState,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    when {
        uiState.isLoading && uiState.snapshot == null -> CenteredBox(modifier) { CircularProgressIndicator() }
        uiState.errorMessage != null ->
            CenteredBox(modifier) { Text(text = uiState.errorMessage, style = MaterialTheme.typography.bodyLarge) }

        uiState.snapshot != null -> content()
    }
}

/** The full daily panchanga, as its own destination. */
@Composable
fun PanchangScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeDataEffects(viewModel)
    HomeDataGate(uiState, modifier) {
        PanchangaDetailView(uiState = uiState, onSetReminder = viewModel::setReminder, modifier = modifier)
    }
}

/** Every upcoming named festival and Sankranti, as its own destination. */
@Composable
fun FestivalsScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeDataEffects(viewModel)
    HomeDataGate(uiState, modifier) {
        EventListView(
            rows = uiState.festivals.map { it.toEventRow() },
            onSetReminder = viewModel::setReminder,
            modifier = modifier,
        )
    }
}

/** The next occurrence of each recurring lunar observance, as its own destination. */
@Composable
fun EventsScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeDataEffects(viewModel)
    HomeDataGate(uiState, modifier) {
        EventListView(
            rows = uiState.events.map { it.toEventRow() },
            onSetReminder = viewModel::setReminder,
            modifier = modifier,
        )
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onNavigateToLocation: () -> Unit,
    onOpen: (HubTarget) -> Unit,
    onTile: (HubTile) -> Unit,
    onSetReminder: (ReminderTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    HomeDataGate(uiState, modifier) {
        HubView(
            uiState = uiState,
            snapshot = checkNotNull(uiState.snapshot),
            onNavigateToLocation = onNavigateToLocation,
            onOpen = onOpen,
            onTile = onTile,
            onSetReminder = onSetReminder,
            modifier = modifier,
        )
    }
}

/** The hub landing: header, a tappable today's-panchang hero, the auspicious-now strip, the full
 *  shortcut grid (all shortcuts on one screen, each tinted by its category), and a peek at the next
 *  upcoming festivals (tap a row for its significance). */
@Composable
private fun HubView(
    uiState: HomeUiState,
    snapshot: AstronomySnapshot,
    onNavigateToLocation: () -> Unit,
    onOpen: (HubTarget) -> Unit,
    onTile: (HubTile) -> Unit,
    onSetReminder: (ReminderTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedRow by remember { mutableStateOf<SectionRow?>(null) }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Header(uiState.locationLabel, onNavigateToLocation)
        HeroCard(snapshot, uiState.nowPanchanga, uiState.maasaReckoning) { onOpen(HubTarget.PANCHANG) }
        uiState.snapshot?.let { AuspiciousStrip(it.muhurtas) }
        if (uiState.festivals.isNotEmpty()) {
            ExpandableSection(
                title = "UPCOMING FESTIVALS",
                accent = MaterialTheme.colorScheme.primary,
                rows = uiState.festivals.map { it.toEventRow() },
                onRowClick = { selectedRow = it },
            )
        }
        // One grid. There used to be a "Today" row above this one, and it repeated the screen it sat
        // on: the hero card already opens Today's Panchanga and says more than a tile can, and
        // Calendar lives under Panchanga where a reader would look for it. Reminders was the only
        // tile earning its place, so it moved into the grid below rather than keeping a row of its
        // own.
        SectionLabel("EXPLORE")
        TileGrid(HubCatalog.explore, onTile)
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

/**
 * The detailed daily panchanga — limbs, sun/moon, planetary positions and the period tables.
 *
 * No title or back arrow of its own: this is a nav destination, so the app bar supplies both. It
 * used to draw them itself, which meant the title appeared twice, once in the bar and once beneath.
 */
@Composable
private fun PanchangaDetailView(
    uiState: HomeUiState,
    onSetReminder: (ReminderTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snapshot = uiState.snapshot ?: return
    var selectedRow by remember { mutableStateOf<SectionRow?>(null) }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
 * A full, scrollable list of [rows] — the body of both the Festivals and the Events destinations.
 * Each row is tappable, opening the significance sheet (and a Set-reminder button for recurring
 * observances).
 *
 * The title and back arrow come from the app bar, as they do for any other destination. Drawing its
 * own meant Festivals and Events each showed their title twice.
 */
@Composable
private fun EventListView(
    rows: List<SectionRow>,
    onSetReminder: (ReminderTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedRow by remember { mutableStateOf<SectionRow?>(null) }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (rows.isEmpty()) {
            Text(
                text = "Nothing upcoming.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            rows.forEach { row -> SectionRowLine(row, onClick = { selectedRow = row }) }
            // Shown untapped, on the same reasoning as a primer one-liner and as ADR 0017's month
            // scheme: a convention that only appears on tap is one most readers never meet.
            if (rows.any { it.dayRule != null }) {
                Text(
                    text = stringResource(festivalDatesNoteRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
    selectedRow?.let { row -> RowDetailSheet(row, onSetReminder = onSetReminder) { selectedRow = null } }
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
    reckoning: MaasaReckoning,
    onClick: () -> Unit,
) {
    // role = Role.Button so this announces as a button rather than as a heap of text. It was a bare
    // clickable while a labelled tile offered the same destination; now that the tile is gone this
    // card is the only one-tap route to Today's Panchanga, and the omission stopped being cosmetic.
    Card(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = "Today's Panchanga",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            // The moon phase sits with the weekday rather than in its own right-hand column. It is
            // a fact about today like the others, and giving it a column of its own narrowed
            // everything beneath it -- which is what made the lines below wrap.
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = snapshot.vara.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = snapshot.moonPhase.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(start = 8.dp, bottom = 3.dp),
                )
            }
            if (nowPanchanga != null) {
                RunningTithi(now = nowPanchanga, sunriseTithi = snapshot.tithi)
            } else {
                Text(
                    text = "${snapshot.tithi.paksha.displayName} ${snapshot.tithi.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(
                text = "${snapshot.maasa.nameIn(reckoning, snapshot.tithi.paksha)} · ${snapshot.samvatsara.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * The tithi that is actually running, and — when the day has already moved on — when the one that
 * named the day gave way.
 *
 * The day is named by its **sunrise** tithi, which is the convention every published panchanga
 * follows, and that name still appears on the Panchanga screen. But a card read at nine in the
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
    // One line, two weights: the tithi is the fact, the countdown qualifies it. Built as a single
    // annotated string rather than two Texts in a Row so the pair cannot be split across lines --
    // "Shukla Trayodashi · ends in" with a stranded "4h 12m" underneath was the glitch here.
    val countdownSize = MaterialTheme.typography.bodySmall.fontSize
    Text(
        text =
            buildAnnotatedString {
                append("${now.tithi.paksha.displayName} ${now.tithi.name}")
                withStyle(SpanStyle(fontSize = countdownSize)) {
                    append(" · ends in ${formatRemaining(remaining)}")
                }
            },
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.padding(top = 4.dp),
    )
    // The current tithi's window opens exactly where the previous one closed, so its start is the
    // handover instant -- no second solve needed. Shown only once the day has actually rolled over;
    // before that the sunrise tithi and the running one are the same thing said twice.
    if (now.tithi.number != sunriseTithi.number) {
        Text(
            text = "${sunriseTithi.paksha.displayName} ${sunriseTithi.name} ended ${formatTime(now.limbs.tithi.start)}",
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

/**
 * The strip, re-resolved once a minute.
 *
 * It re-runs [activeOrNextMuhurta] rather than only re-counting the remaining time, because the
 * window itself changes: one ends, the next becomes current. Counting alone would leave a card
 * reading "ends in moments" for the rest of the evening -- `load()` only runs on `ON_RESUME`, so
 * nothing else would correct it while Home stays open.
 *
 * Once a minute for the same reason the tithi countdown ticks at that rate: a faster tick redraws
 * the same minute over and over.
 */
@Composable
private fun AuspiciousStrip(muhurtas: List<Muhurta>) {
    val now by produceState(initialValue = systemNow(), muhurtas) {
        while (true) {
            value = systemNow()
            delay(MINUTE_MILLIS)
        }
    }
    // One tick drives both the verdict and the bar. A minute is ample for the marker too: across a
    // fourteen-hour span in a few hundred dp, a minute is a third of a pixel.
    val window = activeOrNextMuhurta(muhurtas, now)
    window?.let { AuspiciousCard(it, muhurtaTimeline(muhurtas, now), alsoRunning(muhurtas, now), now) }
}

/**
 * The verdict, and the day it came out of.
 *
 * The card is **neutral**, with the state carried by the heading's colour rather than by the whole
 * fill. It used to be a full-width block of `errorContainer` for a caution, which read clearly enough
 * on its own but leaves nowhere to put a red-and-green bar: on the dark scheme's `#93000A` the
 * caution bands disappear into the card entirely.
 *
 * [timeline] shows what [window] cannot. `activeOrNextMuhurta` picks one window to lead with and
 * discards the rest, so on any Friday between 11:54 and 12:18 the heading reads "Caution now — Rahu
 * Kalam" while Abhijit Muhurta, the day's most favourable window, runs unmentioned. [alsoRunning]
 * names those in words; the bar shows where they sit.
 */
@Composable
private fun AuspiciousCard(
    window: AuspiciousWindow,
    timeline: MuhurtaTimeline?,
    alsoRunning: List<Muhurta>,
    now: Instant,
) {
    val auspicious = window.quality == MuhurtaQuality.AUSPICIOUS
    val accents = LocalVedicAccents.current
    val accent = if (auspicious) accents.auspicious else accents.caution
    val onContainer = MaterialTheme.colorScheme.onSurface
    // Four states in two parallel pairs, so "now" and "next" read as the same fact at different
    // distances. An upcoming caution had no wording at all before -- it was simply never shown.
    val heading =
        when {
            window.isActive && auspicious -> "Auspicious now"
            window.isActive -> "Caution now"
            auspicious -> "Auspicious next"
            else -> "Caution next"
        }
    // A countdown for what is running, a clock time for what is not: "ends in 24m" answers how long
    // is left, which is the question about a window you are inside; "starts at 13:30" answers when,
    // which is the question about one you are not.
    val boundaryLabel =
        if (window.isActive) {
            "ends in ${formatRemaining(window.boundary - now)}"
        } else {
            "starts at ${formatTime(window.boundary)}"
        }
    var selected by remember(timeline) { mutableStateOf<Muhurta?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = heading, style = MaterialTheme.typography.labelMedium, color = accent)
                    Text(text = window.name, style = MaterialTheme.typography.titleMedium, color = onContainer)
                }
                Text(text = boundaryLabel, style = MaterialTheme.typography.bodyMedium, color = onContainer)
            }
            Text(
                text = selected?.let { "${it.name}  ${formatRange(it.start, it.end)}" }
                    ?: detailLine(window, alsoRunning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            timeline?.let { MuhurtaBar(it) { muhurta -> selected = muhurta } }
        }
    }
}

/**
 * The line under the heading: the running window's own hours, and anything else running alongside it.
 *
 * Named separately from the heading because it answers a different question. The heading says what
 * the app thinks of this moment; this says what the moment is actually made of.
 */
private fun detailLine(
    window: AuspiciousWindow,
    alsoRunning: List<Muhurta>,
): String {
    val range = formatRange(window.start, window.end)
    return when {
        alsoRunning.isNotEmpty() ->
            range + " · " + alsoRunning.joinToString(", ") { "${it.name} also running, to ${formatTime(it.end)}" }

        window.isActive -> "$range · nothing else running"
        else -> range
    }
}

/** The two lanes, and the tap that names a band. */
@Composable
private fun MuhurtaBar(
    timeline: MuhurtaTimeline,
    onSelect: (Muhurta?) -> Unit,
) {
    val accents = LocalVedicAccents.current
    val lanes =
        listOf(
            TimelineLane("Good", timeline.auspicious, accents.auspicious),
            TimelineLane("Avoid", timeline.caution, accents.caution),
        )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        VedicTimelineBar(
            lanes = lanes,
            nowFraction = timeline.nowFraction,
            spokenDescription = timeline.spoken(),
        ) { laneIndex, fraction ->
            onSelect(timeline.windowAt(laneIndex, fraction))
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(BAR_LABEL_WIDTH))
            Text(
                text = formatTime(timeline.spanStart),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatTime(timeline.spanEnd),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
        }
    }
}

/** Matches [VedicTimelineBar]'s own label column, so the end times line up with the lanes. */
private val BAR_LABEL_WIDTH = 40.dp

/**
 * A real Friday, at the minute that motivated the bar.
 *
 * At 12:05 Rahu Kalam and Abhijit Muhurta are both running -- Abhijit is always the eighth fifteenth,
 * which straddles the boundary between the fourth and fifth eighths, and Friday's Rahu Kalam is the
 * fourth eighth. They overlap by 24 minutes every Friday. The heading can only name one of them; the
 * line and the bar under it are what say the rest.
 *
 * The second card is the same day with nothing running, which is roughly half of it.
 */
@ThemePreviews
@Preview(name = "Large text", fontScale = 2f, showBackground = true)
@Composable
private fun AuspiciousCardPreview() {
    VedicMitraTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(8.dp)) {
            PreviewCard(at = 12 * 60 + 5)
            PreviewCard(at = 13 * 60 + 30)
        }
    }
}

@Composable
private fun PreviewCard(at: Int) {
    val now = previewFriday(at)
    val muhurtas = PREVIEW_FRIDAY
    val window = activeOrNextMuhurta(muhurtas, now) ?: return
    AuspiciousCard(window, muhurtaTimeline(muhurtas, now), alsoRunning(muhurtas, now), now)
}

/** Minutes past midnight on an arbitrary Friday, as an instant. */
private fun previewFriday(minutes: Int): Instant =
    Instant.fromEpochMilliseconds(PREVIEW_FRIDAY_MIDNIGHT + minutes * MINUTE_MILLIS)

private const val PREVIEW_FRIDAY_MIDNIGHT = 1_789_171_200_000L

/**
 * The Friday above: sunrise 06:12, sunset 18:24, with the windows the calculator produces for it.
 * Written out rather than computed so the preview does not need an ephemeris.
 */
private val PREVIEW_FRIDAY: List<Muhurta> =
    listOf(
        previewMuhurta(MuhurtaKind.BRAHMA, "Brahma Muhurta", 4 * 60 + 36, 5 * 60 + 24, MuhurtaQuality.AUSPICIOUS),
        previewMuhurta(MuhurtaKind.GULIKA_KALAM, "Gulika Kalam", 7 * 60 + 44, 9 * 60 + 15, MuhurtaQuality.INAUSPICIOUS),
        previewMuhurta(MuhurtaKind.DUR_MUHURTA, "Dur Muhurta", 8 * 60 + 38, 9 * 60 + 27, MuhurtaQuality.INAUSPICIOUS),
        previewMuhurta(MuhurtaKind.RAHU_KALAM, "Rahu Kalam", 10 * 60 + 46, 12 * 60 + 18, MuhurtaQuality.INAUSPICIOUS),
        previewMuhurta(MuhurtaKind.ABHIJIT, "Abhijit Muhurta", 11 * 60 + 54, 12 * 60 + 42, MuhurtaQuality.AUSPICIOUS),
        previewMuhurta(MuhurtaKind.VARJYAM, "Varjyam", 14 * 60 + 50, 16 * 60 + 26, MuhurtaQuality.INAUSPICIOUS),
        previewMuhurta(MuhurtaKind.YAMAGANDA, "Yamaganda", 15 * 60 + 21, 16 * 60 + 52, MuhurtaQuality.INAUSPICIOUS),
    )

private fun previewMuhurta(
    kind: MuhurtaKind,
    name: String,
    fromMinutes: Int,
    toMinutes: Int,
    quality: MuhurtaQuality,
): Muhurta =
    Muhurta(
        kind = kind,
        name = name,
        start = previewFriday(fromMinutes),
        end = previewFriday(toMinutes),
        quality = quality,
    )

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
            // A null here is the glossary saying it has no entry, which is ordinary -- most rows
            // are values rather than named items. It is also what a translated display name would
            // produce for every row, since the glossary is still keyed on English labels; see its
            // KDoc.
            row.dayRule?.let { rule ->
                Text(
                    text = stringResource(rule.explanationRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Null is ordinary: most rows are values rather than named items, and only the named
            // ones carry a blurb. It no longer depends on the label matching anything.
            Text(
                text = row.significance?.let { stringResource(it) } ?: "More details coming soon.",
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
    val dayRule: DayRule? = null,
    @param:StringRes val significance: Int? = null,
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
        .map {
            SectionRow(
                label = it.name,
                trailing = formatRange(it.start, it.end),
                reminderTarget = ReminderTarget.Muhurta(it.kind, it.name),
                significance = PanchangaGlossary.significanceOf(it.kind),
            )
        }

/** A festival/observance as a row; observances also carry a [ReminderTarget] so they can be reminded for. */
private fun Festival.toEventRow(): SectionRow =
    SectionRow(
        label = name,
        trailing = formatDate(atSunrise),
        // Both of these used to be looked up by name. The tithis a recurring observance fires on now
        // hang off the kind, and so does its blurb -- resolved here, where the identity is in hand,
        // rather than in the sheet, which only ever had the label to go on.
        reminderTarget = kind.monthlyTithis?.let { ReminderTarget.Observance(name, it) },
        dayRule = dayRule,
        significance = PanchangaGlossary.significanceOf(kind),
    )

private const val AFTERNOON_FROM_HOUR = 12
private const val EVENING_FROM_HOUR = 17

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
            onNavigateToLocation = {},
            onOpen = {},
            onTile = {},
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
                        kind = MuhurtaKind.BRAHMA,
                        name = "Brahma Muhurta",
                        start = Instant.fromEpochMilliseconds(1_705_280_400_000L),
                        end = Instant.fromEpochMilliseconds(1_705_282_200_000L),
                        quality = MuhurtaQuality.AUSPICIOUS,
                    ),
                    Muhurta(
                        kind = MuhurtaKind.ABHIJIT,
                        name = "Abhijit Muhurta",
                        start = Instant.fromEpochMilliseconds(1_705_300_140_000L),
                        end = Instant.fromEpochMilliseconds(1_705_302_960_000L),
                        quality = MuhurtaQuality.AUSPICIOUS,
                    ),
                    Muhurta(
                        kind = MuhurtaKind.RAHU_KALAM,
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
        festivals =
            listOf(
                Festival(
                    kind = FestivalKind.GANESH_CHATURTHI,
                    name = "Ganesh Chaturthi",
                    atSunrise = Instant.fromEpochMilliseconds(1_757_808_000_000L),
                    type = FestivalType.FESTIVAL,
                    dayRule = DayRule.SUNRISE_TITHI,
                ),
                Festival(
                    kind = FestivalKind.MAKARA_SANKRANTI,
                    name = "Makara Sankranti",
                    atSunrise = Instant.fromEpochMilliseconds(1_768_003_200_000L),
                    type = FestivalType.SANKRANTI,
                    dayRule = DayRule.SUNRISE_INGRESS,
                ),
            ),
        events =
            listOf(
                Festival(
                    kind = FestivalKind.PURNIMA,
                    name = "Purnima",
                    atSunrise = Instant.fromEpochMilliseconds(1_757_030_400_000L),
                    type = FestivalType.OBSERVANCE,
                    dayRule = DayRule.SUNRISE_TITHI,
                ),
                Festival(
                    kind = FestivalKind.AMAVASYA,
                    name = "Amavasya",
                    atSunrise = Instant.fromEpochMilliseconds(1_758_240_000_000L),
                    type = FestivalType.OBSERVANCE,
                    dayRule = DayRule.SUNRISE_TITHI,
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
