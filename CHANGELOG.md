# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **Ashtakavarga** — binnashtakavarga per graha and the sarvashtakavarga, on a new Kundali page.
  Seven grahas each mark certain houses benefic from each of eight reference points (the seven
  grahas and the lagna); a sign collects one bindu per reference point that marks it, 0 to 8 per
  graha, and the seven summed always total **337** across the twelve signs. A transit through a sign
  holding 30 bindus is read very differently from the same transit through one holding 20, which is
  what makes the sarva row worth having.

  No new astronomy whatsoever — only which rashi each body occupies, which the chart already knows.

  **The 64 tables were read off an independent implementation's per-reference breakdown rather than
  recalled**, then replayed against 14 further charts spanning four cities and 1968 to 2083: **98
  binnashtakavarga rows and 14 sarvashtakavarga rows, no disagreements.** Two things fell out of the
  tables rather than being put into them, which is what says the reading is right and not merely
  self-consistent: each graha's own total lands on the classical figure (Sun 48, Moon 49, Mars 39,
  Mercury 54, Jupiter 56, Venus 52, Saturn 39), and those sum to 337. Both are asserted, the 337 for
  positions no real chart would produce as well, since it is a property of the tables rather than of
  any chart.

  Rahu and Ketu take no part: no binnashtakavarga of their own and no reference point, which is why
  eight references cover seven grahas plus the lagna.

### Added

- **Pratyantardasha** — the third dasha level — and one recursion in place of two hand-written ones.
  Mahadasha, antardasha and pratyantardasha were three names for a single rule applied again: a
  period divides into sub-periods running through the lord sequence from its own lord, each taking a
  share proportional to that lord's dasha years. `MahadashaPeriod` and `AntardashaPeriod` collapse
  into one `DashaPeriod` carrying its level, and a fourth level would now be free — it is capped at
  three because the next one splits a few weeks into a few hours, finer than a birth time known to
  the minute can support. Checked against an independent implementation at all three levels: **729 of
  729 periods matched on lord and order.**

### Fixed

- **A dasha year is the sidereal year, not the Julian one.** This was 365.25 days and is now
  365.2564, which moves every dasha date in the app — by six hours per century of elapsed timeline,
  reaching **about eighteen hours** by the end of the 120-year cycle. Fitting the constant against
  729 published period boundaries put the worst disagreement at 66,255 seconds for 365.25 against 604
  seconds for the sidereal year, and that remaining ten minutes is fully explained by the
  four-decimal rounding of the Moon longitude fed into the comparison. The Gregorian and tropical
  years came out four to six times worse than the Julian one, so this is a real convention rather
  than a fitted number. Some texts intend a 360-day year, which is a different timeline altogether;
  this app follows the sidereal year, consistent with computing everything else from real positions.

- **Ashtottari and Yogini**, as literal derived tables. Ashtottari runs eight lords over 108 years
  with no Ketu, covering the nakshatras in runs of three and four; Yogini runs eight over 36 years,
  one to eight years each, one nakshatra apiece. Lord orders and year tables were read off an
  independent implementation's period durations, and the starting-lord tables from two sweeps taken
  independently of each other, which agreed on all 27 nakshatras. The Kundali dasha pages now carry a
  system selector.

  **The tables are written out rather than generated, and reproduce the source including where it is
  odd.** Yogini's starting lord advances one per nakshatra everywhere except between Ardra and
  Punarvasu, where it moves *back two*; both sweeps reproduce it, and a test pins it so a later
  tidy-up cannot quietly change every Yogini reading for those nakshatras. Whether that is a real
  convention or the reference's own off-by-one could not be established — and matching the tool
  people compare against is worth more here than being right in a way that agrees with nobody.

  **One thing is not bug-for-bug, and should not be read as if it were.** Ashtottari's first period
  takes its elapsed share across the lord's whole run of nakshatras, which 49 of 57 sampled births
  confirm. The eight that differ are exactly Rahu's run — 26, 27, 1, 2, the one wrapping the end of
  the zodiac back to its start. A controlled sweep, holding the date fixed and moving only the time
  of day, showed the reference's Rahu start shifting about 66 days per eight hours of birth time
  where the run model calls for three years per nakshatra, and placing the birth outside the very
  period it should sit in. Consistent and reproducible across four months, so it is some rule, but
  not one recoverable from the data. This engine applies the run model uniformly: for those four
  nakshatras the **lord is still right** and only the boundaries differ. Their goldens assert the
  lord and skip the start, and a test holds that exclusion at exactly eight so it cannot grow.

### Fixed

- **The app stayed on its fallback location after location was switched on.** Two causes, both real.
  `lastLocation` returns `null` whenever nothing has recently asked the system for a position — which
  is exactly the state after the device's location setting has been off — so the resolver fell through
  to New Delhi. It now falls back to an active fix, bounded by a timeout, with the cache still tried
  first because it is instant and almost always populated. And Home only ever loaded once per
  composition, so nothing re-resolved on return; it now reloads whenever the screen comes back to the
  foreground. That second fix also cures a staleness nobody had reported yet: leave the app open
  overnight and the day, tithi and muhurta windows were all yesterday's until something else
  triggered a load.

  The permission prompt deliberately stays on first composition only. Asking again on every resume
  would re-prompt someone who has already declined, which Android answers by refusing outright after
  the second refusal.

- **The hero card led with a tithi that had already ended.** The day is named by its sunrise tithi —
  the convention every published panchanga follows, and still what the Panchang screen shows — but
  the card read at nine in the evening was headlining a tithi that ended before breakfast, with the
  one actually in force demoted to small print beneath it. The prominence is now the other way round:
  what is running and how long it has left, with "Dwadashi ended 06:22" recorded underneath. The
  handover line appears only once the day has rolled over; before that it would be the same tithi
  said twice.

- **"Tap for full panchang" wrapped mid-phrase.** It had been appended to the maasa and samvatsara
  line, inside a column the moon-phase label was squeezing from the right. It is now its own line
  across the full width of the card.

- **The hub no longer flashes a spinner on every resume**, which reloading on resume would otherwise
  have caused. The loading indicator now takes over the screen only when there is nothing to show yet.

### Added

