# Contributing to Vedic Mitra

Thanks for your interest in improving Vedic Mitra! This guide covers how to set up, the workflow,
and the standards we hold PRs to. For the full, detailed conventions (naming, Compose, DI, testing)
see [AGENTS.md](AGENTS.md).

By participating, you agree to our [Code of Conduct](CODE_OF_CONDUCT.md).

## Getting set up

1. Install **JDK 21** and the **Android SDK** (platform API 36).
2. Clone the repo and let Gradle sync (Android Studio) or run `./gradlew help`.
3. Verify your environment:

   ```bash
   ./gradlew spotlessCheck detekt testDebugUnitTest assembleDebug
   ```

## Workflow

1. **Find or open an issue.** For anything non-trivial, discuss the approach first.
2. **Branch** from `main` using a descriptive name:
   - `feat/<short-description>` for features
   - `fix/<short-description>` for bug fixes
   - `chore/`, `docs/`, `refactor/`, `test/`, `ci/` as appropriate
3. **Make focused commits** following [Conventional Commits](#commit-messages).
4. **Keep quality gates green** locally before pushing.
5. **Open a Pull Request** using the template. Link the issue it closes.
6. Address review feedback; keep the branch up to date with `main`.

## Coding standards

- Follow the module boundaries and layer rules in [AGENTS.md](AGENTS.md) — features never depend on
  other features; depend on `:core:*` ports.
- Public APIs get **KDoc**.
- No wildcard imports; max line length **120**.
- New configuration/versions go in the **version catalog** (`gradle/libs.versions.toml`), never
  hard-coded in a module.
- Reuse the **convention plugins** in `build-logic/`; don't copy build config between modules.

## Quality gates

All of these must pass (CI enforces them):

```bash
./gradlew spotlessCheck   # formatting (auto-fix with spotlessApply)
./gradlew detekt          # static analysis
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## Commit messages

We use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<optional scope>): <description>

[optional body]

[optional footer(s)]
```

- **types**: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`
- **scope**: the module or area, e.g. `feat(home): …`, `fix(core-astronomy): …`
- Keep the subject in the imperative mood, ≤ 72 characters.

Examples:

```
feat(alarm): add recurring alarm scheduling
fix(designsystem): correct dark-theme surface colour
docs: expand module guide with dependency rules
```

## Definition of Done

A change is "done" when:

- [ ] It fits the correct architectural layer and module.
- [ ] Public APIs are documented with KDoc.
- [ ] `spotlessCheck`, `detekt`, and unit tests pass; `assembleDebug` succeeds.
- [ ] Tests cover the new/changed behaviour.
- [ ] UI changes include light + dark screenshots and use design-system tokens.
- [ ] Commits follow Conventional Commits and the PR uses the template.

## Reporting bugs / requesting features

Use the [issue templates](.github/ISSUE_TEMPLATE). Include reproduction steps, environment, and
logs for bugs.
