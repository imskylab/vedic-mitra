# 1. Record architecture decisions

- **Status:** Accepted
- **Date:** 2026-08-01

## Context

We need a lightweight, durable way to capture the significant architectural decisions made on Vedic
Mitra — the choices that are expensive to reverse and that new contributors repeatedly ask "why?"
about. Decisions scattered across chat, PR descriptions, and people's memories get lost.

## Decision

We will use **Architecture Decision Records (ADRs)** as described by Michael Nygard. Each
significant decision is recorded as a numbered Markdown file in `docs/adr/`, using the template
below. ADRs are immutable once accepted; to change a decision we add a new ADR that supersedes the
old one (and mark the old one `Superseded by ADR-XXXX`).

An ADR is warranted when a decision affects structure, cross-module contracts, tooling, or is
otherwise hard to reverse.

## Consequences

- **Positive:** decisions and their rationale are discoverable in-repo and versioned with the code;
  onboarding is faster; debates aren't re-litigated.
- **Negative:** a small amount of writing overhead per significant decision.
- ADR-0001 (this record) establishes the practice itself.

## Template

Copy this for new ADRs (`docs/adr/000N-title.md`):

```markdown
# N. <Short title>

- **Status:** Proposed | Accepted | Superseded by ADR-XXXX
- **Date:** YYYY-MM-DD

## Context
<The forces at play: technical, business, constraints.>

## Decision
<The change we're making and why.>

## Consequences
<What becomes easier or harder as a result.>
```
