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

## 2026-07-28 — A map, Chinese, translated listings, and a README with screenshots

Four asks: a map tab, Chinese as a sixth language, translation of the ad text itself, and a README
that explains the app with screenshots and separates *what was asked for* from *what we added*.

### Screenshots without an emulator

The README needed screenshots and this container has no device. Rather than ship none,
`./gradlew screenshots` rasterises the real screens offscreen through Robolectric's **native
graphics** backend — the real layouts, the real adapters, the real theme, and the real listing
photos fetched into a build directory and served to Coil keyed by URL.

Keyed by URL on purpose: every ad shows *its own* photo, so a screenshot would visibly expose the
ad-1-everywhere bug ADR-0005 exists to prevent.

Three things had to be true before a pixel appeared, and each was found by looking at the output
rather than by reasoning about it:

| Symptom | Cause |
|---|---|
| `ViewTreeLifecycleOwner not found` | A plain `Activity` is not a `LifecycleOwner`; Compose needs `ComponentActivity` |
| Every photo a grey placeholder | Coil suspends until its target view is **attached to a window**. The harness rendered a detached hierarchy |
| Photos still placeholders | A `RecyclerView` binds its rows during the *first* layout pass, which is when the image requests are issued — so the capture needs a second pass with an idle between |

Generation is opt-in: an ordinary `testDebugUnitTest` skips it, so CI neither rewrites committed
images nor depends on a CDN.

### The map

osmdroid over OpenStreetMap tiles, not the Google Maps SDK — a Maps key is a secret, and a
submission should carry none (ADR-0010). It is a real pannable, zoomable map with a pin per ad; a pin
raises a card and the card opens the same detail screen the list opens, through the host's callback,
so `:feature:map` does not know `:feature:detail` exists.

The map reads the **same Room cache** the list does, so it costs no extra request and a favorite
toggled on the list is already true here.

`MapBounds` lives in `:core:model` as pure geometry. Ads without coordinates are filtered out rather
than defaulted: `(0, 0)` is in the Gulf of Guinea, and a single such pin would zoom the map out to
include Africa. There is a test named after that.

### Translated listings

Reported with a screenshot: an English UI wrapped around three thousand characters of Spanish. Fair
— localizing the chrome and leaving the content is the worse of the two bugs, because it looks
finished.

The tempting fix was to bundle hand-written translations of the four mock ads. Rejected: that is a
translation of *this fixture*, not a capability, and ~80 000 characters of generated prose in
`values-*/` would imply the app can translate when it cannot. ML Kit on-device translation was
chosen instead — no API key, offline after the first model download, and it works on whatever the
API sends (ADR-0011).

Three properties matter more than the translation: the screen renders the original **first** and
gains the translation when it arrives (a first-run model download is ~30 MB — a blank description for
that long would be a worse bug than the one being fixed); every failure path falls back to the
Spanish; and a translation is labelled as one.

### Chinese

A sixth language across all seven modules. Worth noting that `zh-Hans-CN` and `zh-TW` both had to
resolve to Chinese — Chinese tags carry a script subtag far more often than not — which the existing
primary-subtag matching already handled, now pinned by a test.

### Verification

| Command | Result |
|---|---|
| `./gradlew lint testDebugUnitTest assembleDebug` | **BUILD SUCCESSFUL** |
| Test count | **122** (was 101) |
| `./gradlew screenshots` | five PNGs in `docs/screenshots`, rendered from the real layouts |

### Not verified

**The ML Kit translator has never run.** Its models download onto a real device and there is none
here. Everything around it is tested against a fake — content shows while a translation is pending, a
failure falls back to the original, following the system language asks for nothing — but the ML Kit
class itself is written and unrun, and `TESTING.md` carries a row that says zero.

**The map screen is not screenshotted.** osmdroid fetches tiles at runtime, so offscreen it renders
an empty grid. A picture of an empty grid is worse than no picture, so the README says why instead.

---

