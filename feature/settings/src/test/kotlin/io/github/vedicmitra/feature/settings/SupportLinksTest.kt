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

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Guards the shape of the Support screen's outbound destinations. The composables themselves are
 * not tested (no module has an instrumentation source set), so these assertions are the only
 * automated defence against a malformed link reaching users.
 */
class SupportLinksTest {
    private val webLinks =
        listOf(
            SupportLinks.REPOSITORY,
            SupportLinks.GITHUB_SPONSORS,
            SupportLinks.KO_FI,
            SupportLinks.COMMERCIAL_LICENSE,
            SupportLinks.PRIVACY_POLICY,
            SupportLinks.REPORT_BUG,
            SupportLinks.CONTRIBUTING,
        )

    @Test
    fun `every browser destination is an https url`() {
        webLinks.forEach { link ->
            assertThat(link).startsWith("https://")
        }
    }

    @Test
    fun `the licensing contact is a mailto link`() {
        assertThat(SupportLinks.LICENSING_EMAIL).startsWith("mailto:")
        assertThat(SupportLinks.LICENSING_EMAIL).contains("@")
    }

    @Test
    fun `the upi id is a bare vpa rather than a uri`() {
        // A `upi://` URI handed to LocalUriHandler crashes on devices with no UPI app installed,
        // so the screen copies a plain VPA instead. This test is that decision's regression guard.
        assertThat(SupportLinks.UPI_ID).matches("[A-Za-z0-9.\\-_]{3,}@[A-Za-z][A-Za-z0-9.\\-_]+")
        assertThat(SupportLinks.UPI_ID).doesNotContain("://")
        assertThat(SupportLinks.UPI_ID).doesNotContain("?")
    }

    @Test
    fun `no destination is listed twice`() {
        assertThat(webLinks).containsNoDuplicates()
    }

    @Test
    fun `no destination still carries a setup placeholder`() {
        // The rails shipped with deliberate placeholders before the accounts existed. This fails
        // loudly if one is ever reintroduced, rather than quietly publishing a dead link.
        val placeholders = listOf("example.com", "your-kofi-slug", "your-vpa", "REPLACE")
        (webLinks + SupportLinks.LICENSING_EMAIL + SupportLinks.UPI_ID).forEach { value ->
            placeholders.forEach { placeholder ->
                assertThat(value).doesNotContain(placeholder)
            }
        }
    }

    @Test
    fun `links point at the maintainer's own repository`() {
        assertThat(SupportLinks.REPOSITORY).isEqualTo("https://github.com/imskylab/vedic-mitra")
        assertThat(SupportLinks.COMMERCIAL_LICENSE).contains("COMMERCIAL_LICENSE.md")
        assertThat(SupportLinks.PRIVACY_POLICY).contains("PRIVACY.md")
    }
}
