/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.home.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Tiles per row. Three keeps a label readable at a large font scale. */
private const val GRID_COLUMNS = 3

/**
 * What a tile says when its domain has no screen yet. Sentence case, not a shouty badge: the app's
 * voice is factual throughout, and this is a fact about the app rather than a promise about a date.
 */
private const val PLANNED_CAPTION = "Soon"

/** A grid of [tiles], [GRID_COLUMNS] per row. */
@Composable
internal fun TileGrid(
    tiles: List<HubTile>,
    onTile: (HubTile) -> Unit,
) {
    // Hand-rolled rather than a LazyVerticalGrid on purpose: every caller sits inside a
    // verticalScroll, and nesting a lazy grid in an unbounded scrollable throws at runtime.
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(GRID_COLUMNS).forEach { rowTiles ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowTiles.forEach { tile ->
                    Box(modifier = Modifier.weight(1f)) { Tile(tile) { onTile(tile) } }
                }
                repeat(GRID_COLUMNS - rowTiles.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

/** A quiet heading above a grid. */
@Composable
internal fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

/**
 * One tile.
 *
 * Every tile is a filled chip at full strength, in its **category's** colour — the chip says what
 * kind of thing this is, never how far along it is. A domain that is not built yet says so in
 * words, in a **"Soon"** caption under its label.
 *
 * Words rather than a treatment, because every non-verbal cue this tile could carry fails for
 * someone. Colour fails in greyscale and for colour blindness; a fade reads as broken rather than
 * planned, and cannot tint the brand glyphs anyway since they hold their own maroon and are drawn
 * with `Color.Unspecified`; an outline reads as a rendering fault beside solid chips. A caption
 * survives all of it, and survives a large font scale by growing with everything else.
 *
 * The caption is hidden from accessibility services on purpose: the tile already carries
 * `stateDescription = "Not built yet"`, and without clearing it a screen reader would announce the
 * same fact twice.
 *
 * No padlock, deliberately: nothing in this app unlocks, and a lock reads as a paywall.
 */
@Composable
private fun Tile(
    tile: HubTile,
    onClick: () -> Unit,
) {
    val built = tile.action !is TileAction.NotYet
    val shape = RoundedCornerShape(15.dp)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { if (!built) stateDescription = "Not built yet" }
                .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(52.dp)
                    .clip(shape)
                    .background(tile.category.container(), shape),
            contentAlignment = Alignment.Center,
        ) { TileGlyph(tile) }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = tile.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (built) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (!built) {
            Text(
                text = PLANNED_CAPTION,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
}

/** The tile's icon, in whichever of the three styles it carries. */
@Composable
private fun TileGlyph(tile: HubTile) {
    val tint = tile.category.onContainer()
    when (val icon = tile.icon) {
        // The brand glyphs are drawn in their own maroon, so they are never tinted.
        is TileIcon.Glyph ->
            Icon(
                painter = painterResource(icon.res),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(38.dp),
            )

        is TileIcon.Symbol ->
            Icon(
                imageVector = icon.vector,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(42.dp),
            )

        is TileIcon.Letter ->
            Text(text = icon.text, style = MaterialTheme.typography.headlineMedium, color = tint)
    }
}

/** The container colour a tile's category tints it with. */
@Composable
internal fun HubCategory.container(): Color =
    when (this) {
        HubCategory.DAILY -> MaterialTheme.colorScheme.primaryContainer
        HubCategory.ASTROLOGY -> MaterialTheme.colorScheme.secondaryContainer
        HubCategory.DEVOTION -> MaterialTheme.colorScheme.tertiaryContainer
    }

/** The matching foreground colour, for symbols and letters. */
@Composable
internal fun HubCategory.onContainer(): Color =
    when (this) {
        HubCategory.DAILY -> MaterialTheme.colorScheme.onPrimaryContainer
        HubCategory.ASTROLOGY -> MaterialTheme.colorScheme.onSecondaryContainer
        HubCategory.DEVOTION -> MaterialTheme.colorScheme.onTertiaryContainer
    }
