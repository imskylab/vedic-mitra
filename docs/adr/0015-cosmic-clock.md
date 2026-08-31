# 15. Cosmic Clock — a family of clock faces

- **Status:** Superseded by the cycle rows on the calendar's day detail (2026-08-31)
- **Date:** 2026-08-29

## Superseded

The circular face is gone. It was replaced by prev / current / next rows on the calendar's day
detail — the same five limbs as a picker wheel rather than as arcs.

**Why it lost.** The face put each limb at its own angle, because each cycle has its own position.
That is defensible and it meant the five current values were scattered around the circle, so a reader
could never see them together. It also only ever showed *now*: a ring cannot say what the previous
tithi was or what the next one is, and that sequence is exactly what someone new to a panchanga does
not know. Rows show it for free, in text, at a glance.

The decisions below are kept because the findings cost something to get and would otherwise be
rediscovered — particularly the two that only appeared when the geometry was rendered, and the note
about which layers are not angular data at all. That last one is what eventually pointed at rows:
the calendar's table turned out to be three different kinds of thing, and only nine of its twenty
rows are cycles.

What survived the retirement: `PanchangaPrimer` (the eleven explanations, in `core:astronomy`),
`PanchangaLimb` / `LimbStep` (the cycle arithmetic), and the observation that `angularFraction` had
been computed and unused since it was written — it is now the progress bar on each row.

## Context

The app could compute a great deal about a moment and could only show it as tables. The ambition was
a *"moving clock style that gives any end user with little or no knowledge of these concepts a good
amount of clarity"*.

An earlier design — one screen of fifteen concentric rings — was abandoned for two reasons. The
document describing it was lost and could not be recovered, so it was redesigned from the goal rather
than reconstructed. And the goal contains a tension worth stating plainly: **"covers every aspect" and
"clear to a beginner" pull against each other.** A dial dense enough to carry fifteen layers is
unreadable; a dial a beginner can read cannot carry fifteen layers. Density is not clarity — five
concentric rings of Sanskrit terms produce awe, not understanding.

A third constraint only became visible while building: `PanchangaGlossary` had entries for named
windows and festivals but **none for the limbs themselves**. Tapping "Tithi" produced *"More details
coming soon."* Whatever was drawn would have been mute.

## Decision

**A family of clock faces, not one dial.** Each face has exactly one angular meaning, and faces are
reached by drilling down rather than stacked on top of each other.

The first is the **Panchanga clock**: five concentric rings, one per limb, each showing a whole cycle
with the current division picked out and filled as far as its progress. Every ring means the same
thing — *position within this limb's cycle* — which is what lets five sit on one face without the
angle changing meaning between them. Time of day gets its own face later.

### Rings are ordered by segment count, not by recitation order

Karana (60) outermost, then Tithi (30), Nakshatra (27), Yoga (27), Vara (7) innermost.

At radius `r` a ring's arc per segment is `2πr / n`, so tick spacing only stays comparable across all
five if `r` grows with `n`. Recitation order would put 60 karana ticks on the smallest ring and 7
vara ticks on the largest. It also happens to place the fastest-moving limb where movement shows.

### Explanatory copy is a first-class deliverable, enforced by the build

`PanchangaPrimer` is keyed by a `PanchangaConcept` enum, not by display string, and looked up with
`getValue` over a complete map. **Adding a concept without writing its copy fails the build.**

Explanatory text is what gets cut when time runs short, and the reason is usually that nothing
enforces it. A string-keyed map degrades silently to a placeholder; an enum-keyed total map fails
loudly in CI. Each entry carries a one-liner shown *without* tapping, because clarity that only
arrives on tap is clarity most readers never get.

### The clock and the list are two renderings of one model

The limb list below the face is not a fallback. It serves three audiences at once: TalkBack (a
`Canvas` is invisible to it), anyone who would rather read exact times as text, and anyone whose tap
missed. The face gets a single spoken summary rather than per-arc semantics nodes, which would mean
maintaining a second hit-test that drifts from the drawing.

## Consequences

**Positive.** Each face keeps one angular meaning, so a second face costs no legibility on the first.
The engine needed no changes at all: `PanchangaLimbWindows` already carried a window and progress
fraction for exactly the six values drawn, and `angularFraction` — documented as *"the right input
for a progress arc"* — had been computed and unused since it was written. The geometry is pure Kotlin
and covered by tests, which matters because the repo has no UI test infrastructure.

**Neutral.** One route hosts the family; a face selector appears when the second lands, with no
navigation change. The Home tile uses a Material icon as a placeholder — the core icon set has no
clock, and `material-icons-extended` is not worth its size for one tile — so a brand glyph is still
wanted.

**Negative, and worth knowing.**

- **This is a slow clock.** Four of its five rings turn over in about a day. Its motion is the active
  arc filling and the countdowns ticking. Time-scrubbing would make all five turn at once and is the
  single best demonstration of what a panchanga is, but it needs a cheaper engine path — the full
  limb windows cost roughly 270 ephemeris evaluations.
- **Ring taps are approximate.** Five rings share the radius between hub and rim, about 19dp each at
  a 160dp face. Five 48dp bands would need 240dp of radius before the hub got any, so this cannot be
  tuned away. The rings are a convenience; the list is the reliable target.
- **The face cannot show pada.** Nesting it inside the active nakshatra arc was tried: that arc spans
  13.3°, so a quarter of it is 3.3° — about five pixels. It survives in the list, the hub and the
  spoken summary, all of which have room to name it.

## Two findings that only appeared when the geometry was rendered

Both were ideas approved in the plan, and both were wrong. Mirroring the drawing in a script — the
same degrees-clockwise-from-three-o'clock convention Compose's `drawArc` uses — produced the picture
the code would actually make, before anyone built the app.

**Dimming does not fix density; weight does.** Inactive divisions drawn at full band width gave a
brick wall of about 150 blocks with the active arcs lost inside. Alpha changed the colour and not the
mass. They are now hairlines at 30% of the band.

**The nested pada is unreadable**, as above.

The general lesson: a radial design's failures are arithmetic, and arithmetic can be checked before
it is built.

## Related

- Home hub and navigation: [ADR 0013](0013-home-hub-landing-and-navigation.md)
- Choghadiya, deferred to the day-windows face: [ADR 0011](0011-choghadiya.md)
