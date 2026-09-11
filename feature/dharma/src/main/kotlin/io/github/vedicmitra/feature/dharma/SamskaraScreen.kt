/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.dharma

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * The samskara reference: every rite the app can cite, with what a named sutra says about it.
 *
 * **Mode: Cite + Teach.** Nothing is computed and nothing is instructed — the screen reports what the
 * grhyasutras hold and explains each rite in plain language.
 *
 * No ViewModel: the content is static, exactly as [SamskaraCatalog] describes, so there is nothing to
 * load and nothing to keep. `StotraScreen` is the precedent.
 *
 * @param onFindMuhurta opens the electional flow for a rite that has a preset. `:feature:dharma`
 *   names the intent; `:app` owns the route.
 */
@Composable
fun SamskaraScreen(
    onFindMuhurta: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var opened by rememberSaveable { mutableStateOf<String?>(null) }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = stringResource(R.string.samskara_screen_title), style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(R.string.samskara_screen_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SamskaraCatalog.all.forEach { entry ->
            SamskaraCard(entry) { opened = entry.kind.id }
        }
        // Said on the screen rather than left to be inferred from a short list: a reference that
        // looks complete and is not would be the more misleading of the two.
        Text(
            text = stringResource(R.string.samskara_screen_partial),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    opened?.let { id ->
        val entry = SamskaraCatalog.all.first { it.kind.id == id }
        SamskaraSheet(entry = entry, onFindMuhurta = onFindMuhurta, onDismiss = { opened = null })
    }
}

/** One rite in the list: its name, the popular form where there is one, and the untapped one-liner. */
@Composable
private fun SamskaraCard(
    entry: SamskaraEntry,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = entry.kind.label, style = MaterialTheme.typography.titleMedium)
                entry.kind.alias?.let { alias ->
                    Text(
                        text = "  ·  $alias",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(text = stringResource(entry.oneLine), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * One rite in full, with its source line and — where the app has a preset — a way into the muhurta
 * flow.
 *
 * The source is shown whether or not a reader asked for it. A citation kept out of sight is one
 * nobody can check, which is most of what citing is for.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SamskaraSheet(
    entry: SamskaraEntry,
    onFindMuhurta: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = entry.kind.label, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = stringResource(entry.oneLine),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = stringResource(entry.body), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = entry.source.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            entry.muhurta?.let { activity ->
                Text(
                    text = stringResource(R.string.samskara_muhurta_link),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onFindMuhurta(activity.name)
                            }.padding(top = 8.dp),
                )
            }
        }
    }
}
