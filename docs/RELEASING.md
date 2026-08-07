# Releasing Vedic Mitra

This guide explains how to build an installable release that **updates** an already-installed copy
of the app rather than colliding with it.

## Why updates work (or don't)

Android will install a new build over an existing one only when all three hold:

1. **Same `applicationId`** — always `io.github.vedicmitra` (fixed in the app convention plugin).
2. **Same signing certificate** — every release must be signed with the *same* keystore. A build
   signed by a different key (including the auto-generated *debug* key, which differs per machine)
   is rejected with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, and the user has to uninstall first.
3. **A higher `versionCode`** — the new build's `versionCode` must be strictly greater than the
   installed one. Equal or lower is treated as a downgrade and refused.

So the two things you manage each release are the **keystore** (create once, reuse forever) and the
**version** (bump every time).

## One-time setup: create your release keystore

Generate a keystore and key. You choose the passwords — they are never stored in the repo.

```bash
mkdir -p keystore
keytool -genkeypair -v \
  -keystore keystore/release.jks \
  -alias vedicmitra \
  -keyalg RSA -keysize 2048 -validity 10000
```

`-validity 10000` (~27 years) keeps the key usable for the app's lifetime. **Back this file up and
remember the passwords** — losing the key means you can never ship an update that installs over
existing copies (for direct APKs) or, without Play key reset, to Play at all.

Then point the build at it by copying the template and filling in your values:

```bash
cp keystore.properties.example keystore.properties
```

```properties
storeFile=keystore/release.jks
storePassword=<your store password>
keyAlias=vedicmitra
keyPassword=<your key password>
```

`keystore.properties`, `*.jks`, and `*.keystore` are all gitignored — never commit them. The build
reads `keystore.properties` only if it exists; without it, `assembleDebug`, tests, and CI still work
and release builds are simply left unsigned.

## Each release: bump the version

Edit the committed [`version.properties`](../version.properties):

```properties
VERSION_CODE=2      # strictly greater than the last released value — never reuse
VERSION_NAME=0.2.0  # human-facing, semantic versioning recommended
```

## Build the artifacts

Two distribution outputs, both signed with the keystore above:

```bash
# Sideloadable APK (direct install / GitHub Releases):
./gradlew :app:assembleRelease
#   -> app/build/outputs/apk/release/app-release.apk

# Android App Bundle for Google Play:
./gradlew :app:bundleRelease
#   -> app/build/outputs/bundle/release/app-release.aab
```

## Install / update on a device (direct APK)

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

`-r` reinstalls, keeping the app's data. Because the key is the same and `versionCode` increased,
this updates the existing install in place.

## Google Play note

When you upload the `.aab` to the Play Console, enrol in **Play App Signing**. Google then holds the
final *app signing key* and re-signs each release; your `keystore/release.jks` becomes the *upload
key*. Updates on Play are automatic as long as `versionCode` increases — you never hand the APK to
users directly. Keep the upload key safe, but if it is ever lost Google can reset it (unlike the
direct-APK case, where the key is irreplaceable).
