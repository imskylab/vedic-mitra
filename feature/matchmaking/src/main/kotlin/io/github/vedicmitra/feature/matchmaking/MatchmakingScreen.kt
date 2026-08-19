/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.matchmaking

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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vedicmitra.core.astronomy.GunaMilanResult
import io.github.vedicmitra.core.astronomy.GunaMilanVerdict
import io.github.vedicmitra.core.astronomy.KootaScore
import io.github.vedicmitra.core.designsystem.component.VedicSelectField

/**
 * Kundali matching screen. Picks one male (groom) and one female (bride) chart-ready profile and
 * renders the Ashtakoota (36-guna) breakdown. Loads through [MatchmakingViewModel].
 */
@Composable
fun MatchmakingScreen(
    onSetUpProfiles: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MatchmakingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }
    MatchmakingContent(
        uiState = uiState,
        onSelectGroom = viewModel::selectGroom,
        onSelectBride = viewModel::selectBride,
        onSetUpProfiles = onSetUpProfiles,
        modifier = modifier,
    )
}

@Composable
private fun MatchmakingContent(
    uiState: MatchmakingUiState,
    onSelectGroom: (String) -> Unit,
    onSelectBride: (String) -> Unit,
    onSetUpProfiles: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        MatchmakingUiState.Loading -> Centered(modifier) { CircularProgressIndicator() }
        is MatchmakingUiState.Ready ->
            if (uiState.males.isEmpty() || uiState.females.isEmpty()) {
                NeedsProfiles(onSetUpProfiles, modifier)
            } else {
                MatchReady(uiState, onSelectGroom, onSelectBride, modifier)
            }
    }
}

@Composable
private fun MatchReady(
    uiState: MatchmakingUiState.Ready,
    onSelectGroom: (String) -> Unit,
    onSelectBride: (String) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Kundali Matching", style = MaterialTheme.typography.titleLarge)
        PickerRow("Groom", uiState.males, uiState.selectedGroomId, onSelectGroom)
        PickerRow("Bride", uiState.females, uiState.selectedBrideId, onSelectBride)
        uiState.result?.let { ResultCard(it) }
        Text(
            text = "Ashtakoota (Guna Milan) from both Moons; general classical guidance, not a ruling.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PickerRow(
    label: String,
    options: List<MatchProfileOption>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    val selected = options.firstOrNull { it.id == selectedId } ?: options.firstOrNull()
    if (selected == null) {
        Text(
            text = "No $label profile available.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        VedicSelectField(
            label = label,
            options = options,
            selected = selected,
            optionLabel = { it.name },
            onSelect = { onSelect(it.id) },
        )
    }
}

@Composable
private fun ResultCard(result: GunaMilanResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${trim(result.total)} / ${trim(result.maxTotal)} gunas",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = result.verdict.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = verdictColor(result.verdict),
                )
            }
            if (result.doshas.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    result.doshas.forEach { dosha ->
                        AssistChip(onClick = {}, label = { Text(text = dosha) })
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            result.scores.forEach { KootaRow(it) }
        }
    }
}

@Composable
private fun KootaRow(score: KootaScore) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = score.koota.displayName,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${trim(score.points)} / ${trim(score.koota.maxPoints)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            text = score.note,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NeedsProfiles(
    onSetUpProfiles: () -> Unit,
    modifier: Modifier,
) {
    Centered(modifier) {
        Text(
            text =
                "Kundali matching needs at least one male and one female profile, each with full birth " +
                    "details (date, exact time, place) and a gender set.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onSetUpProfiles) { Text("Set up profiles") }
    }
}

@Composable
private fun verdictColor(verdict: GunaMilanVerdict): Color =
    when (verdict) {
        GunaMilanVerdict.EXCELLENT, GunaMilanVerdict.GOOD -> MaterialTheme.colorScheme.primary
        GunaMilanVerdict.AVERAGE -> MaterialTheme.colorScheme.onSurface
        GunaMilanVerdict.POOR -> MaterialTheme.colorScheme.error
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

/** Formats a guna value without a trailing ".0" (e.g. 6.0 -> "6", 1.5 -> "1.5"). */
private fun trim(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
