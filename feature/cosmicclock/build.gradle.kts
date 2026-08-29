/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

plugins {
    alias(libs.plugins.vedicmitra.android.feature)
}

android {
    namespace = "io.github.vedicmitra.feature.cosmicclock"
}

dependencies {
    implementation(projects.core.astronomy)
    implementation(projects.core.domain)
    implementation(projects.core.location)
}
