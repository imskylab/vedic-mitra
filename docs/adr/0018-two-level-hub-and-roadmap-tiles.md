# 18. A two-level hub, with a tile for every shastra on the roadmap

- **Status:** Accepted (supersedes ADR 0013)
- **Date:** 2026-09-02

## Context

ADR 0016 redrew the roadmap as a map of shastra domains. None of it was visible in the app: the hub
showed twelve tiles for twelve built features, so a reader had no way to see that Vastu, Ayurveda or
Chandas were intended, and a would-be contributor had no way to see what was wanted without finding
the repository.

ADR 0013 had already proposed the answer — *"tiles for unbuilt features render in a subtle 'coming'
state rather than being hidden, so the hub doubles as feature discovery"* — and it was built. It was
then dismantled one tile at a time as each feature shipped, the last commit titled *"light up the
last tile"*. What remained by 2026-09 was a dead `enabled` parameter on four tile composables, and
two documents describing behaviour that no longer existed: ADR 0013's category tabs (removed in
`7174f57`) and its coming state (finally removed in `9a8b0e5`).

The structure could not absorb the change either. `ShortcutGrid` was a `List<@Composable () -> Unit>`
of twelve closures whose **thirteen navigation lambdas were threaded through four layers and a
preview**, so adding one tile meant editing five signatures plus `MainActivity`. Only detekt's
`ignoreAnnotated: ['Composable']` exemption on `LongParameterList` kept it compiling.

## Decision

**1. The tile tree is data.** `HubCatalog` in `:feature:home/hub` declares every tile at both levels.
`HomeScreen` takes **two** navigation callbacks instead of thirteen, and a new tile costs no
signature change anywhere.

**2. Two levels.** The landing carries a **Today** grid — the three destinations opened daily, kept
one tap away — and a **Shastras** grid of all thirteen user-facing domains. A built domain drills
into a screen listing what it holds; one that is not built reports where it stands.

The daily three are duplicated from their domains deliberately. A pure hierarchy would put the
calendar and the reminder list two taps away every time, which is a poor trade for the tidiness it
buys.

**3. Every domain gets a tile, built or not** — excluding only the roadmap's Declined entries, its
foundations (F1–F6, engineering concerns with nothing to open) and K8, which ADR 0016 re-termed as a
glossary layer rather than a section. `HubCatalogTest` pins the set against the roadmap's own ids, so
a domain added to one without the other fails the build.

**4. A not-yet tile is an outline, not a dimmed fill.** Outline-versus-fill is a difference in
*shape*, which survives greyscale, colour-blindness and high contrast. Colour could not have carried
it in any case: the brand glyphs hold their own maroon and gold and are drawn with
`Color.Unspecified`, so they can only be faded, never tinted. Three cues in total — outline
container, faded icon, and a Devanagari letter in place of an ornate glyph.

**No padlock.** Nothing in this app unlocks anything, and a lock reads as a paywall.

**5. The icon style is a status signal.** Ornate glyph means built; a Devanagari letter means not
yet. This extends a policy `VedicIcons` already stated — ornate glyphs are reserved for signature
features — and it is free: no artwork for domains that may never exist, and no `material-icons-extended`
dependency, which the project has deliberately avoided. A domain earns its glyph when it ships.

**6. The message says where a domain stands, not "coming soon."** The domains differ and a uniform
promise would flatten them. Kalpa and Kala matter most here: both have shipped something, but it
lives *inside* the calendar's day detail rather than as a destination, so their tiles have nothing to
open and their note points at where the built part actually is.

**7. `:feature:home` names an intent; `:app` owns the route.** `HubTarget` is an enum, mapped by an
exhaustive `when` in `MainActivity`, so a destination added without a route is a compile error rather
than a tile that silently goes nowhere. Route constants stay in `MainActivity.kt` because
`NavigationTitleTest` reflects that exact facade for them — moving them would disable the guard
silently, which is the worst failure mode for a test whose whole purpose is catching an omission.

