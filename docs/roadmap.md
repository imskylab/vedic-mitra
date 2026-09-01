<!--
  Copyright (c) 2026 Jayvardhan Potabatti
  SPDX-License-Identifier: AGPL-3.0-or-later
-->

# Roadmap

Vedic Mitra's long-range aim is to be a single, offline, honest place for the practices and
knowledge of the Indian tradition — the panchanga first, and then the shastras that hang off it.

This document replaces the earlier twelve-phase plan. Phases described one person's build order;
this describes **domains**, because a domain is something a contributor can pick up without knowing
what came before it. Every item from the phase plan is still here, re-filed.

## How to read this

**Each domain declares its mode**, from [knowledge-standards.md](knowledge-standards.md):
**Compute** (the app asserts a value it derived), **Cite** (it reports what a tradition holds, with
a source), **Track** (it records what you did, claiming nothing), **Teach** (it explains an idea).
Read that document before contributing to any domain — the bar is different for each mode, and a
knowledge claim without a declared mode is an incomplete change.

**Status vocabulary:**

| | Meaning |
| --- | --- |
| **Shipped** | in the app today |
| **Building** | committed and in progress |
| **Next** | committed, sequenced, not started |
| **Open** | wanted and specified enough to contribute — nobody is on it |
| **Exploring** | wanted, but needs design before code |
| **Declined** | deliberately not doing, with the reason |

**A note on honesty.** This project is one maintainer, a few months old. **"Open" means wanted, not
scheduled** — several of these domains will not exist unless someone else builds them, and the
roadmap says so rather than implying a team. Status marks reflect what is actually in the code.

