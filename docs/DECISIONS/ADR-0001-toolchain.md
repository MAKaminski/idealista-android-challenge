# ADR-0001 — Toolchain: AGP 9.3.1, Gradle 9.5, Kotlin 2.3.10, KSP

**Status:** Accepted · 2026-07-27

## Context

A greenfield project can pick any toolchain, and the choice is more constrained than it looks. Every
version below was read from live Google Maven / Maven Central metadata rather than recalled:

- AGP **9.3.1** is current, and requires Gradle **9.5+**, JDK 17+, build-tools 36, max API 37.
- Kotlin **2.4.10** exists, but **KSP publishes nothing above 2.3.10**. Room and Hilt both need KSP.
- Hilt **2.60.1** is current. **Hilt 2.59+ dropped AGP 8 support**, and 2.59 has a known
  `ComponentTreeDeps` compilation failure on AGP 9 (dagger#5099).

AGP 9 also flips several defaults: built-in Kotlin is on (and is **incompatible with KAPT**), the R
class is non-final, `getDefaultProguardFile("proguard-android.txt")` is disallowed, and
`newDsl`/`useAndroidx`/`enableAppCompileTimeRClass` default to `true`.

So the AGP choice, the Hilt choice and the Kotlin ceiling are one decision, not three.

## Decision

Pin **AGP 9.3.1 / Gradle 9.5 / Kotlin 2.3.10 / KSP 2.3.10 / Hilt 2.60.1**, with compileSdk and
targetSdk 36 and a JDK 17 toolchain. Annotation processing is **KSP only** — no KAPT anywhere.

Validate this combination **empirically before writing app code**: step 1 of the plan builds a
scaffold with `@HiltAndroidApp` and one Room entity and must pass `./gradlew assembleDebug`.

If that gate fails, fall back to **AGP 8.13.2 / Gradle 8.14.3 / Kotlin 2.2.21 / Hilt 2.58** (the last
Hilt that supports AGP 8) and amend this ADR — rather than swapping versions silently.

## Consequences

- Current tooling, and the AGP 9 migration is done once at the start instead of being owed later.
- Kotlin is capped at 2.3.10 by KSP availability. Revisit when KSP ships 2.4.x.
- KAPT is off the table. Any library that is KAPT-only is disqualified from this project.
- The riskiest part of the stack is proven in the first commit, when backing out is cheap.