Level two is one parameterised route, so every domain screen shares the "Shastras" subtitle and draws
its own name — the trade the four muhurat steps already make. The argument is declared once, in
`:app`, because the screen reads it in the `composable` lambda rather than from a `SavedStateHandle`;
that avoids repeating the `MUHURAT_ACTIVITY_ARG` duplication, where both modules declare `"activity"`
with nothing checking they agree.

## Consequences

**ADR 0013's stated risk is now accepted knowingly.** It warned that *"a hub that lists many
not-yet-built tiles can feel empty or 'coming soon'-heavy early on"*, and proposed revealing them only
as each phase approached. That mitigation is abandoned: the whole point is that the map is visible.
What is kept instead is honesty — the top grid is entirely built, the unbuilt tiles sit below it, and
each says something true rather than making a promise.

**Category tabs are declined permanently.** ADR 0013 proposed them, `7174f57` removed them, and the
two-level split is their replacement. Recorded so nobody re-adds them a third time.

**This reversed a line in the roadmap**, which said the shastras were "the contribution map, not the
navigation". That was amended in the same cycle rather than quietly contradicted, and the half of its
reasoning that still holds — a reader arrives with a question, not a shastra — is what justifies
keeping the daily destinations at level one.

**The `LongParameterList` exemption stops being load-bearing.** It was the only reason a
fifteen-parameter composable compiled.

**`TileButton.enabled` is deleted.** It was dead at every call site, changed only the label colour,
and left the tile fully clickable — a flag that said *disabled* and meant nothing. Tiles gained
`Role.Button` and a spoken state, which none of the twelve had.

**A tension with F1 (localization).** This adds roughly twenty-five hardcoded English strings to a
codebase that already has 599 and no `stringResource` calls. The mitigation is real rather than
hopeful: every label now lives as data in one object, so extraction becomes a change of type on
`HubTile` with no composable churn at all — these are now the easiest strings in the app to localize.

**A toast is a weak control, and this uses one.** It cannot be acted on and cannot link a reader to
the roadmap or to the issue that wants a contributor, which is much of why these tiles exist. It was
chosen deliberately for now; because the note is data on the model and the handler is hoisted,
replacing it with a sheet is a change at one call site.

## Amendment — 2026-09-02

Three of the thirteen tiles came off within a day of this decision, and the rule in point 3 is
narrower than it was written.

**Kala (C4) and Kalpa (K3) are untiled.** Both have shipped, but *into the calendar's day detail* —
the era years and the sankalpa frame are rows on a day, not places to go. Point 6 treated that as an
interesting case to be solved by a better message. It is better solved by having no tile at all: one
whose entire message is "look at the Calendar" costs a tap to learn nothing, which is precisely the
dead tap this design set out to avoid. `DomainStatus.PARTLY` goes with them, since it described only
those two.

**Chandas (C6) is held back** until its shape is clearer. Tiling a domain nobody has thought through
advertises a plan that does not exist.

So the rule is: **a domain gets a tile when it is, or would be, a place a reader goes.** All three
remain on the roadmap — not being a destination is not the same as not being wanted — and
`HubCatalogTest` records which are absent and why, since that test is where anyone adding a domain
will look.

## Amendment — 2026-09-03

**Point 5 is retired: the icon style no longer says what is built.**

It was written when no artwork existed for the unbuilt domains, and it made a virtue of that — ornate
glyph meant shipped, Devanagari letter meant not yet, at no cost. Artwork now exists for four of the
five, drawn in the same ornamental line style as `muhurat` and `calendar`, so the distinction has
nothing left to mark. A letter now means only that a domain is still waiting for art; The Arts is the
last one.

**Status is carried by the outline container and the icon's fade.** That is no loss for point 4's
requirement: outline-versus-fill is a difference in *shape*, and shape was always the primary cue —
the icon style was the third of three, and the one a reader was least likely to learn unaided.

`HubCatalogTest` keeps the half of the rule that still holds — nothing that ships falls back to a
placeholder letter — and adds a ratchet on the number of domains still awaiting artwork, so a new
domain cannot quietly arrive without any.