- **Eight more divisional charts** — D-3, D-4, D-10, D-12, D-24, D-40, D-45 and D-60 — taking the
  varga engine from nine to seventeen. D-10 (career) and D-12 (parents) are the two a reader asks for
  by name after the D-9; D-60 is the most heavily weighted chart in classical practice.

  **All seventeen are now one expression**, `sign = start(rashi) + step × division`, differing only in
  where each rashi's first division starts and how far each step moves. That was fitted to an
  independent implementation's output rather than recalled, then checked against **every** observation
  — 520 placements per chart, **8,320 in all, with no disagreements**. Four vargas needed a
  twelve-entry start table and those tables were read off the same data; the drekkana and
  chaturthamsa turned out to need a *step* of four and three rather than a table at all.

  The fit also settled what to leave out. D-2 (hora), D-5, D-30, D-108 and D-144 take two or more
  different steps within a single sign, so they do not fit the expression at all. They are different
  rules rather than variations, and approximating them would be inventing answers.

- **A measured precision ladder for the vargas.** `Varga.divisionArcminutes` and
  `Varga.needsExactBirthTime` replace hand-waving about accuracy with numbers. Measured over the same
  sample, the share of placements sitting within an arcminute of a division edge — where the sign is
  effectively a coin toss — runs from 0.0% for D-3 up to **8.3% for D-60**.

  Below roughly D-24 the **birth time**, not the ephemeris, becomes the limit: the ascendant covers a
  degree in about four minutes, so a birth time known only to the nearest five minutes leaves a D-60
  ascendant meaningless however exact the arithmetic. The Varga page now says so on the finer charts
  instead of repeating a generic caveat. D-81 stays out, at 22-arcminute divisions and 17% at risk.

### Added

- **The four additional porutham** — Mahendra, Vedha, Rajju and Sthree Dheerga — read beside the
  thirty-six gunas on Kundali Matching. Ashtakoota answers "how well matched" as a score; these
  answer "is anything wrong" as yes or no. They are **deliberately not folded into the total**,
  because a strong guna score should not be able to bury a failed Rajju, which is exactly the case
  the score alone would hide. Rajju names the limb both partners fall on rather than reporting a bare
  failure, since which limb is shared is what the rule is held to say.

  **Every table was derived from an independent implementation rather than from memory**, by sweeping
  one partner's nakshatra across a full lunar month and reading back its verdicts — 186 pairings over
  six groom nakshatras and all twenty-seven bride nakshatras, all four rules agreeing throughout.
  That was worth the trouble, because two of the four came out differently from the textbook summary
  they would otherwise have been written from:

  - **Vedha is not thirteen disjoint pairs.** It is a sum relation — two nakshatra numbers piercing
    one another when they total 19, 28 or 37 — which gives most nakshatras *two* partners and the
    nine from Magha to Jyeshtha *three*. Implemented as pairs it would have missed about half of all
    vedha, and nothing in the app would have looked wrong.
  - **Sthree Dheerga is directional and Mahendra is not.** Counting between two nakshatras one way
    and the other always sums to 29; Mahendra's matching set is closed under that, so sources giving
    opposite directions turn out to agree. Sthree Dheerga's range of 14..27 is not, so counting it
    backwards inverts nearly every verdict. It is counted from the bride's star to the groom's.

  One consequence that looks like a bug and is not: **Chitra pierces itself**, since 14 + 14 = 28. It
  falls out of the same arithmetic as every other pairing, and the reference implementation reports
  it too. A test pins it so it is never "fixed" by mistake.

### Added

- **Mangal dosha (Kuja dosha, Manglik, Chevvai dosham).** Across much of India this is the *first*
  question asked of a proposed match, and the app did not answer it at all — Ashtakoota without it
  was missing the question many users came to ask. Mars in the 1st, 2nd, 4th, 7th, 8th or 12th,
  counted separately from the lagna, the Moon and Venus, with classical parihara.

  **Every trigger and every cancellation is shown.** A dosha computed without its parihara is worse
  than no dosha at all: most charts trigger the rule somewhere, so a bare "Manglik" verdict would
  alarm nearly everyone and inform no one. Two kinds of parihara apply and they are not
  interchangeable — general ones, about Mars's own condition, answer the affliction wherever it
  arises, while the house-and-sign rules lift *one* house only. Treating the second kind as the
  first would clear a dosha that still stands, so they are modelled separately and the dosha stands
  while any trigger is unanswered. Two charts that both carry it cancel each other, which is the
  most widely applied parihara of all and the reason the question belongs to the pair.

  The houses and the reference points are both convention choices — the classical verse names the
  lagna, 4th, 7th, 8th and 12th; the 2nd is a South Indian addition — so the app takes the union and
  reports each placement separately, letting a reader who follows the stricter verse discount a
  trigger the app cannot discount for them. The one rule deliberately left out is that the dosha
  lapses with age: widely repeated, no classical basis, and stating it would be inventing
  reassurance.

  Shown on **Kundali Matching** for a pair and on the Kundali **Pramukh Yoga** page for one chart.

- **Graha drishti.** Whole-sign Parashari aspects — every graha on the 7th from itself, Mars also the
  4th and 8th, Jupiter the 5th and 9th, Saturn the 3rd and 10th. Added because the "Jupiter aspects
  Mars" parihara is not optional, and a cancellation rule that silently did nothing would be the
  worst of both worlds. Rahu and Ketu report no drishti: authorities disagree on whether the nodes
  aspect and on which houses, and picking one and presenting it as settled would be inventing a
  convention. Note that this is Vedic drishti and is *asymmetric* — Saturn looks upon the third from
  itself and nothing there looks back — so it must never be conflated with Western aspects.

### Added

- **Varga Kundali page.** The divisional charts are now drawn as charts. `NatalChart.vargaChart(varga)`
  casts a whole chart into a division: the *lagna's* longitude is divided too, its divisional sign
  becomes house 1, and every graha is placed by counting whole signs from there. That framing is the
  point — the seventh house of the D-9 is what a reader is actually looking for, and a per-graha
  accessor can never supply it, because a house only exists relative to an ascendant and the lagna is
  the one longitude such an accessor never sees.

  In the Kundali book it is one page with a chip per varga rather than nine pages. Nine more pages
  would triple the book for what is one page asked nine ways, and comparing two divisions by swiping
  between them is worse than tapping. It opens on the D-9, which is the varga a reader means when
  they do not say which, and each chip carries what that division is traditionally read for.

  The caption states the arcminute caveat on the page rather than only in the KDoc: a graha sitting
  on a division edge may be shown either side of it, and the finer the varga the likelier that is.
  A reader deciding something from a D-27 house deserves to know that from the screen.

