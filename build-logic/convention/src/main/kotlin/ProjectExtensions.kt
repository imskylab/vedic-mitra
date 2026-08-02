/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * Shared constants and helpers used by every convention plugin.
 *
 * Centralising the SDK levels here means a single edit re-targets every module.
 */
internal object ProjectConfig {
    const val NAMESPACE_PREFIX = "io.github.vedicmitra"
    const val MIN_SDK = 26
    const val COMPILE_SDK = 36
    const val TARGET_SDK = 36
    const val JVM_TARGET = "21"
}

/** Convenience accessor for the shared `libs` version catalog from any plugin. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")
