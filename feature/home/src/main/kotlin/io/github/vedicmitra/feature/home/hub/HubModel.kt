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

import androidx.annotation.DrawableRes
import io.github.vedicmitra.core.designsystem.icon.VedicIcons

/**
 * A destination the hub can open.
 *
 * Deliberately **not** a route string. Routes are `internal` constants in `:app`, where a reflection
 * test enforces that each has a title, so this module cannot see them — and duplicating the strings
 * here would repeat the footgun the muhurat flow already carries, where `:app` and `:feature:muhurat`
 * each declare `"activity"` with nothing checking the two agree. `:app` maps this enum with an
 * exhaustive `when` instead, so a destination that loses its route stops compiling.
 */
enum class HubTarget {
    PANCHANG,
    CALENDAR,
    REMINDERS,
    KUNDALI,
    RASHIFAL,
    MATCH,
    MUHURAT,
    STOTRA,
    JAPA,
    MEDITATE,
    FESTIVALS,
    EVENTS,
}

/** How far along a domain is, mirroring `docs/roadmap.md`. */
enum class DomainStatus {
    BUILT,
    NEXT,
    OPEN,
    EXPLORING,
}

/**
 * What a tile draws.
 *
 * [Glyph] is one of the brand's ornate cultural drawables, [Letter] a Devanagari initial, and
 * [Today] the date. The first two briefly carried meaning between them — ornate meant built, a
 * letter meant not yet — because no artwork existed for the unbuilt domains. They no longer do:
 * those domains have their own glyphs now, and a [Letter] means only that one is still waiting
 * for art.
 *
 * The icon carries no status at all, and neither does the chip's colour. A domain that is not built
 * yet says so in a **"Soon"** caption under its label (ADR 0020) — words being the only cue that
 * survives greyscale, colour-blindness, high contrast and a large font scale at once. The chip's
 * tint says only which [HubCategory] the domain belongs to.
 */
sealed interface TileIcon {
    /**
     * An ornate cultural glyph from [VedicIcons].
     *
     * @property tintable whether re-colouring the whole glyph to one colour still leaves it saying
     *   what it depicts. It matters because the dark scheme has to re-colour these to make them
     *   visible at all: the ink was chosen against cream, and on the dark containers it sits at
     *   1.09:1, very nearly the container itself.
     *
     *   True for almost all of them, because almost all are already a single flat maroon or black
     *   shape — a tint gives back the same silhouette in a readable colour. True for **Japa** too,
     *   which is not flat: its mala is twelve maroon beads and one gold guru bead. Tinting costs the
     *   gold, but leaving it costs the twelve, and a lone gold dot where a ring of beads should be
     *   is worse than a ring in one colour. The guru bead still reads, by being larger.
     *
     *   False for the two that are drawings rather than symbols — the Panchanga scribe and the
     *   Rashifal wheel — where a tint would flatten shading and detail into a silhouette.
     */
    data class Glyph(
        @param:DrawableRes val res: Int,
        val tintable: Boolean = true,
    ) : TileIcon

    /** A Devanagari letter, drawn as text — the pattern the Om tile already uses, and the
     *  placeholder for a domain that has no artwork yet. */
    data class Letter(
        val text: String,
    ) : TileIcon

    /**
     * Today's date, drawn as the icon.
     *
     * It carries no date of its own, on purpose. [HubCatalog] is an object whose lists are built
     * once at class-init, so a stored date would freeze at process start; and [HubTile] is a data
     * class used as the identity a tap is dispatched on, so a tile that stopped equalling itself at
     * midnight would break that quietly. The date is read where it is drawn.
     */
    data object Today : TileIcon
}

/** Which container colour a tile takes. */
enum class HubCategory {
    DAILY,
    ASTROLOGY,
    DEVOTION,
}

/** What tapping a tile does. */
sealed interface TileAction {
    /** Navigate to a built screen. */
    data class Open(
        val target: HubTarget,
    ) : TileAction

    /** Open the domain's own screen, listing what it holds. */
    data class Drill(
        val domain: HubDomain,
    ) : TileAction

    /** Say what the state of play is. [note] is the whole message a reader gets, so it has to earn it. */
    data class NotYet(
        val note: String,
    ) : TileAction
}

