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

Use both and note where they disagree (they occasionally differ by a locale/ayanamsa convention):

- **Drik Panchang** — <https://www.drikpanchang.com/> (Panchang, and "Kundali" for charts)
- **Prokerala** — <https://www.prokerala.com/astrology/panchang/> and
  <https://www.prokerala.com/astrology/birth-chart/>

Set the location to **New Delhi** and the ayanamsa to **Lahiri (Chitrapaksha)** so the comparison is
apples-to-apples with the app's built-in default.

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

| Field | App | Drik | Prokerala | Match? |
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

| Field | Expected (Drik-cross-checked) | App | Match? |
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
(28.6139° N, 77.2090° E). Enter it in the app (Profile → add) and in Drik/Prokerala's chart generator.

| Field | App | Drik | Prokerala | Match? |
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

## Known limitations (expected, not bugs)

- The Lahiri ayanamsa uses a **linear fit** (`23.853 + 1.397·t` centuries); it's accurate to a couple
  of arc-minutes near the present but drifts for dates far from now.
- The ephemeris is **low-precision**: rise/set times land within a few minutes, and a tithi/nakshatra
  can flip a source's naming when the boundary falls very close to sunrise.
- Empirical/USP refinements are **not** applied in-app by design; this pass validates the classical
  engine only.

## Logging results

Record a dated run at the bottom (date, app version/commit, which cells failed and by how much) so
drift is visible over time.
