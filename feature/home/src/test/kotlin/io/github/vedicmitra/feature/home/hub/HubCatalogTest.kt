/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.home.hub

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The hub is meant to be the roadmap made visible, and a claim like that decays silently unless
 * something checks it. These tests are that check: the domain set is pinned against the roadmap's
 * own ids, and what a reader is actually told — the note on an unbuilt tile, whether a shipped
 * domain has artwork — is asserted rather than left to whoever edits the catalog next.
 */
class HubCatalogTest {
    @Test
    fun `the domains are exactly the roadmap entries a reader can go to`() {
        // Pinned deliberately. Adding a domain to docs/roadmap.md without a tile, or a tile without
        // a roadmap entry, should fail here rather than drift apart unnoticed.
        //
        // Four kinds of roadmap entry are absent on purpose, and each is a decision -- see
        // HubDomain's KDoc: the F1-F6 foundations have nothing to open; K8 is a glossary layer
        // rather than a section; C4 and K3 shipped into the calendar's day detail, so a tile could
        // only say "look at the Calendar"; and C6 waits until its shape is clearer.
        val ids = HubDomain.entries.map { it.id }

        assertThat(ids).containsExactly("C1", "C2", "C3", "C5", "K1", "K2", "K4", "K5", "K6", "K7")
        assertWithMessage("a roadmap id is used twice").that(ids).containsNoDuplicates()
    }

    @Test
    fun `every destination the app can reach has a tile leading to it`() {
        // The hub is the only way into most screens, so a target with no tile is a screen no reader
        // can open.
        val reached =
            (HubCatalog.today + HubDomain.entries.flatMap { HubCatalog.tilesIn(it) })
                .mapNotNull { (it.action as? TileAction.Open)?.target }
                .toSet()

        assertThat(reached).containsExactlyElementsIn(HubTarget.entries)
    }

    @Test
    fun `a built domain drills into something, and an unbuilt one says where it stands`() {
        HubCatalog.domains.forEach { tile ->
            val domain = HubDomain.entries.first { it.label == tile.label }
            if (domain.isOpenable) {
                assertWithMessage("${domain.id} ${domain.label} should drill")
                    .that(tile.action)
                    .isInstanceOf(TileAction.Drill::class.java)
                assertWithMessage("${domain.id} drills but holds nothing")
                    .that(HubCatalog.tilesIn(domain))
                    .isNotEmpty()
            } else {
                assertWithMessage("${domain.id} ${domain.label} should report its status")
                    .that(tile.action)
                    .isInstanceOf(TileAction.NotYet::class.java)
                assertWithMessage("${domain.id} has no screen, so it must have nothing to drill into")
                    .that(HubCatalog.tilesIn(domain))
                    .isEmpty()
            }
        }
    }

    @Test
    fun `an unbuilt domain's note is the whole message, so it cannot be blank`() {
        HubDomain.entries.filterNot { it.isOpenable }.forEach { domain ->
            assertWithMessage("${domain.id} ${domain.label} note")
                .that(domain.note.orEmpty())
                .isNotEmpty()
            // A note that just repeats the label tells a reader nothing they did not already see.
            assertWithMessage("${domain.id} note should say more than the label")
                .that(domain.note.orEmpty().length)
                .isGreaterThan(domain.label.length)
        }
    }

    @Test
    fun `a built domain always has artwork`() {
        // The icon style used to say what was built -- ornate meant shipped, a letter meant not yet
        // -- because no artwork existed for the unbuilt domains. It does now, so that cue is gone;
        // status is carried by the "Soon" caption instead (ADR 0020). What still has to hold is the
        // weaker half: nothing that ships falls back to a placeholder letter.
        HubDomain.entries.filter { it.isOpenable }.forEach { domain ->
            assertWithMessage("${domain.id} ${domain.label} icon")
                .that(domain.icon)
                .isInstanceOf(TileIcon.Glyph::class.java)
        }
    }

    @Test
    fun `the domains still waiting for artwork can shrink but never grow`() {
        // A Devanagari letter is now a placeholder, not a statement. Pinning the count keeps it that
        // way: a new domain cannot quietly ship without art, and each one that gets drawn brings the
        // bound down. Lower it as artwork lands; never raise it.
        val awaitingArt = HubDomain.entries.count { it.icon is TileIcon.Letter }

        assertWithMessage("domains still using a placeholder letter")
            .that(awaitingArt)
            .isAtMost(AWAITING_ARTWORK)
    }

    @Test
    fun `every domain reads as something, and no two share a name`() {
        HubDomain.entries.forEach { domain ->
            assertWithMessage("${domain.id} label").that(domain.label).isNotEmpty()
            assertWithMessage("${domain.id} blurb").that(domain.blurb).isNotEmpty()
        }
        assertThat(HubDomain.entries.map { it.label }).containsNoDuplicates()
    }

    @Test
    fun `the daily tiles are a shortcut, never the only way to something`() {
        // They duplicate destinations that also sit under a domain. That is the intended trade -- but
        // it must stay a duplicate, or removing the shortcut would strand the screen.
        val underDomains =
            HubDomain.entries
                .flatMap { HubCatalog.tilesIn(it) }
                .mapNotNull { (it.action as? TileAction.Open)?.target }
                .toSet()
        val daily = HubCatalog.today.mapNotNull { (it.action as? TileAction.Open)?.target }

        assertThat(daily).isNotEmpty()
        assertThat(underDomains).containsAtLeastElementsIn(daily)
    }

    private companion object {
        /** Only The Arts, which has no glyph yet. */
        const val AWAITING_ARTWORK = 1
    }
}