/** One tile. */
data class HubTile(
    val label: String,
    val icon: TileIcon,
    val category: HubCategory,
    val action: TileAction,
)

/**
 * An area of the tradition the app either covers or intends to.
 *
 * Deliberately **not** called a shastra. These are not peers in the traditional taxonomy and saying
 * so would be a false claim: Panchanga is an *output* of Jyotisha rather than a discipline, Muhurta
 * and this enum's [JYOTISHA] are two *skandhas* of the one Vedanga, Yoga is a darshana on an
 * entirely different axis, and Mantra & Stotra is a practice. The section is called **Explore** for
 * the same reason. `docs/roadmap.md` carries the actual taxonomy, and the map from these entries to
 * it.
 *
 * What they are instead is the roadmap's domains that are, or would be, **places a reader goes** —
 * which is what a tile is for. Every other kind of roadmap entry is deliberately absent, and the
 * omissions are decisions rather than oversights:
 *
 * - The **foundations** (F1–F6 — localization, accessibility, content provenance, the portable
 *   engine) are engineering concerns with nothing for a reader to open.
 * - **Nirukta/Vyakarana** (K8) was re-termed as a glossary layer reachable from any Sanskrit term
 *   rather than a section, so a tile would contradict the decision that put it there.
 * - **Kala** (C4) and **Kalpa** (K3) have shipped, but into the calendar's day detail — the era
 *   years and the sankalpa frame are rows on a day, not destinations. A tile whose whole message is
 *   "look at the Calendar" is worse than no tile: it costs a tap to learn nothing.
 * - **Chandas** (C6) is held back until its shape is clearer. Tiling a domain nobody has thought
 *   through yet advertises a plan that does not exist.
 *
 * All four remain on the roadmap. Not being a destination is not the same as not being wanted.
 *
 * **The Arts** (K7) is absent differently, and is the one entry that is in this enum without being
 * on the hub — see [tiled]. The others were never destinations; this one is, and is held back for
 * the reason Chandas is: it is blocked on audio and images the app cannot carry, so nobody has
 * decided what it looks like when it arrives. It keeps its label, artwork and note so that tiling it
 * again is one word, and so the reason survives in the place someone will look.
 *
 * @property id the roadmap's own identifier, so the two can be checked against each other.
 * @property note what a reader is told on tapping, when the domain has no screen to open. Null only
 *   for [DomainStatus.BUILT].
 * @property opens the screen this domain *is*, when it holds exactly one. A domain normally drills
 *   into a list of what it contains, but a list of one whose single entry repeats its parent's name
 *   is a tap that teaches nothing — so such a domain opens its screen directly instead.
 * @property tiled whether the hub draws this domain at all. False means it is on the roadmap and in
 *   this enum, but deliberately not offered yet; see the KDoc above for which and why.
 */
