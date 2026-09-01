/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.astronomy

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The point of this file is the first test: it iterates [PanchangaConcept], so **adding a concept
 * without writing its copy fails the build**.
 *
 * Explanatory text is the part of a feature that gets cut when time runs short, and the usual reason
 * is that nothing enforces it. A missing map entry here throws `NoSuchElementException` at the point
 * of use rather than degrading to a placeholder, and this test moves that failure to CI.
 */
class PanchangaPrimerTest {
    @Test
    fun `every concept has copy`() {
        PanchangaConcept.entries.forEach { concept ->
            // getValue throws if the entry is missing, which is the enforcement. The assertions
            // below then catch an entry that exists but was left half-written.
            val entry = PanchangaPrimer.of(concept)
            assertWithMessage("$concept title").that(entry.title).isNotEmpty()
            assertWithMessage("$concept oneLine").that(entry.oneLine).isNotEmpty()
            assertWithMessage("$concept body").that(entry.body).isNotEmpty()
        }
    }

    @Test
    fun `every limb the calendar draws as a wheel can be explained`() {
        // The gap this closes: this copy shipped in 0.9.0 with no consumer at all -- VedicCycleRow
        // took an onClick that nothing passed, and the glossary the other sheets read has no key
        // for a limb name. Wiring it up then found two rows with no concept to map to. A row the
        // calendar can draw but the primer cannot explain would offer a tap and say nothing.
        PanchangaLimb.entries.forEach { limb ->
            val entry = PanchangaPrimer.of(limb.concept)
            assertWithMessage("copy for the ${limb.displayName} row").that(entry.body).isNotEmpty()
        }
    }

    @Test
    fun `one-liners fit beside a value without tapping`() {
        // They are shown untapped, next to a ring or a number, so they have to survive a narrow
        // screen at a large font scale. A one-liner that wraps to three lines is a body in disguise.
        PanchangaConcept.entries.forEach { concept ->
            val oneLine = PanchangaPrimer.of(concept).oneLine
            assertWithMessage("$concept one-liner is ${oneLine.length} chars: \"$oneLine\"")
                .that(oneLine.length)
                .isAtMost(PrimerEntry.ONE_LINE_MAX_CHARS)
        }
    }

    @Test
    fun `bodies are a paragraph, not a phrase and not an essay`() {
        PanchangaConcept.entries.forEach { concept ->
            val body = PanchangaPrimer.of(concept).body
            assertWithMessage("$concept body is ${body.length} chars, want at least $BODY_MIN_CHARS")
                .that(body.length)
                .isAtLeast(BODY_MIN_CHARS)
            assertWithMessage("$concept body is ${body.length} chars, want at most $BODY_MAX_CHARS")
                .that(body.length)
                .isAtMost(BODY_MAX_CHARS)
            assertWithMessage("$concept body ends mid-sentence").that(body).endsWith(".")
        }
    }

    @Test
    fun `copy does not address the reader`() {
        // House voice, matching PanchangaGlossary: explain what a thing is, never instruct. "You
        // should fast" is a claim the app has no business making; "many keep a fast" is reportage.
        val secondPerson = Regex("\\b(you|your|we|our)\\b", RegexOption.IGNORE_CASE)
        PanchangaConcept.entries.forEach { concept ->
            val entry = PanchangaPrimer.of(concept)
            assertWithMessage("$concept addresses the reader")
                .that(secondPerson.containsMatchIn("${entry.oneLine} ${entry.body}"))
                .isFalse()
        }
    }

    @Test
    fun `titles are distinct`() {
        val titles = PanchangaConcept.entries.map { PanchangaPrimer.of(it).title }
        assertThat(titles).containsNoDuplicates()
    }

    @Test
    fun `the five limbs are all covered`() {
        // A screen calling itself a panchanga clock has to be able to explain all five of them.
        listOf(
            PanchangaConcept.TITHI,
            PanchangaConcept.VARA,
            PanchangaConcept.NAKSHATRA,
            PanchangaConcept.YOGA,
            PanchangaConcept.KARANA,
        ).forEach { limb ->
            assertWithMessage("$limb").that(PanchangaPrimer.of(limb).body).isNotEmpty()
        }
    }

    private companion object {
        const val BODY_MIN_CHARS = 120
        const val BODY_MAX_CHARS = 520
    }
}
