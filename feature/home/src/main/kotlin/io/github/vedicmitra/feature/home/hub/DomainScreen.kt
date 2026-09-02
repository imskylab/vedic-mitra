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

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme

/**
 * What one shastra holds — the hub's second level.
 *
 * No ViewModel, following the pattern the muhurat flow's first step already uses: everything on this
 * screen is a pure function of [domain], so there is nothing to load and nothing to keep. That also
 * means opening it costs none of the astronomy the Home screen resolves.
 *
 * The domain's own name is drawn here rather than left to the app bar, which titles every level-two
 * screen the same way — the same trade the four muhurat steps already make.
 */
@Composable
fun DomainScreen(
    domain: HubDomain,
    onOpen: (HubTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val tiles = HubCatalog.tilesIn(domain)
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = domain.label, style = MaterialTheme.typography.titleLarge)
        Text(
            text = domain.blurb,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TileGrid(tiles) { tile ->
            when (val action = tile.action) {
                is TileAction.Open -> onOpen(action.target)
                // A domain screen never lists another domain, so Drill cannot occur here; a tile
                // that is not built says where it stands, exactly as it does on the hub.
                else -> Toast.makeText(context, noteOf(action, domain), Toast.LENGTH_LONG).show()
            }
        }
    }
}

/** The message a tap reports. Falls back to the domain's own note, so it is never empty. */
private fun noteOf(
    action: TileAction,
    domain: HubDomain,
): String = (action as? TileAction.NotYet)?.note ?: domain.note.orEmpty()

@Preview
@Composable
private fun DomainScreenPreview() {
    VedicMitraTheme {
        DomainScreen(domain = HubDomain.JYOTISHA, onOpen = {})
    }
}
