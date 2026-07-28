# idealista Android challenge — submission

An Android app for browsing property ads: a list screen, a detail screen, and favorites that show the
date each ad was saved. Kotlin, XML views, multi-module, 53 tests, CI.

The original brief is preserved verbatim at the bottom of this file.

## Run it

```bash
./gradlew runDebug              # build, install AND launch on a running emulator/device
./gradlew testDebugUnitTest     # 53 tests
./gradlew lint assembleDebug
```

`runDebug` exists because `installDebug` only installs — it does not start the app, and `adb` is
often not on `PATH`. The task finds `adb` via `local.properties` or `ANDROID_HOME` and launches the
activity, so nothing has to be done by hand.

Needs the Android SDK (`platforms;android-37.1`, `build-tools;37.0.0`) and a JDK 21 to run Gradle;
the build itself targets JDK 17 through a toolchain it provisions automatically. Every green CI run
also publishes a debug APK as an artifact, so the app can be sideloaded without a local toolchain.

## Requirements → where they are

| Requirement | Status | Where |
|---|---|---|
| **Kotlin** | ✅ | Every module |
| **XML views** for the two screens | ✅ | `:feature:list`, `:feature:detail` — ViewBinding, no findViewById |
| **List screen** | ✅ | `AdListFragment` — RecyclerView + ListAdapter + DiffUtil, Coil, swipe-to-refresh |
| **Detail screen** | ✅ | `AdDetailFragment` — collapsing toolbar, ViewPager2 gallery, characteristics, energy certificate |
| **Favorite an ad** | ✅ | `favorites` table + `AdRepository.toggleFavorite` |
| **Show the favorited date** | ✅ | On the list card, the detail screen and the favorites screen — one source, three surfaces |
| **Use AI tools** | ✅ | [`docs/AI_USAGE.md`](docs/AI_USAGE.md) — including what the AI got *wrong* |
| *Bonus* — tests | ✅ | 53 automated tests, including image-to-ad alignment, [`docs/TESTING.md`](docs/TESTING.md) |
| *Bonus* — Compose alongside XML | ✅ | A Compose-only favorites screen **and** a `ComposeView` inside the XML detail screen |
| *Bonus* — persistent storage | ✅ | Room: ad cache + favorites, survives restarts and offline |
| *Bonus* — AI context files | ✅ | [`CLAUDE.md`](CLAUDE.md) |

## The one thing worth reading first

`detail.json` **always returns the same payload** (ad 1) no matter which ad you open — the brief says
so in passing, and it is the trap in this challenge. It also uses a different id type (`Int adid` vs
`String propertyCode`) and a different price shape than the list endpoint.

Binding that response straight to the UI produces an app that looks perfect on ad 1 and silently
shows the wrong price, address, location **and photos** for ads 2–4. This app instead **merges**:
identity — including the photo gallery — comes from the cached list ad, while characteristics, the
energy certificate and the description come from the detail response.

The photo half of that was a real bug, caught by a human looking at the app rather than by the suite;
the fix and the alignment tests that now prevent it are in `docs/DELIVERY_LOG.md`.

See [`docs/DECISIONS/ADR-0005-detail-merge-strategy.md`](docs/DECISIONS/ADR-0005-detail-merge-strategy.md).

## Architecture

```
:app                  Application, MainActivity, bottom navigation, screen wiring
:core:model           pure-Kotlin domain models — no Android dependencies
:core:data            Retrofit + kotlinx.serialization, Room, mappers, AdRepository
:core:designsystem    Material 3 theme + Compose theme from the same tokens, shared formatters
:core:testing         shared test fixtures
:feature:list         XML list screen
:feature:detail       XML detail screen + one embedded ComposeView
:feature:favorites    Compose-only screen
```

Room is the single source of truth: the network only writes into the cache, the UI only reads from
it. `observeAds()` is one `combine` of ads and favorites, which is why the favorited date is
identical on all three screens without any screen knowing how favorites are stored.

