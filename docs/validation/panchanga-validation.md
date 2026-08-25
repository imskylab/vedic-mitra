<!--
  Copyright (c) 2026 Jayvardhan Potabatti
  SPDX-License-Identifier: AGPL-3.0-or-later
-->

# Panchanga & Kundali validation pass

A repeatable way to check the app's astronomy against an authoritative reference. Two halves:

- **Automated** — two tests in `:core:astronomy`, both in the normal gate:
  - `EngineValidationHarnessTest` asserts the parts fixed by public astronomy (the Sun's sidereal
    sign by the Sankranti calendar, and the Lahiri ayanamsa for the app's era).
  - `JplReferenceChartTest` checks the natal-chart engine against **NASA JPL HORIZONS** (DE441) — the
    authoritative free ephemeris the paid sites use internally. Geocentric apparent longitudes were
    pulled for a few reference instants (famous birthdays + a synthetic case), converted to
    Lahiri-sidereal, and baked in as golden rashi / nakshatra / lagna values. Because the app and
    HORIZONS are compared at the *same* instant, birth-time uncertainty is irrelevant — it validates
    the engine, not a natal reading.

  Run them directly with:

  ```
  .\gradlew.bat :core:astronomy:testDebugUnitTest --tests *EngineValidationHarnessTest* --tests *JplReferenceChartTest*
  ```

  If a HORIZONS case fails, that's a genuine finding about the low-precision ephemeris — likely a
  planet drifting across a rashi edge at an old epoch. Re-derive the golden or relax that case rather
  than assuming the reference is wrong; HORIZONS is the ground truth here.

- **On-device** — this checklist: enter the reproducible cases below into a reference panchanga and
  into the app, then compare the fast-moving lunar limbs and the Kundali fields by hand.

## Reference sources

Pick **two** independent published panchangas — an established online almanac and a printed one, or
two online ones from different publishers — and note where they disagree. They occasionally differ by
a locale or ayanamsa convention, and seeing that disagreement is the point: a limb where two
references disagree is not a limb the app can be judged wrong on.

Whichever you pick, it must expose a **daily panchang** view and a **birth chart / kundali** view, and
let you set the location and the ayanamsa. Set the location to **New Delhi** and the ayanamsa to
**Lahiri (Chitrapaksha)** so the comparison is apples-to-apples with the app's built-in default.

This document deliberately does not name or link particular services. Which almanac you compare
against is your choice; what matters is that it is independent of this project, that you record which
one you used in the PR or issue, and that its ayanamsa matches.

## How to read a match

- **Tithi, nakshatra, yoga, karana, vara** — should match the reference **exactly** when both are
  sampled at the day's sunrise (the app names the day by its sunrise tithi, as published panchangas
  do). If they differ, first check you're comparing the same sunrise day, not a mid-day rollover.
- **Sun / Moon sign (rashi)** — should match exactly except within a few hours of a sign change.
- **Sunrise, sunset, moonrise, moonset** — the app uses a low-precision ephemeris; treat within
  **~5 minutes** as a pass, up to ~10 near the horizon extremes.
- **Lagna (ascendant)** — very sensitive to the exact birth time; a whole-sign difference means the
  time or place is off; a boundary case (near a rashi edge) can legitimately differ by a source.

## A — Today's panchanga (repeatable)

Open the app's Home → Today's Panchang for your location, and the reference for the same date/location.

| Field | App | Reference A | Reference B | Match? |
| --- | --- | --- | --- | --- |
| Vara (weekday) | | | | |
| Tithi (paksha + name) | | | | |
| Nakshatra | | | | |
| Yoga | | | | |
| Karana | | | | |
| Sunrise / Sunset | | | | |
| Moonrise / Moonset | | | | |
| Sun rashi | | | | |
| Moon rashi | | | | |
| Maasa (amanta) | | | | |
| Samvatsara | | | | |
| Ayana / Ritu | | | | |

## B — Fixed reference days (sanity anchors)

These have known-good values already asserted in the engine tests — use them to confirm the app
build itself is sound. Location: **New Delhi**.

### 2026-08-05 (worked example — expected values filled in)

| Field | Expected (cross-checked) | App | Match? |
| --- | --- | --- | --- |
| Ayana | Dakshinayana | | |
| Ritu | Varsha (monsoon) | | |
| Maasa | Ashadha (amanta, not adhika) | | |
| Samvatsara | Parabhava (Shaka 1948) | | |
| Moonrise / Moonset (IST) | 23:04 / 11:52 | | |

### 2024-01-15 (worked example — expected values filled in)

