# ADR-0001 — Toolchain: AGP 9.3.1, Gradle 9.6.1, built-in Kotlin, KSP

**Status:** Accepted · proposed 2026-07-27 · **amended 2026-07-27 after the build gate ran**

## Context

A greenfield project can pick any toolchain, and the choice is more constrained than it looks. Every
version below was read from live Google Maven / Maven Central metadata rather than recalled:

- AGP **9.3.1** is current, and requires Gradle **9.5+**, JDK 17+, build-tools 36, max API 37.
- AGP 9 enables **built-in Kotlin**, which is **incompatible with KAPT** — annotation processing is
  KSP-only. It also makes the R class non-final and disallows
  `getDefaultProguardFile("proguard-android.txt")`.
- Hilt **2.60.1** is current. **Hilt 2.59+ dropped AGP 8 support**, and 2.59 has a known
  `ComponentTreeDeps` failure on AGP 9 (dagger#5099).

So the AGP choice, the Hilt choice and the Kotlin version are one decision, not three.

## Decision

Pin **AGP 9.3.1 / Gradle 9.6.1 / Hilt 2.60.1 / Room 2.8.4**, with **compileSdk 37.1**, targetSdk 37,
minSdk 24, and a JDK 17 toolchain provisioned by the foojay resolver. Annotation processing is
**KSP only** — no KAPT anywhere.

**Kotlin is not pinned by us.** AGP 9's built-in Kotlin manages it (2.2.10), and AGP's own POM pins
the matching KSP (`2.2.10-2.0.2`). The version catalog declares `kotlin = "2.2.10"` solely so the one
non-Android module (`:core:model`) and `build-logic` compile with the *same* compiler AGP is using.

Validate the combination **empirically before writing app code** — which is what happened below.

## Empirical result (the reason this ADR was amended)

The gate — `:app` (`@HiltAndroidApp`) + `:core:data` (Room entity/DAO/database + a Hilt module),
across the full eight-module graph — now passes `assembleDebug`, `testDebugUnitTest` and `lint`, with
the configuration cache enabled and reused. Four things the plan had wrong, found by building:

| Planned | Actual |
|---|---|
| Gradle **9.5** | **No such version.** Real releases are 9.5.0 / 9.6.1 — pinned to **9.6.1** |
| Kotlin **2.3.10** + KSP 2.3.10 | AGP's built-in Kotlin manages Kotlin at **2.2.10** and pins KSP **2.2.10-2.0.2**. Overriding either desyncs the pair for no benefit |
| compileSdk **36** | `androidx.core:core-ktx:1.19.0` requires **37+**; platforms are now minor-versioned, so **`compileSdk = 37` + `compileSdkMinor = 1`** |
| Standalone KSP works as-is | KSP registers its generated dirs through `kotlin.sourceSets`, which built-in Kotlin **forbids** — needs `android.disallowKotlinSourceSets=false` |

Two smaller findings: the `wrapper` task can't run once AGP 9 is on the root build's classpath
(Gradle 8.14.3 can't load it), so the wrapper is bootstrapped with the build scripts temporarily
moved aside; and Gradle 9 fails a test task that discovers no tests, so the convention plugins set
`failOnNoDiscoveredTests = false` while modules are still empty.

The `android.disallowKotlinSourceSets=false` escape hatch prints an "experimental option" warning on
every build. That warning is left **visible** rather than suppressed: it is the honest signal that
this project is one AGP release ahead of the standalone KSP plugin's own migration, and it should
disappear when KSP ships an AGP-9-native integration.

The AGP 8.13.2 + Hilt 2.58 fallback was **not needed** and is not in use.

## Consequences

- Current tooling, and the AGP 9 migration is done once at the start instead of being owed later.
- Kotlin follows AGP, not us. Raising it means raising AGP — and re-checking KSP and Hilt with it.
- KAPT is off the table. Any library that is KAPT-only is disqualified from this project.
- The riskiest part of the stack was proven in the first commit, when backing out would have been
  cheap. Four wrong assumptions cost one build cycle each instead of surfacing mid-feature.
