# 24. The samskaras are cited from the grhyasutras, and only where they were read

- **Status:** Accepted
- **Date:** 2026-09-11

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
29–30 (1886) is public domain — the archive.org scan records "no visible notice of copyright". Each
entry names the sutra and the passage:

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

**4. Five rites ship, not sixteen — and Karnavedha is the instructive absence.**

The set is bounded by what could be read. **Karnavedha does not appear in any of the four grhyasutras
in SBE 29.** Ear-piercing enters the sixteen later, through other texts. The app offers a muhurta for
it and still cannot explain it, and that gap is recorded in a test rather than closed with a
plausible-looking citation. The same is true of Janana Shanti, which is a shanti rite rather than a
grhya samskara.

Writing an entry for either would have been easy, would have looked exactly like the five real ones,
and would have been the precise failure `ContentSource` exists to prevent.

**5. Where the sutras disagree, the entry says so.** Cite rule 3 requires it. Chudakarana is the
clearest case: Ashvalayana 1.17.1 gives the third year "or according to the custom of the family",
Sankhayana 1.28.2–3 gives the third or the fifth for a kshatriya, Paraskara 2.1.1–2 gives one year or
before the third has passed. The entry reports all three and names the one it follows.

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

**7. Teach is added to K2's declared modes.** The roadmap declared Cite + Compute + Track. This slice
is mostly Teach, and the standards require a primer with a test over `entries` for every new domain.
The declaration now says so.

## Consequences

- The hub tile stops toasting and opens. `DHARMA` moves to `BUILT` with `opens = HubTarget.SAMSKARA`,
  the ADR 0022 shape: a domain holding one screen *is* that screen. It becomes a drill when the
  dharma half arrives.
- **The domain is named Dharma & Samskara and delivers only samskaras.** The blurb narrows to say so.
  Observances by stage of life remain unbuilt, and that is where the roadmap's K2-only prohibition —
  *"no claim about who owes what to whom"* — will bite hardest.
- The screen says on its face that five of sixteen are present. A reference that looks complete and is
  not would mislead more than a short one does.
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
