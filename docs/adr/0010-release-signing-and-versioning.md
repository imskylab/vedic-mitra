# 10. Release signing and versioning for in-place updates

- **Status:** Accepted
- **Date:** 2026-08-07

## Context

The app needs to be distributed as an installable package such that installing a newer build
**updates** the already-installed copy instead of colliding with it. Android grants an in-place
update only when the new build shares the installed one's `applicationId`, is signed with the **same
certificate**, and carries a strictly higher `versionCode`. Until now `versionCode`/`versionName`
were hardcoded (`1` / `0.1.0`) in the app convention plugin, and there was no release signing config
— so `assembleRelease` produced an *unsigned* APK (uninstallable) and only `assembleDebug` was
signed, using the per-machine debug key (which cannot produce stable cross-machine updates).

Distribution targets both direct APK sideloading and Google Play.

## Decision

- **Version out of code.** `versionCode` / `versionName` move to a committed root
  `version.properties`, read by `AndroidApplicationConventionPlugin` via `appVersion()` (with
  `ProjectConfig` default fallbacks). A release is a one-line, code-free bump; version numbers are
  not secrets, so the file is tracked.
- **Guarded release signing.** `:app` defines a `release` signing config sourced from a gitignored
  `keystore.properties` (path + passwords), wired to `buildTypes.release`. The block is read **only
  if the file exists**, so contributors and CI without the keystore still build, test, and
  `assembleDebug`; release builds are left unsigned there rather than failing configuration.
- **Keystore ownership stays with the developer.** The keystore is generated manually with
  `keytool` (the developer chooses and holds the passwords); the repo never contains the `.jks`,
  `keystore.properties`, or any password. `.gitignore` already excludes `*.jks`, `*.keystore`, and
  `keystore.properties`. A committed `keystore.properties.example` documents the required keys.
- **Both outputs.** `:app:assembleRelease` yields a signed APK for sideloading/GitHub Releases;
  `:app:bundleRelease` yields an `.aab` for Play (with Play App Signing, the keystore is the upload
  key). Documented in `docs/RELEASING.md`.

## Consequences

- **Positive:** signed, updatable releases from any machine that holds the keystore; trivial version
  bumps; no secrets in the repo; CI unaffected because signing is optional at configuration time.
- **Negative — irreplaceable key (direct APK).** If the release keystore is lost, no future APK can
  update existing direct installs; users would have to uninstall/reinstall. Mitigated by the backup
  guidance in `RELEASING.md`. (Play App Signing softens this for the Play channel, where Google can
  reset a lost upload key.)
- **Follow-up (not in this change):** R8/minification for release is left disabled to avoid
  stripping reflection-based code without tested ProGuard rules; enabling it is a later task. A
  GitHub Actions release workflow that signs from CI secrets is also a possible follow-up.
