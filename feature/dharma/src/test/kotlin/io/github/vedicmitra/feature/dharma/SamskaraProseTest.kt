/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.dharma

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The copy guardrails, on `PanchangaProseTest`'s shape.
 *
 * It reads `strings.xml` rather than a Kotlin constant, so what is measured is the string that
 * actually ships. `docs/knowledge-standards.md` calls this the primer pattern and requires it of
 * every new domain: **adding a rite without writing its copy breaks the build.**
 *
 * The voice test is the one that matters most here. This domain describes rites that people perform,
 * which makes it the easiest place in the app to slip from reporting into instructing — and "no
 * instruction in anyone's practice" is a red line, not a preference.
 */
class SamskaraProseTest {
    private val strings: Map<String, String> = parseStrings()

    @Test
    fun `every samskara has copy, and the entry points at its own strings`() {
        // Two failures in one: a rite with no copy, and a rite wired to another rite's strings. The
        // second is the one worth guarding -- a misaimed resource id compiles perfectly and shows
        // the wrong rite's explanation under the right rite's name.
        Samskara.entries.forEach { kind ->
            val entry = SamskaraCatalog.of(kind)
            val slug = kind.name.lowercase(Locale.ROOT)
            assertWithMessage("$kind oneLine").that(entry.oneLine).isEqualTo(stringId("samskara_${slug}_one_line"))
            assertWithMessage("$kind body").that(entry.body).isEqualTo(stringId("samskara_${slug}_body"))
        }
    }

    @Test
    fun `no copy addresses the reader`() {
        // House voice, and a red line for this domain in particular: the app reports what a
        // tradition holds and never tells a reader what to do. "In the sixth month the child is fed"
        // is reportage; anything with a "you" in it is not.
        val secondPerson = Regex("\\b(you|your|we|our)\\b", RegexOption.IGNORE_CASE)
        strings.forEach { (key, value) ->
            assertWithMessage("$key addresses the reader: \"$value\"")
                .that(secondPerson.containsMatchIn(value))
                .isFalse()
        }
    }

    @Test
    fun `one-liners fit in a list row without tapping`() {
        Samskara.entries.forEach { kind ->
            val slug = kind.name.lowercase(Locale.ROOT)
            val oneLine = requireNotNull(strings["samskara_${slug}_one_line"])
            assertWithMessage("$kind one-liner is too long to sit in a row")
                .that(oneLine.length)
                .isAtMost(ONE_LINE_MAX_CHARS)
        }
    }

    @Test
    fun `bodies are a paragraph, not a phrase and not an essay`() {
        Samskara.entries.forEach { kind ->
            val slug = kind.name.lowercase(Locale.ROOT)
            val body = requireNotNull(strings["samskara_${slug}_body"])
            assertWithMessage("$kind body").that(body.length).isAtLeast(BODY_MIN_CHARS)
            assertWithMessage("$kind body").that(body.length).isAtMost(BODY_MAX_CHARS)
            assertWithMessage("$kind body ends mid-sentence").that(body).endsWith(".")
        }
    }

    @Test
    fun `the resource file is the one being read`() {
        // Without this the whole class passes vacuously if the file moves or the parse breaks.
        assertWithMessage("no strings parsed -- has the resource file moved?").that(strings).isNotEmpty()
        assertThat(strings).containsKey("samskara_namakarana_body")
    }

    /** The int `R.string.<name>` holds, looked up by name so the mapping can be checked. */
    private fun stringId(name: String): Int = R.string::class.java.getField(name).getInt(null)

    /**
     * Reads `src/main/res/values/strings.xml` the way the platform would: escapes undone, runs of
     * whitespace collapsed to one space. Without the collapsing every wrapped string in the file
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
            val text =
                node.textContent
                    .replace("\\'", "'")
                    .replace(Regex("\\s+"), " ")
                    .trim()
            name to text
        }
    }

    private companion object {
        const val ONE_LINE_MAX_CHARS = 72
        const val BODY_MIN_CHARS = 120
        const val BODY_MAX_CHARS = 520
    }
}
