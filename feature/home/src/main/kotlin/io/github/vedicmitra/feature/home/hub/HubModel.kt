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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.graphics.vector.ImageVector
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

/**
 * How far along a domain is, mirroring `docs/roadmap.md`.
 *
 * [PARTLY] is the awkward and interesting one: Kalpa and Kala have both shipped something, but what
 * shipped lives *inside* the calendar's day detail rather than as a destination of its own, so the
 * tile has nothing to open even though the domain is not untouched.
 */
enum class DomainStatus {
    BUILT,
    PARTLY,
    NEXT,
    OPEN,
    EXPLORING,
}

/**
 * What a tile draws.
 *
 * [Glyph] is one of the brand's ornate cultural drawables and [Letter] a Devanagari initial. The
 * split is not decoration: the ornate glyphs exist only for features that shipped, and the app
 * bundles no Material icon that could honestly stand for Vastu or Chandas. So **the icon style is
 * itself a status signal** — ornate means built, a letter means not yet — which matters because
 * status must not be carried by colour alone, and the glyphs cannot even be tinted (they hold their
 * own maroon and gold, and are drawn with `Color.Unspecified`).
 */
sealed interface TileIcon {
    /** An ornate cultural glyph from [VedicIcons]. Reserved for what is built. */
    data class Glyph(
        @param:DrawableRes val res: Int,
    ) : TileIcon

    /** A Devanagari letter, drawn as text — the pattern the Om tile already uses. */
    data class Letter(
        val text: String,
    ) : TileIcon

    /** A Material symbol, tinted like one. Utilitarian things keep the plainer style, per VedicIcons. */
    data class Symbol(
        val vector: ImageVector,
    ) : TileIcon
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
 * A shastra the app either covers or intends to.
 *
 * The set mirrors `docs/roadmap.md` so that the roadmap is visible in the app rather than only in the
 * repository. Two kinds of roadmap entry are deliberately absent: the **foundations** (F1–F6 —
 * localization, accessibility, content provenance, the portable engine) are engineering concerns with
 * nothing for a reader to open, and **Nirukta/Vyakarana** (K8) was re-termed as a glossary layer
 * reachable from any Sanskrit term rather than a section of its own, so giving it a tile would
 * contradict the decision that put it there. The Declined domains are absent for the obvious reason.
 *
 * @property id the roadmap's own identifier, so the two can be checked against each other.
 * @property note what a reader is told on tapping, when the domain has no screen to open. Null only
 *   for [DomainStatus.BUILT].
 */
enum class HubDomain(
    val id: String,
    val label: String,
    val status: DomainStatus,
    val icon: TileIcon,
    val category: HubCategory,
    val blurb: String,
    val note: String? = null,
) {
    PANCHANGA(
        id = "C1",
        label = "Panchanga",
        status = DomainStatus.BUILT,
        icon = TileIcon.Glyph(VedicIcons.panchang),
        category = HubCategory.DAILY,
        blurb = "The five limbs of the day, and the calendar they sit in.",
    ),
    JYOTISHA(
        id = "C2",
        label = "Jyotisha",
        status = DomainStatus.BUILT,
        icon = TileIcon.Glyph(VedicIcons.kundali),
        category = HubCategory.ASTROLOGY,
        blurb = "Birth charts, dashas, and what a day reads like against them.",
    ),
    MUHURTA(
        id = "C3",
        label = "Muhurta",
        status = DomainStatus.BUILT,
        icon = TileIcon.Glyph(VedicIcons.muhurat),
        category = HubCategory.DAILY,
        blurb = "Choosing a time, and being reminded when it comes.",
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
        icon = TileIcon.Glyph(VedicIcons.japa),
        category = HubCategory.DEVOTION,
        blurb = "Hymns to read, mantras to count, and a timer to sit with.",
    ),
    KALPA(
        id = "K3",
        label = "Kalpa",
        status = DomainStatus.PARTLY,
        icon = TileIcon.Letter("क"),
        category = HubCategory.DEVOTION,
        blurb = "Ritual procedure, tied to the timing the app already computes.",
        note = "Kalpa — the sankalpa frame is on a day's detail in the Calendar.",
    ),
    KALA(
        id = "C4",
        label = "Kala",
        status = DomainStatus.PARTLY,
        icon = TileIcon.Letter("का"),
        category = HubCategory.DAILY,
        blurb = "Reckoning time: the eras, the cycles, and the older units.",
        note = "Kala — the Vikrama, Shaka and Kali years are on a day's detail in the Calendar.",
    ),
    DHARMA(
        id = "K2",
        label = "Dharma & Samskara",
        status = DomainStatus.NEXT,
        icon = TileIcon.Letter("ध"),
        category = HubCategory.DEVOTION,
        blurb = "The sixteen samskaras, and the observances of a stage of life.",
        note = "Dharma and the samskaras — next up, and the closest to being built.",
    ),
    VASTU(
        id = "C5",
        label = "Vastu",
        status = DomainStatus.OPEN,
        icon = TileIcon.Letter("वा"),
        category = HubCategory.ASTROLOGY,
        blurb = "Orientation and placement, computed from a bearing.",
        note = "Vastu — planned, and open for anyone who wants to build it.",
    ),
    CHANDAS(
        id = "C6",
        label = "Chandas",
        status = DomainStatus.OPEN,
        icon = TileIcon.Letter("छ"),
        category = HubCategory.ASTROLOGY,
        blurb = "Prosody: scanning a verse and naming its metre.",
        note = "Chandas — planned, and open for anyone who wants to build it.",
    ),
    AYURVEDA(
        id = "K4",
        label = "Ayurveda",
        status = DomainStatus.OPEN,
        icon = TileIcon.Letter("आ"),
        category = HubCategory.DEVOTION,
        blurb = "The shape of a day and of a season, as tradition describes them.",
        note = "Ayurveda — planned, and deliberately limited to daily and seasonal routine.",
    ),
    YOGA(
        id = "K6",
        label = "Yoga",
        status = DomainStatus.OPEN,
        icon = TileIcon.Letter("यो"),
        category = HubCategory.DEVOTION,
        blurb = "The eight limbs, explained rather than instructed.",
        note = "Yoga — planned, and open for anyone who wants to build it.",
    ),
    ARTS(
        id = "K7",
        label = "The Arts",
        status = DomainStatus.EXPLORING,
        icon = TileIcon.Letter("शि"),
        category = HubCategory.DEVOTION,
        blurb = "Architecture, sculpture, music and drama.",
        note = "The arts — still being explored; they need audio and images the app cannot carry yet.",
    ),
    ;

    /** Whether this domain has a screen of its own to open. */
    val isOpenable: Boolean get() = status == DomainStatus.BUILT
}

/**
 * Every tile the hub draws, at both levels.
 *
 * One list, so that moving the roadmap means editing one place. `HubCatalogTest` checks this against
 * the roadmap's own domain ids, which is what keeps "the roadmap is visible in the app" true rather
 * than merely intended.
 */
object HubCatalog {
    /**
     * The handful of destinations opened daily, kept one tap away.
     *
     * These also appear under their domain below. The duplication is the point: a pure hierarchy
     * would put the calendar and the reminders list two taps deep, every time, which is a poor trade
     * for the tidiness it buys.
     */
    val today: List<HubTile> =
        listOf(
            tile("Today's Panchang", VedicIcons.panchang, HubCategory.DAILY, HubTarget.PANCHANG),
            tile("Calendar", VedicIcons.calendar, HubCategory.DAILY, HubTarget.CALENDAR),
            HubTile("Reminders", reminderIcon, HubCategory.DAILY, open(HubTarget.REMINDERS)),
        )

