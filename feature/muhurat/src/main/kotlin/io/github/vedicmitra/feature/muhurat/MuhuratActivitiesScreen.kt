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

/** The second muhurta step: pick a specific activity within the category named by [categoryName]
 *  (a [MuhurtaCategory] enum name). Emits the chosen activity's enum name. */
@Composable
fun MuhuratActivitiesScreen(
    categoryName: String,
    onOpenActivity: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val category =
        runCatching { MuhurtaCategory.valueOf(categoryName) }.getOrDefault(MuhurtaCategory.BAL_SANSKAR)
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = category.displayName, style = MaterialTheme.typography.titleLarge)
        MuhurtaActivity.inCategory(category).forEach { activity ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onOpenActivity(activity.name) }) {
                Text(
                    text = activity.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun MuhuratActivitiesPreview() {
    VedicMitraTheme {
        MuhuratActivitiesScreen(categoryName = MuhurtaCategory.VASTU.name, onOpenActivity = {})
    }
}