## 2026-07-28 — A settings screen, and the app in five languages

*"Add a language selector under a settings area, allow the user to at least select: Spanish, French,
Portuguese, English, Italian."*

### The interesting decision is what **not** to build

The obvious implementation is a `SharedPreference` holding a language tag, read at startup to wrap
every `Context` in a `Configuration` override. It works, and it is wrong on modern Android.

Since Android 13 the **system** owns per-app language: it lists apps that declare supported locales
under Settings → Apps → *App* → Language and stores the choice itself. An app keeping its own
preference has two sources of truth that disagree the moment the user changes it from the system
screen — and nothing in testing would reveal that, because you have to go to the system screen to
see it.

So the app stores nothing. `AppCompatDelegate.setApplicationLocales` forwards to the framework on
API 33+ and is backported below it, through a manifest-declared service. The in-app picker and
Android's own language screen are two views of one value. Full reasoning in ADR-0009.

### What shipped

| Piece | Where |
|---|---|
| `AppLanguage` — five languages, BCP-47 tag resolution | `:core:model` — pure Kotlin, JVM-tested |
| `AppLocales` — apply / read the selection | `:core:designsystem` — the only `AppCompatDelegate` caller |
| `SettingsScreen` + `SettingsFragment` | `:feature:settings` — a new Compose-only module |
| A third bottom-nav destination | `:app` |
| `values-fr/`, `values-pt/`, `values-it/` for every module | 5 modules × 3 new locales |

The picker offers **System default** alongside the five, and labels each language with its endonym —
Español, Français, Português, Italiano — above its name in whatever language is showing. Someone who
lands in Italian by accident cannot read "Italian"; they can find "Italiano".

`SettingsFragment` has no ViewModel on purpose. There is no state to hold — the selection lives in
the delegate and applying one recreates the activity — so a ViewModel would only mirror a value it
does not own.

### Two failures worth recording

**The new module had no `robolectric.properties`.** Every test in it died on
`targetSdkVersion=37 > maxSdkVersion=36` — the same API 37 pin every other Android module already
carries. A per-module file is a thing a new module silently lacks; noted here so the next one gets
it at creation.

**The picker's tests failed on rows below the fold.** `LazyColumn` only composes what is visible, so
four of seven tests failed on `assertExists` and `Failed to inject touch input`. The fix was the
right container rather than a test workaround: six fixed rows want a `Column` with `verticalScroll`,
and one `selectableGroup` around the whole set is what makes it a single radio group for a screen
reader — which `LazyColumn` had been breaking anyway. The tests then `performScrollTo()` before
anything that touches pixels.

### Deleted rather than weakened

Five tests were written for `AppLocales` round-tripping a language through the delegate. All failed.
A probe explained why: on the API 33+ path the call forwards to the framework's `LocaleManager`, and
Robolectric's sandbox has no per-app locale store — `setApplicationLocales` is a genuine no-op there
and reads back empty.

The tests were deleted. A version that passed would have been asserting against a stub and reporting
coverage the app does not have. `TESTING.md` carries a row that says zero.

### Verification

| Command | Result |
|---|---|
| `./gradlew lint testDebugUnitTest assembleDebug --rerun-tasks` | **BUILD SUCCESSFUL in 2m 10s** |
| Test count | **101** (was 86) |
| Generated locale config | `<locale-config>` lists `en-US`, `es`, `fr`, `it`, `pt` |
| Deleted one `values-fr` string, ran `:feature:settings:lint` | **Error: "settings_language_italian" is not translated in "fr"** — the translation guard is real, then restored and green again |

That last row is the one worth having: five languages across five modules is exactly the change that
rots quietly, and now it cannot — a string missing from any locale fails the build.

---

## 2026-07-28 — Insets, external links, and filtering the list

Three reports from the running app, in one pass: *"fix the margin so it doesn't look like shit"*,
*"if URLs to the underlying sites are provided then embed links as such and leverage the browser"*,
and *"add filters from the Properties page that are based on the tags +/- standard RE logic"*.