MVVM with one `StateFlow<UiState>` per screen, sealed states, collected under `repeatOnLifecycle`.
Hilt for DI. Dispatchers and the `Clock` are injected, so timestamps are asserted exactly in tests.

Full detail in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Documentation

| Document | What it answers |
|---|---|
| [`docs/DELIVERY_LOG.md`](docs/DELIVERY_LOG.md) | What was built, in what order, and the actual command output proving it |
| [`docs/AI_USAGE.md`](docs/AI_USAGE.md) | Which AI tools, what they got wrong, how it was caught |
| [`docs/API.md`](docs/API.md) | Both endpoints, every field, and the three quirks |
| [`docs/TESTING.md`](docs/TESTING.md) | Test tiers, and which cannot run headless |
| [`docs/DECISIONS/`](docs/DECISIONS/) | 7 ADRs |
| [`CLAUDE.md`](CLAUDE.md) | Project context for AI tools |

## Known limitations

- **The Espresso end-to-end test is authored and compiles, but has never been executed** — it needs a
  device, and neither the development environment nor CI has one. It is marked as such everywhere
  rather than counted as passing.
- **Coil is pinned to 3.4.0**, not 3.5.0: the newer build ships Kotlin 2.4 metadata that AGP 9's
  built-in Kotlin 2.2.10 compiler cannot read. Same reason Kotlin itself is not on 2.4 — KSP has not
  shipped it (ADR-0001).
- **Robolectric is pinned to SDK 36** while the app targets 37; Robolectric has no API 37 runtime yet.
- The detail screen shows no map. The coordinates are parsed and available; a map needs an API key,
  which a submission should not carry.

---

# The original brief

idealista Android crew needs you! We need a fellow to face our everyday challenges: new features, problem fixes, UI design, performance, security, backwards compatibility, testing...

We need your help to build the next amazing features that will bring our user experiences to the next level, are you ready to go?

We love clean code and beautiful layouts, structured implementation and testable components. Does it sound good to you? This is your challenge!
&nbsp;

### 🚀 Getting started
- Read the minimum requirements.
- Start a new project from scratch.
- Think, design, code and have fun!
&nbsp;

### 📱 Task
Build an app that allows users to browse through a list of ads and view ad details on a separate screen.
&nbsp;

### 🌐 API endpoints
- List: [https://idealista.github.io/android-challenge/list.json](https://idealista.github.io/android-challenge/list.json)  
- Detail: [https://idealista.github.io/android-challenge/detail.json](https://idealista.github.io/android-challenge/detail.json) *Please note: the response is always the same*.
&nbsp;

### ✅ Minimum Requirements
- The app should include at least **two screens**:
  - A **listing screen** displaying a collection of ads.
  - A **detail screen** for viewing ad information.
- The code must be written in **Kotlin** and use **xml views**.
- Implement feature to allow users to **favorite ads**.
  - If an ad is favorited, display the **date** it was favorited.
- **Use AI tools** (e.g. GitHub Copilot, ChatGPT, Claude, Cursor, etc.) during the development of the challenge. We want to see how you leverage these tools in your workflow.
&nbsp;

### 🎁 Some optional tasks to do (bonus):
- Tests of different types could be great idea.
- Some **Jetpack Compose** code alongside xml.
- Implement **persistent storage**.
- Feel free to go beyond the requirements and **improve the app** in any way you think is best — we love creativity!
- If you used any **AI context files** to help the AI tool understand your project (e.g. `CLAUDE.md`, `.cursorrules`, `.github/copilot-instructions.md`, system prompts, or similar harness/context engineering files), include them in your submission.
&nbsp;

### 🥳 Once you've finished
- Email us at [android@idealista.com](mailto:android@idealista.com) with your repository link you'd like our Android team to review, or send the project folder (including the `.git` directory).
- Celebrate after a well done job! 🥳
