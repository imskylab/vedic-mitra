# Contributing to Vedic Mitra

Thanks for your interest in improving Vedic Mitra! This guide covers how to set up, the workflow,
and the standards we hold PRs to. For the full, detailed conventions (naming, Compose, DI, testing)
see [AGENTS.md](AGENTS.md).

By participating, you agree to our [Code of Conduct](CODE_OF_CONDUCT.md).

## Contributor licensing

Vedic Mitra is **dual-licensed** — GNU AGPL-3.0-or-later plus a commercial license
(see [LICENSING.md](LICENSING.md)). **By submitting a contribution (a pull request,
patch, or similar), you agree that:**

1. your contribution is licensed under the **AGPL-3.0-or-later**, and
2. you grant the project maintainer a perpetual, irrevocable, worldwide,
   royalty-free right to also license and distribute your contribution under the
   project's **commercial license**.

This keeps the project open source while letting commercial licensing fund its
development. If you cannot agree to these terms, please do not submit a
contribution.

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

## Contributing knowledge, not just code

Much of this project is not code — it is claims about a tradition. Those have their own bar, set out
in **[docs/knowledge-standards.md](docs/knowledge-standards.md)**. Read it before contributing to
any knowledge domain.

The short version: every feature declares one of four modes.

- **Compute** — the app asserts a value it derived. Validated against an independent reference where
  one exists (see [the validation pass](docs/validation/panchanga-validation.md)); where none does,
  the rule itself is cited, and the result is never shown at the same confidence as a validated one.
- **Cite** — the app reports what a tradition holds. Needs a **named source as a field on the model,
  not a code comment**, and a voice that attributes ("traditionally", "is said to") rather than
  asserts. Where authorities disagree, say so instead of picking one.
- **Track** — the app records what the user did. Makes no claim about the world; stays on the device.
- **Teach** — the app explains an idea. Follow the `PanchangaPrimer` pattern: a closed enum and a
  total map, so that adding a concept without writing its copy breaks the build.

Two things that will get a change declined regardless of how well it is written: **crossing a
[red line](docs/knowledge-standards.md#red-lines)** — no medical claims, no fatalism, no instruction
in someone's practice, no remedy commerce — and **adding a knowledge claim with no declared mode**,
which is incomplete in the same way a change with no tests is.

Rules are derived from reference data rather than from memory. That is not ceremony: when the four
porutham were derived that way, two came out differently from the textbook they would otherwise have
been written from.

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
- [ ] Any knowledge claim declares its [mode](docs/knowledge-standards.md) and carries the backing
      that mode requires — a validated reference, a named source, or neither if it claims nothing.
- [ ] UI changes include light + dark screenshots and use design-system tokens.
- [ ] Commits follow Conventional Commits and the PR uses the template.

## Reporting bugs / requesting features

Use the [issue templates](.github/ISSUE_TEMPLATE). Include reproduction steps, environment, and
logs for bugs.
