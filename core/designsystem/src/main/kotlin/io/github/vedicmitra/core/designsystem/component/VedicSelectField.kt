/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme

/**
 * A single-choice selector rendered as an expandable dropdown (Material 3 exposed dropdown menu): a
 * read-only field showing the current choice, which expands to a scrollable menu of [options]. The
 * app's standard replacement for hand-rolled chip/segmented selector rows, so every picker looks and
 * behaves the same and scales to long lists (profiles, the twelve rashis, …).
 *
 * Works for any option type, including a nullable one (e.g. `String?` where `null` is a "General"
 * choice) — the caller's [optionLabel] decides how each option, including `null`, reads.
 *
 * @param label the field label.
 * @param options the choices, in display order.
 * @param selected the current choice (must be one of [options]).
 * @param optionLabel the display text for an option.
 * @param onSelect called with the chosen option.
 * @param enabled whether the field can be opened.
 * @param leadingContent optional leading content per option (e.g. a marker on the user's own sign).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> VedicSelectField(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingContent: (@Composable (T) -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = enabled),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    leadingIcon = leadingContent?.let { content -> { content(option) } },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun VedicSelectFieldPreview() {
    VedicMitraTheme {
        VedicSelectField(
            label = "Profile",
            options = listOf("General", "Leo", "Mia"),
            selected = "Leo",
            optionLabel = { it },
            onSelect = {},
        )
    }
}
