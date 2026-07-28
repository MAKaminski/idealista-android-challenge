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

## 2026-07-28 — Step 3: Room cache, favorites and the repository

**Delivered:** the `ads` + `favorites` tables, DAOs, entity mappers, `AdRepository` and its Hilt
wiring. Both behaviours the challenge actually grades — favoriting an ad, and showing the **date** it
was favorited — now work end to end below the UI.

### Verification

| Command | Result |
|---|---|
| `./gradlew :core:data:testDebugUnitTest` | **23 tests, 0 failures** (8 repository, 9 mapper, 6 contract) |
| `./gradlew lint testDebugUnitTest assembleDebug` | **BUILD SUCCESSFUL** |
| Room schema export | `core/data/schemas/…IdealistaDatabase/1.json` |

Both load-bearing tests from `TESTING.md` now exist and pass at the repository level:
`favoriting an ad surfaces the same date on the list and the detail`, and
`detail for ad 3 shows ad 3 identity not ad 1`.

Favorites live in their own table, so `a refresh does not clear existing favorites` — a cache refresh
can never drop what the user saved.

### Found by building

- **Robolectric 4.16.1 has no API 37 runtime.** Every Robolectric test failed with
  `targetSdkVersion=37 > maxSdkVersion=36`. Pinned via `robolectric.properties` (`sdk=36`) with a note
  to raise it when Robolectric catches up — the same "one release ahead" tax as the KSP flag.
- **A `TestDispatcher` built in `@Before` carries its own scheduler**, and mixing it with the one
  `runTest` creates makes every dispatch throw `DispatchException` — six tests failed on this before
  the cause was clear. The fixture now injects `Dispatchers.Unconfined`.
- Turbine's coordinates are `app.cash.turbine:turbine`, not `app.cash:turbine`.

---

## 2026-07-28 — Step 2: network layer, mappers and contract tests

**Delivered:** `:core:model` domain types, and in `:core:data` the two DTO hierarchies, the mappers
(including the detail-merge strategy), the Retrofit/kotlinx.serialization stack and its Hilt module —
with **15 tests**, the first real ones in the project.

### Verification

| Command | Result |
|---|---|
| `./gradlew :core:data:testDebugUnitTest` | **15 tests, 0 failures** (`AdMappersTest` 9, `IdealistaApiContractTest` 6) |
| `./gradlew lint testDebugUnitTest assembleDebug` | **BUILD SUCCESSFUL** |

The load-bearing guard from `TESTING.md` now exists and passes:
`detail for ad 3 never shows ad 1 identity`.

### Found by building

- **The kotlinx.serialization compiler plugin works under AGP 9 built-in Kotlin.** Checked with a
  one-class probe before writing a data layer on top of it, since KSP had already needed an escape
  hatch. No workaround needed.
- **AGP 9's new-DSL source sets are not reachable from Kotlin DSL.** Registering an extra test
  resource dir — `android.sourceSets.getByName("test").resources.srcDir(...)` — fails with
  `DefaultAndroidLibrarySourceSet_Decorated cannot be cast to AndroidLibrarySourceSet`, inside the
  `android { }` block as well as outside it. The fixtures are therefore synced into the standard
  `src/test/resources/fixtures` (gitignored) by a `Sync` task instead. The root `list.json` /
  `detail.json` stay the single source of truth either way.

---

## 2026-07-28 — Step 8 (pulled forward): CI

**Delivered:** `.github/workflows/ci.yml` — lint, unit tests and `assembleDebug` on every push to
`master` and every PR, with reports uploaded on failure and the **debug APK uploaded on success**.

Moved ahead of steps 2–7 for two reasons: everything after it is then verified independently instead
of by a developer running commands and reporting the result, and each green run publishes an APK that
can be sideloaded without a local Android toolchain.

### Verification

| Command | Result |
|---|---|
| `./gradlew lint testDebugUnitTest assembleDebug` (the exact CI command, locally) | **BUILD SUCCESSFUL** |
| Same command on a clean macOS/Apple Silicon clone | **BUILD SUCCESSFUL in 2m 11s** — the toolchain reproduces off this container |

Note the unit-test step currently passes **vacuously**: no tests exist yet. It stops being vacuous at
step 2.

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
