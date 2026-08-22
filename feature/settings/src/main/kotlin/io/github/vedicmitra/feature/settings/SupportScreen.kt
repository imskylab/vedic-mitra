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

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme
import io.github.vedicmitra.core.ui.preview.ThemePreviews

/**
 * "Support Vedic Mitra" — how to fund the project, and how businesses obtain a commercial licence.
 *
 * Everything here is a browser hand-off or a clipboard copy; the screen makes no network call, has
 * no payment SDK, and unlocks nothing. That is deliberate: the app is free and complete for
 * everyone, and paying for it is a thank-you rather than a transaction.
 *
 * Note the UPI row copies a plain VPA instead of launching a `upi://pay` intent. Compose's
 * `AndroidUriHandler` rethrows `ActivityNotFoundException` as `IllegalArgumentException`, so a
 * `upi://` link would crash the app on every device without a UPI app installed.
 */
@Composable
fun SupportScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    SupportContent(
        onCopy = { text, confirmation ->
            clipboardManager.setText(AnnotatedString(text))
            Toast.makeText(context, confirmation, Toast.LENGTH_SHORT).show()
        },
        modifier = modifier,
    )
}

@Composable
private fun SupportContent(
    onCopy: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = "Support Vedic Mitra", style = MaterialTheme.typography.headlineSmall)
        BodyText(
            text =
                "Vedic Mitra is free and always will be. Every feature is available to everyone — " +
                    "there are no ads, no tracking, no account, and nothing on this screen unlocks " +
                    "anything. If the app is useful to you, these are the ways to keep it going.",
            style = MaterialTheme.typography.bodyMedium,
        )
        DonateSection(onCopy = onCopy)
        BusinessSection()
        OtherWaysToHelpSection(onCopy = onCopy)
        SupportFooter()
    }
}

@Composable
private fun DonateSection(onCopy: (String, String) -> Unit) {
    SectionSpacer()
    SettingsSectionHeader(text = "Donate")
    SettingsLinkRow(
        label = "GitHub Sponsors",
        value = "Open",
        url = SupportLinks.GITHUB_SPONSORS,
        supportingText = "One-off or monthly, handled by GitHub",
    )
    SettingsLinkRow(
        label = "Ko-fi",
        value = "Open",
        url = SupportLinks.KO_FI,
        supportingText = "A one-off tip by card or PayPal",
    )
    CopyRow(
        label = "UPI",
        action = "Copy",
        supportingText = SupportLinks.UPI_ID,
        onClick = { onCopy(SupportLinks.UPI_ID, "UPI ID copied") },
    )
}

@Composable
private fun BusinessSection() {
    SectionSpacer()
    SettingsSectionHeader(text = "For businesses")
    BodyText(
        text =
            "Vedic Mitra is licensed under the AGPL, which requires anything built on it — " +
                "including hosted services — to share its source under the same terms. A " +
                "commercial licence lifts that obligation for proprietary products.",
    )
    SettingsLinkRow(
        label = "Commercial licence",
        value = "Pricing",
        url = SupportLinks.COMMERCIAL_LICENSE,
        supportingText = "Tiers for indie, business, and embedded use",
    )
    SettingsLinkRow(
        label = "Licensing enquiry",
        value = "Email",
        url = SupportLinks.LICENSING_EMAIL,
        supportingText = "Questions, quotes, and custom terms",
    )
}

@Composable
private fun OtherWaysToHelpSection(onCopy: (String, String) -> Unit) {
    SectionSpacer()
    SettingsSectionHeader(text = "Other ways to help")
    SettingsLinkRow(label = "Star the repository", value = "GitHub", url = SupportLinks.REPOSITORY)
    SettingsLinkRow(label = "Report a bug", value = "Open", url = SupportLinks.REPORT_BUG)
    SettingsLinkRow(label = "Contribute or translate", value = "Guide", url = SupportLinks.CONTRIBUTING)
    CopyRow(
        label = "Share Vedic Mitra",
        action = "Copy link",
        onClick = { onCopy(SupportLinks.REPOSITORY, "Link copied") },
    )
}

@Composable
private fun SupportFooter() {
    SectionSpacer()
    SettingsLinkRow(label = "Privacy policy", value = "Read", url = SupportLinks.PRIVACY_POLICY)
    BodyText(
        text =
            "Donations fund development and are not a purchase — they carry no entitlement, " +
                "refund, or support obligation. Opening any link above hands off to your " +
                "browser, where that service's own privacy policy applies.",
    )
}

/** A [SettingsActionRow] styled to match the link rows around it, for clipboard actions. */
@Composable
private fun CopyRow(
    label: String,
    action: String,
    onClick: () -> Unit,
    supportingText: String? = null,
) {
    SettingsActionRow(
        label = label,
        action = action,
        onClick = onClick,
        supportingText = supportingText,
        contentPadding = 4.dp,
        labelStyle = MaterialTheme.typography.bodyMedium,
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun BodyText(
    text: String,
    style: TextStyle = MaterialTheme.typography.bodySmall,
) {
    Text(text = text, style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SectionSpacer() {
    Spacer(modifier = Modifier.height(16.dp))
}

@ThemePreviews
@Composable
private fun SupportContentPreview() {
    VedicMitraTheme {
        SupportContent(onCopy = { _, _ -> })
    }
}
