# 24. The samskaras are cited from the grhyasutras, and only where they were read

- **Status:** Accepted
- **Date:** 2026-09-11 (amended 2026-09-11: SBE 30 read, eight rites added)

## Context

K2 was the roadmap's **Next** domain, and the reason to build it was sharper than sequencing: Muhurta
already offers to find an auspicious day for six child samskaras — Janana Shanti, Namkaran,
Annaprashana, Karnavedha, Mundan, Upanayana — and the app could not explain a single one of them. It
sold a date for a rite it never described.

K2 is a **Cite** domain, and this project has a specific, recorded scar about Cite domains. The
stotra and mantra catalogs ship **38 entries declaring `NotRecorded`**, because:

> "They were deliberately not filled in from memory — a wrong attribution is itself a claim, and this
> needs texts to hand rather than code."

The question for K2 was therefore never *what do the samskaras mean* — it was *what can actually be
cited*.

### The source that did not work

`vedicheritage.gov.in` was proposed: a Government of India portal under the Ministry of Culture,
which sounds like the ideal source. Its copyright policy says otherwise:

> "The contents of this website can not be reproduced partially or fully, without written permission
> from Indira Gandhi National Center for the Arts or the contributor."

All-rights-reserved, against a red line reading **"Public domain only… no bundled third-party
translation."** It also does not carry the material: its Vedangas page names the Grhyasutras once, in
passing, and no samskara content sits under it. Government does not imply reusable.

## Decision

**1. Cite the sutra; derive from Oldenberg; write original prose.**

The grhyasutras are ancient and free. Oldenberg's translation in *Sacred Books of the East* vols
29 and 30 is public domain — the archive.org scans record "no visible notice of copyright" for the
first and `NOT_IN_COPYRIGHT` for the second. Between them the two volumes carry **seven** grhyasutras:
Sankhayana, Ashvalayana, Paraskara and Khadira in vol 29; Gobhila, Hiranyakesin and Apastamba in
vol 30. Each entry names the sutra and the passage:

```kotlin
source = ContentSource.Text(work = "Paraskara Grhyasutra", locus = "1.19.1")
```

The portal remains useful as a finding aid. It is never quoted and never cited.

**2. Every locus was read before it was written.** Not recalled, not inferred from a summary. The
volume was downloaded, indexed by running head so each line could be attributed to the right sutra,
and the passages were read in place. That is why this ADR can say Paraskara 1.19.1 opens *"In the
sixth month the Annaprasana"* rather than merely that it probably does.

**3. The bound for unsourced entries is zero, not a ratchet.** The stotra catalog's
`EXPECTED_UNSOURCED = 26` exists because that content predates the rule. This content does not, so
`ContentSource` has no default on `SamskaraEntry` and a test asserts the count is zero. A ratchet is
remediation; it is not a template.

**4. Thirteen rites ship, not sixteen — and the three absences are load-bearing.**

The set is bounded by what could be read. **Karnavedha, Vidyarambha and Vedarambha appear in none of
the seven grhyasutras Oldenberg translated.** Three independent checks say so, and the third is the
one that does not depend on how well a scan was digitised:

1. No `karna` token occurs in either volume in any of the spellings the OCR uses for it — and it is
   demonstrably able to render the word, since *karwe* and *kar«syopari* both appear in vol 29.
2. **`vedha` occurs zero times across both volumes**, 1.55M characters.
3. **Oldenberg's own synoptical survey of the contents of the grhyasutras** (SBE 30, pp. 299 seq.)
   indexes every rite across all seven texts side by side. Its childhood sequence is unbroken:
   17 Annaprasana → 18 Chudakarman → 19 Godana-Karman → 20 Upanayana, with loci for all seven at
   each step and no ear rite anywhere among its 43 items.

The only ear passages in either volume are the Samavartana ear-rings, which are *put on* (Ashvalayana
3.8.10, Hiranyakesin 1.3.10–11), and the Jatakarman formula murmured into the ear. Neither is a
piercing.

Janana Shanti is likewise absent, for a different reason: it is a shanti rite for birth under an
inauspicious asterism rather than a grhya samskara, and no such rite appears.

Writing an entry for any of them would have been easy, would have looked exactly like the thirteen
real ones, and would have been the precise failure `ContentSource` exists to prevent.

**5. Where the sutras disagree, the entry says so.** Cite rule 3 requires it. **Simantonnayana is
now the sharpest case** — four sutras, four different times, and two of them hold the rite to a first
pregnancy:

| | |
|---|---|
| Ashvalayana 1.14.1 | the fourth month |
| Sankhayana 1.22.1 | the seventh month, at a first pregnancy |
| Paraskara 1.15.3 | the sixth or eighth, at a first pregnancy |
| Khadira 2.2.24 | the fourth or sixth |

