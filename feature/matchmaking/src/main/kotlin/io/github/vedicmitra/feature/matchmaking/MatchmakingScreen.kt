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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vedicmitra.core.astronomy.AdditionalPorutham
import io.github.vedicmitra.core.astronomy.GunaMilanResult
import io.github.vedicmitra.core.astronomy.GunaMilanVerdict
import io.github.vedicmitra.core.astronomy.KootaScore
import io.github.vedicmitra.core.astronomy.MangalDosha
import io.github.vedicmitra.core.astronomy.PoruthamResult
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
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PickerRow("Groom", uiState.males, uiState.selectedGroomId, onSelectGroom, Modifier.weight(1f))
            PickerRow("Bride", uiState.females, uiState.selectedBrideId, onSelectBride, Modifier.weight(1f))
        }
        uiState.result?.let {
            MatchResultCard(result = it, porutham = uiState.porutham, mangal = uiState.mangal)
        }
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
    modifier: Modifier = Modifier,
) {
    val selected = options.firstOrNull { it.id == selectedId } ?: options.firstOrNull()
    if (selected == null) {
        Text(
            text = "No $label profile.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
    } else {
        VedicSelectField(
            label = label,
            options = options,
            selected = selected,
            optionLabel = { it.name },
            onSelect = { onSelect(it.id) },
            modifier = modifier,
        )
    }
}

/**
 * The whole match on one surface.
 *
 * This was three cards of equal visual weight — gunas, porutham, Mangal dosha — stacked in the order
 * they happened to be built. That put the eight koota rows above everything that can *veto* a match,
 * so a reader scrolled past the detail to reach the conditions. One card, ordered the way the
 * question is actually asked: what is the score, is anything wrong, and then the breakdown.
 *
 * The conditions stay out of the 36 for the reason they always did — a strong score should not be
 * able to bury a failed Rajju — but they now sit *above* the score's breakdown rather than beneath it.
 */
@Composable
private fun MatchResultCard(
    result: GunaMilanResult,
    porutham: AdditionalPorutham?,
    mangal: MangalMatch?,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

            if (porutham != null || mangal != null || result.doshas.isNotEmpty()) {
                SectionLabel("Conditions")
                if (result.doshas.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        result.doshas.forEach { dosha ->
                            AssistChip(onClick = {}, label = { Text(text = dosha) })
                        }
                    }
                }
                mangal?.let { MangalRow(it) }
                porutham?.all?.forEach { PoruthamRow(it) }
                Text(
                    text =
                        "Pass-or-fail conditions, deliberately kept out of the 36 — a strong guna " +
                            "score should not bury a failed Rajju.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionLabel("The thirty-six gunas")
            result.scores.forEach { KootaRow(it) }
        }
    }
}

/** A heading inside the card, so the sections read apart without three separate surfaces. */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

/**
 * Mangal dosha as one condition among the others, opening to its working on tap.
 *
 * Collapsed by default because its working is the longest here — every trigger on both sides, plus
 * any parihara — and leaving it open pushed the guna breakdown off the screen. Tapping is the same
 * gesture the koota rows already use.
 */
@Composable
private fun MangalRow(mangal: MangalMatch) {
    var expanded by remember { mutableStateOf(false) }
    val accent = if (mangal.standing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
        ConditionBar(accent)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Mangal dosha",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (mangal.standing) "Present" else "Not standing",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = accent,
                )
            }
            Text(
                text =
                    when {
                        mangal.mutuallyCancelled -> "Both charts carry it, which answers it on both sides"
                        mangal.standing -> "Tap for the placements that raise it"
                        else -> "Tap for the working"
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (expanded) {
                if (mangal.mutuallyCancelled) {
                    Text(
                        text =
                            "The objection to a Manglik marrying is that the partner suffers for it, " +
                                "and that does not arise when both carry it.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                MangalSide(label = "Groom", dosha = mangal.groom)
                MangalSide(label = "Bride", dosha = mangal.bride)
                Text(
                    text =
                        "Mars in the 1st, 2nd, 4th, 7th, 8th or 12th from the lagna, the Moon or " +
                            "Venus. Traditions differ on the houses and on what may be counted from, " +
                            "so each placement is listed separately rather than merged into one verdict.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/**
 * One porutham: what it is called, whether it holds, its working, and what it governs.
 *
 * The verdict word is deliberately not "Matched"/"Not matched". Vedha's good outcome is an
 * *absence* — nothing pierced — which "matched" misdescribes, so each rule says the thing that is
 * actually true of it.
 */
@Composable
private fun PoruthamRow(result: PoruthamResult) {
    val accent = if (result.held) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Row(modifier = Modifier.fillMaxWidth()) {
        ConditionBar(accent)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = result.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (result.held) "Holds" else "Does not hold",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = accent,
                )
            }
            Text(
                text = result.working,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = result.governs,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One partner's Mangal triggers and parihara, shown when the Mangal row is opened. */
@Composable
private fun MangalSide(
    label: String,
    dosha: MangalDosha,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 4.dp)) {
        Text(
            text = "$label — ${if (dosha.present) "afflicted" else "clear"}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
        if (!dosha.afflicted) {
            Text(
                text = "Mars falls in no house that raises the dosha.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        dosha.triggers.forEach { trigger ->
            Text(
                text = trigger.description + (trigger.cancellation?.let { " — $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (trigger.cancelled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
        }
        dosha.cancellations.forEach { cancellation ->
            Text(
                text = cancellation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The accent stripe every condition row carries.
 *
 * Shape as well as colour, so pass and fail stay distinguishable for the roughly one reader in
 * twelve who cannot separate red from green.
 */
@Composable
private fun ConditionBar(accent: Color) {
    Box(
        modifier =
            Modifier
                .padding(top = 4.dp, end = 10.dp)
                .size(width = 3.dp, height = 34.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent),
    )
}

@Composable
private fun KootaRow(score: KootaScore) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
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
        if (expanded) {
            Text(
                text = KootaSignificance.of(score.koota),
                style = MaterialTheme.typography.bodySmall,
            )
        }
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