### Changed

- **Third-party services are no longer named anywhere in the repository.** Calculations were
  described as cross-checked against particular commercial almanac sites, and the varga and natal
  goldens were labelled with the name of the reference implementation they came from. Naming other
  people's products as accuracy benchmarks invites trademark and comparative-advertising questions
  this project has no reason to answer, and the claim reads no differently without them: what makes
  a calculation trustworthy is that it was checked against something independent, not which thing.
  Docs, ADRs, KDoc and test names now say "published almanacs" and "an independent reference
  implementation"; `JagannathaHoraReferenceTest` is now `ReferenceImplementationTest`. The manual
  validation checklist now tells the reader to pick two independent almanacs and record which they
  used, rather than linking specific ones.

  Left in place deliberately: **drik ganita**, spelled lowercase, in the Ayana and Ritu
  documentation. That is the classical Sanskrit name for the observed-position convention, as
  against saura ganita's mean positions -- it names the maths the app implements, and one of those
  sites is named after it rather than the other way round. Removing it would have lost real meaning.

### Added
- **Divisional charts (vargas).** The navamsha rule turned out to be general: counting divisions
  continuously from 0° Mesha reproduces the classical rule for nine of the divisional charts, so the
  engine now computes D-1, D-6, D-7, D-8, D-9, D-11, D-16, D-20 and D-27 from one function rather
  than nine. Verified against an independent implementation — 7,500 placements, no
  disagreements.
- **"Ends in" for every limb of the panchanga.** The panchang detail now shows when each limb gives
  way to the next — tithi, nakshatra, pada, yoga, karana, chandra rashi, surya rashi (the sankranti)
  and moon phase — solved by bisection rather than extrapolated from a mean rate, which is wrong by
  up to 30% as the Moon's speed varies between apogee and perigee.
- **Home now distinguishes the day's tithi from the current one.** The card is still named for the
  tithi running at sunrise, as panchangas name the day, with a live line beneath showing what is
  actually running now and how long it lasts.
- **The Kundali screen is now a swipeable book.** The birth chart opens as pages you swipe between,
  starting with the **Lagna Kundali** and the **Rashi (Chandra) Kundali** — the same placements
  counted from the Moon instead of the ascendant, which is how the two charts are read side by side
  in a printed panchang. A header names the open page and dots show where you are.
- **Jataka properties.** A page of the standing details a panchanga lists beside the charts: janma
  rashi and its lord, nakshatra with pada, varna, vashya, yoni, gana and nadi in the classical
  Ashtakoota order, lagna, the Sun in both zodiacs, the ayanamsa at birth, and the Shaka, Vikram and
  samvatsara years.
- **Spashta Graha.** A table of all nine grahas — position to the arcminute within the sign,
  nakshatra and pada, retrograde marker, and the navamsha (D9) sign.
- **Pramukh Yoga.** The named combinations present in the chart — Gajakesari, Budhaditya,
  Chandra-Mangala and the five Panchamahapurusha — each shown with the placement that produced it,
  so the claim can be checked against the chart pages rather than taken on trust. Every rule was
  verified against an independent implementation over 75 charts before being kept.
- **Astangata (combustion)** in the Spashta Graha table, marking a graha lost in the Sun's glare,
  using the classical Parashari orbs.
- **Mahadasha and Antardasha.** The full Vimshottari cycle from birth with the running period marked,
  and the nine sub-periods of whichever mahadasha is running now.

### Changed
- **The Support tab now carries a donation-box glyph** — an Om coin dropping into a daana-patra. Drawn
  as an alpha stencil, so it takes the navigation bar's colours like every other tab, shows the
  selected state, and stays legible on a dark theme.
- **Angular divisions are now bucketed in exact integer arcseconds.** Every division of the zodiac
  is a whole number of arcseconds (a nakshatra is 48,000, a pada 12,000, a tithi 43,200) while most
  are non-terminating in degrees, so dividing in degrees left boundary cases to the mercy of
  floating-point rounding. Divisions are documented as half-open — a longitude exactly on a boundary
  belongs to the division beginning, so 26°40′00″ is Krittika rather than Bharani.
- **Every screen's title bar now reads the same way** — "Vedic Mitra" with the open destination
  beneath it. Previously the four tabs showed their own label, pushed screens showed the app name
  with nothing under it, and the Home tab kept saying "Home" while a detail view was open.

### Fixed
- **Seven yogas were removed after checking them against an independent implementation.** The
  Sunapha/Anapha/Durudhara/Kemadruma and Vesi/Vasi/Ubhayachari families agreed with it only 45% and
  72% of the time across 75 charts — the disagreements are matters of convention rather than
  arithmetic, and a yoga wrong one time in four does not belong on a page describing someone's
  chart. What remained was measured at 100% and 97%.
- **Pada was computed wrongly at its boundaries.** Deriving the Moon's pada by taking its longitude
  modulo the nakshatra span and dividing again rounds twice, which put **40 of the 108 pada
  boundaries in the wrong quarter**. Pada feeds the Navamsha and the Nadi-dosha cancellation in
  Kundali Matching, so the fix reaches beyond the panchang itself.

## [0.7.0] - 2026-08-22

### Added
- **Support the project.** A new **Support** tab in the bottom bar (also reachable from About) gathers
  the ways to keep Vedic Mitra going: GitHub Sponsors, Ko-fi, and a copyable UPI ID for donations; a
  commercial-licensing route for businesses that cannot accept the AGPL; and the free ways to help —
  starring the repo, reporting a bug, contributing a translation. Every feature in the app stays free
  and nothing on the screen unlocks anything. The screen makes no network calls: links open in the
  browser and the UPI ID is copied to the clipboard rather than launched as a `upi://` intent, which
  would crash on devices without a UPI app.
- **Commercial licensing is now a real offer** — published tiers and pricing
  ([docs/COMMERCIAL_LICENSE.md](docs/COMMERCIAL_LICENSE.md)), a template agreement
  ([LICENSE-COMMERCIAL.md](LICENSE-COMMERCIAL.md)), and a dedicated contact address, replacing
  "terms and pricing on request".
