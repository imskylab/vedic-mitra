/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

@file:Suppress("MagicNumber")

package io.github.vedicmitra.feature.rashifal

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vedicmitra.core.astronomy.Bala
import io.github.vedicmitra.core.astronomy.Nakshatra
import io.github.vedicmitra.core.astronomy.OutlookBand
import io.github.vedicmitra.core.astronomy.RASHI_NAMES
import io.github.vedicmitra.core.astronomy.RashiDay
import io.github.vedicmitra.core.astronomy.RashiOutlook
import io.github.vedicmitra.core.astronomy.Rasi
import io.github.vedicmitra.core.astronomy.Tara
import io.github.vedicmitra.core.astronomy.Vara
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Instant

private val AuspiciousColor = Color(0xFF2E7D32)
private val MixedColor = Color(0xFFB07A00)
private val ChallengingColor = Color(0xFFB3261E)

/**
 * Rashifal screen. Loads the selected sign's transit outlook via [RashifalViewModel] and renders
 * today's verdict plus the week ahead, letting the user switch profile or browse any rashi. The
 * stateless [RashifalContent] is previewable.
 */
@Composable
fun RashifalScreen(
    onSetUpProfile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RashifalViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }
    RashifalContent(
        uiState = uiState,
        onSelectSign = viewModel::selectSign,
        onSelectProfile = viewModel::selectProfile,
        onSetUpProfile = onSetUpProfile,
        modifier = modifier,
    )
}

@Composable
private fun RashifalContent(
    uiState: RashifalUiState,
    onSelectSign: (Int) -> Unit,
    onSelectProfile: (String) -> Unit,
    onSetUpProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        RashifalUiState.Loading ->
            Column(
                modifier = modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }

        is RashifalUiState.Ready -> ReadyView(uiState, onSelectSign, onSelectProfile, onSetUpProfile, modifier)
    }
}

@Composable
private fun ReadyView(
    state: RashifalUiState.Ready,
    onSelectSign: (Int) -> Unit,
    onSelectProfile: (String) -> Unit,
    onSetUpProfile: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Rashifal", style = MaterialTheme.typography.titleLarge)

        if (state.profiles.isEmpty()) {
            AddProfileHint(onSetUpProfile)
        } else if (state.profiles.size > 1) {
            ProfilePicker(state.profiles, state.selectedProfileId, onSelectProfile)
        }

        SignPicker(state.signs, state.selectedRasiIndex, state.yourRasiIndex, onSelectSign)

        val outlook = state.outlook
        if (outlook == null) {
            Text(
                text = "Couldn't compute the outlook right now. Check your connection and try again.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            TodayCard(outlook, state.personalized)
            WeekCard(outlook)
        }

        if (state.usingDefaultLocation) {
            Text(
                text = "Showing ${state.locationLabel} — grant location access for your area.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AddProfileHint(onSetUpProfile: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Add a birth profile to personalise your reading with Tarabala from your birth star.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onSetUpProfile) { Text("Set up profile") }
        }
    }
}

/** A horizontally scrolling row of chips to pick which profile's sign is read. */
@Composable
private fun ProfilePicker(
    profiles: List<RashifalProfileOption>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        profiles.forEach { profile ->
            FilterChip(
                selected = profile.id == selectedId,
                onClick = { onSelect(profile.id) },
                label = { Text(text = profile.name) },
            )
        }
    }
}

/** The twelve rashis as chips; the profile's own sign is marked with a star. */
@Composable
private fun SignPicker(
    signs: List<RashiOption>,
    selectedIndex: Int,
    yourIndex: Int?,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        signs.forEach { sign ->
            FilterChip(
                selected = sign.index == selectedIndex,
                onClick = { onSelect(sign.index) },
                label = { Text(text = sign.name) },
                leadingIcon =
                    if (sign.index == yourIndex) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Your sign",
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        }
                    } else {
                        null
                    },
            )
        }
    }
}