enum class HubDomain(
    val id: String,
    val label: String,
    val status: DomainStatus,
    val icon: TileIcon,
    val category: HubCategory,
    val blurb: String,
    val note: String? = null,
    val opens: HubTarget? = null,
    val tiled: Boolean = true,
) {
    PANCHANGA(
        id = "C1",
        label = "Panchanga",
        status = DomainStatus.BUILT,
        icon = TileIcon.Glyph(VedicIcons.panchang, tintable = false),
        category = HubCategory.DAILY,
        blurb = "The five limbs of the day, and the calendar they sit in.",
    ),

    // Labelled Jyotish, the popular spelling rather than the Sanskrit *Jyotisha* -- ADR 0019 rule 4,
    // the form a person uses for something they are choosing. It is also not Kundali: that is the
    // name of the screen *inside* this domain, and a tile sharing its name with one of its own
    // children reads as a mistake.
    //
    // The cost, stated rather than hidden: what this domain holds is *hora*, one skandha, and
    // Panchanga and Muhurta beside it are the other two -- so the tile carries the parent's name
    // while holding one branch of it. The blurb opens with "Hora" to say so. `docs/roadmap.md` has
    // the full taxonomy, and spells the discipline *Jyotisha* because there it is naming the
    // Vedanga rather than a thing a reader taps.
    JYOTISHA(
        id = "C2",
        label = "Jyotish",
        status = DomainStatus.BUILT,
        icon = TileIcon.Glyph(VedicIcons.jyotisha),
        category = HubCategory.ASTROLOGY,
        blurb = "Hora — birth charts, dashas, and what a day reads like against them.",
    ),
    // Opens its screen rather than drilling. It used to hold two tiles, one of them called
    // "Muhurta" -- the domain's own name, one row below itself. That was not drift: ADR 0019 found
    // *Muhurat / Muhurta* colliding and settled on "Muhurta" for both, which made parent and child
    // identical. Reminders moved up to the landing, leaving a list of one, and a list of one that
    // repeats its parent is a tap that teaches nothing.
    MUHURTA(
        id = "C3",
        label = "Muhurta",
        status = DomainStatus.BUILT,
        icon = TileIcon.Glyph(VedicIcons.muhurat),
        category = HubCategory.DAILY,
        blurb = "Choosing a time, and being reminded when it comes.",
        opens = HubTarget.MUHURAT,
    ),
    FESTIVALS(
        id = "K1",
        label = "Festivals & Vrata",
        status = DomainStatus.BUILT,
        icon = TileIcon.Glyph(VedicIcons.festivals),
        category = HubCategory.DAILY,
        blurb = "Named festivals, and the observances that recur each month.",
    ),
    MANTRA(
        id = "K5",
        label = "Mantra & Stotra",
        status = DomainStatus.BUILT,
        icon = TileIcon.Glyph(VedicIcons.mantra),
        category = HubCategory.DEVOTION,
        blurb = "Hymns to read, mantras to count, and a timer to sit with.",
    ),
    DHARMA(
        id = "K2",
        label = "Dharma & Samskara",
        status = DomainStatus.NEXT,
        icon = TileIcon.Glyph(VedicIcons.dharma),
        category = HubCategory.DEVOTION,
        blurb = "The sixteen samskaras, and the observances of a stage of life.",
        note = "Dharma and the samskaras — next up, and the closest to being built.",
    ),
    VASTU(
        id = "C5",
        label = "Vastu",
        status = DomainStatus.OPEN,
        icon = TileIcon.Glyph(VedicIcons.vastu),
        category = HubCategory.ASTROLOGY,
        blurb = "Orientation and placement, computed from a bearing.",
        note = "Vastu — planned, and open for anyone who wants to build it.",
    ),
    AYURVEDA(
        id = "K4",
        label = "Ayurveda",
        status = DomainStatus.OPEN,
        icon = TileIcon.Glyph(VedicIcons.ayurveda),
        category = HubCategory.DEVOTION,
        blurb = "The shape of a day and of a season, as tradition describes them.",
        note = "Ayurveda — planned, and deliberately limited to daily and seasonal routine.",
    ),
    YOGA(
        id = "K6",
        label = "Yoga",
        status = DomainStatus.OPEN,
        icon = TileIcon.Glyph(VedicIcons.yoga),
        category = HubCategory.DEVOTION,
        blurb = "The eight limbs, explained rather than instructed.",
        note = "Yoga — planned, and open for anyone who wants to build it.",
    ),
    // Kept here and on the roadmap, but off the hub. K7 is blocked on media the app cannot carry --
    // raga needs audio, iconography needs images -- and nobody has decided what it would look like
    // if it arrived. A tile advertises a section; this one would advertise a shape that does not
    // exist yet. The note stays accurate for the day it is tiled again.
    ARTS(
        id = "K7",
        label = "The Arts",
        status = DomainStatus.EXPLORING,
        icon = TileIcon.Glyph(VedicIcons.arts),
        category = HubCategory.DEVOTION,
        blurb = "Architecture, sculpture, music and drama.",
        note = "The arts — still being explored; they need audio and images the app cannot carry yet.",
        tiled = false,
    ),
    ;

    /**
     * Whether this domain has anything behind it yet.
     *
     * Not the same as "drills": a built domain either holds a list of its own or, when [opens] is
     * set, *is* a single screen. Both are openable; only the first drills.
     */
    val isOpenable: Boolean get() = status == DomainStatus.BUILT

    /** What tapping this domain's tile does. */
    internal fun tileAction(): TileAction {
        val target = opens
        return when {
            target != null -> TileAction.Open(target)
            isOpenable -> TileAction.Drill(this)
            else -> TileAction.NotYet(note.orEmpty())
        }
    }
}

