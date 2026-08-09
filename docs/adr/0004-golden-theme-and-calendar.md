# 4. Golden brand theme and the calendar feature

- **Status:** Accepted
- **Date:** 2026-08-06

## Context

Two gaps needed closing together. First, the app only showed *today's* panchanga (on Home) — the
roadmap's "Hindu Calendar" calls for browsable calendar views. Second, the visual identity was a
Phase-1 placeholder (indigo/saffron/teal) that didn't match the app's ornate golden/maroon/bronze
emblem, and dynamic colour defaulted **on**, so on Android 12+ the wallpaper palette won and the
brand colours were essentially never seen. A calendar built on the placeholder theme would have
looked generic; the theme is a prerequisite for the calendar to look like *this* app.

## Decision

**Theme.** Replace the placeholder palette with a full golden/maroon/bronze Material 3 tonal scheme
(light and dark) drawn from the emblem, add a softly-rounded shape scale, and flip the
`dynamicColor` default to **false** in all three places that set it (the DataStore default, the
app-level initial `ThemeSettings`, and `VedicMitraTheme`'s parameter). The Settings toggle is
unchanged — users can still opt into wallpaper colour — but the brand palette is now the
out-of-the-box experience. Typography stays on the Material 3 default scale (no bundled font this
pass). Colour values are placed at Material 3 tonal positions and can be regenerated with the
Material Theme Builder; they live only in `:core:designsystem` and are consumed via `MaterialTheme`,
so Home/Alarm/Settings re-skinned with no code change.

**Calendar.** A new `:feature:calendar` module (mirroring `:feature:home`) renders a monthly grid —
each cell shows the day's tithi — with month paging and a detail card showing the selected day's
full panchanga. It reuses the existing `AstronomyEngine`, which already accepts an arbitrary
`Instant`, computing each day at that day's **sunrise** — the convention by which panchangas name
the day — and follows the established location-with-default-fallback pattern.

> Superseded detail: earlier revisions sampled each day at local noon. On days where the tithi
> rolls over between sunrise and midday, noon reads one tithi ahead of published panchangas
> (Drik/Date Panchang), which name the day by its sunrise tithi. Both Home and Panchang now resolve
> the day's sunrise via `AstronomyEngine.sunriseAt(...)` and sample the panchanga identity there, so
> they agree with each other and with reference panchangas.

**A lightweight engine method.** A month grid needs ~28–42 days of panchanga but only tithi +
moon phase per cell. Running the full `snapshotAt` for every cell would run its expensive
moonrise/moonset altitude search (48 samples/day) 42 times for data the grid never shows. So the
`AstronomyEngine` port gains `daySummaryAt(instant, location): PanchangaDaySummary` — tithi,
nakshatra, moon phase only, reusing the existing internal `tithiOf`/`nakshatraOf`/`moonPhaseOf`.
The grid uses `daySummaryAt`; only the selected day uses the full `snapshotAt`.

**Navigation.** With four top-level destinations (Home, Calendar, Reminders, Settings) the previous
TopAppBar text-button scheme didn't scale, so `:app` moved to a Material 3 bottom navigation bar
with standard single-top / save-and-restore-state per tab.

## Consequences

- **Positive:** the app finally looks like its emblem out of the box; the calendar delivers the
  core "browse the panchang" experience; the cheap `daySummaryAt` keeps month paging snappy; the
  bottom bar is a cleaner home for a growing set of destinations.
- **Positive:** adding `daySummaryAt` to the port is small and reuses existing internals — but it is
  a contract change, so every implementer (the real engine plus the test fakes in Home/Alarm/
  Calendar) must implement it.
- **Negative:** the concrete hex palette is a hand-placed first pass; it may want tuning against the
  emblem on real devices (light and dark) — the values are isolated in `Color.kt` for easy
  iteration.
- **Negative:** deferred for a follow-up — list/agenda and yearly calendar views, per-cell festival
  highlighting, and a custom display font. The calendar's detail card also duplicates Home's
  panchang rows rather than sharing a component, deliberately, to avoid coupling `:core:ui` to
  astronomy domain types; visual consistency comes from the shared theme, not a shared composable.
