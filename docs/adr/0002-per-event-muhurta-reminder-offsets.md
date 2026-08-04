# 2. Per-event muhurta reminder offsets

- **Status:** Accepted
- **Date:** 2026-08-04

## Context

Muhurta reminders (Brahma Muhurta, Abhijit Muhurta, Rahu Kalam, Yamaganda, Gulika Kalam) could fire
a configurable number of minutes before the window started, but the lead time was a single global
value shared by all five window types. Different windows warrant different lead times — e.g. Brahma
Muhurta benefits from a longer heads-up (time to wake and prepare) than Rahu Kalam (a shorter
just-in-time nudge is enough) — and changing the shared value only affected reminders enabled
afterwards, silently leaving already-scheduled alarms on the old lead time.

## Decision

Replace the single `leadTimeMinutes: Flow<Int>` / `setLeadTimeMinutes()` pair on
`ReminderRepository` with a sparse per-name override: `offsetMinutesByName: Flow<Map<String, Int>>`
and `setOffsetMinutes(name, minutes)`. Only muhurtas the user has explicitly customized get an
entry; anything absent falls back to `ReminderRepository.DEFAULT_OFFSET_MINUTES` (10), resolved in
`AlarmViewModel`, not in the repository. The map is persisted as a second `stringSetPreferencesKey`
in Preferences DataStore, encoded with a small delimiter-based codec (`MuhurtaOffsetCodec`)
mirroring the existing `ReminderCodec` pattern — deliberately not a JSON blob, and not one
`intPreferencesKey` per sanitized muhurta name, to avoid coupling `:core:datastore` to the literal
name strings owned by `:core:astronomy`.

`AlarmViewModel.setOffsetMinutes(name, minutes)` persists the override and then, if a reminder for
that name is currently enabled, immediately re-schedules it — a retroactive reschedule, not just a
default for future toggles. The generated notification body is now offset-aware (e.g. "Brahma
Muhurta starts in 30 minutes" vs "... is starting now") while still carrying the
auspicious/inauspicious tone the previous fixed copy conveyed.

This feature was never in a shipped release (the `[Unreleased]` changelog section never mentioned
the earlier global lead time either), so the old `reminder_lead_time_minutes` preference key is
simply abandoned rather than migrated.

`:core:scheduler` (`TaskScheduler`, `DefaultTaskScheduler`, `ReminderReceiver`) and
`:core:notifications` (`Notifier`) are untouched — they only ever see an absolute trigger `Instant`
and a pre-baked notification string, so this is entirely a `:core:datastore` + `:feature:alarm`
change.

## Consequences

- **Positive:** each muhurta type can have its own lead time; changing one no longer leaves stale
  already-scheduled alarms behind; adding a sixth muhurta name in the future needs no schema change
  (the store is sparse and name-keyed, not enumerated).
- **Positive:** the notification body communicates how far ahead of the event it fires, which the
  previous fixed copy didn't.
- **Negative:** the reminders screen now shows a lead-time selector per row instead of one shared
  control, a small increase in screen density.
- **Negative:** changing an offset for an enabled reminder now does two persisted writes (the
  override, then the re-scheduled reminder) instead of one; acceptable since both are cheap
  DataStore writes on a user-initiated action, not a hot path.