### The margin was an edge-to-edge bug, not a padding value

Two screenshots showed it precisely: the detail toolbar title sat under the camera cutout, and the
first favorites card sat under the status bar. Targeting SDK 35+ makes the app draw edge-to-edge by
default; nothing was consuming the system bar insets, so content drew behind them. Adding a fixed
`marginTop` would have looked right on one device and wrong on every other.

Fixed at the three places that own a system bar:

| Surface | Fix |
|---|---|
| Detail app bar | `fitsSystemWindows="true"` on the `AppBarLayout` |
| List app bar | same |
| Compose favorites | `windowInsetsPadding(WindowInsets.statusBars)` on all three state branches |
| Bottom nav | `setOnApplyWindowInsetsListener` applying the `navigationBars` bottom inset as padding |

`activity_main.xml` became a vertical `LinearLayout` (weighted container + nav) so the nav bar's own
inset padding does not overlap the content it sits below.

### Links: the data has real URLs, so they are now real links

Three destinations exist in the payloads and none of them were reachable from the UI:

| Destination | Source | Opens as |
|---|---|---|
| The listing page | `propertyCode` → `https://www.idealista.com/inmueble/{code}/` | Custom Tab |
| The property on a map | `latitude` / `longitude` | `geo:` URI, maps web fallback |
| A gallery photo full-size | `multimedia.images[].url` | Custom Tab |

Custom Tabs rather than a raw browser hand-off: it keeps the back stack and the app's colours.
`ExternalLinks` falls back to `ACTION_VIEW`, and a device with no handler gets a Toast — not a crash.
That fallback is the one path a user could hit on a stripped device, so it has its own test.

Honest limit, stated in `AdLinks`' KDoc: the mock property codes are `1`–`4`, so the constructed
listing URLs are **well-formed but will not resolve to real listings**. The URL shape is the
production one; the data is not.

### Filters: only what the list payload can actually answer

Standard real-estate filters, restricted to fields present on **every** list ad — operation, rooms,
bathrooms, price, size, exterior, parking — plus the amenity flags that vary per ad
(`hasAirConditioning`, `hasSwimmingPool`, `hasTerrace`, `hasGarden`), which are treated as opt-in
and AND-ed. Lift, floor and community costs are detail-only and were deliberately **not** offered: a
filter that silently matches nothing is worse than no filter.

The logic is pure Kotlin in `:core:model` (`AdFilters`, `Ad.matches`, `List<Ad>.applyFilters`), so it
tests on the JVM with no Android runtime. The ViewModel combines it as a fourth flow over the cache —
filtering is client-side, so it works offline and costs no request. A new `NoMatches` state says
"nothing matches these filters" rather than reusing `Empty`, which would have read as a failed load.

### Verification

| Command | Result |
|---|---|
| `./gradlew lint testDebugUnitTest assembleDebug` | **BUILD SUCCESSFUL in 57s** |
| Test count | **86** (was 71) |
| `./gradlew :core:model:test` | 18 tests — filter and link logic, no Android runtime |
| `./gradlew :feature:list:testDebugUnitTest` | 23 tests — 4 new ViewModel filter tests, 8 new chip-row tests |
| `./gradlew :core:designsystem:testDebugUnitTest` | 9 tests — 3 new, including the no-handler fallback |

### Not verified

The insets fix is asserted by inspection and by the layout attributes, not by a test — window insets
need a real window, and Robolectric does not dispatch them meaningfully. The two screenshots that
reported the bug are the evidence for the fix; a reviewer with a device should re-check on a cutout
display. Said plainly rather than counted as covered.

---

## 2026-07-28 — Wrong photos, and collapsible detail sections

Reported from the running app: *"various links are loading the incorrect pictures"*. Correct, and it
was a real bug.

### The bug

