# 13. Home hub landing page and navigation redesign

- **Status:** Superseded by ADR-0018
- **Date:** 2026-08-12

## Context

The current Home screen is a single-purpose panchang dashboard, and the bottom bar is
Home · Panchang · Reminders · Settings. That was right for a panchang app, but the roadmap now adds a
whole astrology arc (Profile, Kundali, Rashifal, Muhurta),
devotion tools (Phase 11), and more. A single panchang dashboard cannot be the front door to all of
that: there is nowhere to surface the growing set of features, and no room to grow without either
overloading Home or hiding features behind Settings.

The reference pattern (banking/super-apps like ICICI iMobile and HDFC) is a **hub landing page**: a
contextual hero on top, a categorised grid of shortcuts, and a bottom navigation bar. It scales by
adding tiles, not by redesigning the screen.

## Decision

Introduce a **Home hub** as the new default landing screen, and restructure navigation around it. The
existing panchang dashboard is **not removed** — it becomes the Panchang destination, reachable from
the hub's hero card and its own bottom-nav tab.

**Layout — four zones (hero + grid):**

1. **Top bar** — brand, search, profile avatar, plus a greeting ("Namaste, <name>") and the active
   location + sunrise chip.
2. **Contextual hero** — today's panchang at a glance (tithi, nakshatra, yoga, sunset, Moon phase),
   tapping through to the full daily Panchang screen; plus a live "auspicious now" strip (current
   Choghadiya / kalam).
3. **Category tabs** — `Daily` · `Astrology` · `Devotion` — keep the grid uncluttered as features grow.
4. **Shortcut grid** — icon tiles per category (the launcher pattern).

**Bottom navigation:** `Home` · `Panchang` · `Reminders` · `Explore` · `Profile`.

- `Home` — the hub (default).
- `Panchang` — the former Home dashboard (daily panchang) plus the monthly calendar.
- `Reminders` — unchanged.
- `Explore` — festivals, knowledge/devotion, discover.
- `Profile` — the astrology birth profile (Phase 5) and settings (theme, location, about).

**Tile inventory, mapped to roadmap phases** — tiles for unbuilt features render in a subtle
"coming" state rather than being hidden, so the hub doubles as feature discovery:

| Category | Tiles | Phase |
| --- | --- | --- |
| Daily | Panchang, Calendar, Muhurat, Choghadiya, Festivals, Reminders | shipped (2–4, 8) |
| Astrology | Kundali, Rashifal, Grahas, Muhurat (electional), Match | 5–6 |
| Devotion | Daily shloka, Stotra, Chant counter, Meditation | 11 |

**Behaviour:**

- A tile whose feature isn't built yet is visibly de-emphasised and, on tap, explains what unlocks it
  (e.g. "Set up your birth profile to see your Kundali") rather than dead-ending.
- Search spans features first, then panchang/festival lookups.
- The hub is theming-native: golden/maroon brand, Material You / dynamic colour, and dark mode all
  apply, consistent with the rest of the app.

## Consequences

- **Positive:** one scalable entry point for the entire roadmap — new phases drop tiles into a
  category instead of forcing a redesign. Unbuilt-feature tiles turn the hub into a discovery
  surface. The panchang dashboard is preserved, just no longer the sole front door.
- **Neutral:** navigation grows from four tabs to five; Settings moves under Profile; the hub is a new
  screen in `:feature:home` alongside the existing dashboard (which is promoted into the Panchang
  destination). This is a UI/UX change (Phase 9), independent of the astrology engine work.
- **Negative / risks:** a hub that lists many not-yet-built tiles can feel empty or "coming soon"-
  heavy early on — mitigated by keeping the default `Daily` tab all-shipped, and only revealing
  Astrology/Devotion tiles as those phases approach. Five bottom-nav items is the practical maximum;
  further destinations must go inside Explore or the hub, not the bar.

## Amendment — 2026-08-27

What shipped differs from the Decision above in one respect, and this records it rather than leaving
the ADR to drift.

**The bottom bar has four tabs, not five:** `Home` · `Settings` · `Profile` · `Support`. `Panchang`,
`Reminders` and `Explore` were never promoted to tabs. Reminders and the calendar are reached from
hub tiles; `Explore` was never built as a destination at all, its contents (festivals, devotion)
being hub tiles instead. `Support` was added after this ADR, for daana.

The hub-and-tiles decision itself held up — new features have landed as tiles without a redesign,
which was the point. It is only the bar that stayed smaller, and that looks like the better call: the
ADR itself notes five is the practical maximum, and four leaves the room it warns about needing.

**Panchang is now a destination, as this ADR always said it should be.** For a period it was neither
a tab nor a route but *state inside the Home screen*, along with the Festivals and Events lists. That
cost four separate workarounds — the app-bar title could not see which sub-view was open, the
app-level back handler had to be disabled on Home, the bottom Home tab could not return to the hub
without a counter passed down into the screen, and each sub-view drew its own title and back arrow
because the app bar could not, so the title appeared twice. All three are now ordinary pushed routes
(`panchang`, `festivals`, `events`) and all four workarounds are gone.

The general lesson is worth keeping: **a drill-down is a route.** Screen-local state is right for
peer sections that are simultaneously visible and swipe-connected — the Kundali tabs are correctly
built that way — but anything reached by tapping *into* it, and left by pressing back, belongs on the
back stack. Every one of those four workarounds existed to re-implement something the back stack
provides for free.

## Related

- Roadmap: Phase 9 (UI & User Experience → Dashboard / Landing hub)
- Prior Home/landing work: [ADR 0008](0008-festivals-and-home-landing.md)
