# 22. One hub grid, and a domain may be a destination

- **Status:** Accepted (supersedes points 2 and 3 of ADR 0018)
- **Date:** 2026-09-08

## Context

ADR 0018 gave the landing two grids: a **Today** row of the three destinations opened daily, and a
grid of every domain on the roadmap. Both halves have since been overtaken by the screen around them.

**The Today row repeated what was already on screen.** It held Today's Panchanga, Calendar and
Reminders. By now the hero card at the top of Home is clickable across its whole surface and opens
Today's Panchanga — while showing the weekday, the moon phase, the running tithi with a live
countdown, and the maasa and samvatsara. The tile below it offered the same destination and less
reason to tap. Calendar, meanwhile, sits under Panchanga, which is where someone looking for a
calendar looks. ADR 0018 justified the duplication as buying one tap; two of the three tiles were by
then buying nothing, because the thing they shortcut to was already in view.

**Muhurta had come to duplicate its own name.** It drilled into a list of two, the first of which was
also called "Muhurta". That was not drift. ADR 0019 found *Muhurat / Muhurta* colliding across the
hub and settled on **Muhurta** everywhere in display text, which made parent and child identical —
the same fault [ADR 0018's own point 2 invited](0018-two-level-hub-and-roadmap-tiles.md) by assuming
every built domain is a container. It is the fault fixed for Panchanga in #239, one level up.

**The Arts had a tile and no shape.** K7 is *Exploring*, blocked on audio and images the app cannot
carry. ADR 0018's point 3 — every domain gets a tile, built or not — was written so the roadmap would
be visible rather than hidden. But a tile is an offer, and nobody has decided what this one would
open onto. "Soon" (ADR 0020) answers *when*; it cannot answer *what*.

## Decision

**1. One grid.** The landing draws a single **Explore** grid. `HubCatalog.today` is gone.

Nothing became less reachable. Today's Panchanga is one tap via the hero card, and two under
Panchanga; Calendar is two under Panchanga. `HubCatalogTest` still asserts every `HubTarget` has a
tile leading to it, which is the guarantee that actually mattered — the deleted
*"the daily tiles are a shortcut, never the only way to something"* test existed only to protect a
duplication that no longer exists.

**2. Reminders sits in that grid, and is not a domain.** It is opened often and belongs to no
shastra. Keeping it at one tap means the Explore grid is *the domain map plus one*, which weakens
the claim that this grid is the roadmap made visible. That is the cost, stated rather than absorbed:
one tap was judged worth it, and a test pins Reminders as the **only** tile there that is not a
domain, so the exception cannot quietly become two.

**3. A domain may be a destination rather than a container.** `HubDomain.opens` names a screen; when
it is set the tile opens that screen instead of drilling. Muhurta is the only such domain today.

This replaces the assumption that built ⇒ drills. A list of one whose single entry repeats its
parent's name costs a tap and teaches nothing.

**4. A domain can be on the roadmap and off the hub.** `HubDomain.tiled = false` keeps The Arts in
the enum, on the roadmap, with its label, artwork and note intact — and draws no tile.

Deliberately *not* deletion. The reasoning survives where the next person will look, and tiling it
again is one word. It is a different kind of absence from C4, K3, C6 and K8, which are not in the
enum at all because they were never destinations; The Arts is one, held back until it has a shape.

**5. The hero card announces itself as a button.** It was a bare `clickable` with no role. That was
survivable while a labelled tile offered the same destination; as the only one-tap route, it is not.

## Consequences

- Home is one section shorter, and the first tile grid a reader meets is the map rather than a row of
  shortcuts to things above it.
- **The hub no longer shows every domain**, so it is no longer a complete picture of the roadmap. The
  gap is one domain, recorded in `HubDomain`'s KDoc and pinned by name in a test, and `docs/roadmap.md`
  remains the complete list.
- `TileIcon.Today` — the live date drawn as an icon (#239) — now appears once, under Panchanga,
  rather than twice.
- Muhurta has no level-two screen, so the parameterised domain route has one fewer user. It still has
  four.
- **A future domain that holds exactly one screen should set `opens` rather than drilling.** The test
  now asserts all three outcomes, so getting it wrong fails rather than shipping a wasted tap.

## Alternatives considered

**Keep Today, drop only its Panchanga tile.** A one-tile row is not a section, and Calendar under
Panchanga is not a hardship. Deleting the row was simpler than defending a remnant of it.

**Delete `HubDomain.ARTS`.** Cleanest code, and it loses the reasoning. Restoring the domain would
then be a rewrite rather than a word, and the roadmap-to-hub id mapping in `HubCatalogTest` would
have to be edited — the one place that currently proves the two agree.

**Leave Reminders under Muhurta only.** Honest about the grid being purely domains, and it puts a
daily destination two taps away behind a domain screen that would then exist solely to hold it. That
is the arrangement this ADR replaces.