Chudakarana is the same shape with three. Each entry reports every reading and names the one it
follows.

A second kind of disagreement is reported too: **the sutras differ on where a rite begins and ends.**
Ashvalayana folds the naming into Jatakarman at 1.15.4–8; Paraskara gives it a tenth day of its own at
1.17.1. Both entries ship, and each says what the other does.

**6. Both registers, split by role — amending ADR 0019.**

ADR 0019 says a word is spelled one way throughout, but its own rule 4 assigns register by *role*:
Sanskrit for what the tradition names, the popular form for what a person chooses. A reference entry
and a muhurta preset are different roles.

| | |
|---|---|
| Reference | **Chudakarana**, with *Mundan* alongside |
| Muhurta picker | **Mundan**, unchanged |

Someone choosing a date for a child's first haircut looks for *Mundan*; someone reading what the
grhyasutras prescribe meets *Chudakarana*. `Samskara.alias` carries both so the two are visibly one
rite.

Worth recording, since it was nearly the other way: ADR 0019 rule 6 freezes display names that double
as persisted keys, and that was read as blocking a rename. **It does not apply here.** Rule 6 names
`MuhurtaKind`, whose keys are `"muhurta:${kind.id}"` and were fixed in #211. Nothing keys on
`MuhurtaActivity.displayName`. The register split was chosen on its merits, not because a rename was
impossible.

**7. Nothing is equated that a source does not equate.** `MuhurtaActivity.DOHALE_JEVAN` sits close
to Simantonnayana and is not linked to it. Dohale Jevan is a regional ceremony for a pregnant woman;
the hair-parting rite of the grhyasutras is not obviously the same thing, and no text read here says
it is. An unsourced equation between a popular name and a sutra name would be exactly the quiet claim
this ADR exists to stop — it would also have been invisible, since the link would simply work.

**8. Two passages are reported flatly rather than softened or dropped.**

Garbhadhana and Pumsavana are the hardest content in the domain. Paraskara 1.11 frames the fourth
night with expiation oblations directed at the bride, asking that what is held to dwell in her be
taken out; Ashvalayana 1.13 states the object of Pumsavana as the generation of a male child.

Both ship, attributed, in the same reporting voice as everything else. The alternative was to drop
them silently, which would have been the app editing the tradition and calling the result a
reference — a worse failure than an uncomfortable sentence, and an invisible one. What the entries do
not do is transcribe: the curse against a wife's lover at 1.11.4–6 is not reproduced, and neither is
the procedure of either rite. **Reporting that a text says something is not repeating it, and neither
is instruction.** The rule from ADR 0016 holds unchanged — sentences describe, they do not direct.

**9. Teach is added to K2's declared modes.** The roadmap declared Cite + Compute + Track. This slice
is mostly Teach, and the standards require a primer with a test over `entries` for every new domain.
The declaration now says so.

## Consequences

- The hub tile stops toasting and opens. `DHARMA` moves to `BUILT` with `opens = HubTarget.SAMSKARA`,
  the ADR 0022 shape: a domain holding one screen *is* that screen. It becomes a drill when the
  dharma half arrives.
- **The domain is named Dharma & Samskara and delivers only samskaras.** The blurb narrows to say so.
  Observances by stage of life remain unbuilt, and that is where the roadmap's K2-only prohibition —
  *"no claim about who owes what to whom"* — will bite hardest.
- The screen says on its face that thirteen of sixteen are present, **and now names the missing
  three**. A count alone invites the reader to assume the rest are merely pending; naming them says
  which, and the test says why.
- **Four entries carry no muhurta link, and that is the same gap running the other way.** Muhurta
  offers a date for Karnavedha that this catalog cannot explain; this catalog describes Garbhadhana,
  Keshanta, Samavartana and Antyeshti, for which Muhurta offers no date. Antyeshti will never get one.
  Neither half is closed by inventing the other.
- **Each further rite is a PR with a passage behind it**, the shape #247 uses. Sixteen entries drafted
  in one sitting is exactly what this ADR exists to prevent.

## What is deliberately not done

- **No procedure.** What is performed, by whom, with which mantras, is K3's ground and is not
  described here. Reporting that a rite exists is not the same as setting it out to be followed.
- **No prescription.** Nothing addresses the reader; a test over the whole string file enforces it.
- **No claim about entitlement.** Ashvalayana 1.19 sets different ages by varna and an outer limit
  after which it holds the right to learn the Savitri to have lapsed. The entry reports what the
  sutra says, attributed to it, and draws nothing from it about anyone alive.
- **No dates computed.** A samskara has no date until someone chooses one, which is Muhurta's job —
  and the link into it is the whole bridge.