- **Privacy policy** ([docs/PRIVACY.md](docs/PRIVACY.md)) documenting what the app stores on device,
  why each permission exists, and the single network call (Android's own `Geocoder` for city search).
- **AGPL section 7 linking exception** ([LICENSE-EXCEPTIONS.md](LICENSE-EXCEPTIONS.md)) permitting
  Vedic Mitra to be linked with proprietary platform libraries — needed because the app already uses
  Google Play services Location for optional fused location.
- **Expanded the Stotra library.** Added the **Hanuman Chalisa** (Tulsidas's full forty verses with the
  opening and closing dohas) plus the **Lingashtakam** and **Nirvana Shatkam** (Shiva), the
  **Madhurashtakam** (Krishna), and the **Gajananam** dhyana (Ganesha) — each in Devanagari with a
  line-for-line transliteration and a short note. The reader already scrolls, so the longer texts read
  cleanly. Tuesday's "today's stotra" now suggests the Hanuman Chalisa.

### Changed
- The bottom navigation bar gains a fourth destination, **Support**, alongside Home, Settings and
  Profile. No new dependency — the icon ships with Material 3.
- Settings, About, and Support now share one set of row composables instead of each hand-rolling its
  own, so the sections stay consistent as they accumulate. Settings scrolls on short screens.

### Fixed
- The "Question / discussion" link in the issue templates pointed at a non-existent `vedicmitra` org.

## [0.6.0] - 2026-08-20

### Fixed
- **Crash when picking a birthplace (release builds).** Choosing a place while adding or editing a
  profile aborted the app in release (R8-minified) builds only. The offline time-zone lookup
  decompresses its bundled data with `zstd-jni`, whose native library reads two JVM fields
  (`ZstdInputStreamNoFinalizer.srcPos`/`dstPos`) by name over JNI; R8 was renaming those fields, so the
  native lookup failed with `NoSuchFieldError` and aborted the process (an uncatchable native crash — no
  Kotlin `try/catch` could stop it). Added R8 keep rules for `com.github.luben.zstd.**` (alongside the
  existing `timezonemap`/`flatbuffers` keeps) so the fields keep their names and the lookup resolves.
- **Muhurat reminders now actually notify.** Setting a reminder for an auspicious day scheduled it but
  the notification never appeared, because the Muhurat day screen never requested the runtime
  notification permission (default-denied on Android 13+). It (and the Meditation screen) now request
  `POST_NOTIFICATIONS` up front, so scheduled reminders are delivered.

### Added
- **Tap placements to learn what they mean.** Several screens now reveal a plain-language significance
  when you tap: each Kundali card and graha row (the ascendant sign, the Moon's nakshatra, the running
  mahadasha, and each graha's sign/house), each Rashifal week-day (its full reading), and each Guna
  Milan koota row in Kundali Matching (what that koota measures). Kundali Matching also shows the groom
  and bride pickers side by side.
- **Stotra — a library of hymns and shlokas.** The Stotra tile now opens a browsable library
  (`:feature:stotra`) of 20+ traditional stotras — Ganesha, Shiva, Vishnu, Devi, the Navagraha stotra,
  the peace mantras and more — each shown in Devanagari with a roman transliteration and a short note.
  A "today's stotra" is suggested by the weekday's ruling graha (e.g. Gayatri on Sunday, Hanuman on
  Tuesday, Guru on Thursday). Tapping one opens a reader with an adjustable font size for chanting. All
  text is public-domain Sanskrit. This lights up the last of the hub tiles.
- **Meditation — timed sit with a breath guide.** The Meditate tile now opens a working timer
  (`:feature:meditation`): pick a length (5–30 min), and a countdown runs with a breath-pacing circle
  that expands and contracts, plus a soft start/end bell (a generated tone — no bundled audio). Each
  finished sit is logged to a history with a daily total and a streak, stamped with the day's nakshatra.
  Panchanga hook: it surfaces today's Brahma Muhurta window with a "sit now" shortcut and an optional
  daily pre-dawn reminder (scheduled through the existing reminder pipeline and rolled forward on each
  visit).
- **Japa — mala chant counter.** The Japa tile now opens a working counter (`:feature:japa`): tap the
  ring to count beads, with a mala completing every 108 (a haptic buzz marks each round). Pick from a
  bundled catalog of mantras (Gayatri, Om Namah Shivaya, Mahamrityunjaya, and the nine Navagraha beeja
  mantras, shown in Devanagari + transliteration). Sittings are logged to a history with a daily total
  and a streak, and an in-progress mala is saved so it can be resumed. Two panchanga hooks: it suggests
  the beeja mantra of your current mahadasha lord (from your primary profile's chart), stamps each
  logged sitting with the day's nakshatra, and surfaces today's Brahma Muhurta as an auspicious time to
  sit.
- **Rashifal — daily & weekly Moon-transit outlook.** The Rashifal tile now opens a working reading
  (`:feature:rashifal`): today's verdict for a rashi plus the seven days ahead, computed from real
  astronomy rather than generic text. Each day is graded by Chandrabala (the transit Moon's house from
  the read sign); when you read your own birth Moon sign it's fully personalised with Tarabala from
  your birth star. Defaults to your primary profile's sign, and you can tap any of the twelve rashis to
  browse it (a sign-only transit reading) — the profile's own sign is starred.
- **Matchmaking — kundali matching (Guna Milan).** The Match tile now opens Ashtakoota matching: pick
  one male and one female chart-ready profile and see the 36-guna breakdown across the eight kootas
  (Varna, Vashya, Tara, Yoni, Graha Maitri, Gana, Bhakoot, Nadi), the total, the verdict band, and any
  Nadi / Bhakoot dosha. Scored from both Moons via the JPL-validated chart engine.
- **About — maintainer links.** The About screen now shows tappable GitHub (@imskylab) and LinkedIn
  links for the creator, and the same links were added to the README, project site, AUTHORS,
  LICENSING contact, and the issue-template contact links.
- **Muhurat — personalised to a birth chart.** The best-days results now have a profile selector
  ("General" plus each chart-ready profile, defaulting to your primary). Picking a profile re-ranks
  the days for that person by layering their Tarabala (the day's nakshatra counted from their birth
  star) and Chandrabala (the day's Moon sign counted from their birth Moon sign) onto the general
  panchanga score, with the reasons shown on each day. "General" gives the non-personalised ranking.
- **Kundali — pick which profile to view.** When you keep more than one chart-ready profile, the
  Kundali screen now shows a chip picker to switch between them (defaulting to your primary profile)
  instead of only ever showing the primary.
- **Profiles — gender.** A profile can now record a gender (Male / Female / Other), shown on the
  profile list. It's optional for a chart and groundwork for kundali matching, which pairs one male
  and one female profile.
- **Muhurat — set a reminder for a chosen day.** The muhurta day detail now has a "Set reminder for
  this day" button: it schedules a one-shot notification for that day's sunrise (named for the
  activity) and persists it, so it survives a reboot. A day already begun is declined with a message.
- **Muhurat — choose the search window.** The best-days results now have a 30 / 60 / 90-day selector,
  so you can widen or narrow how far ahead the ranking looks (default 60 days) instead of a fixed
  window.
- **Muhurat — day detail with time windows.** Tapping a ranked day in the Muhurat results now opens
  that day's detail: its weekday/tithi/nakshatra and the day's time windows split into the auspicious
  ones to prefer and the inauspicious ones to avoid (the muhurtas — Abhijit, Brahma, Rahu Kalam,
  Yamaganda, … — plus the Choghadiya), each with its start–end time.
- **Muhurat — find your best days.** The Home hub's Muhurat tile now opens a working flow
  (`:feature:muhurat`): pick a category (Bal Sanskar, Vivah, Vastu, Purchases, Medical, Business,
  Agriculture, Ceremonies), then an activity, and see the most auspicious upcoming days ranked — each
  with a star rating, a 0–100 score and the panchanga reasons that earned it. General
  (panchanga-based) guidance for now; personalisation to a birth chart comes later.
- **Muhurat: ranked best-days finder (engine).** `AstronomyEngine.bestMuhurtasFor(activity, days,
  location)` walks a date range, samples each day at its own sunrise, scores the panchanga with the
  muhurta scorer, and returns the days best-first (ties broken by the earlier date) — the data the
  upcoming "find best dates" screen will show. Still engine-only.
- **Muhurat scoring engine (foundation).** `:core:astronomy` gains an electional-muhurta layer: a
  catalog of activities grouped into eight categories (Bal Sanskar, Vivah, Vastu, Purchases, Medical,
  Business, Agriculture, Ceremonies — including Gardening), a per-activity rules table (starting with
  Griha Pravesh, Vivah, Namkaran, Vehicle and Bhoomi Poojan, the rest on a sensible default), and a
  `scoreMuhurta` that rates a day's panchanga 0–100 with reasons — nakshatra weighted most, then
  weekday and tithi, with the Rikta/Amavasya tithis, Vyatipata/Vaidhriti yogas and Vishti (Bhadra)
  karana penalised. Not yet wired to a screen; the ranked "best days" finder builds on it next.
- **About Vedic Mitra.** Settings now has an About screen with the app's version (read from the
  installed build), a short description, the author (Jayvardhan Potabatti), copyright, and the
  licensing (GNU AGPL-3.0-or-later, with a commercial license also available).
- **Kundali (birth chart).** The Kundali tile on the Home hub now opens your birth chart, computed
  from your primary profile: the lagna, the nine grahas by rashi and house (with retrograde), the
  Moon's nakshatra and pada, and your current Vimshottari mahadasha. Drawn as a **North-Indian diamond
  chart** — the square divided by its diagonals and the side-midpoint diamond into twelve fixed houses
  (house 1 at the top centre, running anticlockwise), each showing its rashi number and the grahas
  placed there (retrograde in red) — above the same details as tappable cards.
- **All nine grahas.** Planetary positions now cover the full classical set — Sun, Moon, Mars,
  Mercury, Jupiter, Venus, Saturn, Rahu and Ketu (sidereal / Lahiri) — up from four. Mars, Mercury and
  Saturn use the JPL Keplerian ephemeris; Rahu/Ketu are the mean lunar nodes. This shows on the Home
  planetary-positions list and is the first piece of the birth-chart engine.
- **Birthplace geocoding.** A profile's place of birth is now searched through the geocoder and, on
  picking a result, resolved to coordinates + an IANA time zone (reusing `:core:location`) — the exact
  location and moment a birth chart needs.
- **Birth profiles.** Settings → Profiles keeps multiple birth profiles — yourself plus family or
  friends — each with name, relation, date, exact time, and place of birth, stored on-device
  (`:feature:profile` + a `ProfileRepository` in `:core:datastore`). One is always the primary
  "Self"; the first profile you add becomes primary, and you can switch it. They're the foundation the
  upcoming astrology features (Kundali, Rashifal, personalised Muhurat) build on.
- **Home hub landing.** Home is now a hub: a tappable "today's panchang" hero, the auspicious-now
  strip, and a categorised shortcut grid (Daily · Astrology · Devotion) built with the brand's saffron
  theme and custom cultural glyphs. Daily tiles open the calendar, reminders, or the full daily
  panchang (the previous dashboard, now behind the hero and the Panchang tile); Astrology/Devotion
  tiles are shown with a "coming soon" state so the growing roadmap stays discoverable.
- **Intro splash video.** A short branded video plays full-screen (muted) when the app opens, then
  hands off to the home screen. Tapping anywhere — or the back button — skips it. The clip is bundled
  in the app (`res/raw`).
- **Tap a Home list item for its significance — and set a reminder.** Rows in the
  Auspicious/Inauspicious periods and Upcoming festivals/events lists are tappable, opening a bottom
  sheet with the item's time or date and a short, offline explanation of what it is and why it
  matters (from `PanchangaGlossary`). Muhurta and recurring-observance rows also offer a **Set
  reminder** button that schedules the reminder (via a shared `AddReminderUseCase`) so it appears and
  can be edited on the Reminders screen.
- **Flexible reminder lead time.** A reminder's "remind before" time is now any whole value in
  minutes, hours, or days (up to 30 days), entered via a number field and a unit picker — replacing
  the fixed 0/5/10/15/30-minute chips. It is still stored as total minutes, and the fired
  notification phrases the lead naturally (e.g. "starts in 2 hours").
- **Rename reminders.** Each added reminder can be given a custom display name (pencil icon → rename
  dialog); it shows as the reminder's label, survives the daily renew, and reverts to the derived
  name when cleared. Persisted alongside the reminder (older saved reminders decode unchanged).
- **Planetary positions.** The home screen shows the rashi of the Sun, Moon, Guru (Jupiter), and
  Shukra (Venus), each with the date it next changes rashi (pravesh), in a collapsible list. Sun and
  Moon reuse the existing Meeus ephemeris; Guru and Shukra are computed geocentrically from JPL
  "Approximate Positions" Keplerian elements (heliocentric solve → Earth subtraction → sidereal via
  Lahiri), with the ingress found by a daily scan plus bisection. Venus's separation from the Sun
  stays within its ~47° maximum elongation as a physical sanity check.
- **Festivals, observances, and a redesigned home dashboard.** `FestivalCalculator` derives upcoming
  named festivals (Ugadi, Rama Navami, Ganesh Chaturthi, Navaratri, Diwali, Holi, Janmashtami, Maha
  Shivaratri, …), recurring lunar observances (Ekadashi, Purnima, Amavasya, Sankashti Chaturthi,
  Pradosh, Masik Shivaratri, Vinayaka Chaturthi), and Sankrantis — each judged by that day's
  **sunrise** panchanga and cross-checked against published 2026 dates. The home screen was reworked
  around panchanga-limb and season/ayana strips, an auspicious-now band, and collapsible
  Auspicious/Inauspicious/Festivals/Events sections. See
  [docs/adr/0008-festivals-and-home-landing.md](docs/adr/0008-festivals-and-home-landing.md).
- **Choghadiya.** The sixteen Choghadiya windows (eight day, eight night, weekday-sequenced) are now
  computed and available for reminders. See [docs/adr/0011-choghadiya.md](docs/adr/0011-choghadiya.md).
- **Reminders redesign with tithi reminders.** The reminders screen now works like a clock app —
  add and remove reminders from a unified list whose sources are the day's muhurta and Choghadiya
  windows plus **custom tithi** targets (built from Maasa · Paksha · Tithi, or presets like Ekadashi,
  Purnima, Amavasya). Each is resolved to its next occurrence, fires at sunrise minus a lead, and is
  renewed on load.
- **Panchang calendar highlighting.** The monthly grid highlights festival, observance, and
  Sankranti days, and each day's detail card names its notable entry.
- **Selectable and saved locations with offline timezone detection.** GPS, city search, and manual
  latitude/longitude, with multiple saved locations, resolved to a time zone (and DST) entirely
  offline. See [docs/adr/0006-selectable-and-saved-locations.md](docs/adr/0006-selectable-and-saved-locations.md)
  and [docs/adr/0007-coordinate-timezone-resolution.md](docs/adr/0007-coordinate-timezone-resolution.md).
- **Maasa and Samvatsara.** `:core:astronomy` now derives the amanta lunar month (Maasa) and the
  sixty-year-cycle year name (Samvatsara), shown on the home dashboard and each calendar day's
  detail. The month is named from the Sun's rashi at the new moon that begins it (found by a
  synodic-seeded bisection over the Moon's elongation), and a lunation with no Sankranti is flagged
  as an **Adhika** (leap) month — correctly reporting Adhika Jyeshtha in 2026, so early August
  reads Ashadha rather than Shravana. Samvatsara follows the South-Indian Chandramana convention,
  advancing at Chaitra Shukla Pratipada (Ugadi) from the elapsed Shaka year. Cross-checked against
  published almanacs, including the Ugadi 2026 almanac (Parabhava Nama Samvatsara, Shaka 1948).
  See [docs/adr/0005-maasa-and-samvatsara.md](docs/adr/0005-maasa-and-samvatsara.md).
- **Hindu calendar screen.** A new `:feature:calendar` module with a monthly panchang grid — each
  day shows its tithi (Shukla/Krishna + number); tap a day for the full panchanga of that date, and
  page between months. Backed by a new lightweight `AstronomyEngine.daySummaryAt` so a whole month
  of days is computed cheaply (no per-cell sunrise/moonrise search). Reached via a new bottom
  navigation bar (Home · Calendar · Reminders · Settings).
- **Golden brand theme.** The Material 3 theme was reworked into a golden / maroon / bronze palette
  (full tonal scheme, light and dark, plus softly-rounded shapes) drawn from the app emblem,
  replacing the placeholder indigo/saffron/teal. Dynamic (wallpaper) colour now defaults **off** so
  the brand palette is the out-of-the-box look; users can still opt into dynamic colour in Settings.
  See [docs/adr/0004-golden-theme-and-calendar.md](docs/adr/0004-golden-theme-and-calendar.md).
- **Moonrise and moonset.** `LunarDay` computes the Moon's topocentric altitude (extending
  `Ephemeris` with the Moon's ecliptic latitude, distance, equatorial conversion, and Greenwich
  Mean Sidereal Time — none of which existed before) and finds rise/set crossings via a coarse
  scan plus bisection, since the Moon moves too fast and irregularly for the Sun's closed-form
  hour-angle formula. Cross-checked against published almanacs for Delhi across 7 consecutive days;
  every reference matched within ~5 minutes. Because the lunar day (~24h50m) is longer than the
  civil day, a civil day can have two moonrises/moonsets or none of one kind roughly once a month —
  `MoonTimes` follows the same nullable convention as `SunTimes` and reports the first occurrence
  within the civil day.