/**
 * Every tile the hub draws, at both levels.
 *
 * One list, so that moving the roadmap means editing one place. `HubCatalogTest` checks this against
 * the roadmap's own domain ids, which is what keeps "the roadmap is visible in the app" true rather
 * than merely intended.
 */
object HubCatalog {
    // A temple-bell glyph. Declared first -- an object's properties initialise in source order, and
    // the list below reads it.
    private val remindersTile =
        HubTile("Reminders", TileIcon.Glyph(VedicIcons.reminders), HubCategory.DAILY, open(HubTarget.REMINDERS))

    /**
     * The one grid the landing draws: the domain map, plus the single destination that is not a
     * domain.
     *
     * There used to be a **Today** row above this one, holding Today's Panchanga, Calendar and
     * Reminders. Two of the three earned nothing: the hero card at the top of Home already opens
     * Today's Panchanga — showing far more than a tile could — and Calendar sits under Panchanga
     * where a reader looking for a calendar would look. A shortcut to something already on screen is
     * not a shortcut.
     *
     * **Reminders is the exception, and it is not a domain.** It is a place a reader goes often and
     * belongs to no shastra, so it sits here beside Muhurta, where a reminder is set. The cost is
     * stated rather than hidden: this grid is otherwise the roadmap made visible, and one tile in it
     * is not on the roadmap. One tap was judged worth that.
     */
    val explore: List<HubTile> =
        HubDomain.entries.filter { it.tiled }.flatMap { domain ->
            val tile =
                HubTile(
                    label = domain.label,
                    icon = domain.icon,
                    category = domain.category,
                    action = domain.tileAction(),
                )
            if (domain == HubDomain.MUHURTA) listOf(tile, remindersTile) else listOf(tile)
        }

    /**
     * What sits inside [domain]. Empty unless the domain actually drills — which means anything not
     * built yet, and also Muhurta, which is one screen rather than a list and so opens directly.
     */
    fun tilesIn(domain: HubDomain): List<HubTile> =
        when (domain) {
            // Today's Panchanga is deliberately not the panchang glyph: that is this domain's own
            // icon, and a child repeating its parent's symbol directly beneath it reads as a
            // mistake. The date also says what the glyph could not -- which day this opens.
            HubDomain.PANCHANGA ->
                listOf(
                    HubTile("Today's Panchanga", TileIcon.Today, domain.category, open(HubTarget.PANCHANG)),
                    tile("Calendar", VedicIcons.calendar, domain.category, HubTarget.CALENDAR),
                )

            HubDomain.JYOTISHA ->
                listOf(
                    tile("Kundali", VedicIcons.kundali, domain.category, HubTarget.KUNDALI),
                    tile("Rashifal", VedicIcons.rashifal, domain.category, HubTarget.RASHIFAL, tintable = false),
                    tile("Match", VedicIcons.matchmaking, domain.category, HubTarget.MATCH),
                )

            HubDomain.FESTIVALS ->
                listOf(
                    tile("Festivals", VedicIcons.festivals, domain.category, HubTarget.FESTIVALS),
                    tile("Events", VedicIcons.events, domain.category, HubTarget.EVENTS),
                )

            HubDomain.MANTRA ->
                listOf(
                    HubTile("Stotra", TileIcon.Letter("ॐ"), domain.category, open(HubTarget.STOTRA)),
                    tile("Japa", VedicIcons.japa, domain.category, HubTarget.JAPA),
                    tile("Meditate", VedicIcons.meditate, domain.category, HubTarget.MEDITATE),
                )

            else -> emptyList()
        }

    private fun tile(
        label: String,
        @DrawableRes glyph: Int,
        category: HubCategory,
        target: HubTarget,
        tintable: Boolean = true,
    ): HubTile = HubTile(label, TileIcon.Glyph(glyph, tintable), category, open(target))

    private fun open(target: HubTarget): TileAction = TileAction.Open(target)
}
