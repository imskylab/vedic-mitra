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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.vedicmitra.core.astronomy.MuhurtaActivity
import io.github.vedicmitra.core.astronomy.MuhurtaCategory
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme

/** The first muhurta step: pick a category of activity. Emits the chosen category's enum name. */
@Composable
fun MuhuratCategoriesScreen(
    onOpenCategory: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Choose a muhurta", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Pick what you're planning and we'll rank the most auspicious days ahead.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MuhurtaCategory.entries.forEach { category ->
            CategoryCard(category = category, onClick = { onOpenCategory(category.name) })
        }
    }
}

@Composable
private fun CategoryCard(
    category: MuhurtaCategory,
    onClick: () -> Unit,
) {
    val examples =
        MuhurtaActivity
            .inCategory(category)
            .take(EXAMPLE_COUNT)
            .joinToString(separator = " · ") { it.displayName }
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = category.displayName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = examples,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val EXAMPLE_COUNT = 3

@Preview
@Composable
private fun MuhuratCategoriesPreview() {
    VedicMitraTheme {
        MuhuratCategoriesScreen(onOpenCategory = {})
    }
}
