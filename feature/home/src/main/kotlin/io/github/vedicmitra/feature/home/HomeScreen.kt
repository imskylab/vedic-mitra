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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vedicmitra.core.astronomy.AstronomySnapshot
import io.github.vedicmitra.core.astronomy.Ayana
import io.github.vedicmitra.core.astronomy.Festival
import io.github.vedicmitra.core.astronomy.FestivalType
import io.github.vedicmitra.core.astronomy.GoldenHour
import io.github.vedicmitra.core.astronomy.Karana
import io.github.vedicmitra.core.astronomy.Maasa
import io.github.vedicmitra.core.astronomy.MoonPhase
import io.github.vedicmitra.core.astronomy.MoonTimes
import io.github.vedicmitra.core.astronomy.MuhurtaQuality
import io.github.vedicmitra.core.astronomy.Nakshatra
import io.github.vedicmitra.core.astronomy.Paksha
import io.github.vedicmitra.core.astronomy.Ritu
import io.github.vedicmitra.core.astronomy.Samvatsara
import io.github.vedicmitra.core.astronomy.SunTimes
import io.github.vedicmitra.core.astronomy.Tithi
import io.github.vedicmitra.core.astronomy.Vara
import io.github.vedicmitra.core.astronomy.Yoga
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Instant

/**
 * Home screen entry point — a glanceable landing view. Resolves the location permission, drives
 * [HomeViewModel.load], and delegates to the stateless, previewable [HomeContent]. Quick actions
 * navigate out to the other destinations.
 */
@Composable
fun HomeScreen(
    onNavigateToCalendar: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToLocation: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.load()
        }

    LaunchedEffect(Unit) {
        val granted =
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.load() else permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    HomeContent(
        uiState = uiState,
        onNavigateToCalendar = onNavigateToCalendar,
        onNavigateToReminders = onNavigateToReminders,
        onNavigateToLocation = onNavigateToLocation,
        modifier = modifier,
    )
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onNavigateToCalendar: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snapshot = uiState.snapshot
    when {
        uiState.isLoading -> CenteredBox(modifier) { CircularProgressIndicator() }
        uiState.errorMessage != null ->
            CenteredBox(modifier) { Text(text = uiState.errorMessage, style = MaterialTheme.typography.bodyLarge) }

        snapshot != null ->
            Column(
                modifier =
                    modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Header(uiState.locationLabel, onNavigateToLocation)
                HeroCard(snapshot)
                uiState.auspicious?.let { AuspiciousCard(it) }
                SunMoonStrip(snapshot)
                uiState.nextFestival?.let { FestivalCard(it) }
                QuickActions(onNavigateToCalendar, onNavigateToReminders, onNavigateToLocation)
                if (uiState.usingDefaultLocation) {
                    Text(
                        text = "Showing New Delhi — grant location access for your area.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
    }
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
private fun HeroCard(snapshot: AstronomySnapshot) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = snapshot.vara.displayName, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "${snapshot.tithi.paksha.title} ${snapshot.tithi.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = "${snapshot.maasa.displayName} · ${snapshot.samvatsara.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                text = snapshot.moonPhase.displayName,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

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
private fun FestivalCard(festival: Festival) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = festival.typeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = festival.name, style = MaterialTheme.typography.titleMedium)
            }
            Text(text = formatDate(festival.atSunrise), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun QuickActions(
    onNavigateToCalendar: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToLocation: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionTile("Calendar", Icons.Filled.DateRange, onNavigateToCalendar, Modifier.weight(1f))
        ActionTile("Reminders", Icons.Filled.Notifications, onNavigateToReminders, Modifier.weight(1f))
        ActionTile("Location", Icons.Filled.LocationOn, onNavigateToLocation, Modifier.weight(1f))
    }
    OutlinedButton(onClick = onNavigateToCalendar, modifier = Modifier.fillMaxWidth()) {
        Text(text = "See full panchang")
    }
}

@Composable
private fun ActionTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.clickable(onClick = onClick)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Text(text = label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

private val Festival.typeLabel: String
    get() =
        when (type) {
            FestivalType.FESTIVAL -> "Upcoming festival"
            FestivalType.OBSERVANCE -> "Upcoming observance"
            FestivalType.SANKRANTI -> "Upcoming Sankranti"
        }

private val Paksha.title: String
    get() =
        when (this) {
            Paksha.SHUKLA -> "Shukla"
            Paksha.KRISHNA -> "Krishna"
        }

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

/** Formats a festival's sunrise instant as a local date (e.g. "Wed, 8 Nov"). */
private fun formatDate(instant: Instant): String = zoned(instant).format(festivalDateFormatter)

@Preview
@Composable
private fun HomeContentPreview() {
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
            muhurtas = emptyList(),
        )
    VedicMitraTheme {
        HomeContent(
            uiState =
                HomeUiState(
                    isLoading = false,
                    snapshot = sample,
                    auspicious =
                        AuspiciousWindow(
                            name = "Abhijit Muhurta",
                            quality = MuhurtaQuality.AUSPICIOUS,
                            boundary = Instant.fromEpochMilliseconds(1_705_302_960_000L),
                            isActive = true,
                        ),
                    nextFestival =
                        Festival(
                            name = "Ganesh Chaturthi",
                            atSunrise = Instant.fromEpochMilliseconds(1_757_808_000_000L),
                            type = FestivalType.FESTIVAL,
                        ),
                    locationLabel = "New Delhi",
                ),
            onNavigateToCalendar = {},
            onNavigateToReminders = {},
            onNavigateToLocation = {},
        )
    }
}
