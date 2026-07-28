# ADR-0002 — Multi-module by layer and feature

**Status:** Accepted · 2026-07-27

## Context

The app has two mandatory screens and four ads. A single `:app` module would be entirely sufficient
to satisfy the spec, and for a product this size it would arguably be the more proportionate choice.

But the challenge is a hiring signal: the reviewer is asking "how does this person structure code
that has to grow?" A module graph makes the layering rules enforceable by the compiler instead of by
convention — a feature module *cannot* reach into another feature's internals, and a DTO *cannot*
leak into the UI, because the dependency simply isn't declared.

## Decision

Eight modules: `:app`, `:core:model`, `:core:data`, `:core:designsystem`, `:core:testing`,
`:feature:list`, `:feature:detail`, `:feature:favorites`.

Dependency rules: `:feature:*` → `:core:data` → `:core:model`; features never depend on each other;
DTOs never leave `:core:data`; `:core:model` has no Android imports.

Boilerplate is contained with a `build-logic` included build providing convention plugins
(`android.application`, `android.library`, `android.feature`, `android.hilt`, `jvm.library`), so each
module's build script stays around ten lines and versions live only in `gradle/libs.versions.toml`.

## Consequences

- Layer violations become compile errors; parallel module builds and finer test scoping come free.
- More Gradle files, and a heavier first-time build. Convention plugins keep the per-module cost low
  but do not eliminate it.
- This is deliberately more structure than four ads need. That trade is made openly here so a
  reviewer reads it as a considered choice rather than reflexive over-engineering.