| Field | Expected | App | Match? |
| --- | --- | --- | --- |
| Tithi | Shukla Panchami (5) | | |
| Nakshatra | #25 (Purva Bhadrapada) | | |
| Yoga | Variyana (18) | | |
| Karana | Balava | | |

## C — Kundali (birth chart)

Reproducible synthetic birth so anyone can regenerate it: **1 Jan 2000, 06:00 IST, New Delhi**
(28.6139° N, 77.2090° E). Enter it in the app (Profile → add) and in each reference's chart generator.

| Field | App | Reference A | Reference B | Match? |
| --- | --- | --- | --- | --- |
| Lagna (ascendant rashi) | | | | |
| Moon nakshatra + pada | | | | |
| Moon rashi (Chandra) | | | | |
| Sun rashi | | | | |
| Each graha's rashi (Su…Ke) | | | | |
| Retrograde grahas | | | | |
| Current mahadasha lord | | | | |

## D — Muhurat spot-check

Not a numeric match, but a sanity pass on the personalised ranking:

1. Pick an activity (e.g. Griha Pravesh) and note the top few ranked days on **General**.
2. Select a chart-ready profile. The order and scores should shift, and each day's reasons should now
   include a **Tarabala** ("Favourable/Weak tara …") and, when the day's Moon sign is known, a
   **Chandrabala** line.
3. Confirm the auspicious days avoid the universal doshas (Amavasya, Rikta tithis, Vyatipata/Vaidhriti
   yoga, Vishti/Bhadra karana) — these should show as unfavourable reasons when present.

## E — Matchmaking (Guna Milan)

Pick a male and a female profile and compare the app's Ashtakoota total to a free calculator (e.g.
any published "Ashtakoot Guna Milan" calculator) for the same two Moon nakshatras/signs.

- **Nadi, Bhakoot, Gana, Tara, Varna, Graha Maitri** should match a standard calculator. Gana uses
  the standard asymmetric groom/bride table, and Nadi/Bhakoot apply the classical dosha cancellations
  (parihara) — so a "dosha cancelled" note with the points still at 0 is expected, not a bug.
- **Yoni** and **Vashya** use documented but simplified tables (Yoni collapses the finer
  friendly/unfriendly gradations to neutral; Vashya omits the Dhanu/Makara half-sign split). Expect
  these two kootas to occasionally differ by a point or two from a full-table calculator — note any
  gaps so the tables can be refined.

## F — Rashifal (daily & weekly outlook)

The rashifal is **computed, not editorial**: each day is graded by **Chandrabala** (the transit Moon's
sign counted from the read rashi) and, when you read your own birth sign, **Tarabala** (the day's
nakshatra counted from your janma nakshatra). So validating it means confirming those two counts and
their favourable/weak classification against a reference — not matching prose.

Reference: any published **Tarabala & Chandrabala** table for the day, set to the same date and to
New Delhi. Such a table lists the favourable **Chandra Bala** rashis and the favourable **Tara Bala**
nakshatras — the two counts to check against. A per-sign daily rashifal from the same source gives a
looser sanity read, but it is editorial prose and cannot settle a disagreement; the two counts can.

Steps:

1. Open the app → **Rashifal** for your sign (the starred one). Note today's band, the "Moon in
   … (the Nth from …)" line, and the week strip.
2. On the reference Tarabala/Chandrabala table for the **same sunrise date + New Delhi**:
   - **Chandrabala** — your rashi should be listed favourable exactly when the app shows a strong
     position (1, 3, 6, 7, 10, 11) and unfavourable at 4 / 8 / 12. The app's "Nth from *your sign*" is
     the day's Moon sign counted from yours; confirm it matches the day's Moon rashi.
   - **Tarabala** (personalised read only) — for your janma nakshatra, the app's tara name and verdict
     (Sampat / Kshema / Sadhaka / Mitra / Ati-Mitra favourable; Vipat / Pratyari / Vadha weak; Janma
     neutral) should match the reference tarabala for the day.
3. **Week strip** — step the reference forward a day at a time and confirm each pip's colour (green auspicious /
   amber mixed / red challenging) tracks that day's Chandrabala (plus your Tarabala), i.e. follows the
   transit Moon moving ~1 sign every 2.25 days.
4. **Cross-sign sanity** — browse a sign whose Moon-transit position today is 4 / 8 / 12; it should read
   "a day for care". One at 1 / 3 / 6 / 7 / 10 / 11 should read favourable.

