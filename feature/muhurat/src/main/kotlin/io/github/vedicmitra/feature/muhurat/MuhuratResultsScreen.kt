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
import io.github.vedicmitra.core.astronomy.MuhurtaActivity
import io.github.vedicmitra.core.astronomy.MuhurtaRating
import io.github.vedicmitra.core.astronomy.MuhurtaReason
import io.github.vedicmitra.core.astronomy.RankedMuhurtaDay
import io.github.vedicmitra.core.astronomy.hasActivityRules
import io.github.vedicmitra.core.astronomy.muhurtaRuleSourceFor
import io.github.vedicmitra.core.common.model.ContentSource
import io.github.vedicmitra.core.designsystem.component.VedicSelectField
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Instant

/**
 * The final muhurta step: the ranked best upcoming days for the chosen activity, each with its
 * rating, score and the main reasons. Loads through [MuhuratResultsViewModel].
 */
@Composable
fun MuhuratResultsScreen(
    onOpenDay: (String, Long, List<String>) -> Unit,
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
        onSelectGroom = viewModel::selectGroom,
        onSelectBride = viewModel::selectBride,
        modifier = modifier,
    )
}

@Composable
private fun MuhuratResultsContent(
    uiState: MuhuratResultsUiState,
    onOpenDay: (String, Long, List<String>) -> Unit,
    onSetWindow: (Int) -> Unit,
    onSelectProfile: (String?) -> Unit,
    onSelectGroom: (String?) -> Unit,
    onSelectBride: (String?) -> Unit,
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
                val couple = uiState.couple
                if (couple != null) {
                    CoupleSelector(couple, onSelectGroom, onSelectBride)
                } else if (uiState.profiles.isNotEmpty()) {
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
                        DayCard(day, uiState.personNames) {
                            onOpenDay(
                                uiState.activity.name,
                                day.atSunrise.toEpochMilliseconds(),
                                uiState.personIds,
                            )
                        }
                    }
                }
                Text(
                    text = personalisationNote(uiState),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = rulesNote(uiState.activity),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
    }
}

/**
 * Who the ranking was for, or how to make it personal.
 *
 * For a pair this also states the rule being applied, because it is a convention rather than
 * something derived: the app takes each factor at its worst across the two, and never trades one
 * person's strong tara against the other's weak one. A reader is entitled to know that before acting
 * on a star rating. See ADR 0023.
 */
private fun personalisationNote(uiState: MuhuratResultsUiState.Ready): String {
    val names = uiState.personNames.values.toList()
    return when {
        names.size > 1 ->
            "Personalised for ${names.joinToString(" and ")} with their Tarabala and Chandrabala. " +
                "A day counts as favourable only when it is favourable for each of them."

        names.size == 1 -> "Personalised for ${names.first()} with their Tarabala and Chandrabala."
        uiState.couple != null -> "Pick both people to personalise these days to their birth stars."
        else -> "General guidance from the day's panchanga; pick a profile to personalise it."
    }
}

/**
 * Which rules produced this ranking, and what backs them.
 *
 * The picker offers 31 activities and the engine has rules of its own for a minority of them. Until
 * this line existed, a reader choosing between two of the rest was being shown two identical lists
 * as though the choice had changed something. **An interface that offers a distinction the engine
 * does not make is making a claim it cannot keep** — so the screen says which it is doing. See
 * [#247](https://github.com/imskylab/vedic-mitra/issues/247).
 */
private fun rulesNote(activity: MuhurtaActivity): String {
    val name = activity.displayName
    if (!hasActivityRules(activity)) {
        return "Ranked by the general panchanga rules — there is nothing specific to $name in the " +
            "engine yet, so most other activities would rank these days the same way."
    }
    return when (val source = muhurtaRuleSourceFor(activity)) {
        is ContentSource.Text ->
            "Rules specific to $name. The karana rule follows ${source.label}; the nakshatras have " +
                "no text named for them yet."

        ContentSource.NotRecorded ->
            "Rules specific to $name, from the widely-taught classical preferences. No text is " +
                "named for them yet."
    }
}

/**
 * The two sides of a couple's muhurta.
 *
 * Gender-filtered, mirroring the matchmaking screen. Where that one silently prints "No Groom
 * profile." and leaves a reader wondering where their profile went, this says what is missing —
 * a gender on the profile is what these lists filter by.
 */
@Composable
private fun CoupleSelector(
    couple: CoupleSelection,
    onSelectGroom: (String?) -> Unit,
    onSelectBride: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoleSelector("Groom", couple.grooms, couple.groomId, onSelectGroom, Modifier.weight(1f))
            RoleSelector("Bride", couple.brides, couple.brideId, onSelectBride, Modifier.weight(1f))
        }
        if (couple.withoutGender > 0) {
            Text(
                text =
                    "${couple.withoutGender} profile(s) are not listed here — these two ask for a " +
                        "gender, and theirs is not set.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One side of a couple: a dropdown over the profiles eligible for that role. */
@Composable
private fun RoleSelector(
    label: String,
    options: List<MuhuratProfileOption>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (options.isEmpty()) {
        Text(
            text = "No $label profile.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }
    VedicSelectField(
        label = label,
        options = listOf<String?>(null) + options.map { it.id },
        selected = selectedId,
        optionLabel = { id -> id?.let { pid -> options.firstOrNull { it.id == pid }?.name } ?: "Not chosen" },
        onSelect = onSelect,
        modifier = modifier,
    )
}

/** A dropdown to pick whose birth chart the ranking is personalised for, or General for none. */
@Composable
private fun ProfileSelector(
    profiles: List<MuhuratProfileOption>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
) {
    VedicSelectField(
        label = "Personalise for",
        options = listOf<String?>(null) + profiles.map { it.id },
        selected = selectedId,
        optionLabel = { id -> id?.let { pid -> profiles.firstOrNull { it.id == pid }?.name } ?: "General" },
        onSelect = onSelect,
    )
}

@Composable
private fun WindowSelector(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    VedicSelectField(
        label = "Search window",
        options = MUHURAT_WINDOW_OPTIONS,
        selected = selected,
        optionLabel = { "$it days" },
        onSelect = onSelect,
    )
}

@Composable
private fun DayCard(
    day: RankedMuhurtaDay,
    personNames: Map<String, String>,
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
                    text = "+ ${attributed(reason, personNames)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            day.score.reasons.filterNot { it.favourable }.take(REASON_LIMIT).forEach { reason ->
                Text(
                    text = "− ${attributed(reason, personNames)}",
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

/**
 * A reason, naming whose it is when it is about a person.
 *
 * Formatted from one pattern rather than concatenated, because the two halves swap order in plenty of
 * languages. It is a `String.format` and not a string resource only because nothing in this module is
 * extracted yet — #218 does that, and this becomes one `getString` call when it lands.
 */
private fun attributed(
    reason: MuhurtaReason,
    personNames: Map<String, String>,
): String {
    val name = reason.personId?.let { personNames[it] } ?: return reason.text
    return REASON_FOR_PERSON.format(reason.text, name)
}

private const val REASON_FOR_PERSON = "%1\$s for %2\$s"
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
