# Delivery log

Chronological record of what was actually delivered and how it was verified. The rule for this file:
**nothing is logged as done unless a command was run and its real output backs it up.** Planned work
lives in [`PLAN.md`](PLAN.md), not here.

---

## 2026-07-27 — Research, planning and documentation

**Delivered:** this `docs/` package and the root [`CLAUDE.md`](../CLAUDE.md). No application code.

### Verified by running

| Check | Command | Result |
|---|---|---|
| Repo contents | `git log`, `ls` | 11 commits, no source code — only `README.md`, `list.json`, `detail.json` |
| List payload shape | `python3` parse of `list.json` | array of 4; `propertyCode` is a **String**; `features` keys vary per ad; `parkingSpace` only on ad 2 |
| Detail payload shape | `python3` parse of `detail.json` | single object; `adid` is an **Int**; `priceInfo.amount` is one level shallower than the list's `priceInfo.price.amount` |
| Live endpoints | `curl` both URLs | HTTP 200; live responses match the committed fixtures (4 ads, detail `adid=1`) |
| Image CDN | `curl -I` an `img4.idealista.com` URL | HTTP 200, `image/webp` |
| JDK | `java -version` | OpenJDK 21.0.10 |
| Gradle | `gradle --version` | 8.14.3 on PATH (the wrapper will fetch 9.5) |
| Android SDK | `$ANDROID_HOME`, `which sdkmanager` | **not installed** — bootstrap needed before any build |
| Disk | `df -h` | ~30 GB free — enough for the SDK |
| Network | `curl` | `dl.google.com`, `repo1.maven.org`, `services.gradle.org` all reachable |
| Current AGP | Google Maven metadata | 9.3.1 (needs Gradle 9.5+, JDK 17+, buildTools 36, max API 37) |
| Current Kotlin / KSP | Maven Central metadata | Kotlin 2.4.10 exists, but **KSP stops at 2.3.10** → Kotlin pinned to 2.3.10 |
| Hilt ↔ AGP coupling | Maven metadata + release notes | Hilt 2.60.1 current; **2.59+ dropped AGP 8**; 2.59 has a known AGP 9 `ComponentTreeDeps` failure |
| AGP 9 breaking changes | AGP release notes | built-in Kotlin on by default (KAPT incompatible), non-final R class, `proguard-android.txt` disallowed, `newDsl`/`useAndroidx` defaults flipped |

### Decisions recorded

[`DECISIONS/`](DECISIONS/) — ADR-0001 through ADR-0007: toolchain, modularization, Hilt, Room,
detail-merge strategy, XML + Compose interop, minSdk 24 with desugaring.

### Not done yet

No Gradle project, no source, no tests, no CI. Step 1 of [`PLAN.md`](PLAN.md) — the build scaffold —
is the gate that proves the AGP 9 / Hilt / KSP combination compiles before any app code is written.

---

## 2026-07-27 — Step 0 + Step 1: SDK bootstrap and Gradle scaffold

**Delivered:** the Android SDK in this environment, and the eight-module Gradle scaffold with
convention plugins, Hilt and Room. Still no feature code — this step exists to prove the toolchain
compiles before anything is built on it.

### Verification

| Command | Result |
|---|---|
| `sdkmanager --list_installed` | `platforms;android-37.1`, `build-tools;37.0.0`, `build-tools;36.0.0`, `platform-tools` |
| `./gradlew assembleDebug` | **BUILD SUCCESSFUL** — `app/build/outputs/apk/debug/app-debug.apk`, 4.0 MB |
| `./gradlew testDebugUnitTest lint` | **BUILD SUCCESSFUL** — no lint errors (no tests yet; see below) |
| `./gradlew clean && ./gradlew assembleDebug testDebugUnitTest lint` | **BUILD SUCCESSFUL**, "Configuration cache entry reused" |
| Room KSP output | `core/data/schemas/…ScaffoldDatabase/1.json` exported |

The gate exercised the genuinely risky combination end-to-end: `@HiltAndroidApp` in `:app`, a Room
entity + DAO + database and a Hilt `@Module` in `:core:data`, both processed by KSP, across the
module graph — plus ViewBinding, core library desugaring and the configuration cache.

### Four planned pins were wrong, and the build said so

| Planned | Actual | Fix |
|---|---|---|
| Gradle 9.5 | no such release (9.5.0 / 9.6.1 exist) | wrapper on **9.6.1** |
| Kotlin 2.3.10 + KSP 2.3.10 | AGP's built-in Kotlin manages Kotlin (**2.2.10**) and pins KSP **2.2.10-2.0.2** | stopped pinning Kotlin ourselves |
| compileSdk 36 | `core-ktx:1.19.0` needs 37+; platforms are minor-versioned | `compileSdk = 37`, `compileSdkMinor = 1`, installed `platforms;android-37.1` |
| standalone KSP just works | it registers generated dirs via `kotlin.sourceSets`, which built-in Kotlin forbids | `android.disallowKotlinSourceSets=false` |

Two further snags, both logged because they'll bite anyone reproducing this: the Gradle `wrapper`
task can't run once AGP 9 is on the root classpath (system Gradle 8.14.3 can't load it) — the wrapper
was bootstrapped with the build scripts moved aside; and Gradle 9 fails a test task that discovers no
tests, so the convention plugins set `failOnNoDiscoveredTests = false` until real tests land.

ADR-0001 was amended rather than quietly re-pinned. The AGP 8.13.2 + Hilt 2.58 fallback was **not**
needed.

### Not done yet

No network layer, no screens, no tests, no CI. Next is step 2 — Retrofit + serialization DTOs with
MockWebServer contract tests against the committed fixtures.

---

<!-- Append one section per implementation commit: what shipped, the verification command, and its
     actual output. Failures get logged too — a build that went red and why is more useful to a
     reviewer than a log that only records successes. -->
