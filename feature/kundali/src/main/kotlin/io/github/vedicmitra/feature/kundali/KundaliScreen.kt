/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.kundali

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vedicmitra.core.astronomy.Graha
import io.github.vedicmitra.core.astronomy.Lagna
import io.github.vedicmitra.core.astronomy.MahadashaPeriod
import io.github.vedicmitra.core.astronomy.Nakshatra
import io.github.vedicmitra.core.astronomy.NatalChart
import io.github.vedicmitra.core.astronomy.NatalGraha
import io.github.vedicmitra.core.astronomy.Rasi
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Instant

/**
 * Kundali screen. Loads the primary profile's [NatalChart] via [KundaliViewModel] and renders it, or
 * prompts to set up a birth profile. The stateless [KundaliContent] is previewable and testable.
 */
@Composable
fun KundaliScreen(
    onSetUpProfile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: KundaliViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }
    KundaliContent(
        uiState = uiState,
        onSetUpProfile = onSetUpProfile,
        onSelectProfile = viewModel::select,
        modifier = modifier,
    )
}

@Composable
private fun KundaliContent(
    uiState: KundaliUiState,
    onSetUpProfile: () -> Unit,
    onSelectProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        KundaliUiState.Loading -> Centered(modifier) { CircularProgressIndicator() }
        KundaliUiState.NeedsProfile -> NeedsProfile(onSetUpProfile, modifier)
        is KundaliUiState.Ready -> ChartView(uiState, onSelectProfile, modifier)
    }
}

@Composable
private fun NeedsProfile(
    onSetUpProfile: () -> Unit,
    modifier: Modifier,
) {
    Centered(modifier) {
        Text(
            text = "Add your date, exact time and place of birth to see your kundali.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onSetUpProfile) { Text("Set up profile") }
    }
}

@Composable
private fun ChartView(
    uiState: KundaliUiState.Ready,
    onSelectProfile: (String) -> Unit,
    modifier: Modifier,
) {
    val chart = uiState.chart
    val now = remember { System.currentTimeMillis() }
    val currentDasha = currentDashaOf(chart, now)
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (uiState.options.size > 1) {
            ProfilePicker(options = uiState.options, selectedId = uiState.selectedId, onSelect = onSelectProfile)
        }
        Text(text = "${uiState.name}'s Kundali", style = MaterialTheme.typography.titleLarge)
        NorthIndianChart(chart)
        Text(
            text =
                "North-Indian style. House 1 (top centre) is the lagna; numbers are the rashi in each " +
                    "house. Retrograde grahas are shown in red.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        InfoCard(label = "Lagna (Ascendant)", value = chart.lagna.rasi.name)
        InfoCard(label = "Moon", value = "${chart.moonNakshatra.name} · pada ${chart.moonPada}")
        currentDasha?.let {
            InfoCard(label = "Current dasha", value = "${it.lord.displayName} · until ${formatMonthYear(it.end)}")
        }
        Text(text = "Grahas", style = MaterialTheme.typography.titleMedium)
        chart.grahas.forEach { GrahaRow(it) }
    }
}

/** A horizontally scrolling row of chips to pick which chart-ready profile's kundali to show. */
@Composable
private fun ProfilePicker(
    options: List<KundaliProfileOption>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option.id == selectedId,
                onClick = { onSelect(option.id) },
                label = { Text(text = option.name) },
            )
        }
    }
}

/**
 * A North-Indian style chart: a fixed square divided by both diagonals and the diamond joining the
 * side midpoints, giving twelve houses. House 1 is the top-centre triangle and the rest run
 * anticlockwise. Each house shows the number of the rashi that falls in it and the grahas placed
 * there (retrograde in the error colour).
 */
@Composable
private fun NorthIndianChart(
    chart: NatalChart,
    modifier: Modifier = Modifier,
) {
    val line = MaterialTheme.colorScheme.outline
    Box(modifier = modifier.fillMaxWidth().aspectRatio(1f)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val s = 1.5.dp.toPx()
            drawRect(color = line, style = Stroke(width = s))
            drawLine(line, Offset(0f, 0f), Offset(w, h), s)
            drawLine(line, Offset(w, 0f), Offset(0f, h), s)
            drawLine(line, Offset(w / 2f, 0f), Offset(w, h / 2f), s)
            drawLine(line, Offset(w, h / 2f), Offset(w / 2f, h), s)
            drawLine(line, Offset(w / 2f, h), Offset(0f, h / 2f), s)
            drawLine(line, Offset(0f, h / 2f), Offset(w / 2f, 0f), s)
        }
        HOUSE_ANCHORS.forEachIndexed { index, anchor ->
            HouseCell(
                rasi = chart.houses.getOrNull(index),
                grahas = chart.grahas.filter { it.house == index + 1 },
                modifier = Modifier.align(BiasAlignment(anchor.first, anchor.second)),
            )
        }
    }
}

