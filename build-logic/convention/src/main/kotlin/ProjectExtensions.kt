/*
 * Copyright (c) 2026 Vedic Mitra contributors
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
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
    const val COMPILE_SDK = 37
    const val TARGET_SDK = 37
    const val JVM_TARGET = "21"
}

/** Convenience accessor for the shared `libs` version catalog from any plugin. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")
