# 3. Moonrise/moonset via numerical bisection, not Meeus's interpolation method

- **Status:** Accepted
- **Date:** 2026-08-05

## Context

Sunrise/sunset already has a closed-form solution (`SolarDay`): the Sun's declination changes
slowly enough over a day that a single hour-angle equation, evaluated once at local solar noon,
gives both crossings directly. The Moon has no such shortcut — its right ascension and declination
change fast and non-uniformly through the day, and its horizontal parallax (varying with distance,
roughly a degree — vastly more than the Sun's ~9″) means the rise/set altitude threshold isn't even
a fixed constant the way the Sun's -0.833° is.

Meeus's classical answer (*Astronomical Algorithms* ch. 15) evaluates the body's position at 0h on
the day before/of/after, then uses quadratic interpolation across those three points to estimate the
crossing — a technique designed for hand computation with a small, fixed number of position
evaluations. This project already had precedent for a different, more directly numerical technique:
`VarjyamCalculator`'s bisection search for a nakshatra-longitude crossing (added the same day).

## Decision

`LunarDay.moonTimes()` computes the Moon's topocentric altitude at 48 evenly-spaced samples across
the civil day, detects any sign change against a per-instant threshold, then bisects each bracket to
the exact crossing (40 iterations — far more precision than the low-precision ephemeris itself
warrants, but cheap). This trades a fixed, small number of position evaluations (Meeus's approach)
for roughly 50-100 (48 coarse samples + up to two ~40-iteration bisections) — computationally
irrelevant on a phone, and it sidesteps needing to correctly reproduce Meeus's specific
interpolation-with-correction procedure.

Supporting decisions, each cross-checked against drikpanchang.com for Delhi across 2026-08-02..08 (7
consecutive days, every rise/set matching within ~5 minutes):

- **New `Ephemeris` primitives**: the Moon's ecliptic latitude and distance (Meeus Tables 47.B and
  the R-column of 47.A, both verified against the PyMeeus reference implementation rather than
  recalled from memory), ecliptic→equatorial conversion, and Greenwich Mean Sidereal Time. None of
  these existed before; sunrise/sunset never needed them because the Sun's ecliptic latitude is by
  definition zero and its parallax is negligible.
- **Rise/set threshold**: `horizontalParallax - 50′` (34′ refraction + 16′ semi-diameter), applied to
  geocentric altitude directly — algebraically equivalent to computing topocentric altitude and
  comparing it to the Sun's fixed -0.833°, but avoids a separate topocentric-correction step.
- **Fixed 16′ semi-diameter**, not distance-varying — matches `SolarDay`'s existing fixed-0.833°
  convention; only parallax (the dominant, ~1°-scale effect) varies per instant.
- **`MoonTimes` nullability**: because the lunar day (~24h50m) is longer than the civil day, roughly
  once a month a civil day has two moonrises (or two moonsets) and the next has none of one kind.
  `MoonTimes` reports the *first* occurrence within the civil day and `null` if none — the same
  convention as `SunTimes`, rather than modelling every crossing.

## Consequences

- **Positive:** no new astronomical convention was invented — every constant and table is traceable
  to Meeus via the PyMeeus reference implementation, and every result is cross-checked against a
  live reference site rather than asserted from memory.
- **Positive:** the bisection technique is reusable for any future "find when a computed quantity
  crosses a threshold" problem (as `VarjyamCalculator` already demonstrated) without needing a
  body-specific interpolation formula each time.
- **Negative:** ~50-100 ephemeris evaluations per `moonTimes()` call versus Meeus's ~3 — negligible
  on-device, but worth knowing if this pattern is copied somewhere latency-sensitive.
- **Negative:** the day-boundary attribution (which civil day a rise/set "belongs to") can disagree
  with sites like drikpanchang.com by exactly one day near the monthly no-crossing gap — confirmed
  during cross-checking, not a bug, just a different convention (see `MoonTimes`'s KDoc).
