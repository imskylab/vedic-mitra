/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.designsystem.component

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme

/**
 * One repeating value as a picker wheel: what it was, what it is, what it becomes.
 *
 * Showing the neighbours is the point. A bare "Tithi — Chaturdashi" tells a reader what today is
 * called; putting Trayodashi behind it and Purnima ahead tells them it is a *sequence*, which is the
 * thing someone new to a panchanga does not know and cannot infer from a table.
 *
 * ## Emphasis decays outward from the middle
 *
 * The current value is the brightest thing in the row and the neighbours recede on both sides. The
 * tempting alternative — making "next" the most vivid, as something to look forward to — pulls the
 * eye onto the one column that is not true yet, which is the wrong answer to "what is it now?".
 *
 * Colours come from the theme's roles rather than fixed values, so the row follows a Material You
 * palette and dark mode without a second definition.
 *
 * ## Domain-free on purpose
 *
 * Plain strings, not a panchanga type. `core:designsystem` carries the theme and its components and
 * has no dependencies at all; teaching it about tithis to save the caller a `map` would trade that
 * away. It also means this row can show any cycle later — a dasha period, a choghadiya — without
 * being renamed.
 *
 * @param label the row's name. Deliberately quieter than the value: the label never changes and the
 *   value is the whole point.
 * @param progress how far through [current] we are, `0..1`, or `null` if that is not known. This is
 *   the only thing on screen showing *rate* — karana visibly moves faster than vara.
 * @param previousNote when [previous] gave way, e.g. "22:40".
 * @param currentNote how long [current] has left, e.g. "ends in 1h 39m".
 * @param nextNote when [next] begins.
 * @param spokenDescription the whole row as one sentence; without it a screen reader reads seven
 *   fragments and a bare percentage.
 */
@Composable
fun VedicCycleRow(
    label: String,
    previous: String,
    current: String,
    next: String,
    modifier: Modifier = Modifier,
    previousNote: String? = null,
    currentNote: String? = null,
    nextNote: String? = null,
    progress: Float? = null,
    spokenDescription: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .then(clickable)
                .padding(vertical = 6.dp)
                .rowSemantics(spokenDescription, onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            modifier = Modifier.weight(LABEL_WEIGHT),
        )
        SideCell(value = previous, note = previousNote, alpha = PAST_ALPHA, modifier = Modifier.weight(SIDE_WEIGHT))
        CurrentCell(
            value = current,
            note = currentNote,
            progress = progress,
            modifier = Modifier.weight(CURRENT_WEIGHT),
        )
        SideCell(value = next, note = nextNote, alpha = COMING_ALPHA, modifier = Modifier.weight(SIDE_WEIGHT))
    }
}

/**
 * The row's spoken identity, and — when the row is tappable — its action.
 *
 * `clearAndSetSemantics` replaces what this node reports, so the action a [androidx.compose.foundation.clickable]
 * modifier contributes has to be restated here. Without it a screen reader is handed a row it can
 * read and cannot activate, which is worse than a row that was never tappable: the explanation
 * exists, is announced as present, and cannot be opened.
 *
 * It is a separate function rather than an inline block so that `onClick` inside it unambiguously
 * means the semantics action — the composable above has a parameter of the same name, and which one
 * won would be a question no reader should have to answer.
 */
private fun Modifier.rowSemantics(
    description: String?,
    activate: (() -> Unit)?,
): Modifier =
    if (description == null) {
        this
    } else {
        clearAndSetSemantics {
            contentDescription = description
            if (activate != null) {
                role = Role.Button
                onClick { activate(); true }
            }
        }
    }

/**
 * The value now running, framed like a picker's window onto the drum.
 *
 * The frame is what makes three columns read as one wheel rather than three lists, and it is where
 * the eye should land first.
 */