- **Dur Muhurta and Varjyam.** `MuhurtaCalculator` now adds Dur Muhurta (one or two of the day's 15
  equal parts, by a verified per-weekday table cross-checked against published almanacs) and Varjyam
  (Nakshatra Thyajyam — a 4-ghati inauspicious window positioned within the current nakshatra by a
  verified 27-nakshatra ghati table, requiring a new backward search — `VarjyamCalculator` — for the
  exact instant the current nakshatra began, since ghatis are counted from nakshatra-start, not
  sunrise or midnight). Fixed a related correctness bug this surfaced: Abhijit Muhurta is now
  correctly suppressed on Wednesdays, when it would otherwise coincide with that day's Dur Muhurta.
- **Ayana and Ritu.** `:core:astronomy` now derives Ayana (Uttarayana/Dakshinayana) and Ritu (the
  six Indian seasons) from the Sun's sidereal longitude — the drik ganita (observed-position) convention.
  Both, along with the existing tithi/nakshatra/yoga/karana/vara/sunrise/Rahu Kalam/Brahma Muhurta
  calculations, were cross-checked against published almanacs for Mumbai/Delhi, 5 August 2026, and matched
  within a minute or so throughout.
- **Moon phase and golden hour.** `:core:astronomy` now derives the Moon's phase (one of the eight
  traditional divisions, from its elongation) and computes the day's golden-hour windows (Sun
  elevation between -4° and +6°, bracketing sunrise and sunset) via the same NOAA solar-position
  equations already used for sunrise/sunset. Both are shown on the home screen.