The detail screen took its gallery from the detail response. That response **always describes ad 1**,
so ads 2, 3 and 4 showed another flat's rooms. ADR-0005 exists precisely to stop this, and the
original merge simply drew the identity/rich-content line in the wrong place: it treated photos as
rich content when they are identity. Each list ad carries its own `multimedia.images`.

Fixed by sourcing the gallery from the cached ad, with the response's images used only when the ad
has none. ADR-0005 amended with the amendment stated rather than the table quietly edited.

Accepted cost: ad 1's detail payload has ten photos with localized room names; its list entry has
seven without them. Seven correct photos beat ten possibly-wrong ones.

### Hardening so it cannot recur

| Test | Module |
|---|---|
| `every card uses its own ad's thumbnail` (all four fixtures) | `:feature:list` |
| `no two cards share an image url` | `:feature:list` |
| `the fixture ads have distinct thumbnails` — so the above can actually fail | `:feature:list` |
| `an ad without a thumbnail yields a null url rather than borrowing one` | `:feature:list` |
| `the model carries the property code a click will report` | `:feature:list` |
| `the detail gallery shows the opened ad's photos not ad 1's` (all four ads) | `:core:data` |
| `every gallery photo url belongs to the cached ad` | `:core:data` |
| `the merged gallery is the opened ad's photos not the response's` | `:core:data` |

The list card now binds through an `AdCardUiModel`, so the image URL and the property code a click
reports come from **one** object — they cannot drift apart — and alignment is assertable on the JVM
without an image loader or a device.

### Collapsible sections

The detail screen was one long column. It now has an `AccordionSection` component in
`:core:designsystem`: a tappable header with title, summary and chevron over collapsible content,
with animated expand/collapse, a `contentDescription` that states the action, and expanded state that
survives rotation. The detail screen uses four — Characteristics (open), Features and extras,
Location, Description — and a Features section was added that surfaces amenities the screen never
showed before.

### Verification

| Command | Result |
|---|---|
| `./gradlew lint testDebugUnitTest assembleDebug` | **BUILD SUCCESSFUL** |
| Test census | **53 tests, 0 failures** (was 37) |

### Worth recording

This bug was found by a person looking at the screen, not by the 37 tests that existed. The suite
verified that identity fields merged correctly and never asked whether the pictures matched. That is
the honest limit of the previous test pyramid, and the gap is now closed.

---

## 2026-07-28 — Steps 6, 7 and 9: Compose, the test pyramid, and the docs

**Delivered:** the remaining plan. The app is now feature-complete against the brief.

### Step 6 — Compose in both placements (ADR-0006)

- `:feature:favorites` is **Compose end-to-end**: a `LazyColumn` of favorited ads sorted by when they
  were saved, with an empty state and per-row removal, hosted in a Fragment via `ComposeView` and
  reached from a bottom navigation bar.
- The XML detail screen's characteristics panel is now a **`ComposeView` embedded in the layout** —
  the incremental-adoption half of the decision. The screen around it is still XML with ViewBinding,
  as the brief requires.
- `:core:designsystem` gained a Compose theme built from the same colour tokens as `themes.xml`, so
  the two toolkits are visually indistinguishable.

### Step 7 — the test pyramid

| Added | Where |
|---|---|
| 4 Compose UI tests (`createComposeRule` under Robolectric) | `:feature:favorites` |
| 1 Espresso end-to-end journey — **authored, compiling, never executed** | `:app` |
| Shared `TestAds` fixtures | `:core:testing` |

### Step 9 — documentation

`README.md` rewritten as a submission front page with a requirement → implementation matrix, the
detail-endpoint trap explained up front, and a **Known limitations** section. `TESTING.md`,
`CLAUDE.md` and `AI_USAGE.md` updated to describe what exists rather than what was planned.

### Verification

| Command | Result |
|---|---|
| `./gradlew lint testDebugUnitTest assembleDebug` | **BUILD SUCCESSFUL** |
| Test census | **37 tests, 0 failures** — `:core:data` 23, `:feature:list` 5, `:feature:detail` 5, `:feature:favorites` 4 |
| `./gradlew assembleDebugAndroidTest` | **BUILD SUCCESSFUL** — the Espresso test compiles |

