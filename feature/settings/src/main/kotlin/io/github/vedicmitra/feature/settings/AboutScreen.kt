/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme

/**
 * "About Vedic Mitra" — the app's identity, author, and licensing, reached from Settings. Reads the
 * version name from the installed package so it always matches the build, and is otherwise static.
 */
@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val version =
        remember {
            runCatching {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrNull()
        }
    AboutContent(version = version, modifier = modifier)
}

@Composable
private fun AboutContent(
    version: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = "Vedic Mitra", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Your Vedic panchanga & astrology companion.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        AboutRow(label = "Version", value = version ?: "—")
        AboutRow(label = "Created by", value = "Jayvardhan Potabatti")
        AboutRow(label = "Copyright", value = "© 2026 Jayvardhan Potabatti")
        AboutRow(label = "License", value = "GNU AGPL-3.0-or-later")
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text =
                "Vedic Mitra computes the Hindu panchanga and core Vedic astrology — kundali, " +
                    "muhurta, festivals and reminders — offline on your device. Predictions are as " +
                    "accurate as the birth details you provide and are offered as guidance only.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text =
                "Free software under the GNU Affero General Public License v3.0 or later; a commercial " +
                    "license is also available (see LICENSING.md).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AboutRow(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview
@Composable
private fun AboutContentPreview() {
    VedicMitraTheme {
        AboutContent(version = "1.0.0")
    }
}
