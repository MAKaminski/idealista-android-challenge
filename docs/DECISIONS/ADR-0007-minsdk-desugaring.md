# ADR-0007 — minSdk 24 with core library desugaring

**Status:** Accepted · 2026-07-27

## Context

The favorite feature must display the **date an ad was favorited**, formatted for the user's locale.
`java.time` (`Instant`, `DateTimeFormatter.ofLocalizedDate`) is the right API for that, but it is only
natively available from API 26.

The upstream spec lists *backwards compatibility* among the things the team deals with day to day, so
quietly setting `minSdk 26` to dodge the problem would sidestep something they explicitly care about.
The alternatives — `SimpleDateFormat` (mutable, not thread-safe, poor timezone handling) or ThreeTenABP
(a third-party dependency for what the platform now solves) — are both worse than the supported fix.

## Decision

`minSdk 24`, `targetSdk`/`compileSdk 36`, with **core library desugaring** enabled so `java.time` works
back to API 24.

Dates are stored as epoch millis, converted to `Instant` at the Room boundary via a `TypeConverter`, and
rendered with `DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)` in the system zone through a
localized string resource (`favorite_saved_on`), so the English and Spanish resource sets both read
naturally.

## Consequences

- Reaches roughly the entire active Android install base while using the modern date API.
- Adds the desugaring dependency and a small amount of dex overhead — negligible for this app.
- The formatter is injected rather than constructed inline, so tests can pin a fixed locale and zone and
  assert on the rendered date deterministically.