@Composable
private fun CurrentCell(
    value: String,
    note: String?,
    progress: Float?,
    modifier: Modifier,
) {
    Column(
        modifier =
            modifier
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = FRAME_ALPHA))
                .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // The rollover is the one moment this screen is alive: the value slides up and its
        // replacement rises into place, once or twice a day per row. Cheap, because nothing animates
        // while nothing is changing.
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                (slideInVertically { it } + fadeIn(tween(ROLLOVER_MILLIS)))
                    .togetherWith(slideOutVertically { -it } + fadeOut(tween(ROLLOVER_MILLIS)))
                    .using(SizeTransform(clip = false))
            },
            label = "cycle-value",
        ) { shown ->
            Text(
                text = shown,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (note != null) {
            Text(
                text = note,
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = TABULAR_FIGURES),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
            )
        }
    }
}

/** A neighbour: present as context, clearly not the answer. */
@Composable
private fun SideCell(
    value: String,
    note: String?,
    alpha: Float,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (note != null) {
            Text(
                text = note,
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = TABULAR_FIGURES),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

/**
 * The three column headings, shown once above a set of rows.
 *
 * Without them a time beside a faded name is ambiguous about which end of the value it refers to.
 */
@Composable
fun VedicCycleHeader(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(bottom = 2.dp)) {
        Text(text = "", modifier = Modifier.weight(LABEL_WEIGHT))
        HeaderCell("ENDED", MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = PAST_ALPHA), SIDE_WEIGHT)
        HeaderCell("NOW", MaterialTheme.colorScheme.primary, CURRENT_WEIGHT)
        HeaderCell("NEXT", MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = COMING_ALPHA), SIDE_WEIGHT)
    }
}

@Composable
private fun RowScope.HeaderCell(
    text: String,
    color: Color,
    weight: Float,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = Modifier.weight(weight),
    )
}

/** Settled: the dimmest thing in the row. */
private const val PAST_ALPHA = 0.45f

/** Coming: quieter than now, but a little more present than what has gone. */
private const val COMING_ALPHA = 0.7f

/** The selection frame sits behind the value without competing with it. */
private const val FRAME_ALPHA = 0.55f

private const val LABEL_WEIGHT = 1.1f
private const val SIDE_WEIGHT = 1.0f
private const val CURRENT_WEIGHT = 1.4f

private const val ROLLOVER_MILLIS = 420

/** Digits of equal width, so a ticking countdown does not jitter the layout. */
private const val TABULAR_FIGURES = "tnum"

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun VedicCycleRowPreview() {
    VedicMitraTheme {
        Column(modifier = Modifier.padding(12.dp)) {
            VedicCycleHeader()
            VedicCycleRow(
                label = "Tithi",
                previous = "Trayodashi",
                current = "Chaturdashi",
                next = "Purnima",
                previousNote = "22:40",
                currentNote = "ends in 1h 39m",
                nextNote = "15:42",
                progress = 0.83f,
            )
            VedicCycleRow(
                label = "Nakshatra",
                previous = "Krittika",
                current = "Rohini",
                next = "Mrigashira",
                previousNote = "04:10",
                currentNote = "ends in 11h 17m",
                nextNote = "01:20",
                progress = 0.47f,
            )
            // Vara at a latitude with no sunrise: the value is known, its boundaries are not.
            VedicCycleRow(
                label = "Vara",
                previous = "Guruvara",
                current = "Shukravara",
                next = "Shanivara",
                currentNote = "boundary unknown",
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun VedicCycleRowDarkPreview() = VedicCycleRowPreview()

/** Where the row is tightest: long names in narrow cells at twice the font size. */
@Preview(showBackground = true, widthDp = 360, fontScale = 2f)
@Composable
private fun VedicCycleRowLargeFontPreview() {
    VedicMitraTheme {
        Column(modifier = Modifier.padding(12.dp)) {
            VedicCycleHeader()
            VedicCycleRow(
                label = "Nakshatra",
                previous = "Uttara Bhadrapada",
                current = "Purva Phalguni",
                next = "Uttara Phalguni",
                previousNote = "04:10",
                currentNote = "ends in 11h 17m",
                nextNote = "01:20",
                progress = 0.47f,
            )
        }
    }
}