@Composable
private fun TodayCard(
    outlook: RashiOutlook,
    personalized: Boolean,
) {
    val today = outlook.today
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${outlook.rasi.name} · Today",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                BandBadge(today.band)
            }
            Text(
                text = RashifalText.headline(today.band),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = RashifalText.chandraNarrative(today.chandraPosition),
                style = MaterialTheme.typography.bodyMedium,
            )
            today.tara?.takeIf { personalized }?.let {
                Text(text = RashifalText.taraNarrative(it), style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text =
                    "Moon in ${today.nakshatra.name} · ${today.moonRasi.name} " +
                        "(the ${ordinal(today.chandraPosition)} from ${outlook.rasi.name})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!personalized) {
                Text(
                    text = "Sign-only transit reading. Pick your own sign (starred) for a Tarabala-personalised one.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WeekCard(outlook: RashiOutlook) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "The week ahead", style = MaterialTheme.typography.titleMedium)
            Text(
                text = RashifalText.weekSummary(outlook),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                outlook.week.forEach { WeekDayPip(it) }
            }
        }
    }
}

@Composable
private fun WeekDayPip(day: RashiDay) {
    Column(
        modifier = Modifier.width(44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = weekdayOf(day.atSunrise), style = MaterialTheme.typography.labelSmall)
        Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(bandColor(day.band)))
        Text(
            text = dayOfMonthOf(day.atSunrise),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BandBadge(band: OutlookBand) {
    Surface(color = bandColor(band), shape = MaterialTheme.shapes.small) {
        Text(
            text = band.label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

private fun bandColor(band: OutlookBand): Color =
    when (band) {
        OutlookBand.AUSPICIOUS -> AuspiciousColor
        OutlookBand.MIXED -> MixedColor
        OutlookBand.CHALLENGING -> ChallengingColor
    }

private fun ordinal(n: Int): String =
    when (n) {
        1 -> "1st"
        2 -> "2nd"
        3 -> "3rd"
        else -> "${n}th"
    }

private val weekdayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE")
private val dayOfMonthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d")

private fun weekdayOf(instant: Instant): String = zoned(instant).format(weekdayFormatter)

private fun dayOfMonthOf(instant: Instant): String = zoned(instant).format(dayOfMonthFormatter)

private fun zoned(instant: Instant) =
    java.time.Instant
        .ofEpochMilli(instant.toEpochMilliseconds())
        .atZone(ZoneId.systemDefault())

@Preview
@Composable
private fun RashifalContentPreview() {
    VedicMitraTheme {
        RashifalContent(
            uiState = sampleReady(),
            onSelectSign = {},
            onSelectProfile = {},
            onSetUpProfile = {},
        )
    }
}

private fun sampleReady(): RashifalUiState.Ready {
    val bands = listOf(OutlookBand.AUSPICIOUS, OutlookBand.MIXED, OutlookBand.CHALLENGING)
    val positions = listOf(11, 12, 1, 3, 4, 6, 7)
    val week =
        positions.mapIndexed { i, position ->
            RashiDay(
                atSunrise = Instant.fromEpochMilliseconds(1_760_000_000_000L + i * 86_400_000L),
                moonRasi = Rasi(index = (position - 1) % 12, name = RASHI_NAMES[(position - 1) % 12]),
                nakshatra = Nakshatra(number = 5, name = "Mrigashira"),
                vara = Vara.entries[i % Vara.entries.size],
                chandraPosition = position,
                chandrabala = if (position in setOf(1, 3, 6, 7, 10, 11)) Bala.STRONG else Bala.WEAK,
                tara = Tara(number = 2, name = "Sampat", strength = Bala.STRONG),
                band = bands[i % bands.size],
            )
        }
    return RashifalUiState.Ready(
        signs = RASHI_NAMES.mapIndexed { index, name -> RashiOption(index, name) },
        selectedRasiIndex = 0,
        profiles = listOf(RashifalProfileOption(id = "a", name = "Leo"), RashifalProfileOption(id = "b", name = "Mia")),
        selectedProfileId = "a",
        yourRasiIndex = 0,
        personalized = true,
        locationLabel = "New Delhi",
        usingDefaultLocation = false,
        outlook = RashiOutlook(rasi = Rasi(0, "Mesha"), personalized = true, today = week.first(), week = week),
    )
}
