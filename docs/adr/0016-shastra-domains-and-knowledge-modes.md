# 16. Shastra domains and knowledge modes, replacing the phase roadmap

- **Status:** Accepted
- **Date:** 2026-09-01

## Context

The project's stated ambition has widened: from a panchanga and muhurta app to a single place for
the practices and knowledge of the Indian tradition, spanning roughly eighteen shastras. The
repository is public, AGPL, and expects outside contributors.

Two problems stood in the way.

**The phase roadmap does not survive that widening.** Twelve phases described one person's build
order. They imply a sequence and a single worker, which is exactly wrong for a contributor arriving
who wants to work on one domain. The phases had also begun to contradict each other — "vrat
reminders" was checked in Phase 4 and unchecked in Phase 8, "festival descriptions" checked in
Phase 4 while "festival significance" was unchecked in Phase 11 — because the same work appeared
under two headings with no single owner.

**More seriously, the eighteen shastras are not one kind of thing.** Everything shipped so far is
*computed and verifiable*, and the project's credibility rests on how hard it verifies: two
independent references, goldens inlined, roughly fifty documented narrowings, rules derived from
reference data rather than memory. That last habit is not decorative — when the four porutham were
derived that way, two came out differently from the textbook they would otherwise have been written
from.

Most of the new domains cannot work like that. Vastu principles, Ayurvedic routine, rasa theory and
ritual procedure are texts and traditions. No golden test can say a stotra's recension is right.
Shipped in the same voice as a tithi, their unverifiability leaks backwards: a reader who finds one
dubious health claim has no reason to keep trusting the dasha boundaries.

The app already contains one instance of this and had never named it. `StotraCatalog` (26 hymns) and
`MantraCatalog` (12 mantras) assert only that their Sanskrit is public-domain — a licensing claim,
not a provenance one. No edition, no recension, no source; `StotraCatalogTest` checks field
non-emptiness and line counts, and `MantraCatalog` has no test at all. It is the one narrowing in
the codebase not written down anywhere, and fourteen more content domains would multiply it.

## Decision

**1. Retire the twelve phases in favour of a domain map**, in `docs/roadmap.md`. Every item from the
phase plan is re-filed rather than discarded — the checkbox status was honest and verified against
the code, and that information is kept. Status becomes Shipped / Building / Next / Open / Exploring
/ Declined, where **Open explicitly means wanted but unscheduled**, so the roadmap cannot be read as
a commitment the maintainer has not made.

**2. Every feature declares one of four knowledge modes**, defined in `docs/knowledge-standards.md`:

- **Compute** — the app asserts a value it derived
- **Cite** — it reports what a tradition holds, with a named source
- **Track** — it records what the user did, claiming nothing about the world
- **Teach** — it explains an idea

The modes govern voice and evidence rather than subject matter; one screen may mix them, but every
sentence belongs to exactly one and carries that mode's backing.

**3. Compute splits into two tiers.** *Oracle-validated* means an independent implementation can
disagree with us, so a golden test holds the line — the panchanga and jyotisha work. *Rule-
transcribed* means the arithmetic is provable but the rule can only be cited, because nothing
independent exists to check it against — Vastu and chandas. The two must not be presented at the
same confidence.

**4. Shastras are the contribution and provenance map; navigation stays task-shaped.** Users ask
*what is today*, *when should I do this*, *what does this mean*. A shastra-shaped navigation would
be a taxonomy, and one question routinely draws on several shastras.

**5. Some domains are declined, with reasons recorded** — Arthashastra and Dhanurveda have no app
affordance beyond prose; standalone Sanskrit-grammar tutoring is a different app. Nirukta and
Vyakarana are re-termed from sections into a glossary layer reachable from any Sanskrit term on any
screen, which serves every domain instead of one.

**6. Red lines are stated once and apply across all modes** — no medical claims (Ayurveda is bounded
to dinacharya and ritucharya), no fatalism, no instruction in anyone's practice, no remedy commerce.

## Consequences

**Content now has a bar it can fail.** Astronomy is validated; knowledge is cited. A `source` field
is data on the model rather than a comment, and a missing one fails a test — the same mechanism that
already makes primer copy break the build when it is absent.

**Three foundations move ahead of the breadth.** Localization (599 hardcoded strings, zero
`stringResource` calls, no locale at all) gates the knowledge domains, because every English screen
added first multiplies the translation debt. Content provenance gates further devotional content.
Regional variation is a correctness debt already owed — the amanta/purnimanta split alone moves
festival dates by a fortnight for many users.

**Existing content is out of compliance and stays shipped.** The stotra and mantra catalogs do not
meet the rule they are now subject to. The roadmap lists the remediation as foundation work, and the
gap is recorded rather than quietly tolerated.

**The roadmap leaves the README**, which keeps a short status summary and a link. The README had
grown to roughly 370 lines of roadmap.

**This ADR does not commit to building the declined domains later.** Reversing a decline needs a new
ADR, per ADR 0001.
