# 20. A planned tile says so in words

- **Status:** Accepted (supersedes points 4 and 5 of ADR 0018, and both of its amendments)
- **Date:** 2026-09-04

## Context

The hub shows every non-declined domain on the roadmap, built or not — that is ADR 0018's point 3
and it stands. What has never settled is **how a tile shows that its domain has no screen yet**.
This is the third attempt in three days, which is itself the finding: each attempt failed for a
different reason, and none of the failures was predictable from the previous one.

**Attempt one — a Devanagari letter instead of an ornate glyph.** It worked only because no artwork
existed for the unbuilt domains. When artwork arrived the distinction marked nothing, and a letter
came to mean *waiting for art* instead. Retired in ADR 0018's first amendment.

**Attempt two — an outline container and a faded icon.** The reasoning was sound on paper:
outline-versus-fill is a difference in *shape*, so it survives greyscale, colour-blindness and high
contrast, which colour cannot. What the reasoning missed is that **a faint outline sitting beside
solid chips does not read as "planned", it reads as "broken"** — as a tile that failed to load. A
cue that is legible but says the wrong thing is not a working cue.

**Attempt three — a filled chip in a single "planned" colour.** This fixed the broken-looking tile
and was right to. But it put the status back into colour, and it collided: the chosen tint was
`tertiaryContainer`, which *is* `HubCategory.DEVOTION`'s container. Mantra & Stotra — a built
devotion domain — rendered pixel-for-pixel identically to Dharma, Ayurveda, Yoga and The Arts. Six
identical rose chips in one grid, one of which opened. It also made colour mean *category* on built
tiles and *status* on planned ones, so Vastu, an astrology domain, was drawn in the devotion rose.

Three attempts, three different failure modes, one shared cause: **every non-verbal cue available to
a 52dp chip fails for somebody.** Colour fails in greyscale, for colour-blindness, and whenever it
collides with a colour already carrying meaning. Opacity reads as a fault. Shape reads as a fault.
The icon can carry nothing at all — the brand glyphs hold their own maroon and are drawn with
`Color.Unspecified`, so they can be faded but never tinted.

## Decision

**A planned tile says "Soon", in words, under its label.**

Text is the only cue that survives every condition at once: greyscale, colour-blindness, high
contrast, and a large font scale — where it grows with everything else rather than staying a fixed
number of pixels. It is also the only cue a reader does not have to *learn*. An outline or a tint
has to be decoded; a word does not.

**Colour goes back to meaning one thing.** Every chip takes its category's container tint, built or
not. The chip says what kind of thing the domain is; it never says how far along it is. That removes
the collision rather than working around it, and Vastu is gold again because it is an astrology
domain, which is the only thing its colour was ever supposed to say.

**The icon is never faded.** `TileGlyph` no longer takes an alpha, because there is no longer a
caller that wants one.

**The caption is hidden from accessibility services**, with `clearAndSetSemantics {}`. The tile
already carries `stateDescription = "Not built yet"`, which is better wording for a screen reader
than "Soon". Without clearing it, the same fact would be announced twice.

**Sentence case, not a badge.** "Soon", not "SOON" or a pill. The app's voice is factual throughout,
and this is a statement about the app rather than a promise about a date — which is also why the word
is *Soon* and not a quarter or a version number.

## Consequences

A planned tile is now one line taller than a built one. Rows size to their tallest tile, so a row
containing any planned domain grows; at `fontScale = 2f` the Shastras section gets materially longer.
That is the cost, it is on the device pass, and it is the right trade: the alternative treatments
were all free and all wrong.

Status is now carried by exactly **one** cue rather than three overlapping weak ones. That sounds
worse and is better — the three cues in ADR 0018 were three chances to be misread, and two of them
actively said the wrong thing.

The label keeps its `onSurfaceVariant` colour on planned tiles. That is reinforcement, not the
signal; nothing depends on a reader seeing it.

**The pattern in this ADR's context section is worth keeping.** Three attempts in three days, each
one sound in the abstract and wrong on a screen, is a strong argument for rendering a design at true
size before writing it down as a decision. Each of these was reasoned about carefully; none of the
faults would have survived one honest look at the grid.
