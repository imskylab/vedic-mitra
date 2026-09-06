/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.ui.panchanga

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import io.github.vedicmitra.core.astronomy.PanchangaConcept
import io.github.vedicmitra.core.astronomy.PanchangaLimb
import io.github.vedicmitra.core.ui.R
import org.junit.Test
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The copy guardrails, kept working after the prose moved into `strings.xml`.
 *
 * They used to read Kotlin constants. Rather than lose them — they are most of what keeps
 * explanatory text a first-class part of a feature rather than the thing cut when time runs short —
 * they now **read the resource file itself**, which is a stronger check: it is the shipped string
 * that is measured, not a constant that happens to feed it.
 *
 * The first test is still the point of the file: it iterates [PanchangaConcept], so **adding a
 * concept without writing its copy fails the build**.
 */
class PanchangaProseTest {
    private val strings: Map<String, String> by lazy { parseStrings() }

    @Test
    fun `every concept has copy, and the entry points at its own strings`() {
        // Two failures in one: a concept with no copy at all, and a concept wired to another
        // concept's strings. The second is the one worth guarding -- the mapping was generated, and
        // a misaimed resource id compiles perfectly and shows the wrong explanation.
        PanchangaConcept.entries.forEach { concept ->
            val entry = PanchangaPrimer.of(concept)
            val slug = concept.name.lowercase(Locale.ROOT)

            assertWithMessage("$concept title").that(entry.title).isEqualTo(stringId("primer_${slug}_title"))
            assertWithMessage("$concept oneLine").that(entry.oneLine).isEqualTo(stringId("primer_${slug}_one_line"))
            assertWithMessage("$concept body").that(entry.body).isEqualTo(stringId("primer_${slug}_body"))
        }
    }

    @Test
    fun `every limb the calendar draws as a wheel can be explained`() {
        // The gap this closes: this copy shipped in 0.9.0 with no consumer at all -- VedicCycleRow
        // took an onClick that nothing passed, and the glossary the other sheets read has no key
        // for a limb name. Wiring it up then found two rows with no concept to map to. A row the
        // calendar can draw but the primer cannot explain would offer a tap and say nothing.
        PanchangaLimb.entries.forEach { limb ->
            assertWithMessage("copy for the ${limb.displayName} row")
                .that(body(limb.concept))
                .isNotEmpty()
        }
    }

    @Test
    fun `one-liners fit beside a value without tapping`() {
        // They are shown untapped, next to a ring or a number, so they have to survive a narrow
        // screen at a large font scale. A one-liner that wraps to three lines is a body in disguise.
        PanchangaConcept.entries.forEach { concept ->
            val oneLine = text(concept, "one_line")
            assertWithMessage("$concept one-liner is ${oneLine.length} chars: \"$oneLine\"")
                .that(oneLine.length)
                .isAtMost(ONE_LINE_MAX_CHARS)
        }
    }

    @Test
    fun `bodies are a paragraph, not a phrase and not an essay`() {
        PanchangaConcept.entries.forEach { concept ->
            val body = body(concept)
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
    fun `no copy addresses the reader`() {
        // House voice: explain what a thing is, never instruct. "You should fast" is a claim the app
        // has no business making; "many keep a fast" is reportage. Checked across the glossary too,
        // which is held to the same voice and was previously only held to it by hand.
        val secondPerson = Regex("\\b(you|your|we|our)\\b", RegexOption.IGNORE_CASE)

        strings.forEach { (key, value) ->
            assertWithMessage("$key addresses the reader: \"$value\"")
                .that(secondPerson.containsMatchIn(value))
                .isFalse()
        }
    }

    @Test
    fun `titles are distinct`() {
        val titles = PanchangaConcept.entries.map { text(it, "title") }

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
            assertWithMessage("$limb").that(body(limb)).isNotEmpty()
        }
    }

    @Test
    fun `the resource file is the one being read`() {
        // If the path ever stops resolving, every test above passes vacuously over an empty map.
        assertThat(strings).isNotEmpty()
        assertThat(strings.keys).contains("primer_tithi_body")
    }

    private fun body(concept: PanchangaConcept) = text(concept, "body")

    private fun text(
        concept: PanchangaConcept,
        field: String,
    ): String = strings.getValue("primer_${concept.name.lowercase(Locale.ROOT)}_$field")

    /** The int `R.string.<name>` holds, looked up by name so the mapping can be checked. */
    private fun stringId(name: String): Int = R.string::class.java.getField(name).getInt(null)

    /**
     * Reads `src/main/res/values/strings.xml` the way the platform would: escapes undone, runs of
     * whitespace collapsed to one space. Without the collapsing, every wrapped string in the file
     * would measure as though its indentation were part of the sentence.
     */
    private fun parseStrings(): Map<String, String> {
        val file = File("src/main/res/values/strings.xml")
        check(file.isFile) { "expected the resources at ${file.absolutePath}" }
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = document.getElementsByTagName("string")

        return (0 until nodes.length).associate { index ->
            val node = nodes.item(index)
            val name = node.attributes.getNamedItem("name").nodeValue
            name to
                node.textContent
                    .replace("\\'", "'")
                    .replace("\\\"", "\"")
                    .replace(WHITESPACE, " ")
                    .trim()
        }
    }

    private companion object {
        /** The width a one-liner has to live within. */
        const val ONE_LINE_MAX_CHARS = 72
        const val BODY_MIN_CHARS = 120
        const val BODY_MAX_CHARS = 520
        val WHITESPACE = Regex("\\s+")
    }
}
