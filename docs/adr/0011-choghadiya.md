# 11. Choghadiya muhurta windows

- **Status:** Accepted
- **Date:** 2026-08-08

## Context

Choghadiya is one of the most widely used muhurta systems in Indian day-planning: the day and night
are each split into eight parts, each named one of seven Choghadiya whose nature (good/neutral/bad)
tells you whether a time is favourable. The app already computes the sun/weekday muhurtas (Rahu
Kalam, Abhijit, etc.) and has accurate sunrise/sunset, so Choghadiya is a natural, self-contained
addition — and it will become a reminder source in the reminders redesign.

## Decision

Add a pure `choghadiyaOf(sunTimes, nextSunrise, dayOfWeek)` calculator (mirroring `muhurtasOf`) and
expose the result as `AstronomySnapshot.choghadiya`.

- **Two halves of eight.** The **day** (sunrise→sunset) and the **night** (sunset→the *following*
  sunrise) are each divided into eight equal windows. The engine resolves the next day's sunrise
  specifically to bound the night half. The final window of each half is clamped to the exact
  boundary so the halves tile without an integer-division gap.
- **Fixed cyclic order.** The seven Choghadiya repeat in the order **Udveg → Char → Labh → Amrit →
  Kaal → Shubh → Rog** (the `ChoghadiyaName` enum's declaration order). Each half steps through this
  cycle from a starting index:
  - **Day start** = `(dayOfWeek × 3) mod 7`
  - **Night start** = `((dayOfWeek × 3) + 5) mod 7`

  This reproduces the traditional vaar tables — e.g. Sunday day begins **Udveg** and night begins
  **Shubh**; Monday day begins **Amrit**; Saturday day begins **Kaal**. The per-weekday day and
  night start names are asserted in `ChoghadiyaCalculatorTest`.
- **Nature under a binary model.** Amrit, Shubh and Labh are auspicious; Udveg, Kaal and Rog are
  inauspicious. **Char** ("chal", movable) is traditionally *neutral* but generally usable — with the
  app's binary `MuhurtaQuality` it is classified **auspicious**. If a three-value nature (good /
  neutral / bad) is wanted later, `ChoghadiyaName.quality` is the single place to change.
- **Polar safety.** Returns an empty list when the sun does not both rise and set, or the next
  sunrise is unknown.

## Consequences

- **Positive:** an accurate, fully unit-tested Choghadiya table with no new astronomy — it reuses the
  existing `SolarDay` sunrise/sunset. Available to the reminders feature and, later, Home/Calendar.
- **Neutral:** `AstronomySnapshot.choghadiya` defaults to empty, so existing lightweight/synthetic
  snapshots (test fakes, previews) compile unchanged; only the real engine populates it.
- **Negative — Char nuance lost.** Collapsing Char's "neutral" into auspicious is a simplification;
  revisit if a neutral tier is added.
