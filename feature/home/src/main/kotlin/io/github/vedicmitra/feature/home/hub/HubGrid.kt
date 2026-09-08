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
import androidx.compose.ui.graphics.luminance
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
 * planned, and could not be applied evenly anyway — two of the glyphs are drawings carrying their
 * own tones and are never tinted (see [needsRecolouring]), so a fade would land differently on them
 * than on the rest; an outline reads as a rendering fault beside solid chips. A caption
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
        is TileIcon.Glyph ->
            Icon(
                painter = painterResource(icon.res),
                contentDescription = null,
                tint = if (icon.needsRecolouring(tile.category.container())) tint else Color.Unspecified,
                modifier = Modifier.size(38.dp),
            )

        is TileIcon.Letter ->
            Text(text = icon.text, style = MaterialTheme.typography.headlineMedium, color = tint)

        // Not the category tint the letter takes. The date stands in for artwork, and the artwork
        // beside it is drawn in the brand maroon -- so the date is drawn in the theme's maroon role
        // rather than in the chip's own foreground, and reads as one of the glyphs.
        is TileIcon.Today -> TileDateGlyph(MaterialTheme.colorScheme.tertiary)
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
 *
 * [ink] is the theme's maroon role rather than a fixed colour, which is the only way this survives
 * the dark scheme: the maroon that reads on cream is invisible on temple-stone brown, and the role
 * flips to the light tone there.
 */
@Composable
private fun TileDateGlyph(ink: Color) {
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
            color = ink,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            text = date.month,
            fontSize = monthSize,
            fontWeight = FontWeight.Medium,
            color = ink,
            maxLines = 1,
            softWrap = false,
        )
    }
}

private const val MINUTE_MILLIS = 60_000L

/**
 * Whether this glyph has to be re-coloured to be seen on the chip it is about to be drawn on.
 *
 * The artwork is inked in a fixed maroon chosen against the light scheme's cream containers, where it
 * reads at about 8:1. The dark scheme's containers are mid-tone browns of nearly the same luminance,
 * and the same ink lands between 1.09:1 and 1.51:1 there — 1.0:1 being two identical colours. Nearly
 * every tile in the hub was, in effect, a blank rectangle in the dark theme.
 *
 * Tinting is the house answer to this, not a new one: the Support tab's glyph is already drawn as an
 * alpha stencil for the same reason, and `MainActivity` says so — *"an opaque near-black illustration
 * would have half-disappeared"*. This applies it to the hub.
 *
 * Decided from the **container's own luminance** rather than from a dark-theme flag, because
 * `VedicMitraTheme` can also be handed a dynamic palette off the wallpaper, and then neither scheme's
 * values are what is on screen. Whatever the chip actually is, this asks the one question that
 * matters: is it too dark to show maroon?
 *
 * Glyphs that carry their own tones are left alone — a tint would flatten a drawing to a silhouette,
 * which is a worse loss than low contrast. Those are marked by [TileIcon.Glyph.tintable].
 */
private fun TileIcon.Glyph.needsRecolouring(chip: Color): Boolean = tintable && chip.luminance() < DARK_CHIP

/**
 * Below this the chip counts as dark. Halfway is deliberately blunt: the light containers sit around
 * 0.76 and the dark ones around 0.07, so nothing real is near the line and a more precise threshold
 * would only be false precision.
 */
private const val DARK_CHIP = 0.5f

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
