<!--
  Copyright (c) 2026 Jayvardhan Potabatti
  SPDX-License-Identifier: AGPL-3.0-or-later
-->

# Knowledge standards

What Vedic Mitra is allowed to say, and what has to stand behind each kind of statement.

Read this before adding a feature in any knowledge domain. It is the companion to
[the validation pass](validation/panchanga-validation.md): that document says how a *calculation* is
checked, this one says what to do when there is nothing to check against.

## Why this document exists

Everything the app has shipped so far is **computed** — a tithi, a dasha boundary, a guna score.
That kind of claim can be verified, and the project verifies it hard: two independent references,
goldens inlined so nothing depends on a server, a failing reference test blamed on the engine rather
than on the reference. Rules are derived from reference data rather than from memory, and when that
was done for the four porutham, **two of them came out differently from the textbook** they would
otherwise have been written from.

The roadmap now reaches into shastras where none of that is available. Nothing can tell you whether
a stotra's text follows the right recension, whether a Vastu rule is the one its tradition actually
holds, or whether a seasonal routine is stated the way its source states it. There is no oracle.

**The app already has this problem once, and it is the only narrowing in the codebase that is not
written down.** `StotraCatalog` carries 26 hymns and `MantraCatalog` twelve mantras. Both state that
the Sanskrit is traditional and public-domain — which is a *licensing* claim, not a provenance one.
Neither names an edition, a recension, or a source. `StotraCatalogTest` asserts that fields are
non-empty and that a verse and its transliteration have the same number of lines: that catches a
dropped line and nothing else. There is no test on `MantraCatalog` at all.

Nearly every other narrowing in this repo *is* recorded, in the words a comparing reader would use.
`Dignity` has a heading called "What is deliberately not here". `Drishti` refuses to pick a side of
a genuine dispute rather than present one as settled. `ChartYoga` deleted two rule families after
measuring them at 45% and 72% agreement, because "a yoga is a claim a person repeats about
themselves". `MangalDosha` declines the age-lapse claim because "stating it would be inventing
reassurance".

The devotional content never got that treatment because nothing forced it to. Fourteen more
knowledge domains would multiply the gap, so the standard lands before the breadth does.

## The four modes

Every feature, screen, and roadmap entry declares which of these it is.

| Mode | The app... | What must stand behind it |
| --- | --- | --- |
| **Compute** | asserts a value it derived | a stated convention, and either an independent reference or a cited rule — see below |
| **Cite** | reports what a tradition holds | a named source, and a voice that attributes rather than asserts |
| **Track** | records what *you* did | nothing — it makes no claim about the world |
| **Teach** | explains an idea | the primer pattern: closed enum, total map, test over `entries` |

The modes are about **voice and evidence, not subject matter.** A single screen may mix them —
Kundali computes a placement, cites what the tradition reads into it, and explains what a house is —
but every individual sentence belongs to exactly one mode and carries that mode's backing. Mixing
them *silently* is the failure this document exists to prevent: it is how an unverifiable claim
inherits the credibility of a verified one.

## Compute — the app asserts a value it derived

Two tiers, and the difference is whether anything independent is able to disagree with us.

**Oracle-validated.** An independent implementation computes the same thing, so a golden test can
hold the line: panchanga limbs, planetary positions, vargas, dashas, ashtakavarga, guna milan. The
full regime is in [the validation pass](validation/panchanga-validation.md). This is the app's
strongest claim, and the reason anyone should trust the rest.

**Rule-transcribed.** The arithmetic is verifiable but the *rule* is not — nothing independent
exists to check it against. A Vastu orientation calculation, a chandas scan, a proportional canon:
you can prove the code implements the rule, and you cannot prove the rule is the one the tradition
holds. These need a citation for the rule itself, exactly as a **Cite** claim would.

A rule-transcribed result is only as good as its rule, and its rule is a citation rather than a
measurement. **The two tiers must not be presented at the same confidence.** Where a screen shows
both, the rule-transcribed value says which text it follows.

