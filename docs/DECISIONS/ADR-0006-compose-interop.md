# ADR-0006 — Compose in two places, XML for the mandatory screens

**Status:** Accepted · 2026-07-27

## Context

The spec **requires XML views** for the app and lists *"some Jetpack Compose code alongside xml"* as a
bonus. Those pull in opposite directions, so the placement of Compose matters: rewriting the list or
detail screen in Compose would violate a hard requirement to chase a soft one.

Two natural placements exist — a separate Compose screen (clean, but arguably not really "alongside"),
or `ComposeView` embedded inside an XML layout (true interop, but it touches a mandatory screen).

## Decision

Do both, keeping the mandatory screens' structure XML:

1. **`:feature:favorites` is Compose end-to-end** — a `LazyColumn` of favorited ads sorted by date,
   with swipe-to-dismiss and an empty state, hosted in a Fragment via `ComposeView` and reached from
   the same Navigation graph as the XML screens.
2. **One `ComposeView` inside the XML detail layout** — the characteristics grid and energy
   certificate badge, sharing the ViewModel's `StateFlow` with the surrounding XML.

`:core:designsystem` holds both the XML Material 3 theme and the Compose theme, derived from the same
color and type tokens, so the two toolkits are visually indistinguishable.

## Consequences

- The hard requirement is untouched: list and detail remain XML + ViewBinding.
- Both kinds of Compose adoption a real migration involves — new screens, and incremental replacement
  inside existing screens — are demonstrated.
- Two theming systems must be kept in sync; shared tokens in `:core:designsystem` is the mitigation.
- Compose UI tests are added alongside the Robolectric fragment tests, so the bonus carries its own
  test coverage rather than borrowing credibility from the XML tests.
