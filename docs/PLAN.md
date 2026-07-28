# Implementation plan

> Status: **approved, not yet started.** Progress is tracked in [`DELIVERY_LOG.md`](DELIVERY_LOG.md).

## Goal

Deliver the idealista Android challenge as a production-shaped app: two XML screens (list + detail),
favorites with the favorited date, plus the bonuses (tests, Compose alongside XML, persistent
storage, AI context files) — and document the delivery honestly enough that a reviewer can audit it.

## Scope decisions

| Decision | Choice | Rationale |
|---|---|---|
| Ambition | Full showcase | Multi-module, Room, CI, full test pyramid, Compose bonus |
| DI | **Hilt 2.60.1** | Industry standard; reviewers expect it. Couples to AGP 9 (see ADR-0001) |
| Compose | **Both placements** | Compose-only Favorites screen **and** a `ComposeView` inside the XML detail screen |
| Toolchain | **Latest, validated empirically** | Build and tests actually run in the container before anything is claimed |

## Pinned versions

**As built** (step 1 corrected four of these — see ADR-0001):
AGP 9.3.1 · Gradle 9.6.1 · Kotlin 2.2.10 via AGP's built-in Kotlin · KSP 2.2.10-2.0.2 · Hilt 2.60.1 ·
Room 2.8.4 · compileSdk 37 + compileSdkMinor 1 · targetSdk 37 · minSdk 24 + core library desugaring ·
JDK 17 toolchain (foojay-provisioned)

**Planned for later steps:** Compose BOM 2026.06.01 · Navigation 2.9.8 · Lifecycle 2.11.0 ·
Retrofit 3.0.0 · OkHttp 5.4.0 · kotlinx-serialization-json 1.11.0 · Coil3 3.5.0 · Material 1.14.0 ·
Turbine 1.2.1 · MockK 1.14.11 · Robolectric 4.16.1 · Espresso 3.7.0

Rationale and the hard constraints behind these numbers are in
[`DECISIONS/ADR-0001-toolchain.md`](DECISIONS/ADR-0001-toolchain.md).

## Delivery sequence

Each step is one independently reviewable commit on `claude/idealista-android-plan-sr4l2h`, and each
has a verification command that must pass before the next step starts.

| # | Commit | Delivers | Verify |
|---|---|---|---|
| 0 | *(environment)* ✅ | Android SDK: cmdline-tools, `platforms;android-37.1`, `build-tools;37.0.0` | `sdkmanager --list_installed` ✅ |
| 1 | `chore(build)` ✅ | Gradle wrapper 9.6.1, `libs.versions.toml`, `build-logic` convention plugins, 8 modules, Hilt + Room gate, `.gitignore` | `./gradlew assembleDebug testDebugUnitTest lint` ✅ green, configuration cache reused |
| 2 | `feat(data)` | Retrofit + serialization DTOs, mappers, fixtures wired as test resources | `./gradlew :core:data:test` |
| 3 | `feat(data)` | Room cache + favorites DAO, `AdRepository` incl. the detail-merge strategy | `./gradlew :core:data:test` |
| 4 | `feat(list)` | XML list screen, ViewModel, favorite toggle + date badge, loading/error/empty states | `./gradlew :feature:list:test` |
| 5 | `feat(detail)` | XML detail screen, `ViewPager2` gallery, merged detail, favorite FAB | `./gradlew :feature:detail:test` |
| 6 | `feat(favorites)` | Compose favorites screen + the detail screen's `ComposeView` component | `./gradlew testDebugUnitTest` |
| 7 | `test` | Robolectric fragment/Compose tests, Espresso e2e (authored), Jacoco report | `./gradlew testDebugUnitTest jacocoTestReport` |
| 8 | `ci` ✅ **pulled forward** | GitHub Actions: lint + unit tests + `assembleDebug`, reports **and the debug APK** uploaded as artifacts | workflow green on the PR |
| 9 | `docs` | Docs refreshed with real results, screenshots, requirement matrix in `README.md` | manual review |

A **draft PR** is opened after the first push and kept current.

**Step 8 was pulled forward to run second.** Every step after it is then verified by something other than a developer running commands and reporting the result, and each green run publishes a downloadable debug APK — which is how the app gets onto a device without a local Android toolchain.

## Step 1 is a gate

The build scaffold is validated before a line of app code is written, because AGP 9 + built-in
Kotlin + KSP + Hilt is the one genuinely risky combination in this stack. If `assembleDebug` with a
trivial `@HiltAndroidApp` + a Room entity does not pass, we fall back to AGP 8.13.2 / Gradle 8.14.3 /
Kotlin 2.2.21 / **Hilt 2.58** (2.59+ dropped AGP 8 support) and record the fallback in ADR-0001
rather than swapping it silently.

## Requirement traceability

| Spec requirement | Where it lands |
|---|---|
| Kotlin + XML views | `:feature:list`, `:feature:detail` — steps 4–5 |
| Two screens (list, detail) | steps 4–5 |
| Favorite an ad | `favorites` table + `toggleFavorite` — step 3 |
| Show the favorited **date** | date badge on list item and detail FAB label — steps 4–5 |
| Use AI tools | [`AI_USAGE.md`](AI_USAGE.md) |
| *Bonus* tests | steps 2–7, [`TESTING.md`](TESTING.md) |
| *Bonus* Compose alongside XML | step 6 (screen **and** embedded component) |
| *Bonus* persistent storage | Room — step 3 |
| *Bonus* AI context files | [`../CLAUDE.md`](../CLAUDE.md) |

## Risks

| Risk | Mitigation |
|---|---|
| AGP 9 built-in Kotlin vs KSP/Hilt | Gated at step 1; documented fallback to AGP 8.13.2 + Hilt 2.58 |
| Kotlin capped at 2.3.10 (no KSP 2.4.x) | Pinned deliberately, noted in the version catalog |
| No emulator/KVM in this environment | Espresso tests authored and CI-wired, explicitly marked as not executed locally |
| Android SDK download (~2–3 GB) | One-off at step 0; 30 GB free disk verified |
| Image CDN availability | Verified reachable; Coil gets placeholder/error drawables so the UI degrades cleanly |
| Mock detail endpoint returning ad 1 for everything | Merge strategy + regression test (ADR-0005) |
