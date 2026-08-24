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
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.withType

/**
 * Makes a failing test say *why* it failed in the console.
 *
 * Gradle's default is to print only the test name, so on CI — where the HTML report is thrown away
 * with the runner — a failure arrives as a bare `SomeTest > some case FAILED` with no assertion
 * message. That is nearly useless for the reference-data tests in `:core:astronomy`, whose whole
 * value is in the expected-versus-actual detail.
 *
 * Only failures are made verbose; a passing run stays quiet.
 */
internal fun Project.configureTestLogging() {
    tasks.withType<Test>().configureEach {
        testLogging {
            events(TestLogEvent.FAILED)
            exceptionFormat = TestExceptionFormat.FULL
            showStackTraces = true
            showCauses = true
        }
    }
}
