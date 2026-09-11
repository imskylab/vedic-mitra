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
import io.github.vedicmitra.core.common.model.ContentSource
import org.junit.Test

/**
 * That the rules say what their sources say, and that unsourced rules can only become fewer.
 *
 * The nakshatra numbers in particular are easy to transpose and a transposition compiles perfectly,
 * so the karana rules — the ones with a text behind them — are asserted against the verse rather
 * than against themselves.
 */
class ActivityMuhurtaRulesTest {
    @Test
    fun `the number of unsourced rule sets only shrinks`() {
        // A ratchet, on the stotra catalog's model: these three predate the rule about citing and no
        // public-domain text has been found that gives lists of that shape. The bound is what is
        // honestly true today. If this fails because the count went *down*, lower the number --
        // Vivah came off this list when Brihat Samhita 100.1 turned out to name its eleven exactly.
        val unsourced =
            MuhurtaActivity.entries
                .filter { hasActivityRules(it) }
                .count { muhurtaRuleSourceFor(it) is ContentSource.NotRecorded }

        assertWithMessage("a new rule set arrived without a source").that(unsourced).isAtMost(EXPECTED_UNSOURCED)
    }

    @Test
    fun `the default rules admit they are not specific to anything`() {
        // The claim the results screen depends on. If the fallback ever acquired a source, the screen
        // would start naming a text for a ranking that is not specific to what was asked about.
        val general = MuhurtaActivity.entries.filterNot { hasActivityRules(it) }

        assertThat(general).isNotEmpty()
        general.forEach {
            assertWithMessage("$it").that(muhurtaRuleSourceFor(it)).isEqualTo(ContentSource.NotRecorded)
        }
    }

    @Test
    fun `sowing and gardening follow the Gara verse`() {
        // "In a Gara Karana lands shall be tilled, seeds sown, houses built and the like."
        // -- Brihat Samhita 99.7.
        listOf(MuhurtaActivity.SOWING, MuhurtaActivity.GARDENING).forEach {
            assertWithMessage("$it").that(muhurtaRulesFor(it).favorableKaranas).containsExactly(KaranaKind.GARA)
        }
    }

    @Test
    fun `the trade activities follow the Vanija verse`() {
        // "In a Vanik Karana, works of a fixed nature shall be done as well as dealings with
        // merchants." -- Brihat Samhita 99.7.
        val trade =
            listOf(
                MuhurtaActivity.SHOP_OPENING,
                MuhurtaActivity.BUSINESS_INAUGURATION,
                MuhurtaActivity.PRODUCT_SALE,
            )

        trade.forEach {
            assertWithMessage("$it").that(muhurtaRulesFor(it).favorableKaranas).containsExactly(KaranaKind.VANIJA)
        }
    }

    @Test
    fun `ground-breaking takes both verses that name the building of a house`() {
        // Taitila at 99.6 ("houses shall be built") and Gara at 99.7 ("houses built and the like").
        assertThat(muhurtaRulesFor(MuhurtaActivity.BHOOMI_POOJAN).favorableKaranas)
            .containsExactly(KaranaKind.TAITILA, KaranaKind.GARA)
    }

    @Test
    fun `taking medicine follows Shakuni alone`() {
        // "In a Sakuni Karana a person shall do acts for the increase of his health and comfort,
        // shall take medicine..." -- Brihat Samhita 99.8. Bava and Kimstughna name health and comfort
        // without naming the act, so they are deliberately absent; this asserts that restraint.
        assertThat(muhurtaRulesFor(MuhurtaActivity.AUSHADHI_SEVAN).favorableKaranas)
            .containsExactly(KaranaKind.SHAKUNI)
    }

    @Test
    fun `buying livestock follows the Chatushpada verse`() {
        // "In a Chatushpada Karana a person shall do deeds connected with cows..." -- 99.8.
        assertThat(muhurtaRulesFor(MuhurtaActivity.LIVESTOCK_PURCHASE).favorableKaranas)
            .containsExactly(KaranaKind.CHATUSHPADA)
    }

    @Test
    fun `no rule set recommends Vishti`() {
        // "In a Vishti or Bhadra Karana, auspicious deeds shall not be done" -- 99.7, which is also
        // the source of the penalty the scorer applies to every activity. A rule set that named it as
        // favourable would be asking the scorer to add and subtract for the same fact.
        MuhurtaActivity.entries.forEach {
            assertWithMessage("$it").that(muhurtaRulesFor(it).favorableKaranas).doesNotContain(KaranaKind.VISHTI)
        }
    }

    @Test
    fun `marriage takes the eleven asterisms the verse names`() {
        // "Marriages shall take place when the Moon passes through the asterism of Rohini,
        // U. Phalguni, U. Ashadha, U. Bhadrapada, Revati, Mrigasirsha, Mula, Anuradha, Magha, Hasta
        // or Swati" -- Brihat Samhita 100.1. Spelled out by name here rather than as bare numbers,
        // because the numbers are the part that can be transposed without anyone noticing.
        val named = listOf("Rohini", "Uttara Phalguni", "Uttara Ashadha", "Uttara Bhadrapada", "Revati")
        val alsoNamed = listOf("Mrigashira", "Mula", "Anuradha", "Magha", "Hasta", "Swati")
        val expected = (named + alsoNamed).map { NAKSHATRA_NAMES.indexOf(it) + 1 }

        assertWithMessage("a name in the verse is not a name in NAKSHATRA_NAMES").that(expected).doesNotContain(0)
        assertThat(muhurtaRulesFor(MuhurtaActivity.VIVAH).favorableNakshatras)
            .containsExactlyElementsIn(expected)
    }

    @Test
    fun `the auspicious tithis are everything but Rikta and Amavasya`() {
        // Brihat Samhita 99.2 classifies the fifteen and 100.2 sets aside the Rikta ones; 99.2 gives
        // the new moon to the Pitris. Nothing else in either verse narrows the field, so nothing else
        // narrows this set. Asserted as a derivation rather than as a literal list, so a stray edit
        // to the set has to disagree with the rule rather than merely with a copy of it.
        val everyTithi = (1..30).toSet()

        assertThat(AUSPICIOUS_TITHIS).containsExactlyElementsIn(everyTithi - RIKTA_TITHIS - AMAVASYA_TITHI)
    }

    @Test
    fun `every rule set that names a text names a place in it`() {
        MuhurtaActivity.entries
            .map { muhurtaRuleSourceFor(it) }
            .filterIsInstance<ContentSource.Text>()
            .forEach { assertWithMessage(it.work).that(it.locus).isNotNull() }
    }

    private companion object {
        const val EXPECTED_UNSOURCED = 3
    }
}