Either tier: state the convention, show the working where the working is the interesting part — the
existing doshas do this, reporting which grahas fell outside the arc whether or not the dosha
stands — and write down every narrowing in KDoc beside the code.

## Cite — the app reports what a tradition holds

1. **A named source, as data rather than as a comment.** A `source` field on the model, carrying the
   text and — where recensions differ — the recension. A source that lives in a code comment is not
   available to the reader who needs it.
2. **The app's voice never asserts it.** The existing rule stands: traditional claims are attributed
   ("traditionally", "is said to"), never stated as fact. `PanchangaPrimerTest` already enforces the
   related half of this by failing any copy that addresses the reader as "you".
3. **Disagreement is reported, not resolved.** Where authorities differ, say so, and say which one
   this screen follows. `Drishti` is the precedent: it reports `false` for node aspects because
   "there is no answer here that could be called the classical one".
4. **Public domain only.** Existing policy — verse text and original notes, no bundled third-party
   translation.

**Remediation, partly done:** the stotra and mantra catalogs predate this rule. `ContentSource` now
exists in `:core:common` and `source` is required on both models — a new entry **cannot compile**
without deciding — and every existing entry declares `ContentSource.NotRecorded`, which the reader
is shown rather than being quietly hidden. A test pins the count of unsourced entries, so the debt
can shrink and never grow.

What remains is identifying the sources themselves. That needs texts to hand rather than code, and
it was deliberately **not** done from memory: this project's own habit is to derive from a reference
rather than recall, precisely because recall was wrong on two of the four porutham.

## Track — the app records what you did

Japa counts, meditation sits, vratas kept, a sadhana log. These make **no claim about the world at
all**, which is what makes them the safest and often the most valuable thing the app can offer.

They stay on the device. They carry no judgement — a broken streak is reported, never scolded — and
they are never used to infer anything about the person.

## Teach — the app explains an idea

`PanchangaPrimer` is the machine to copy, and its value is mechanical rather than editorial: a
closed enum, a total map read with `getValue`, and a test that iterates `entries`. **Adding a
concept without writing its copy breaks the build.** That is what keeps explanatory text a
first-class part of a feature instead of the thing that gets cut when time runs short.

`PanchangaPrimerTest` also asserts the writing itself — one-liners short enough to sit beside a
value untapped, bodies that are a paragraph and end in a full stop, and no second person anywhere.
Every new domain gets its own primer with the same test shape. **A domain the app cannot explain in
plain language is a domain it is not ready to ship.**

## Red lines

These hold regardless of which mode a feature is in.

- **No medical claims.** Ayurveda in this app means dinacharya and ritucharya — the shape of a day
  and of a season — presented as tradition. Nothing diagnostic, nothing therapeutic, no constitution
  assessment offered as health guidance, no remedy for a condition. If a sentence could change what
  someone does about an illness, it does not ship.
- **No fatalism.** No claim about anyone's death, disease, or fixed fate, from a chart or otherwise.
  The astrology features already hold this line — Rashifal is computed from Chandrabala and Tarabala
  with "no invented predictions, just the traditional meaning of the transit read back in words".
- **No instruction in anyone's practice.** The app reports what a tradition holds; it does not tell
  a reader what they should do. This is already enforced in CI for primer copy.
- **No remedy commerce.** No gemstones, no yantras, no paid remedies, no referrals to practitioners.
  The funding model is donations and commercial licensing (ADR 0014); nothing else.
- **No third-party translation text**, and **no naming of other panchanga or astrology services** —
  both existing project policy.

## Declaring a mode

State it in two places:

- The module or file's top KDoc, in a sentence — what this computes, cites, tracks or teaches.
- The [roadmap](roadmap.md) entry for the domain.

A pull request that adds a knowledge claim without a declared mode is incomplete in the same way a
PR without tests is.