    /** One tile per domain — the shastra map, as the hub shows it. */
    val domains: List<HubTile> =
        HubDomain.entries.map { domain ->
            HubTile(
                label = domain.label,
                icon = domain.icon,
                category = domain.category,
                action = if (domain.isOpenable) TileAction.Drill(domain) else TileAction.NotYet(domain.note.orEmpty()),
            )
        }

    /** What sits inside [domain]. Empty for anything not yet built, which is why those tiles do not drill. */
    fun tilesIn(domain: HubDomain): List<HubTile> =
        when (domain) {
            HubDomain.PANCHANGA ->
                listOf(
                    tile("Today's Panchang", VedicIcons.panchang, domain.category, HubTarget.PANCHANG),
                    tile("Calendar", VedicIcons.calendar, domain.category, HubTarget.CALENDAR),
                )

            HubDomain.JYOTISHA ->
                listOf(
                    tile("Kundali", VedicIcons.kundali, domain.category, HubTarget.KUNDALI),
                    tile("Rashifal", VedicIcons.rashifal, domain.category, HubTarget.RASHIFAL),
                    tile("Match", VedicIcons.matchmaking, domain.category, HubTarget.MATCH),
                )

            HubDomain.MUHURTA ->
                listOf(
                    tile("Muhurat", VedicIcons.muhurat, domain.category, HubTarget.MUHURAT),
                    HubTile("Reminders", reminderIcon, domain.category, open(HubTarget.REMINDERS)),
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
    ): HubTile = HubTile(label, TileIcon.Glyph(glyph), category, open(target))

    private fun open(target: HubTarget): TileAction = TileAction.Open(target)

    // A bell, not a cultural glyph: VedicIcons reserves the ornate style for signature features and
    // sends utilitarian ones to Material Symbols.
    private val reminderIcon = TileIcon.Symbol(Icons.Filled.Notifications)
}