### Found by building

- **Compose BOM 2026.06.01 works under AGP 9's built-in Kotlin 2.2.10**, unlike Coil 3.5.0. Checked
  with a one-composable probe before building a screen on it.
- **Lint failed on `app_name` missing a Spanish translation.** It is a brand name, so the fix is
  `translatable="false"` rather than a translation — lint was right to ask.
- **A Compose test cannot click an icon with `onNodeWithText`**: the remove control carries a
  `contentDescription`, so the selector had to be `onNodeWithContentDescription`.

---

## 2026-07-28 — Step 5: the XML detail screen

**Delivered:** the second mandatory screen. Cards are now tappable: opening one shows a swipeable
photo gallery, price, address, characteristics, energy certificate and the full description, with an
extended FAB that favorites the ad and shows **"Favorited on <date>"**. Back returns to the list.

### Verification

| Command | Result |
|---|---|
| `./gradlew :feature:detail:testDebugUnitTest` | **5 tests, 0 failures** |
| `./gradlew lint testDebugUnitTest assembleDebug` | **BUILD SUCCESSFUL**, 33 tests total |

The ADR-0005 guard now exists at the screen level too:
`opening ad 3 shows ad 3 identity not ad 1`.

### A real bug, caught by its own test

`retry re-subscribes after a failure` failed on the first run. The cause was `catch` sitting
*outside* `flatMapLatest`: a failure completed the entire chain, so `retries` stopped being collected
and the retry button could never re-subscribe — the error state was permanent until the screen was
recreated. Moving `catch` inside the `flatMapLatest` fixed it. Nothing about the UI would have looked
wrong; only the test found it.

### Navigation

Screen-to-screen wiring lives in `MainActivity` — the list exposes `onAdSelected`, the activity
decides what to open — so neither feature module depends on the other (ADR-0002). The Navigation
Component's SafeArgs plugin was skipped: its Gradle plugin marker is not resolvable for this AGP, and
a single string argument does not justify a plugin. Arguments go through `SavedStateHandle`, so the
detail screen survives process death.

---

## 2026-07-28 — Step 4: the XML list screen (first runnable app)

**Delivered:** the Material 3 design system, the mandatory **XML list screen** (RecyclerView +
ListAdapter + ViewBinding + Coil), its ViewModel, and the app shell with a launcher activity. The app
now installs and runs: ads load, images load, tapping the heart favorites an ad and the card shows
**"Favorited on <date>"**.

### Verification

| Command | Result |
|---|---|
| `./gradlew :feature:list:testDebugUnitTest` | **5 tests, 0 failures** |
| `./gradlew lint testDebugUnitTest assembleDebug` | **BUILD SUCCESSFUL**, 28 tests total |
| APK | `app/build/outputs/apk/debug/app-debug.apk`, 11 MB |

### Found by building

- **Coil 3.5.0 is compiled with Kotlin 2.4**, whose metadata AGP's built-in 2.2.10 compiler cannot
  read (`can read versions up to 2.3.0`). Pinned to **Coil 3.4.0**. This is the first real cost of the
  Kotlin ceiling in ADR-0001: the ecosystem has started shipping 2.4 artifacts, and KSP still caps us
  at 2.3.10. Revisit when KSP ships 2.4.x.
- **CI was red while the same commands passed locally**: Robolectric's Android SDK 36 sandbox
  requires a Java 21 JVM, and the workflow ran Gradle on 17. CI now runs Gradle on **JDK 21** while
  the build still *targets* 17 through the toolchain. A green local run genuinely did not mean a
  green CI run.
- `android:layout_marginHorizontal` is **API 26+** and minSdk is 24 — lint caught it and failed the
  build. Replaced with start/end margins. Exactly the backwards-compatibility check ADR-0007 exists
  to get.

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
