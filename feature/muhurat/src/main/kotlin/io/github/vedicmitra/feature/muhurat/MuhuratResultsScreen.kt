/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.muhurat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vedicmitra.core.astronomy.DayMuhurtaScore
import io.github.vedicmitra.core.astronomy.MuhurtaRating
import io.github.vedicmitra.core.astronomy.RankedMuhurtaDay
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Instant

/**
 * The final muhurta step: the ranked best upcoming days for the chosen activity, each with its
 * rating, score and the main reasons. Loads through [MuhuratResultsViewModel].
 */
@Composable
fun MuhuratResultsScreen(
    onOpenDay: (String, Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MuhuratResultsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }
    MuhuratResultsContent(
        uiState = uiState,
        onOpenDay = onOpenDay,
        onSetWindow = viewModel::setWindow,
        onSelectProfile = viewModel::selectProfile,
        modifier = modifier,
    )
}

@Composable
private fun MuhuratResultsContent(
    uiState: MuhuratResultsUiState,
    onOpenDay: (String, Long) -> Unit,
    onSetWindow: (Int) -> Unit,
    onSelectProfile: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        MuhuratResultsUiState.Loading ->
            Column(
                modifier = modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }

        is MuhuratResultsUiState.Ready ->
            Column(
                modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Best days for ${uiState.activity.displayName}",
                    style = MaterialTheme.typography.titleLarge,
                )
                if (uiState.profiles.isNotEmpty()) {
                    ProfileSelector(
                        profiles = uiState.profiles,
                        selectedId = uiState.selectedProfileId,
                        onSelect = onSelectProfile,
                    )
                }
                WindowSelector(selected = uiState.windowDays, onSelect = onSetWindow)
                if (uiState.usingDefaultLocation) {
                    Text(
                        text = "Showing ${uiState.locationLabel} — set your location for local timings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (uiState.days.isEmpty()) {
                    Text(
                        text = "No suitable days found in the coming weeks.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                } else {
                    uiState.days.forEach { day ->
                        DayCard(day) { onOpenDay(uiState.activity.name, day.atSunrise.toEpochMilliseconds()) }
                    }
                }
                Text(
                    text = personalisationNote(uiState),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
    }
}

/** The selected profile's name, or a general-guidance note when ranking without a birth chart. */
private fun personalisationNote(uiState: MuhuratResultsUiState.Ready): String {
    val selected = uiState.profiles.firstOrNull { it.id == uiState.selectedProfileId }
    return if (selected != null) {
        "Personalised for ${selected.name} with their Tarabala and Chandrabala."
    } else {
        "General guidance from the day's panchanga; pick a profile to personalise it."
    }
}

/** Chips to pick whose birth chart the ranking is personalised for, or General for none. */
@Composable
private fun ProfileSelector(
    profiles: List<MuhuratProfileOption>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedId == null,
            onClick = { onSelect(null) },
            label = { Text(text = "General") },
        )
        profiles.forEach { profile ->
            FilterChip(
                selected = profile.id == selectedId,
                onClick = { onSelect(profile.id) },
                label = { Text(text = profile.name) },
            )
        }
    }
}

@Composable
private fun WindowSelector(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MUHURAT_WINDOW_OPTIONS.forEach { days ->
            FilterChip(
                selected = days == selected,
                onClick = { onSelect(days) },
                label = { Text(text = "$days days") },
            )
        }
    }
}

@Composable
private fun DayCard(
    day: RankedMuhurtaDay,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatDate(day.atSunrise),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                RatingBadge(day.score)
            }
            day.score.reasons.filter { it.favourable }.take(REASON_LIMIT).forEach { reason ->
                Text(
                    text = "+ ${reason.text}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            day.score.reasons.filterNot { it.favourable }.take(REASON_LIMIT).forEach { reason ->
                Text(
                    text = "− ${reason.text}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun RatingBadge(score: DayMuhurtaScore) {
    Text(
        text = "${stars(score.rating)}  ${score.rating.label}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.End,
    )
}

private const val MAX_STARS = 5
private const val REASON_LIMIT = 2

/** The rating as filled/empty stars, e.g. "★★★★☆". */
private fun stars(rating: MuhurtaRating): String = "★".repeat(rating.stars) + "☆".repeat(MAX_STARS - rating.stars)

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")

/** Formats a day's sunrise instant as a local date, e.g. "Wed, 8 Nov 2026". */
private fun formatDate(instant: Instant): String =
    java.time.Instant
        .ofEpochMilli(instant.toEpochMilliseconds())
        .atZone(ZoneId.systemDefault())
        .format(dateFormatter)