- **Muhurta reminders.** Reboot-survivable, exact-alarm notification reminders for the day's
  muhurta windows (Brahma Muhurta, Abhijit Muhurta, Rahu Kalam, Yamaganda, Gulika Kalam), each with
  its own independently configurable lead time (0–30 minutes before the window starts). Changing a
  window's lead time immediately re-schedules its alarm if already enabled, and the notification
  text reflects how far ahead it fires (e.g. "Brahma Muhurta starts in 30 minutes"). See
  [docs/adr/0002-per-event-muhurta-reminder-offsets.md](docs/adr/0002-per-event-muhurta-reminder-offsets.md).

### Changed
- **Selectors are now dropdowns.** The single-choice pickers throughout the app — the Muhurat profile
  and search-window selectors, the Kundali/Rashifal/Matchmaking profile pickers, the Rashifal rashi
  picker, the Japa mantra picker, the Meditation duration, the Profile relation/gender fields, and the
  Settings theme — are now a shared expandable dropdown (`VedicSelectField`) instead of hand-rolled
  chip/segmented rows. This reads consistently and scales to long lists. (The Location and
  profile-primary lists stay as inline radio lists, since those screens manage the items in place.)
- **Smaller release builds.** Release builds now run R8 code shrinking/obfuscation and resource
  shrinking (`isMinifyEnabled` + `isShrinkResources`, with an app `proguard-rules.pro`), and ship only
  English resources (`resourceConfigurations`), dropping the dozens of translations AndroidX/Material
  bundle. The `material-icons-extended` dependency was also dropped — the app only uses a handful of
  common icons, all of which live in `material-icons-core` (supplied via material3) — removing a large
  icon library from the graph. Debug builds are otherwise unchanged, so the CI gate (which builds
  `assembleDebug`) is unaffected — the size win applies to `assembleRelease`/`bundleRelease`. Publish
  the App Bundle (`bundleRelease`) so Play delivers per-device (ABI/density/language) splits and users
  download only their slice.
