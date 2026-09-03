# 19. Sanskrit vocabulary and transliteration

- **Status:** Accepted
- **Date:** 2026-09-03

## Context

The app displays several hundred Sanskrit-derived terms — tithi and nakshatra names, the sixty
samvatsaras, muhurta windows, the activities in the electional flow — and it has never said how they
should be spelled. Every one of them was decided by whoever typed it. That was survivable while the
app was small; it is not survivable through F1, where **599 string literals** move into
`strings.xml` and then into ten languages. A convention that has never been written down gets
decided implicitly, once per string, by ten different translators.

An audit of the shipped vocabulary found it is in better shape than that history suggests, but with
three genuine faults — and, more usefully, one undocumented pattern that turns out to be right.

**What looked like inconsistency but is not.** Stotra titles carry their neuter `-am`
(*Lingashtakam*, *Shantakaram*) because that is the name of the work. *Rahu Kalam* and *Gulika
Kalam* keep the southern form because that is what readers call them. *Swati* and *Ashwini* use `w`
while *Vishvavasu* and *Ishvara* use `v` — which looks like drift, but is the same word-by-word
convention that lets *Diwali* and *Shiva* sit in one paragraph of English without anyone noticing.
Popular romanization is conventional per word, not derived by rule, and treating it as a rule to be
applied uniformly would produce *Shiwa* and *Diva*.

**The pattern worth naming.** The app already speaks in two registers, and they divide by role
rather than by screen:

- **Sanskrit** for what the engine computes and the tradition names — *Chaitra*, *Shukla*,
  *Abhijit Muhurta*, *Vishvavasu*, *Ashwini*.
- **The popular, usually Hindi, form** for what a person chooses or asks for — *Vivah*,
  *Griha Pravesh*, *Mundan*, *Bhoomi Poojan*, *Rashifal*.

Nobody decided this, but it is correct. "Griha Praveshana" is not a thing anyone books. The registers
only cause trouble where they collide on the *same word*, which is where the three real faults were:

| Term | Collision | Where a reader sees both |
| --- | --- | --- |
| Panchang / Panchanga | "Today's Panchang" tile under the "Panchanga" domain | the hub, one glance apart |
| Muhurat / Muhurta | "Muhurat" tile under the "Muhurta" domain; app bar said *Muhurat*, content said *Brahma Muhurta* | the hub, and every muhurat screen |
| Sanskar / Samskara | "Bal Sanskar" category vs the "Dharma & Samskara" domain | two screens |

## Decision

**1. No diacritics in source text.** The app writes *Chaitra Shukla Pratipada*, not *Caitra Śukla
Pratipadā*. IAST is unambiguous and is what a Sanskritist expects, but it renders the most familiar
words as apparent misspellings to the readers this app is for — people who read English and know
these words from daily use, not from a grammar. That trade is wrong at the default.

**2. IAST is a display preference, not a source decision.** Transliteration-scheme support is
already on the roadmap under F1. Routing the scholarly form through a user setting turns an
irreversible editorial call into something a reader chooses, and is the reason point 1 costs nothing.

**3. Spelling is conventional per word, not derived.** There is no transliteration rule to apply.
The test is what a reader who knows the word expects to see, which is why `v` and `w` both appear and
should keep appearing.

**4. Two registers, assigned by role.** Sanskrit for what the engine computes and the tradition
names; the popular form for what a person chooses. This is the rule the app was already following;
writing it down is what stops it drifting.

**5. A word is spelled one way throughout.** Where the registers collide, rule 4 decides which form
wins, and it wins in every place the word appears. Applied:

- **Panchanga**, everywhere. The panchanga is what the engine computes; the prose, the domain tile
  and the settings label already said so, and only the two most-seen labels disagreed.
- **Muhurta**, everywhere in display text. The computed windows — Abhijit, Brahma, Dur — are the
  engine's own vocabulary and already dominated. The *routes* stay `muhurat/...`: they are internal
  identifiers, invisible to readers, and renaming them buys nothing while touching the
  `MUHURAT_ACTIVITY_ARG` duplication that `:app` and `:feature:muhurat` already share unchecked.
- **Samskara**, everywhere. The muhurta category becomes **"Child samskaras"** rather than
  "Bal Samskara": its siblings are already plain English (*Purchases*, *Medical*, *Ceremonies &
  Milestones*), so an English qualifier is in register there, and *Bal* + *Samskara* would splice
  the two registers inside a single label.

**6. Existing display names that double as persisted keys are frozen until F1 decouples them.**
Reminder identity is built as `"muhurta:$name"` from the *display* name, with
`"muhurta:Brahma Muhurta"` and `"muhurta:Rahu Kalam"` hardcoded in the alarm feature. Respelling any
of those silently orphans reminders a user has already set. None of the renames above touch one, and
none may, until the keys are separated from the labels.

## Consequences

The vocabulary is now settled ahead of extraction rather than during it, which is the whole point of
the timing: a translator working from `strings.xml` inherits a decision instead of making six hundred
of them.

**Point 6 is a latent bug, not just a constraint.** A display name used as a persisted key breaks the
moment that string is translated — the Hindi build would compute a different key for the same window
and lose the user's reminder. F1 cannot ship without decoupling them, and this ADR is the first place
that is written down. It is filed as
[#211](https://github.com/imskylab/vedic-mitra/issues/211) rather than fixed here.

Four of the app's most-seen labels changed spelling, which existing users will notice. That is the
cost of having left it undecided, and it is smallest today.

This ADR records **how** terms are written. It says nothing about which terms the app should use, or
about the app's voice, which `docs/knowledge-standards.md` governs.