| Field | App | Reference | Match? |
| --- | --- | --- | --- |
| Today's Moon rashi | | | |
| Today's Moon nakshatra | | | |
| Chandra position (from my sign) | | | |
| Chandrabala verdict | | | |
| Tarabala (my janma nakshatra) | | | |
| Today's band (Auspicious/Mixed/Challenging) | | | |

Notes: the day's Moon sign/nakshatra are sampled at **sunrise** (the app's day-naming convention), so on
a day the Moon changes sign soon after sunrise the app can differ from a source reading the Moon at a
different clock time — confirm you're on the same sunrise day. Tarabala shows only on your own (starred)
sign; every other sign is Chandrabala-only by design, so their reading won't carry a tara line.

## G — Divisional charts (vargas)

Same synthetic birth as section C. The Charts section's chip row holds the lagna figure, the Chandra
figure, and sixteen vargas.

| Varga | App lagna sign | Reference | Match? |
| --- | --- | --- | --- |
| D-9 (navamsa) | | | |
| D-10 (dasamsa) | | | |
| D-12 (dwadasamsa) | | | |
| D-30 — *absent by design* | — | — | — |

Check a couple of graha placements per varga rather than the whole grid; if the lagna and two grahas
agree, the table behind it is right. **Expect disagreement near a division edge** — see the precision
ladder in `Varga.kt`, which runs from 0.0% at risk for D-3 to 8.3% for D-60. A mismatch is only
interesting if the graha sits well away from an edge.

## H — Dashas

| Check | App | Reference | Match? |
| --- | --- | --- | --- |
| Vimshottari: first mahadasha lord | | | |
| Vimshottari: its start date | | | |
| Current mahadasha → antardasha → pratyantardasha | | | |
| Ashtottari: first mahadasha lord | | | |
| Yogini: first mahadasha lord | | | |

Two known and deliberate divergences:

- **Ashtottari period boundaries** for a Moon in nakshatras 26, 27, 1 or 2 (Rahu's run, which wraps
  the end of the zodiac). The **lord will match**; the dates will not. Documented in
  `VimshottariCalculator.kt`.
- **Yogini's starting lord** between Ardra and Punarvasu moves back two rather than forward one. This
  reproduces the reference deliberately; if a *third* source disagrees with both, that is worth
  knowing and worth recording here.

Dasha dates are extremely sensitive to the Moon: one arcminute of difference moves every boundary in
the timeline by about three days. Judge a mismatch by that scale, not by the calendar.

## I — Ashtakavarga

| Check | App | Reference | Match? |
| --- | --- | --- | --- |
| Sarvashtakavarga per sign (12 numbers) | | | |
| Sarva total | 337 | 337 | must always be 337 |
| Binnashtakavarga for the Sun (12 numbers) | | | |

The total is the quickest check in this whole document: it is 337 for every chart ever cast, so if it
is not, something is wrong before any comparison is needed.

## J — Mangal dosha and the four porutham

Needs two profiles (section E's pair will do).

| Check | App | Reference | Match? |
| --- | --- | --- | --- |
| Mangal dosha present for each partner | | | |
| Which placement raised it (house + from what) | | | |
| Whether a parihara cancels it | | | |
| Mahendra / Vedha / Rajju / Sthree Dheerga | | | |

The app shows **every trigger and every cancellation**, so compare the working and not just the
verdict. Sources differ on whether the 1st and 2nd houses count and on which parihara apply; a
different verdict with the same placements listed is a convention difference, not an error.

## Known limitations (expected, not bugs)

- **Fine vargas near a division edge.** D-40, D-45 and D-60 divide a sign into 45, 40 and 30
  arcminutes; this engine's longitudes are good to about an arcminute, and two independent
  ephemerides can differ by nearly five. Roughly one D-60 placement in twelve is not decidable by
  comparison at all. Below about D-24 the **birth time** matters more than the arithmetic does.
- **Ashtottari boundaries in Rahu's run** (nakshatras 26, 27, 1, 2) — lord right, dates ours.
- **No degree-based house cusps.** Houses are whole-sign throughout, so a chart drawn with bhava
  chalit cusps will place some grahas in a different house. That is a different house system, not a
  disagreement.

- The Lahiri ayanamsa uses a **linear fit** (`23.853 + 1.397·t` centuries); it's accurate to a couple
  of arc-minutes near the present but drifts for dates far from now.
- The ephemeris is **low-precision**: rise/set times land within a few minutes, and a tithi/nakshatra
  can flip a source's naming when the boundary falls very close to sunrise.
- Empirical/USP refinements are **not** applied in-app by design; this pass validates the classical
  engine only.

## Logging results

Record a dated run at the bottom (date, app version/commit, which cells failed and by how much) so
drift is visible over time.
