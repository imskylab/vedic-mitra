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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    KundaliContent(uiState = uiState, onSetUpProfile = onSetUpProfile, modifier = modifier)
}

@Composable
private fun KundaliContent(
    uiState: KundaliUiState,
    onSetUpProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        KundaliUiState.Loading -> Centered(modifier) { CircularProgressIndicator() }
        KundaliUiState.NeedsProfile -> NeedsProfile(onSetUpProfile, modifier)
        is KundaliUiState.Ready -> ChartView(uiState.name, uiState.chart, modifier)
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
    name: String,
    chart: NatalChart,
    modifier: Modifier,
) {
    val now = remember { System.currentTimeMillis() }
    val currentDasha = currentDashaOf(chart, now)
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "$name's Kundali", style = MaterialTheme.typography.titleLarge)
        InfoCard(label = "Lagna (Ascendant)", value = chart.lagna.rasi.name)
        InfoCard(label = "Moon", value = "${chart.moonNakshatra.name} · pada ${chart.moonPada}")
        currentDasha?.let {
            InfoCard(label = "Current dasha", value = "${it.lord.displayName} · until ${formatMonthYear(it.end)}")
        }
        Text(text = "Grahas", style = MaterialTheme.typography.titleMedium)
        chart.grahas.forEach { GrahaRow(it) }
    }
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
        KundaliContent(uiState = KundaliUiState.Ready(name = "Leo", chart = sampleChart()), onSetUpProfile = {})
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
