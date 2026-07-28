# ADR-0003 — Hilt for dependency injection

**Status:** Accepted · 2026-07-27

## Context

Three ViewModels, one repository, a Retrofit service and a Room database need wiring. A hand-rolled
`AppContainer` plus `ViewModelProvider.Factory` would cover that in well under a hundred lines, with
no code generation and a faster build.

Against that: Hilt is the standard on Android teams, it is what a reviewer at a company like
idealista will expect to see in a multi-module app, and it makes swapping fakes in instrumented tests
(`@HiltAndroidTest`, `@TestInstallIn`) a solved problem rather than a bespoke one.

The cost is not neutral. Hilt pulls KSP into the build, and Hilt's version is coupled to AGP's — 2.59
dropped AGP 8 support, and 2.59 itself fails on AGP 9 (see [ADR-0001](ADR-0001-toolchain.md)).

## Decision

Use **Hilt 2.60.1** with KSP. `@HiltAndroidApp` on the Application, `@AndroidEntryPoint` on
fragments, `@HiltViewModel` on ViewModels, and `@Module`s in `:core:data` binding the repository,
Retrofit service, Room database and dispatchers.

Dispatchers are injected via qualifiers (`@IoDispatcher`) so no class under test hardcodes
`Dispatchers.IO`.

## Consequences

- Test doubles swap cleanly at the module boundary in both JVM and instrumented tests.
- KSP annotation processing is now on the critical build path, and the Hilt version is pinned to the
  AGP choice. Both are documented constraints in `CLAUDE.md` so a future upgrade doesn't break them.
- If the AGP 9 gate in ADR-0001 fails, the fallback pins Hilt 2.58.
