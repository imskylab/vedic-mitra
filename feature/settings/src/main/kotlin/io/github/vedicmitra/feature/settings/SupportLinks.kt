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

/**
 * Every outbound destination offered by the Support screen, in one place.
 *
 * Held as compile-time constants rather than string resources (the app hard-codes its copy
 * everywhere) or `BuildConfig` fields (`android.defaults.buildfeatures.buildconfig=false`).
 *
 * These values are duplicated in two other places that must move with them:
 *  - `.github/FUNDING.yml` — drives GitHub's "Sponsor" button on the repository.
 *  - `README.md` → "Support the project" — the web-facing copy of the same asks.
 *
 * Some values below are **placeholders** that must be replaced before a release ships; the
 * checklist in `docs/MONETIZATION_SETUP.md` names each one. `SupportLinksTest` pins the shape of
 * every constant so a malformed link cannot ship silently — in particular, the UPI entry is a bare
 * VPA to copy, never a `upi://` URI (see [SupportScreen] for why that distinction matters).
 */
internal object SupportLinks {
    /** Canonical repository — also the "share" payload and the star-the-repo destination. */
    const val REPOSITORY = "https://github.com/imskylab/vedic-mitra"

    /** GitHub Sponsors profile. Live only once Sponsors is enabled for the account. */
    const val GITHUB_SPONSORS = "https://github.com/sponsors/imskylab"

    /** Ko-fi page for one-off tips. Placeholder slug — see `docs/MONETIZATION_SETUP.md`. */
    const val KO_FI = "https://ko-fi.com/your-kofi-slug"

    /**
     * UPI virtual payment address, shown for the user to copy into their own UPI app.
     * Deliberately a plain VPA and not a `upi://pay?pa=…` URI. Placeholder — replace before release.
     */
    const val UPI_ID = "your-vpa@bank"

    /** Commercial-licensing pricing and terms, for businesses that cannot accept the AGPL. */
    const val COMMERCIAL_LICENSE = "$REPOSITORY/blob/main/docs/COMMERCIAL_LICENSE.md"

    /** Primary channel for commercial-licence enquiries. Placeholder — replace before release. */
    const val LICENSING_EMAIL = "mailto:licensing@example.com"

    /** Privacy policy — what the app does and does not collect. */
    const val PRIVACY_POLICY = "$REPOSITORY/blob/main/docs/PRIVACY.md"

    /** Pre-filled bug report against the issue template. */
    const val REPORT_BUG = "$REPOSITORY/issues/new?template=bug_report.md"

    /** Contribution guide — the entry point for code, translations, and panchanga corrections. */
    const val CONTRIBUTING = "$REPOSITORY/blob/main/CONTRIBUTING.md"
}