- **Matchmaking — more accurate Gana koota and dosha cancellations.** The Gana koota now uses the
  standard asymmetric groom/bride table (same 6; Deva–Manushya 5; groom-Manushya + bride-Rakshasa 1;
  other Rakshasa pairings 0) instead of a symmetric one. Nadi and Bhakoot doshas now apply their
  classical cancellation (parihara) rules — Bhakoot cancels under a shared or friendly sign lord;
  Nadi cancels for same-nakshatra-different-pada, same-rashi-different-nakshatra, or
  same-nakshatra-different-rashi — so a cancelled clash keeps its lost points but drops the warning.
  (Yoni and Vashya keep their documented simplified tables; their full classical matrices vary by
  lineage and are only published as images.)
- **New hub tile artwork and larger tile icons.** The Today's Panchang tile now shows a lotus
  zodiac-wheel mandala, Muhurat a mangal kalash (sacred pot with mango leaves), Rashifal the twelve
  rashis in a zodiac wheel, Meditate a seated figure with the chakras, Calendar a mandala-bordered
  almanac page with Om marks and a turning leaf, Events a woodcut almanac page with a central Om and
  endless-knot corners, and the Match tile a clasped-hands (hastamelap) motif with a mehndi mandala —
  replacing the marigold panchang glyph, the kalash-outline muhurat glyph, the simple meditate figure,
  the plain date-range calendar icon, the plain event icon, and the placeholder Star/heart icons. All
  hub-tile icons are also enlarged to fill their chip.
- **Bottom navigation is now Home · Settings · Profile.** Panchang and Reminders left the bottom bar
  (they're already tiles on the Home hub) and Profile joined it as a first-class tab. The Home hub's
  Daily / Astrology / Devotion tabs were removed too — every shortcut now sits on one screen, each
  tile still tinted by its category. Panchang, Reminders, Kundali and the location/profile-edit
  screens open as pushed sub-routes with a back arrow in the top bar.
- **Festivals and Events are now their own full, tappable lists.** The Home hub's Festivals tile opens
  the complete list of upcoming festivals and Sankrantis, and a new Events tile opens the full list of
  lunar observances (Amavasya, Purnima, Ekadashi) — each row tappable for its significance, with a
  Set-reminder button on recurring observances. The upcoming-festivals peek moved onto the hub landing,
  and both lists were removed from the Today's Panchang detail screen (which keeps the day's limbs,
  sun/moon, planetary positions and auspicious/inauspicious periods). The Panchang shortcut is renamed
  "Today's Panchang", and Muhurat is now marked coming soon until it's implemented.
- **Reminder lead time reads as a label until you edit it.** Each reminder card now shows its lead
  time as compact text (e.g. "Remind 2 hours before" / "Remind at start") and expands to the
  number-and-unit editor only when tapped — lighter to scan and less prone to accidental edits.
- **The Calendar screen is now "Panchang."** The tab and screen were renamed to reflect what they
  show; the bottom navigation reads Home · Panchang · Reminders · Settings.
