/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/**
 * Applies Detekt to a module and points it at the shared ruleset in `config/detekt/detekt.yml`.
 * Every module runs the same rules, so static analysis is consistent project-wide.
 */
internal fun Project.configureDetekt() {
    pluginManager.apply("io.gitlab.arturbosch.detekt")

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        parallel = true
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    }

    // Pin Detekt's analysis JVM target to the project's JDK level. Detekt otherwise infers it from
    // the Gradle daemon's JVM and passes that as --jvm-target, which fails when the daemon runs on a
    // JDK newer than Detekt's supported range (e.g. JDK 25 from a recent Android Studio install).
    tasks.withType<Detekt>().configureEach {
        jvmTarget = ProjectConfig.JVM_TARGET
    }

    dependencies {
        add("detektPlugins", libs.findLibrary("detekt-formatting").get())
    }
}