/** One house of the chart: the rashi number above the grahas placed in that house. */
@Composable
private fun HouseCell(
    rasi: Rasi?,
    grahas: List<NatalGraha>,
    modifier: Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (rasi != null) {
            Text(
                text = "${rasi.index + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        grahas.forEach { graha ->
            Text(
                text = graha.graha.shortLabel,
                style = MaterialTheme.typography.bodySmall,
                color = if (graha.retrograde) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Horizontal/vertical bias (each in -1..1) that centres each house's label at its centroid. */
private val HOUSE_ANCHORS: List<Pair<Float, Float>> =
    listOf(
        0.0f to -0.40f, // 1 — top centre (lagna)
        -0.50f to -0.76f, // 2 — top left
        -0.76f to -0.50f, // 3 — left upper
        -0.44f to 0.0f, // 4 — left centre
        -0.76f to 0.50f, // 5 — left lower
        -0.50f to 0.76f, // 6 — bottom left
        0.0f to 0.40f, // 7 — bottom centre
        0.50f to 0.76f, // 8 — bottom right
        0.76f to 0.50f, // 9 — right lower
        0.44f to 0.0f, // 10 — right centre
        0.76f to -0.50f, // 11 — right upper
        0.50f to -0.76f, // 12 — top right
    )

/** Two-letter code used inside the chart cells. */
private val Graha.shortLabel: String
    get() =
        when (this) {
            Graha.SUN -> "Su"
            Graha.MOON -> "Mo"
            Graha.MANGALA -> "Ma"
            Graha.BUDHA -> "Me"
            Graha.GURU -> "Ju"
            Graha.SHUKRA -> "Ve"
            Graha.SHANI -> "Sa"
            Graha.RAHU -> "Ra"
            Graha.KETU -> "Ke"
        }

@Composable
private fun InfoCard(
    label: String,
    value: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun GrahaRow(graha: NatalGraha) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = graha.graha.displayName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(text = graha.rasi.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            text = "House ${graha.house}" + if (graha.retrograde) " · R" else "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Centered(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { content() }
}

private fun currentDashaOf(
    chart: NatalChart,
    nowMillis: Long,
): MahadashaPeriod? =
    chart.vimshottari.firstOrNull {
        nowMillis >= it.start.toEpochMilliseconds() && nowMillis < it.end.toEpochMilliseconds()
    }

private val monthYearFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy")

private fun formatMonthYear(instant: Instant): String =
    java.time.Instant
        .ofEpochMilli(instant.toEpochMilliseconds())
        .atZone(ZoneId.systemDefault())
        .format(monthYearFormatter)

@Preview
@Composable
private fun KundaliContentPreview() {
    VedicMitraTheme {
        KundaliContent(
            uiState =
                KundaliUiState.Ready(
                    name = "Leo",
                    chart = sampleChart(),
                    selectedId = "a",
                    options =
                        listOf(
                            KundaliProfileOption(id = "a", name = "Leo"),
                            KundaliProfileOption(id = "b", name = "Mia"),
                        ),
                ),
            onSetUpProfile = {},
            onSelectProfile = {},
        )
    }
}

@Suppress("MagicNumber")
private fun sampleChart(): NatalChart =
    NatalChart(
        lagna = Lagna(siderealLongitude = 15.0, rasi = Rasi(0, "Mesha")),
        houses = (0 until 12).map { Rasi(it, "Rashi ${it + 1}") },
        grahas =
            listOf(
                NatalGraha(Graha.SUN, 10.0, Rasi(0, "Mesha"), house = 1, retrograde = false),
                NatalGraha(Graha.MOON, 42.0, Rasi(1, "Vrishabha"), house = 2, retrograde = false),
                NatalGraha(Graha.SHANI, 195.0, Rasi(6, "Tula"), house = 7, retrograde = true),
            ),
        moonNakshatra = Nakshatra(number = 3, name = "Krittika"),
        moonPada = 2,
        vimshottari =
            listOf(
                MahadashaPeriod(
                    lord = Graha.KETU,
                    start = Instant.fromEpochMilliseconds(0L),
                    end = Instant.fromEpochMilliseconds(9_999_999_999_999L),
                ),
            ),
    )
