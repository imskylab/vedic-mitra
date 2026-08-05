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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vedicmitra.core.astronomy.AstronomySnapshot
import io.github.vedicmitra.core.astronomy.Ayana
import io.github.vedicmitra.core.astronomy.GoldenHour
import io.github.vedicmitra.core.astronomy.Karana
import io.github.vedicmitra.core.astronomy.MoonPhase
import io.github.vedicmitra.core.astronomy.MoonTimes
import io.github.vedicmitra.core.astronomy.Muhurta
import io.github.vedicmitra.core.astronomy.MuhurtaQuality
import io.github.vedicmitra.core.astronomy.Nakshatra
import io.github.vedicmitra.core.astronomy.Paksha
import io.github.vedicmitra.core.astronomy.Ritu
import io.github.vedicmitra.core.astronomy.SunTimes
import io.github.vedicmitra.core.astronomy.Tithi
import io.github.vedicmitra.core.astronomy.Vara
import io.github.vedicmitra.core.astronomy.Yoga
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Instant

/**
 * Home screen entry point. Resolves the location permission, drives [HomeViewModel.load], and
 * renders today's panchanga. The stateless [HomeContent] takes state as a parameter so it is
 * trivially previewable.
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Reload regardless of the result — the ViewModel falls back to a default location.
            viewModel.load()
        }

    LaunchedEffect(Unit) {
        val granted =
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.load()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    HomeContent(uiState = uiState, modifier = modifier)
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when {
            uiState.isLoading -> CircularProgressIndicator()
            uiState.errorMessage != null ->
                Text(text = uiState.errorMessage, style = MaterialTheme.typography.bodyLarge)

            uiState.snapshot != null -> {
                Panchanga(snapshot = uiState.snapshot)
                if (uiState.usingDefaultLocation) {
                    Text(
                        text = "Showing New Delhi — grant location access for your area.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Panchanga(snapshot: AstronomySnapshot) {
    Text(text = snapshot.vara.displayName, style = MaterialTheme.typography.headlineMedium)
    PanchangaRow(label = "Tithi", value = "${snapshot.tithi.paksha.title} ${snapshot.tithi.name}")
    PanchangaRow(label = "Nakshatra", value = snapshot.nakshatra.name)
    PanchangaRow(label = "Yoga", value = snapshot.yoga.name)
    PanchangaRow(label = "Karana", value = snapshot.karana.name)
    PanchangaRow(label = "Ayana", value = snapshot.ayana.displayName)
    PanchangaRow(label = "Ritu", value = snapshot.ritu.displayName)
    PanchangaRow(label = "Sunrise", value = formatTime(snapshot.sunTimes.sunrise))
    PanchangaRow(label = "Sunset", value = formatTime(snapshot.sunTimes.sunset))
    PanchangaRow(label = "Moonrise", value = formatTime(snapshot.moonTimes.moonrise))
    PanchangaRow(label = "Moonset", value = formatTime(snapshot.moonTimes.moonset))
    PanchangaRow(label = "Moon Phase", value = snapshot.moonPhase.displayName)
    if (snapshot.goldenHour.morningStart != null || snapshot.goldenHour.morningEnd != null) {
        PanchangaRow(
            label = "Golden Hour (Morning)",
            value = "${formatTime(snapshot.goldenHour.morningStart)}–${formatTime(snapshot.goldenHour.morningEnd)}",
        )
    }
    if (snapshot.goldenHour.eveningStart != null || snapshot.goldenHour.eveningEnd != null) {
        PanchangaRow(
            label = "Golden Hour (Evening)",
            value = "${formatTime(snapshot.goldenHour.eveningStart)}–${formatTime(snapshot.goldenHour.eveningEnd)}",
        )
    }
    snapshot.muhurtas.forEach { muhurta ->
        PanchangaRow(
            label = muhurta.name,
            value = "${formatTime(muhurta.start)}–${formatTime(muhurta.end)}",
        )
    }
}

@Composable
private fun PanchangaRow(
    label: String,
    value: String,
) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 8.dp),
    )
}

private val Paksha.title: String
    get() =
        when (this) {
            Paksha.SHUKLA -> "Shukla"
            Paksha.KRISHNA -> "Krishna"
        }

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Formats an instant as local wall-clock time in the device's zone, or an em dash if absent. */
private fun formatTime(instant: Instant?): String {
    if (instant == null) return "—"
    val local =
        java.time.Instant
            .ofEpochMilli(instant.toEpochMilliseconds())
            .atZone(ZoneId.systemDefault())
    return local.format(timeFormatter)
}

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
            ayana = Ayana.UTTARAYANA,
            ritu = Ritu.SHISHIRA,
            moonPhase = MoonPhase.WAXING_GIBBOUS,
            goldenHour =
                GoldenHour(
                    morningStart = Instant.fromEpochMilliseconds(1_705_281_000_000L),
                    morningEnd = Instant.fromEpochMilliseconds(1_705_283_880_000L),
                    eveningStart = Instant.fromEpochMilliseconds(1_705_318_740_000L),
                    eveningEnd = Instant.fromEpochMilliseconds(1_705_321_620_000L),
                ),
            muhurtas =
                listOf(
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
    VedicMitraTheme {
        HomeContent(uiState = HomeUiState(isLoading = false, snapshot = sample))
    }
}
