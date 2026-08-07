# 9. Ringing-alarm reminders

- **Status:** Accepted
- **Date:** 2026-08-07

## Context

Muhurta reminders (ADR-era `:core:scheduler` + `:core:notifications`) fire as a heads-up
notification: good for a nudge, but it doesn't reliably *wake* someone — a notification doesn't ring
continuously and, by default, doesn't pierce Do Not Disturb. Users asked for a second, per-reminder
option: a real ringing alarm for the muhurtas they want to wake up for (e.g. Brahma Muhurta), while
keeping quiet notifications for the rest.

Two ways to make an alarm ring:

- **Hand off to the system Clock** via `AlarmClock.ACTION_SET_ALARM`. Rejected: it clutters the
  user's Clock app with app-created alarms, gives no control over content/branding/behaviour (the
  Clock's UI rings, not ours), can't be reliably updated or cancelled programmatically, and can't
  model muhurta times, which are **dynamic** (recomputed daily per location).
- **Build our own** with the standard full-screen-intent pattern. Chosen.

## Decision

Add a per-reminder `AlertStyle` (`NOTIFICATION` | `ALARM`), persisted per muhurta name
(`ReminderRepository.alertTypeByName`, mirroring the lead-time overrides) and carried on
`AppNotification` → the broadcast intent → `ReminderReceiver`.

- **Scheduling.** Alarm-mode reminders schedule with `AlarmManager.setAlarmClock()` — the
  highest-priority exact alarm, exempt from Doze, and (unlike `setExactAndAllowWhileIdle`) **not**
  gated on the `SCHEDULE_EXACT_ALARM` permission on API 31+. So the alarm path is actually more
  reliable than the notification path. Notification-mode keeps `setExactAndAllowWhileIdle`.
- **Ringing.** A new `:core:alarm` module. On fire, `ReminderReceiver` calls `AlarmAlert.raise`,
  which posts a **full-screen-intent** notification on a high-importance channel. On a locked or
  sleeping device the system launches `AlarmActivity` directly; otherwise it shows as a heads-up the
  user taps to open it. `AlarmActivity` surfaces over the lock screen (`showWhenLocked` /
  `turnScreenOn`, reinforced by window flags for API 26), and `AlarmRinger` plays the **system
  default alarm ringtone on a loop on the alarm audio stream** (bypassing ringer mute / DnD) plus
  vibration, until the user taps **Dismiss** or a safety timeout (~2 min) elapses.
- **No foreground service.** The activity owns the sound, so there is no foreground service, no
  background-FGS-start timing to manage, and no `FOREGROUND_SERVICE_*` permissions. Permissions are
  just `USE_FULL_SCREEN_INTENT` and `VIBRATE`.
- **Dependencies:** `:core:scheduler` → `:core:alarm` → `:core:notifications`. Features never depend
  on `:core:alarm`; the receiver drives it.

## Consequences

- **Positive:** a true wake-you alarm, per reminder, reusing the OS ringtone; full control over the
  UI and behaviour; alarm scheduling that sidesteps the exact-alarm permission entirely.
- **Negative — device-verified, not unit-tested.** The service-less ringing path is platform-heavy
  (activity lifecycle, `MediaPlayer`, vibration, lock-screen flags, full-screen intent) and can't be
  meaningfully unit-tested; only the plumbing (alert style through persistence/intent/scheduler
  branch) is covered. It needs a real device to verify the ring, lock-screen surfacing, and Dismiss.
- **Negative — unlocked, active device.** A full-screen intent auto-launches the activity only when
  the device is locked or the screen is off. If the phone is unlocked and in active use, the alarm
  degrades to a heads-up notification the user taps to start the ring. This covers the primary
  wake-me case; a foreground service that rings independently of the activity would close the gap and
  is a possible follow-up.
- **Negative — full-screen-intent policy.** Android 14 restricts `USE_FULL_SCREEN_INTENT` for
  non-calling/alarm apps; an alarm app qualifies, but the grant may need requesting on some devices.
- Snooze is intentionally out of scope for this first cut (Dismiss only).
