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

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import io.github.vedicmitra.core.astronomy.rajjuOf
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
        uiState.result?.let { ResultCard(it) }
        uiState.porutham?.let { poruthamValue ->
            PoruthamCard(
                porutham = poruthamValue,
                groomNakshatra = uiState.groomNakshatra,
                brideNakshatra = uiState.brideNakshatra,
            )
        }
        uiState.mangal?.let { MangalCard(it) }
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

/**
 * The four porutham read beside the thirty-six gunas.
 *
 * Kept out of the guna total on purpose. These are pass-or-fail conditions rather than points, and
 * folding them in would let a strong score bury a failed Rajju — which is the one case a reader most
 * needs to see. Rajju names the limb both fall on rather than reporting a bare failure, since which
 * limb is shared is what the rule is held to say.
 */
@Composable
private fun PoruthamCard(
    porutham: AdditionalPorutham,
    groomNakshatra: Int?,
    brideNakshatra: Int?,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Four additional porutham",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${porutham.matched} / 4",
                    style = MaterialTheme.typography.titleMedium,
                    color =
                        if (porutham.matched >= 3) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                )
            }
            PoruthamRow("Mahendra", porutham.mahendra, "Progeny and longevity")
            PoruthamRow("Vedha", porutham.vedha, "Absence of mutual affliction")
            PoruthamRow(
                label = "Rajju",
                held = porutham.rajju,
                detail =
                    if (porutham.rajju || groomNakshatra == null || brideNakshatra == null) {
                        "Different limbs of the body"
                    } else {
                        "Both fall on the ${rajjuOf(groomNakshatra).displayName.lowercase()} — " +
                            "the affliction this rule warns of"
                    },
            )
            PoruthamRow("Sthree Dheerga", porutham.sthreeDheerga, "The bride's welfare and longevity")
            Text(
                text =
                    "Pass-or-fail conditions, deliberately kept out of the 36 — a strong guna score " +
                        "should not bury a failed Rajju.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One porutham: whether it holds, and what it is held to govern. */
@Composable
private fun PoruthamRow(
    label: String,
    held: Boolean,
    detail: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (held) "Matched" else "Not matched",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (held) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Mangal dosha across the pair, with its working shown.
 *
 * The working is the point. A bare "Manglik" verdict is the thing this card exists to avoid: most
 * charts trigger the rule somewhere, so the number that matters to a reader is *which* placement and
 * whether anything answers it. Every trigger and every parihara is listed.
 */
@Composable
private fun MangalCard(mangal: MangalMatch) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Mangal dosha",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (mangal.standing) "Present" else "Not standing",
                    style = MaterialTheme.typography.titleMedium,
                    color =
                        if (mangal.standing) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                )
            }
            if (mangal.mutuallyCancelled) {
                Text(
                    text =
                        "Both charts carry it, which classically answers it on both sides — the " +
                            "objection is that the partner suffers for it, and that does not arise here.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            MangalSide(label = "Groom", dosha = mangal.groom)
            MangalSide(label = "Bride", dosha = mangal.bride)
            Text(
                text =
                    "Mars in the 1st, 2nd, 4th, 7th, 8th or 12th from the lagna, the Moon or Venus. " +
                        "Traditions differ on the houses and on what may be counted from, so each " +
                        "placement is listed separately rather than merged into one verdict.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One partner's triggers and parihara. */
@Composable
private fun MangalSide(
    label: String,
    dosha: MangalDosha,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "$label — ${if (dosha.present) "afflicted" else "clear"}",
            style = MaterialTheme.typography.bodyMedium,
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
