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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.LocalDate

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

        is TileIcon.Letter ->
            Text(text = icon.text, style = MaterialTheme.typography.headlineMedium, color = tint)

        is TileIcon.Today -> TileDateGlyph(tint)
    }
}

/**
 * Today's date, drawn in place of a symbol.
 *
 * Re-read once a minute so a hub left open overnight does not keep yesterday's number. The same
 * ticker the tithi countdown and the auspicious strip use; a minute is far finer than a date needs,
 * but re-setting an equal [LocalDate] does not recompose, so the cost is a comparison a minute and
 * it stays the one idiom in the module rather than a second one.
 *
 * The two lines are announced as a single date instead of as themselves — see [TileDate.spoken].
 * The tile's own label follows, so a reader hears "8 September, Today's Panchanga".
 *
 * Sized in dp converted to sp rather than in sp: this is an icon inside a chip that is a fixed
 * 52.dp, so text that grew with the font scale would spill out of it. It still tracks display
 * density. The label underneath is the part that grows, as it should.
 */
@Composable
private fun TileDateGlyph(tint: Color) {
    val today by produceState(LocalDate.now()) {
        while (true) {
            value = LocalDate.now()
            delay(MINUTE_MILLIS)
        }
    }
    val date = tileDate(today)
    val density = LocalDensity.current
    val daySize = with(density) { 22.dp.toSp() }
    val monthSize = with(density) { 10.dp.toSp() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clearAndSetSemantics { contentDescription = date.spoken },
    ) {
        // Line height left to the font on purpose. Tightening it to the font size would fit the two
        // lines a little closer, and would clip the matras of a script that draws above the letter --
        // which is exactly what the month abbreviation becomes once #190 lands.
        Text(
            text = date.day,
            fontSize = daySize,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            text = date.month,
            fontSize = monthSize,
            fontWeight = FontWeight.Medium,
            color = tint,
            maxLines = 1,
            softWrap = false,
        )
    }
}

private const val MINUTE_MILLIS = 60_000L

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