**Shastras are the contribution map, not the navigation.** Nobody opens an app thinking "I need
Gandharva Veda"; they think *what is today*, *when should I do this*, *what does this word mean*.
The shastra structure below says where knowledge comes from and who can contribute what. The app's
own navigation stays task-shaped — see [What the user sees](#what-the-user-sees).

---

## Part I — Foundations

These gate everything else. Breadth added before them multiplies work rather than reach.

### F1. Language and reach — **Open** · the largest single gap

The app is **English-only, and not even localized**: 599 hardcoded string literals across the
feature and design-system modules, **zero** `stringResource` calls, and one `strings.xml` containing
the app name.

This is both the biggest limit on reach and the best contributor on-ramp the project has. Most of
the people this app is for do not read English first, and translation is work that needs Indic
language and domain knowledge rather than Android knowledge — which is exactly the contributor this
project can attract.

It is also **strictly a prerequisite for the knowledge domains below**: every new screen of English
content added before extraction multiplies the eventual translation debt.

- [ ] Extract UI strings to `strings.xml` per module (mechanical, large, high value — good first issues)
- [ ] English as a real locale
- [ ] Hindi
- [ ] Sanskrit · Telugu · Tamil · Kannada · Malayalam · Marathi · Gujarati · Bengali · Odia
- [ ] Devanagari and Indic script rendering verified at each font scale
- [ ] Transliteration scheme support (IAST / ITRANS / regional) as a user preference

### F2. Regional variation — **Next** · a correctness debt, not a feature

The app computes one tradition's answer and presents it as *the* answer. For a large share of users
it is quietly wrong — the amanta/purnimanta split alone moves festival dates by a fortnight across
much of North India.

- [x] Amanta lunar month naming (ADR 0005)
- [ ] Purnimanta month naming as a user preference
- [ ] Tamil, Malayalam, Bengali and Odia solar calendars
- [ ] Regional festival sets and regional variants of shared festivals
- [ ] Regional Panchang support (per-tradition conventions surfaced, not hidden)
- [ ] Say which tradition a screen follows, wherever traditions differ

### F3. Content provenance — **Next** · owed remediation

Required by [knowledge-standards.md](knowledge-standards.md#cite--the-app-reports-what-a-tradition-holds).
The existing devotional content predates the rule and does not meet it.

- [ ] A `source` field on `Stotra` and `Mantra` — text, and recension where recensions differ
- [ ] Populate it for all 26 stotras and all 12 mantras
- [ ] A test asserting every entry carries a source (`MantraCatalog` currently has **no test at all**)
- [ ] A shared content model other domains can reuse rather than reinvent
- [ ] Record the gap in the validation doc's known limitations until it is closed

### F4. Accessibility — **Open**

Untouched. A project about making knowledge reachable should not be hard to read.

- [ ] Screen reader support (the cycle rows already carry a spoken summary; the rest do not)
- [ ] Large text mode verified at `fontScale = 2f`
- [ ] High contrast
- [ ] Never carry meaning by colour alone

### F5. A portable engine — **Exploring**

`:core:astronomy` is 55 files of pure Kotlin with no Android dependency. Extracting it to a
Kotlin Multiplatform module would serve three separate goals at once: the iOS port assessed in
[ios-port-assessment.md](ios-port-assessment.md), the "public calculation library" the old plan
wanted, and a desktop or web companion.

- [ ] Extract `:core:astronomy` to KMP (`kotlin.time` over `java.time`)
- [ ] Publish as a standalone library
- [ ] Reconsider the 25.6 MB offline timezone database — **78% of the APK**, and the single biggest
      obstacle to bundling any media content later

### F6. Engineering foundation — **Shipped**

- [x] Clean Architecture, modular structure, Compose, Material 3, Hilt, offline-first
- [x] CI, unit testing, documentation, `AGENTS.md` conventions
- [x] Deliberately not Room (DataStore) and not WorkManager (`AlarmManager` + `BootReceiver`)

---

## Part II — Computed domains

Where the app asserts a value it derived. This is its strongest ground and the reason anyone should
trust the rest — see [the validation pass](validation/panchanga-validation.md).

### C1. Panchanga Shastra — **Shipped** · Compute (oracle-validated), Teach

The core, and the connective tissue for everything else in this roadmap.

- [x] Tithi, nakshatra, yoga, karana, vara, paksha; maasa, ritu, ayana, samvatsara
- [x] Sunrise, sunset, moonrise, moonset, moon phase, golden hour
- [x] Brahma and Abhijit Muhurta; Rahu Kalam, Yamaganda, Gulika, Dur Muhurta, Varjyam
- [x] Sixteen Choghadiya windows; planetary rasi positions with next pravesh
- [x] Monthly calendar, day detail, cycle rows showing what each limb was, is and becomes
- [x] Plain-language explanations of every limb, enforced by the build
- [ ] Time scrubbing across the day (**Open** — the best demonstration of what a panchanga is)
- [ ] Yearly overview; agenda and timeline calendar views
- [ ] Home-screen widgets

### C2. Jyotisha Shastra — **Shipped**, with reporting gaps · Compute (oracle-validated), Cite, Teach

The deepest domain here and roughly complete on the computation side.

- [x] Natal chart, whole-sign houses, lagna, Spashta Graha to the arcminute
- [x] Seventeen divisional charts from one general rule
- [x] Vimshottari, Ashtottari and Yogini dashas at arbitrary depth
- [x] Ashtakavarga (binna and sarva), graha drishti, dignity, astangata, named yogas
- [x] Mangal, Kala Sarpa and Ganda Moola doshas, each showing its working
- [x] Guna Milan (36 gunas) plus the four porutham
- [x] Daily and weekly Rashifal, computed from Chandrabala and Tarabala — no invented predictions
- [ ] Bhava chalit / degree-based house cusps
- [ ] Monthly and yearly Rashifal
- [ ] Birth report — nothing exports or shares a chart yet
- [ ] Transit (gochara) narrated over time
- [ ] Shadbala — drik bala exists; the remaining five balas are **Open**
- [ ] Prashna (horary) and Varshaphala (annual) — **Exploring**, each a domain in itself

### C3. Muhurta Shastra — **Shipped** · Compute (oracle-validated), Cite

- [x] General panchanga muhurta; personalized muhurta against a birth Moon
- [x] Event-type presets, ranked days with the reasoning shown
- [x] Reboot-survivable reminders with per-event offsets and ringing alarms
- [ ] Parana (fast-breaking) timings
- [ ] Muhurta for the samskaras — see K2, where the two domains meet
- [ ] Snooze, repeat schedules, dedicated sunrise/sunset toggles

### C4. Kala — time reckoning — **Open** · Compute

Partly built and never named as a domain. The era and calendar arithmetic the app already does,
made explicit and complete.

- [x] Samvatsara, ayana, ritu, maasa
- [ ] Era conversion (Vikrama, Shaka, Kali) both ways
- [ ] Yuga and kalpa reckoning, as explanation rather than assertion
- [ ] Ghati / vighati / muhurta as a time display option

### C5. Vastu Shastra — **Open** · Compute (rule-transcribed), Cite, Teach

The strongest new computable domain, and the highest-demand one. Orientation, plot and room
placement, and the proportional calculations are genuine arithmetic on a compass bearing and a set
of dimensions — this is engine work, not an article.

**The honest caveat, and it must be stated in the app:** unlike the panchanga, **no independent
implementation exists to validate against.** Vastu is *rule-transcribed*, not oracle-validated — the
arithmetic can be proven correct, the rule can only be cited. Traditions differ substantially, so
every rule names the text it follows and the app never presents a Vastu result at the same
confidence as a tithi.

- [ ] Direction and orientation from a compass bearing, with the sixteen-fold division
- [ ] Room and function placement by direction, per a named text
- [ ] Plot proportion and ayadi calculations
- [ ] The Vastu Purusha Mandala as explanation
- [ ] A primer, to the same standard as the panchanga primer
- [ ] Explicitly **not**: any claim that a layout causes an outcome

### C6. Chandas Shastra — **Open** · Compute (rule-transcribed), Teach

Under-rated on the original list at two stars. Prosody is **an algorithm**: scan a verse into laghu
and guru syllables, match the pattern, name the metre. It is small, self-contained, pure Kotlin,
testable against classical verses whose metres are not in dispute — and it directly serves the
stotra reader, which is a shipped feature.

The best first domain for a new contributor: no Android knowledge needed, no UI, and a clear right
answer.

- [ ] Syllable weight analysis over Devanagari and IAST
- [ ] Metre identification for the common metres (anustubh, trishtubh, and the samavritta family)
- [ ] Surface the metre in the stotra reader
- [ ] Recitation guidance derived from the metre — **Exploring**

---

## Part III — Cited domains

Where the app reports what a tradition holds. Every claim names a source; the app's voice attributes
rather than asserts. These are the domains most open to contribution and the ones where the standard
matters most.

### K1. Festivals, vrata and observance — **Shipped**, with a long tail · Compute + Cite

- [x] Thirteen named festivals and every recurring lunar observance, computed from the tithi
- [x] Sankrantis; fasting days; festival and observance descriptions for what is surfaced
- [ ] Regional festivals (see F2 — this is the same work)
- [ ] Vrata procedure: what is kept, and what a source says about keeping it
- [ ] Parana timings (see C3)
- [ ] A vrata log — see Part IV

### K2. Dharma Shastra and the samskaras — **Next** · Cite + Compute + Track

**The best next knowledge domain, because it uses the engine rather than needing a new one.** Each
of the sixteen samskaras has a traditional muhurta the app can already compute and a procedure it
can cite. It is the natural bridge between what is built and what is planned.

- [ ] The sixteen samskaras: what each is, when it is traditionally held, what a source says
- [ ] Muhurta presets per samskara, using the existing electional engine
- [ ] Observances and duties by stage of life, attributed
- [ ] Explicitly **not**: prescriptive instruction, or any claim about who owes what to whom

### K3. Kalpa Shastra — ritual procedure — **Open** · Cite

Procedure as reference, tied to the timing the app already computes. Sequence, materials and
sankalpa, each attributed to a named text, with regional variation reported rather than flattened.

### K4. Ayurveda — **Open**, deliberately narrow · Cite

**Bounded to dinacharya and ritucharya** — the shape of a day and of a season. The app already
computes the ritu, so this hangs directly off the engine.

Everything therapeutic is a [red line](knowledge-standards.md#red-lines): nothing diagnostic,
no constitution assessment offered as health guidance, no remedy for a condition. If a sentence
could change what someone does about an illness, it does not ship. This bound is not negotiable and
a contribution that crosses it will be declined regardless of quality.

- [ ] Dinacharya — the traditional daily order, tied to the computed day windows
- [ ] Ritucharya — the seasonal order, tied to the computed ritu
- [ ] A primer explaining the concepts, attributed throughout

### K5. Mantra Shastra and stotra — **Shipped**, provenance owed · Cite + Track

- [x] 26 stotras with Devanagari, transliteration and a short original note
- [x] 12 mantras including the nine graha beeja; japa counter suggesting by mahadasha lord
- [ ] **Sources for all of it** — see F3, which blocks further content here
- [ ] Pronunciation guidance (text; audio is constrained — see K7)
- [ ] Traditional associations, attributed
- [ ] More stotras — **only after** F3 lands, so the catalog does not grow unsourced

### K6. Yoga Shastra — **Open** · Cite + Track + Teach

Asana, pranayama, meditation and the philosophy, as reference and as practice tracking. The
meditation timer is shipped and has **no content at all** — it is a timer, which makes it the
obvious place for this to begin.

- [ ] The eight limbs as explanation
- [ ] Practice reference, attributed, with no instruction in the app's own voice
- [ ] Brahma Muhurta practice tie-in — already computed
- [ ] Explicitly **not**: physical instruction that could injure someone unsupervised

### K7. The arts — Sthapatya, Shilpa, Gandharva, Natyashastra — **Exploring** · Cite + Teach

Grouped because they share one constraint the original list did not account for: **they need media
the app cannot currently carry.** Raga needs audio. Iconography and temple architecture need images.
The APK is already 78% timezone database, and the project makes no network calls by design — so
these are blocked on a content-delivery decision (see F5), not on willingness.

What *is* available now is the computable and textual part: proportional canons (tala measurement,
ayadi), the samaya ragas assigned to each prahara — which ties directly to the panchanga — and rasa
theory as explanation.

- [ ] Samaya raga by prahara, as text (**Open** — genuinely small, and a real panchanga tie-in)
- [ ] Proportional canons as computation
- [ ] Rasa theory and temple-architecture concepts as explanation
- [ ] Audio and image support — **Exploring**, blocked on F5

### K8. Nirukta and Vyakarana — **Re-termed** · Teach

**Not sections.** A section called "Sanskrit Grammar" is a different app, and one nobody would open
twice. The valuable form is a **layer**: every Sanskrit term the app already shows — and it shows
hundreds — becomes tappable for its meaning and derivation.

This also serves every other domain at once, and the mechanism already exists in `PanchangaGlossary`
and `PanchangaPrimer`.

- [ ] Etymology and derivation for the terms already on screen
- [ ] Sandhi and compound explanation where it aids reading, in the stotra reader
- [ ] Shared transliteration infrastructure (see F1)

---

## Part IV — Personal practice

Where the app records what *you* did and claims nothing about the world. The safest mode, and often
the most valuable: **an encyclopedia entry does not revive a practice — knowing it is Ekadashi at
4:50 tomorrow does.** This is where the app's knowledge of *when* becomes something someone acts on.

- [x] Japa counter with history; meditation timer with streak
- [ ] Vrata log — which were kept
- [ ] Daily sadhana tracking
- [ ] Personal tithis, family birthdays by tithi rather than by date, spiritual milestones
- [ ] Favourite festivals and frequently observed vrats
- [ ] Reading tracker; temple visits
- [ ] Backup and restore (local, no account)

All of it stays on the device, carries no judgement, and is never used to infer anything about the
person.

---

## Part V — Platform and ecosystem — **Exploring**

- [ ] Home-screen widgets; Wear OS; Android Auto notifications
- [ ] Calendar export (`.ics`)
- [ ] Public calculation library (see F5)
- [ ] iOS — assessed in [ios-port-assessment.md](ios-port-assessment.md); an assessment, not a commitment
- [ ] Desktop companion
- [ ] Explicitly **not**: cloud sync or any account system, unless it can be end-to-end private

---

## Part VI — Declined, and why

Recording what the project will *not* do, in the habit the codebase already keeps.

| | Why |
| --- | --- |
| **Arthashastra** | Classical statecraft and economics is a text to read, not a thing an app can do. There is no computation, no timing hook, and no practice to track — a section here would be a wall of prose nobody opens twice. Better served by a library link than by a feature. |
| **Dhanurveda** | Same shape, plus a physical-instruction risk the app will not take on. |
| **Sanskrit language learning** | A different app. The *infrastructure* — transliteration, script rendering — is kept as F1; the tutoring is not. |
| **Standalone Nirukta / Vyakarana sections** | Re-termed as a glossary layer (K8), which serves every domain instead of one. |
| **Remedy commerce** | Gemstones, yantras, paid remedies, astrologer referrals — a [red line](knowledge-standards.md#red-lines). |
| **In-app purchases, ads, feature gating** | ADR 0014. |

---

## What the user sees

The shastra map above is for contributors and for provenance. The app's navigation answers
questions instead, and one question may draw on several shastras at once:

| The question | What answers it |
| --- | --- |
| **What is today?** | Panchanga, festivals, observances, the calendar |
| **When should I do this?** | Muhurta, samskara timing, vrata, parana |
| **What does my chart say?** | Jyotisha — kundali, dasha, rashifal, matching |
| **How is this practised?** | Kalpa, dharma, yoga, ayurveda, mantra — cited, never instructed |
| **What does this mean?** | The primer and glossary layer, reachable from any term on any screen |
| **What have I done?** | Japa, meditation, vrata log, sadhana |
| **Where am I?** | Vastu, and sacred geography if it is ever built |

---

## The original shastra table, mapped

Every row of the source list, and where it now lives.

| Shastra | Where | Status |
| --- | --- | --- |
| Jyotisha | C2 | Shipped, reporting gaps |
| Panchanga | C1 | Shipped |
| Vastu | C5 | Open — best new compute domain |
| Muhurta | C3 | Shipped |
| Dharma | K2 | Next — the bridge domain |
| Yoga | K6 | Open |
| Ayurveda | K4 | Open, bounded to dinacharya and ritucharya |
| Mantra | K5 | Shipped; provenance owed |
| Sthapatya | K7 | Exploring — media-constrained |
| Shilpa | K7 | Exploring — media-constrained |
| Gandharva | K7 | Exploring; samaya raga as text is Open |
| Dhanurveda | Part VI | Declined |
| Arthashastra | Part VI | Declined |
| Natyashastra | K7 | Exploring — media-constrained |
| Chandas | C6 | Open — computable, and the best first domain |
| Vyakarana | K8 | Re-termed as a glossary layer |
| Nirukta | K8 | Re-termed as a glossary layer |
| Kalpa | K3 | Open |

**Added, because the list did not cover them and they matter more than several rows that were on
it:** F1 language and reach, F2 regional variation, F3 content provenance, F4 accessibility,
C4 time reckoning, and the samskaras under K2.
