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

## Update (2026-08-09) — ring via a foreground service

Device testing on Android 15 (Poco M7) confirmed the predicted gap: an alarm-mode reminder fired
and posted its notification, but did **not** ring — the sideloaded app had not been granted the
Android 14+ full-screen-intent permission, so the system demoted the full-screen intent to a plain
notification and `AlarmActivity` (which owned the sound) never launched. Because the ringtone lived
in the activity, no activity meant no sound.

Fix: move the ringtone into a foreground service so sound no longer depends on the full-screen
intent.

- **`AlarmService` (`:core:alarm`)** now owns `AlarmRinger`. On fire, `ReminderReceiver` starts it
  with `ContextCompat.startForegroundService` (permitted from the exact-alarm broadcast, which
  temporarily exempts the app from background-FGS-start limits). The service `startForeground`s with
  the ongoing full-screen-intent notification built by `AlarmAlert.notification(...)`, rings until
  Dismiss or the ~2-min timeout, and is declared `foregroundServiceType="specialUse"` (subtype
  `alarm`) with `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE`.
- **`AlarmActivity`** is now only the lock-screen UI; it no longer creates a ringer. Its Dismiss and
  the notification's new **Dismiss** action both stop the service. The full-screen intent still
  launches the activity when the permission is granted, so a locked device shows the full-screen
  alarm as before — but the tone plays regardless.
- **Full-screen-intent permission.** The reminders screen now shows a banner
  (`Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`) when `NotificationManager.canUseFullScreenIntent()`
  is false, so the user can restore lock-screen surfacing. Sound works with or without it.
- **OEM note.** Aggressive vendors (MIUI/HyperOS, etc.) may additionally require the user to enable
  "Autostart" / "show on lock screen" for reliable ringing; these are user-only settings with no API.

This supersedes the "No foreground service" decision above.

### Follow-up (2026-08-09) — never show nothing

A second device test surfaced a worse symptom: nothing appeared at all. Two causes, both fixed:

- **Reminders were not re-armed after an app update.** Reinstalling the APK cancels pending alarms
  just like a reboot, but `BootReceiver` only listened for `BOOT_COMPLETED`, so an alarm set before
  the update never fired until the Reminders screen was reopened. `BootReceiver` now also handles
  `MY_PACKAGE_REPLACED`.
- **A failed foreground-service start could swallow the alarm.** `ReminderReceiver` now posts the
  alarm notification *before* starting `AlarmService` and wraps the start in `runCatching`, so even
  if the platform refuses the background FGS start the alarm is still visible; the service adopts the
  same notification id when it does start.
- **Sound no longer depends solely on the foreground service.** `AlarmRinger` is now a process-wide
  singleton with an idempotent `ensureRinging`/`stop`. Both `AlarmService` and `AlarmActivity` drive
  it, so the alarm rings whether the background FGS start succeeds *or* the full-screen intent (or a
  tap on the notification, whose content intent opens `AlarmActivity`) launches the activity —
  without ever double-playing. This restores a working sound path on OEMs that block background FGS
  starts, provided the full-screen-intent permission is granted (hence the banner). Reliable
  lock-screen ringing on such OEMs still also needs the user's Autostart / battery-unrestricted
  settings.
