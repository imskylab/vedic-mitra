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

    // Fallbacks used only if version.properties is missing or malformed; the real values live in
    // the committed root version.properties and are bumped per release.
    const val DEFAULT_VERSION_CODE = 1
    const val DEFAULT_VERSION_NAME = "0.1.0"
}

/**
 * Reads the app [versionCode][AppVersion.code] / [versionName][AppVersion.name] from the committed
 * root `version.properties`, falling back to [ProjectConfig] defaults when absent. Keeping the
 * version in a plain properties file means a release bump is a one-line edit with no code change.
 */
internal fun Project.appVersion(): AppVersion {
    // Read via providers.fileContents so version.properties is a tracked configuration input:
    // a bump invalidates the configuration cache instead of silently reusing a stale versionCode.
    val text =
        providers
            .fileContents(rootProject.layout.projectDirectory.file("version.properties"))
            .asText
            .orNull
    val props = java.util.Properties().apply { text?.let { load(it.reader()) } }
    val code = props.getProperty("VERSION_CODE")?.trim()?.toIntOrNull() ?: ProjectConfig.DEFAULT_VERSION_CODE
    val name = props.getProperty("VERSION_NAME")?.trim()?.ifEmpty { null } ?: ProjectConfig.DEFAULT_VERSION_NAME
    return AppVersion(code, name)
}

/** The app's version coordinates resolved from `version.properties`. */
internal data class AppVersion(
    val code: Int,
    val name: String,
)

/** Convenience accessor for the shared `libs` version catalog from any plugin. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")
