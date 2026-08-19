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

package io.github.vedicmitra.feature.japa

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vedicmitra.core.astronomy.Graha
import io.github.vedicmitra.core.designsystem.component.VedicSelectField
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme

/**
 * Japa screen. Counts malas for the chosen mantra via [JapaViewModel], persisting the count to resume,
 * with panchanga hooks (a mahadasha-lord beeja suggestion and today's Brahma Muhurta) and a history of
 * sittings. The stateless [JapaContent] is previewable.
 */
@Composable
fun JapaScreen(
    modifier: Modifier = Modifier,
    viewModel: JapaViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }
    JapaContent(
        uiState = uiState,
        onCountBead = viewModel::countBead,
        onSelectMantra = viewModel::selectMantra,
        onFinish = viewModel::finish,
        onReset = viewModel::reset,
        modifier = modifier,
    )
}

@Composable
private fun JapaContent(
    uiState: JapaUiState,
    onCountBead: () -> Unit,
    onSelectMantra: (String) -> Unit,
    onFinish: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        JapaUiState.Loading ->
            Column(
                modifier = modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }

        is JapaUiState.Ready -> ReadyView(uiState, onCountBead, onSelectMantra, onFinish, onReset, modifier)
    }
}

@Composable
private fun ReadyView(
    state: JapaUiState.Ready,
    onCountBead: () -> Unit,
    onSelectMantra: (String) -> Unit,
    onFinish: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var previousRounds by remember { mutableIntStateOf(state.rounds) }
    LaunchedEffect(state.rounds) {
        if (state.rounds > previousRounds) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        previousRounds = state.rounds
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Japa", style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth())

        state.suggestion?.let { suggestion ->
            AssistChip(
                onClick = { onSelectMantra(suggestion.mantra.id) },
                label = { Text("In ${suggestion.dashaLordName} mahadasha — chant ${suggestion.mantra.name}") },
            )
        }

        MantraPicker(state.mantras, state.mantra.id, onSelectMantra)
        MantraHeading(state.mantra)
        BeadCounter(beads = state.beads, rounds = state.rounds, onCountBead = onCountBead)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onFinish, enabled = state.beads > 0) { Text("Finish") }
            OutlinedButton(onClick = onReset, enabled = state.beads > 0) { Text("Reset") }
        }

        Text(
            text = "Today: ${state.todayBeads} beads · Streak: ${state.streak} ${dayWord(state.streak)}",
            style = MaterialTheme.typography.bodyMedium,
        )

        state.brahmaMuhurta?.let {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Brahma Muhurta today · ${it.label} — an auspicious time to sit.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        if (state.history.isNotEmpty()) {
            HistorySection(state.history)
        }
    }
}

@Composable
private fun MantraPicker(
    mantras: List<Mantra>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    VedicSelectField(
        label = "Mantra",
        options = mantras,
        selected = mantras.firstOrNull { it.id == selectedId } ?: mantras.first(),
        optionLabel = { it.name },
        onSelect = { onSelect(it.id) },
    )
}

@Composable
private fun MantraHeading(mantra: Mantra) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = mantra.devanagari,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = mantra.transliteration,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BeadCounter(
    beads: Int,
    rounds: Int,
    onCountBead: () -> Unit,
) {
    val displayBead = if (beads == 0) 0 else ((beads - 1) % JapaLogic.BEADS_PER_MALA) + 1
    val fraction = displayBead.toFloat() / JapaLogic.BEADS_PER_MALA
    Box(
        modifier = Modifier.size(220.dp).clickable(onClick = onCountBead),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 10.dp,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "$displayBead", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
            Text(text = "of ${JapaLogic.BEADS_PER_MALA}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "$rounds ${malaWord(rounds)} done",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Text(
        text = "Tap the circle to count",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun HistorySection(history: List<JapaSessionView>) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "History", style = MaterialTheme.typography.titleMedium)
        history.forEach { session ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = session.mantraName, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = session.dateLabel + (session.nakshatraLabel?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "${session.rounds} ${malaWord(session.rounds)} · ${session.beads}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            HorizontalDivider()
        }
    }
}

private fun malaWord(n: Int): String = if (n == 1) "mala" else "malas"

private fun dayWord(n: Int): String = if (n == 1) "day" else "days"

@Preview
@Composable
private fun JapaContentPreview() {
    VedicMitraTheme {
        JapaContent(
            uiState =
                JapaUiState.Ready(
                    mantra = MantraCatalog.default,
                    mantras = MantraCatalog.all,
                    beads = 145,
                    rounds = 1,
                    beadInMala = 37,
                    todayBeads = 253,
                    streak = 4,
                    history =
                        listOf(
                            JapaSessionView(
                                dateLabel = "Mon, 17 Aug",
                                mantraName = "Gayatri Mantra",
                                rounds = 2,
                                beads = 216,
                                nakshatraLabel = "Rohini",
                            ),
                            JapaSessionView(
                                dateLabel = "Sun, 16 Aug",
                                mantraName = "Om Namah Shivaya",
                                rounds = 1,
                                beads = 108,
                                nakshatraLabel = null,
                            ),
                        ),
                    suggestion =
                        MantraCatalog.forGraha(Graha.SHANI)?.let { MantraSuggestion(it, "Shani") },
                    brahmaMuhurta = BrahmaMuhurtaView("04:52 – 05:40"),
                ),
            onCountBead = {},
            onSelectMantra = {},
            onFinish = {},
            onReset = {},
        )
    }
}
