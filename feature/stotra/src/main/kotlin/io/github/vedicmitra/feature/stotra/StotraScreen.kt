/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

@file:Suppress("MagicNumber")

package io.github.vedicmitra.feature.stotra

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme
import java.time.LocalDate

/**
 * Stotra screen. Shows today's stotra plus the browsable library; tapping one opens an in-screen
 * reader with an adjustable font size. Back returns from the reader to the list, then leaves the
 * screen. Content is static (see [StotraCatalog]), so no ViewModel is needed.
 */
@Composable
fun StotraScreen(modifier: Modifier = Modifier) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = selectedId?.let(StotraCatalog::byId)

    BackHandler(enabled = selected != null) { selectedId = null }

    if (selected == null) {
        StotraList(onOpen = { selectedId = it }, modifier = modifier)
    } else {
        StotraReader(stotra = selected, onBack = { selectedId = null }, modifier = modifier)
    }
}

@Composable
private fun StotraList(
    onOpen: (String) -> Unit,
    modifier: Modifier,
) {
    val today = remember { StotraCatalog.forWeekday(LocalDate.now().dayOfWeek) }
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = "Stotra",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        item { TodayCard(today, onOpen) }
        StotraCatalog.byDeity.forEach { (deity, stotras) ->
            item {
                Text(
                    text = deity,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(stotras, key = { it.id }) { stotra -> StotraRow(stotra, onOpen) }
        }
    }
}

@Composable
private fun TodayCard(
    stotra: Stotra,
    onOpen: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onOpen(stotra.id) }) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "TODAY'S STOTRA",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(text = stotra.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = stotra.significance,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StotraRow(
    stotra: Stotra,
    onOpen: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable { onOpen(stotra.id) }.padding(vertical = 8.dp),
    ) {
        Text(text = stotra.title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stotra.deity,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StotraReader(
    stotra: Stotra,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    var fontScale by rememberSaveable { mutableFloatStateOf(1f) }
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(text = stotra.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { fontScale = (fontScale - 0.15f).coerceAtLeast(0.8f) }) { Text("A−") }
            OutlinedButton(onClick = { fontScale = (fontScale + 0.15f).coerceAtMost(1.8f) }) { Text("A+") }
        }
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stotra.deity,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stotra.devanagari,
                fontSize = (22 * fontScale).sp,
                lineHeight = (34 * fontScale).sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stotra.transliteration,
                fontSize = (15 * fontScale).sp,
                lineHeight = (24 * fontScale).sp,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = stotra.significance, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview
@Composable
private fun StotraListPreview() {
    VedicMitraTheme {
        StotraList(onOpen = {}, modifier = Modifier)
    }
}

@Preview
@Composable
private fun StotraReaderPreview() {
    VedicMitraTheme {
        StotraReader(stotra = StotraCatalog.all.first(), onBack = {}, modifier = Modifier)
    }
}
