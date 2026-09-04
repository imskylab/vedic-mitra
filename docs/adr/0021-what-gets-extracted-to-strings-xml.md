# 21. What gets extracted to strings.xml, and what does not

- **Status:** Accepted
- **Date:** 2026-09-04

## Context

F1 moves the app's hardcoded literals into `strings.xml`. It is the largest single item on the
roadmap, it is deliberately shaped as the project's contributor on-ramp, and it is about to be filed
as a set of per-module issues. Before that happens it needs a boundary, because "extract UI strings
per module" read literally leads somewhere the roadmap does not want to go.

**Working alphabetically through the modules reaches `:core:astronomy` first, and it holds 396 string
literals — a third of the codebase's total.** Extracting them would give the engine an Android
resource dependency. [F5](../roadmap.md) wants the opposite: `:core:astronomy` is pure Kotlin with no
Android dependency today, and the plan is to lift it to Kotlin Multiplatform so it can serve an iOS
port, a publishable calculation library, and a desktop or web companion. F1 and F5 pull in opposite
directions on the same module and nothing said so.

Looking at what those 396 literals actually are makes the boundary obvious, because they are two
unrelated things wearing the same coat:

- **Domain vocabulary** — nakshatra names, the sixty samvatsaras, month and graha names. These are
  computed values, not copy. [ADR 0019](0019-sanskrit-vocabulary-and-transliteration.md) already
  settled that they are *not translated*: they stay the same word, rendered in the reader's script.
- **English teaching prose** — `PanchangaPrimer` and `PanchangaGlossary` are between them **129
  literals of explanatory sentences**. This is UI copy, it is the app's voice, and it must be
  translated. It has no business in an engine module at all, and it is the single thing most firmly
  blocking F5.

## Decision

**1. UI copy is extracted; domain vocabulary is not.** The test is whether a translator would need to
see it. A button label, a hint, a validation message, an error: extract. A tithi's name: leave it —
translating it would be a category error, and ADR 0019 explains why.

**2. `:core:astronomy` is not extracted.** No `strings.xml` in that module, so that F5 stays
reachable. Where the engine's vocabulary needs a script-appropriate form, that mapping belongs at the
display boundary in the UI layer, not in the engine.

**3. The prose in the engine moves out** — `PanchangaPrimer` and `PanchangaGlossary` belong in a UI
module and are filed separately. Points 1 and 2 are in tension until that happens: the prose is UI
copy that has to be translated, sitting in the one module that must not gain resources. Moving it is
the resolution, and it is worth doing on its own merits — it is teaching copy in a calculation
library.

**4. Display copy does not live in data-layer types.** `ProfileRelation` and `Gender` carried a
`displayName` in `:core:datastore`, a module that persists values and knows nothing about a locale.
One field was doing two jobs, and the display half could not be translated where it sat. This is the
same fault as keying a reminder on its label ([#211](https://github.com/imskylab/vedic-mitra/issues/211)):
a value the data layer owns and the UI layer names needs the two kept apart. The enums are persisted
by enum name, so their labels are now UI-layer resources and free to change without a migration.

**5. A ViewModel names a string; it does not resolve one.** `stringResource` needs a composition, and
handing a ViewModel a `Context` to call `getString` on ties it to Android, makes it need a context in
tests, and resolves against whatever locale was current when the ViewModel ran rather than when the
text is drawn. `UiText` in `:core:ui` names the resource and its arguments; the composable resolves
it. A ViewModel test then asserts on a **resource id**, which is a stronger assertion than a string —
it cannot pass by accident because two messages happen to read alike, and it does not break when the
copy is reworded.

**6. Keys are `<feature>_<screen>_<thing>`**, so a translator working one file at a time can tell
where a string appears without reading the code. Format arguments are indexed (`%1$s`, never `%s`):
a translator will reorder them, and unindexed placeholders cannot be reordered. Sentences are one
resource, not concatenated fragments — word order is not universal, and neither is the separator.

## Consequences

`:feature:profile` is extracted as the worked example, and it was chosen because it hits every hard
case at a small size: formatted arguments, content descriptions, an enum's labels, a ViewModel that
emits validation messages, and a string built up in three steps.

**The engine keeps 396 literals for now**, and roughly 129 of them are prose that will move rather
than be extracted in place. Anyone counting remaining work should not count those twice.

This ADR exists because the alternative was for the boundary to be set by whichever contributor got
to `:core:astronomy` first — and by then F5 would have been quietly spent, in a pull request that
looked like it was doing exactly what the roadmap asked.
