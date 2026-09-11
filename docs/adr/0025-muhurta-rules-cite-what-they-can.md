# 25. Muhurta rules cite what they can, and say so when they cannot

- **Status:** Accepted
- **Date:** 2026-09-11 (amended 2026-09-11: chapter 100 read, tithi set widened)

## Context

[#247](https://github.com/imskylab/vedic-mitra/issues/247) records a real defect: the muhurta picker
offers **31 activities** and the engine had electional rules for **5**. Everything else fell through
to one generally-auspicious set, so Mundan and Shop Opening returned the same ranked days, differing
only by the reader's own Tarabala.

> An interface that offers a distinction the engine does not make is making a claim it cannot keep.

The issue asked for the obvious remedy: for each remaining activity, its favourable nakshatras and
varas, **plus a citation** — "a source plus a `when` branch, and a PR with only the branch is
incomplete in the way a PR without tests is."

## The part of the issue that cannot be done as written

**No public-domain text has been found that gives per-activity nakshatra lists of the shape the
engine holds.** This was looked for rather than assumed:

- The **grhyasutras** (SBE 29–30, already in hand from [ADR 0024](0024-samskaras-cited-from-the-grhyasutras.md))
  give *ayana* and *paksha* and occasionally a single nakshatra — Ashvalayana 1.4.1 sets the tonsure,
  the initiation, the beard-cutting and marriage in the northern course of the sun and the bright
  fortnight, "under an auspicious Nakshatra", without saying which. They do not enumerate.
- The **Brihat Samhita** does not carry such a table for activities in general. Varahamihira says at
  1.10 that nativity, *yatra* and marriage are treated "in my work on Horoscopy". **For marriage he
  gives one anyway, at 100.1** — see the amendment below, which was found only after this ADR was
  first written.
- The lists of that shape belong to the later muhurta literature — Muhurta Chintamani and its
  kin — whose English translations are modern and in copyright. A red line reads **"Public domain
  only… no bundled third-party translation."**

Filling the remaining 26 in anyway would produce something indistinguishable from real work and
worth nothing, which is the failure `ContentSource` exists to prevent.

## Decision

**1. A rule set must declare where it came from.** `ActivityMuhurtaRules.source` has **no default**,
the same device as `SamskaraEntry`. The five pre-existing rule sets declare `NotRecorded`, honestly,
and a ratchet test holds that count so it can shrink but never grow.

**2. Cite what can be cited: the karana verses.** The Brihat Samhita's chapter on lunar days and half
lunar days (99.6–99.8) names particular kinds of work for particular karanas. That is the uncommon
case of a classical text saying something *activity-specific* that this engine already computes — the
karana has been passed into the scorer since the beginning and was used only to penalise Vishti.

| Activity | Karana | Verse |
|---|---|---|
| Sowing, Gardening | Gara | 99.7 — "lands shall be tilled, seeds sown" |
| Bhoomi Poojan | Taitila, Gara | 99.6, 99.7 — "houses shall be built" |
| Shop Opening, Business Inauguration, Product Sale | Vanija | 99.7 — "dealings with merchants" |
| Aushadhi Sevan | Shakuni | 99.8 — "shall take medicine" |
| Livestock Purchase | Chatushpada | 99.8 — "deeds connected with cows" |

Read in N. Chidambaram Iyer's 1884 translation, which is public domain and prints the chapter as
Part II chapter 52. Every locus was read before it was written, per ADR 0024's rule.

**Only where the verse names the act.** Bava and Kimstughna speak of health and comfort in general;
neither is taken for Aushadhi Sevan, because "health and comfort" is not "shall take medicine".
Harvesting, farmland purchase and well construction get nothing, because nothing names them. A test
asserts that restraint rather than leaving it to a comment.

**3. The existing Vishti penalty gains a source.** 99.7 — "In a Vishti or Bhadra Karana, auspicious
deeds shall not be done." It was already implemented; it was not previously attributable.

**4. The screen says which rules it used.** A line under the ranking states whether the days were
ranked by rules specific to the activity or by the general ones, and names the text when there is
one. This is what actually closes the defect: after this change the engine distinguishes 12 of 31
activities, not 31, and a reader can now tell which they are looking at.

**5. Karanas are matched by identity, not by name.** The scorer asked `karana.name == "Vishti"` —
the fifth instance in this repo of *"a claim is not its own key"*. A karana's identity is its
**position** in the lunar month, which is derived from the Moon's elongation and cannot be renamed.
`KaranaKind` carries a frozen `id`, `karanaKindAt(number)` is the only mapping, and the printed name
is now derived from the kind rather than sitting in a second table.

This immediately found a latent defect: **four test fixtures described `Karana(number = 7, name =
"Vishti")`, a day that cannot occur** — position 7 is Vanija, and Vishti falls at 8, 15, 22 … 57.
They passed only because the scorer trusted the name it was handed. Corrected, and pinned by a test
over all sixty positions.

## Amendment: the tithi set is widened, and chapter 100 sources more than expected

The first version of this ADR recorded `AUSPICIOUS_TITHIS` as narrower than Brihat Samhita 99.2 and
left it alone, because widening it moves every ranking in the app and that deserved its own decision.
**That decision was taken: widen it.** Going back to the text for the justification turned up three
further things.

**A correction to what this ADR first said.** It claimed 99.2 "sets aside only the Rikta ones". It
does not. 99.2 *classifies* — Nanda (1, 6, 11), Bhadra (2, 7, 12), Vijaya (3, 8, 13), Rikta
(4, 9, 14), Poorna (5, 10, 15) — and ranks nothing. The sentence that actually sets a class aside is
in the next chapter: a marriage is fixed **"when the lunar day is other than a Rikta one"** (100.2).
Both citations were needed, and the code now carries both.

**So the set becomes every tithi except the Rikta ones, less Amavasya** — which 99.2 opens by giving
to the Pitris, a different kind of day rather than an auspicious one to begin on. Twenty-three of the
thirty, up from fifteen.

The consequence is worth stating plainly: **the tithi is no longer a differentiator between good
days, only a penalty on bad ones.** Eight tithis that scored nothing now score the same +10 as every
other non-Rikta day, so ranking is carried by the nakshatra, vara, yoga, karana and the reader's own
balas. That is what the verses support, and no more. A rule set that finds a text narrowing the
field for its own activity can still override `favorableTithis`.

**Vivah's nakshatras had a source all along.** Brihat Samhita 100.1: *"Marriages shall take place when
the Moon passes through the asterism of Rohini, U. Phalguni, U. Ashadha, U. Bhadrapada, Revati,
Mrigasirsha, Mula, Anuradha, Magha, Hasta or Swati."* Those eleven are **exactly** the eleven already
in the rule set. The set was not adjusted to fit the verse; the verse was found to match the set, and
a test now pins it by name rather than by number. The unsourced ratchet drops from four to three.

**Three more rules the app already applied can now say where they come from**, all from 100.2, which
lists the conditions for a marriage:

| Rule in the engine | Verse |
|---|---|
| `RIKTA_TITHIS` avoided | "when the lunar day is other than a Rikta one" |
| `INAUSPICIOUS_YOGAS` = Vyatipata, Vaidhriti | "when the Yoga is neither the Vyatipata nor that of Vaidhriti" |
| `BENEFIC_VARAS` excludes Tue, Sat, Sun | "when the week day is other than that of a malefic planet" |
| Vishti penalised | "when the Karana is other than Vishti" |

**One honesty note on those.** 100.2 states them *of marriage*. The engine applies all four to every
activity. That generalisation is the conventional reading rather than something the verse says, and
the code comments say so at each one rather than letting a citation imply more than it carries.

## Consequences

- 12 of 31 activities now have rules of their own, up from 5. The remaining 19 are ranked by the
  general set **and the screen says so**, which is the honest half of the fix and the half that did
  not need a source.
- `unfavorableNakshatras` is still never populated, so the `-30` branch in the scorer remains
  unreachable. It needs a text that names prohibitions, and none was found here.
- **Karana is a half-tithi**, so a rule keyed on it is coarser than it looks: the scorer takes the
  karana at sunrise and credits the whole day. That is exactly how the existing Vishti penalty has
  always worked, so the two are at least consistent — but a future change that resolves windows
  within a day should revisit both together.
- The four remaining unsourced rule sets stay until a text is found for them. The ratchet means the
  next rule added cannot join them.

## What is deliberately not done

- **No invented nakshatra lists.** The 19 activities without their own rules keep none rather than
  gaining plausible ones.
- **No third-party service consulted.** The rules come from a named text, chapter and verse, read in
  a public-domain translation.
- **No change to any existing score input.** The nakshatra, vara, tithi, yoga and Vishti weights are
  untouched; the only new term is a credit for a karana a verse names.
