# Getting Started

This guide gets you from a fresh clone to a running build. **Android Studio is not required** — the
project builds entirely from the command line (and on GitHub CI). Use whatever editor you like; a
lightweight VS Code setup is documented below.

## Choose your path

- **Build on GitHub only (zero local setup).** Push a branch — [CI](../.github/workflows/ci.yml)
  builds the APK and uploads it as an artifact you can download from the Actions run. Tag a commit
  (`v*`) and [the release workflow](../.github/workflows/release.yml) attaches an installable APK to
  a GitHub Release. You need nothing installed locally. Jump to [Editing without a full IDE](#editing-without-a-full-ide).
- **Build locally from the CLI.** Install a JDK and the Android SDK command-line tools (no Android
  Studio), then run the Gradle wrapper. Continue below.

## Prerequisites (local CLI build)

| Tool | Version | Notes |
| --- | --- | --- |
| JDK | **21** | Required — Gradle runs on it. Verify with `java -version`. Temurin recommended. |
| Android SDK command-line tools | latest | Standalone `cmdline-tools` — **no Android Studio needed**. |
| Android platform + build-tools | **API 36**, `build-tools;36.0.0` | Installed with `sdkmanager` (below). |
| Git | any recent | — |

### 1. Install a JDK 21

Any JDK 21 distribution works. For example, with a package manager:

```bash
# Windows (winget)
winget install EclipseAdoptium.Temurin.21.JDK
# macOS (Homebrew)
brew install temurin@21
# Linux (Debian/Ubuntu)
sudo apt-get install temurin-21-jdk   # or your distro's OpenJDK 21 package
```

### 2. Install the Android SDK command-line tools (no Android Studio)

1. Download **"Command line tools only"** from <https://developer.android.com/studio#command-tools>.
2. Unzip so the layout is `<sdk>/cmdline-tools/latest/bin/…`.
3. Set `ANDROID_HOME` to `<sdk>` and add the tools to `PATH`.
4. Install the packages this project needs and accept licenses:

```bash
sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"
sdkmanager --licenses
```

### 3. Point the build at your SDK

Set `ANDROID_HOME` (or `ANDROID_SDK_ROOT`), **or** create a `local.properties` file in the repo
root:

```properties
sdk.dir=/absolute/path/to/Android/sdk
```

> `local.properties` is git-ignored — never commit it.

## First build

The repository ships a Gradle **wrapper**, so you don't need Gradle installed.

```bash
# macOS / Linux
./gradlew help

# Windows (PowerShell / cmd)
.\gradlew.bat help
```

Then build the debug APK:

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`.

## Everyday commands

```bash
./gradlew projects            # list all modules
./gradlew testDebugUnitTest   # run unit tests
./gradlew spotlessApply       # auto-format code
./gradlew spotlessCheck       # verify formatting
./gradlew detekt              # static analysis
./gradlew clean               # remove build outputs
```

Or use the helper scripts:

```bash
scripts/format.sh   # spotlessApply
scripts/check.sh    # spotlessCheck + detekt + unit tests
```

(On Windows use `scripts\format.bat` and `scripts\check.bat`.)

## Editing without a full IDE

You don't need Android Studio to contribute. Options, lightest first:

- **In the browser** — press `.` on the GitHub repo (or go to `github.dev`) to edit in a web VS Code,
  commit, and let CI build. No local install at all.
- **VS Code** — open the repo root and install the recommended extensions (VS Code prompts you; see
  [.vscode/extensions.json](../.vscode/extensions.json)). Run builds from **Terminal → Run Task**
  (or `Ctrl+Shift+B`) using the tasks in [.vscode/tasks.json](../.vscode/tasks.json): *Assemble debug
  APK*, *Unit tests*, *Format*, *Quality gate*, *Clean*.
- **Any editor + terminal** — just use the `./gradlew` commands above.

To install the app on a device without an IDE:

```bash
./gradlew installDebug          # build + install on a connected device (adb)
# or copy app/build/outputs/apk/debug/app-debug.apk to the device and open it
```

> Prefer not to set anything up locally? Download the APK artifact from a CI run, or from a GitHub
> Release created by pushing a `v*` tag.

### Android Studio (optional)

Android Studio still works if you want it: open the repo root and let Gradle sync — it picks up the
wrapper and JDK 21 toolchain. It's entirely optional.

## Troubleshooting

- **"Cannot find a Java installation … matching languageVersion=21"** — install a JDK 21 (see step 1);
  Gradle auto-detects it. This happens when only a newer/older JDK is present and the auto-download
  fell back and failed.
- **"Unsupported class file major version" / toolchain errors** — same cause: ensure JDK 21 is
  available.
- **SDK location not found** — set `sdk.dir` in `local.properties` or `ANDROID_HOME`, and install
  `platforms;android-36` with `sdkmanager`.
- **Formatting failures in CI** — run `./gradlew spotlessApply` and commit the result.

## Next steps

- Read [architecture.md](architecture.md) and [../AGENTS.md](../AGENTS.md).
- See [module-guide.md](module-guide.md) before adding a module.