- **License changed from MIT to a dual license** — GNU AGPL-3.0-or-later for open-source use, plus a
  commercial license for proprietary use. See [LICENSING.md](LICENSING.md) and
  [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
- **Toolchain lifted to AGP 9 / Gradle 9 / Kotlin 2.3.** Gradle 9.6.1, AGP 9.3.1, Kotlin 2.3.10 +
  KSP 2.3.10 (now using AGP's built-in Kotlin — the standalone `kotlin-android` plugin was removed),
  and Hilt 2.60.1. Migrated the astronomy/scheduler ports from `kotlinx.datetime.Instant` to the
  stdlib `kotlin.time.Instant`.

### Fixed
- **System back now returns to the hub from the Festivals, Events and Panchang views.** These open
  inside the Home tab (not as pushed routes), so the app-level back handler couldn't act on them and
  the device back button exited the app. Back from any of them now returns to the Home hub.
- **System back now navigates within the app instead of exiting.** Pressing the device back button
  from any screen other than the Home landing could close the app straight to the Android home screen
  (the platform's predictive-back path bypassing the in-app navigation). Back is now handled
  explicitly: it retraces the in-app journey and only leaves the app when you're already on the Home
  landing.
- **The Reminders permission banners no longer stick around after they're granted.** The "Allow exact
  alarms" and "Allow full-screen alarms" prompts were read once (the exact-alarm grant when the screen
  loaded, the full-screen grant during composition), so after the user granted a permission — or came
  back to the screen once an alarm had fired — the banner could still be showing a stale "not granted"
  state. Both are now re-checked on every screen resume, so a banner disappears the moment its
  permission is granted and never lingers.
- **The bottom-nav Home tab works after opening a shortcut.** After tapping a Home-hub tile (Panchang,
  Reminders, Kundali, …) and then switching tabs, tapping Home did nothing: because the shortcut
  screens are pushed at the graph root, Home's saved back stack included the shortcut, and the tab
  jump *restored* it instead of showing the Home landing. Tab navigation no longer saves/restores that
  state — it pops to the start destination — so every tab always lands on its own root.
- **The full-screen alarm no longer lingers after the ring stops.** When an alarm-mode reminder was
  stopped through the notification's Dismiss action or the two-minute auto-stop, the ringing service
  stopped but the lock-screen `AlarmActivity` was never told to close, so its full-screen page (with
  the Dismiss button) could stay on top of the app until its own timeout. Stopping the alarm now
  routes through the shared `AlarmRinger`, which fires a one-shot callback the activity uses to finish
  itself — so the alarm screen closes on every dismiss path, not just its own Dismiss button.
- **Returning Home from a hub tile no longer gets stuck.** Opening Panchang or Reminders from a Home
  tile and then tapping Home could freeze on a blank screen: the tiles pushed onto destinations that
  were also bottom tabs, so the save/restore state of the two navigation paths collided. Those
  screens are now plain pushed sub-routes and every tab jump goes through a single
  `navigateToTab` helper, so back and tab switching behave consistently.
- **Reminders now survive an app update, and alarm-mode reminders always surface.** Reinstalling the
  APK (like a reboot) clears pending alarms, but `BootReceiver` only re-armed on `BOOT_COMPLETED`, so
  after an update a reminder silently never fired until the Reminders screen was reopened. It now also
  re-arms on `MY_PACKAGE_REPLACED`. And the alarm receiver posts the alarm notification before starting
  the ringing foreground service (and guards the start), so the alarm is visible even if the platform
  refuses the background foreground-service start.
- **The day's tithi is now named by its sunrise, not local noon.** On days where the tithi rolls
  over between sunrise and midday, sampling at noon read one tithi ahead of published panchangas —
  e.g. 9 August 2026 showed Krishna Dwadashi where published almanacs show Krishna Ekadashi (the
  tithi prevailing at sunrise, by which the day is named). Home and Panchang now resolve the day's
  sunrise (`AstronomyEngine.sunriseAt`) and sample the panchanga identity there, so they agree with
  each other and with reference panchangas. (Tithi is Moon−Sun elongation and so ayanamsa-independent;
  this was a sampling-instant convention, not a computation error.) See
  [docs/adr/0004-golden-theme-and-calendar.md](docs/adr/0004-golden-theme-and-calendar.md).
- **Muhurta reminders could not be set once a window had passed.** The reminders screen only ever
  computed *today's* windows, so by later in the day every row showed "already passed" with a
  disabled toggle. It now resolves each muhurta's **next upcoming** occurrence — today's if still
  ahead, otherwise tomorrow's (labelled "Tomorrow") — so a reminder can always be toggled on, and
  each time the screen opens it renews already-enabled reminders onto their next occurrence.

### Notes
- Held the latest androidx/Compose line: it requires compiling against **compileSdk 37**, which is
  currently only a preview (`android-CANARY`). The project stays on stable compileSdk 36 and adopts
  those libraries once API 37 ships as a stable platform. See
  [docs/migration/agp-9-migration.md](docs/migration/agp-9-migration.md).

## [0.1.0] - 2026-08-01

### Added
- **Phase 1 — repository foundation.**
  - Gradle (Kotlin DSL) build with a centralized version catalog and `build-logic/` convention
    plugins (`vedicmitra.android.application/library/compose/hilt/feature`, `vedicmitra.kotlin.library`).
  - Modular, feature-first Clean Architecture skeleton: `:app`; `:core:common`, `:core:ui`,
    `:core:designsystem`, `:core:astronomy`, `:core:scheduler`, `:core:notifications`,
    `:core:location`; `:feature:home`, `:feature:settings`, `:feature:alarm`.
  - Hilt DI scaffolding (application root, `@HiltViewModel` skeletons) and a Material 3 design
    system (`VedicMitraTheme`) with light/dark and dynamic colour.
  - Contract-only ports for astronomy, scheduler, notifications, and location (no implementations).
  - Quality tooling: Detekt, Spotless, Ktlint, and a shared `.editorconfig`.
  - GitHub Actions CI (Android SDK setup → Spotless → Detekt → unit tests → assemble APK, APK +
    test-report artifacts), a tag-triggered Release workflow that publishes an installable APK,
    Dependabot, and issue/PR templates.
  - IDE-optional workflow: VS Code settings, recommended extensions, and build tasks
    (`.vscode/tasks.json`) so the app builds without Android Studio.
  - Documentation: README, CONTRIBUTING, CODE_OF_CONDUCT, AGENTS.md, and `docs/` (architecture,
    getting started incl. Android-Studio-free setup, module guide, ADRs).
  - Gradle 8.14 wrapper.

### Notes
- Business logic, astronomy calculations, and alarm behaviour are intentionally **not** included in
  this release — see the roadmap in the README.

[Unreleased]: https://github.com/imskylab/vedic-mitra/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/imskylab/vedic-mitra/releases/tag/v0.1.0
