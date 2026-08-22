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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// The row vocabulary shared by the Settings, About, and Support screens, which were each
// hand-rolling the same `Row` shapes before this file existed.
//
// Deliberately kept `internal` to `:feature:settings` while it has a single consumer module. If a
// second feature ever needs these shapes, promote them to `core/designsystem/component/` rather
// than copying them — that module already owns the shared `VedicSelectField`.

/** Title above a group of related rows. */
@Composable
internal fun SettingsSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, modifier = modifier)
}

/** A read-only `label → value` row, as used for version and copyright on the About screen. */
@Composable
internal fun SettingsValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth().padding(vertical = ValueRowPadding)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * A tappable row: [label] on the left, [action] as the trailing call-to-action, and an optional
 * [supportingText] line beneath the label for a short explanation.
 *
 * The label defaults to the prominent styling used by the Settings sections; [SettingsLinkRow]
 * overrides it with the quieter treatment the About screen's link rows use.
 */
@Composable
internal fun SettingsActionRow(
    label: String,
    action: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    contentPadding: Dp = ActionRowPadding,
    labelStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = labelStyle, color = labelColor)
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(text = action, style = labelStyle, color = MaterialTheme.colorScheme.primary)
    }
}

/**
 * A [SettingsActionRow] that opens [url] in the browser, showing [value] as the trailing text.
 * Only ever pass an `https:` or `mailto:` URL — see [SupportLinks] for why other schemes are unsafe.
 */
@Composable
internal fun SettingsLinkRow(
    label: String,
    value: String,
    url: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    contentPadding: Dp = ValueRowPadding,
) {
    val uriHandler = LocalUriHandler.current
    SettingsActionRow(
        label = label,
        action = value,
        onClick = { uriHandler.openUri(url) },
        modifier = modifier,
        supportingText = supportingText,
        contentPadding = contentPadding,
        labelStyle = MaterialTheme.typography.bodyMedium,
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private val ValueRowPadding = 4.dp
private val ActionRowPadding = 12.dp
