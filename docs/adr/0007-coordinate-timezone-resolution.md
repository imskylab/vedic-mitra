# 7. Offline coordinate-to-timezone resolution

- **Status:** Accepted (refines ADR 0006)
- **Date:** 2026-08-07

## Context

Phase 7 (ADR 0006) let the user pick a location, and gave each saved location an IANA `zoneId` used
to place day boundaries (sunrise, "today") in that location's local time. To stay offline and small,
that zone was a deliberate stop-gap: a **longitude-based fixed-offset guess** (`TimeZoneEstimator`,
e.g. `UTC+05:00`), with a manual IANA override on the coordinates screen.

Two problems with the guess:

- **No DST.** A fixed offset can't follow daylight-saving transitions, so half the year is wrong for
  any location that observes DST.
- **Wrong near meridian-crossing borders.** Political time-zone boundaries don't follow lines of
  longitude, so the 15°-per-hour estimate misnames zones for many real cities.

The panchanga's day boundaries depend on the *correct* local time, so for a location feature this is
a correctness gap, not a cosmetic one. The remaining question was how to resolve coordinates to a
true IANA zone **offline** (the app is offline-first; no network time-zone API).

## Decision

Resolve coordinates to an IANA zone with **`us.dustinj.timezonemap`** (`TimeZoneResolver` port,
`DefaultTimeZoneResolver` impl in `:core:location`). It does an offline point-in-polygon lookup
against OpenStreetMap-derived boundaries (timezone-boundary-builder). Once we have the true zone id,
DST is handled for free by `java.time` (`ZoneId` / `ZonedDateTime`), so no separate DST logic is
needed.

- **Per-point region load.** Rather than `forEverywhere()` (loads the whole world into memory),
  the resolver builds a `TimeZoneMap.forRegion` over a small ±0.5° box around the query point. That
  is enough for a point-in-polygon test and keeps init time and memory minimal. The work runs on the
  default dispatcher.
- **Fallback preserved.** For points with no polygon (open ocean), the resolver falls back to the
  longitude estimate from ADR 0006, so `TimeZoneEstimator` stays as a last resort and `resolve`
  never fails.
- **Where it's used.** City-search and custom-coordinate saves resolve the zone from the point; the
  coordinates screen's time-zone field became **optional** ("leave blank to auto-detect"), with a
  validated IANA id as an override. The device-location path continues to use the OS zone
  (`ZoneId.systemDefault()`), which is already authoritative for the device.
- **Android packaging.** `timezonemap`'s default `zstd-jni` is a JVM-only jar; on Android we exclude
  it and add the `@aar` variant that ships the native `.so` files.

## Consequences

- **Positive:** correct time zones — and correct DST — anywhere on earth, fully offline. Distant
  saved locations now compute the right day boundaries year-round.
- **Positive:** DST needs no bespoke code; it rides on `java.time` once the zone id is right.
- **Negative — APK size.** The boundary data (`timezonemap-data`) is ~25 MB and cannot be shrunk by
  R8 (it is data, not code), plus the Esri geometry and zstd-jni dependencies. This is the main cost
  and was accepted deliberately in favour of no-compromise accuracy; a regionalized data build could
  shrink it later if size becomes a concern.
- **Negative — native dependency.** `zstd-jni` adds native libraries per ABI; the `@aar` wiring is
  Android-specific, and older `.so` files may need attention for Play's 16 KB-page requirement when
  the app is eventually published.
- **Licensing.** The boundary data is **ODbL** (OpenStreetMap contributors) and the library code is
  MIT; both are recorded in [THIRD-PARTY-NOTICES.md](../../THIRD-PARTY-NOTICES.md).
- The `TimeZoneEstimator` longitude heuristic is now a fallback only, not the primary path.
