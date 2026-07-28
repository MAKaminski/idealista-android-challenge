# ADR-0009 — Per-app language through the platform, not a private preference

**Status:** Accepted · 2026-07-28

## Context

The app ships in five languages — English, Spanish, French, Portuguese and Italian — and needs a
settings screen where the user picks one. The obvious implementation is a `SharedPreference` holding
a language tag, read at startup and used to wrap every `Context` with a `Configuration` override.

That obvious implementation is wrong on modern Android, for a reason that only shows up after
shipping: since Android 13 the **system** owns per-app language. It lists every app that declares
supported locales under Settings → Apps → *App* → Language, and stores the user's choice itself. An
app that also keeps its own preference has two sources of truth that silently disagree the moment
the user changes it from the system screen.

## Decision

Use `AppCompatDelegate.setApplicationLocales` / `getApplicationLocales`, and store nothing.

- **API 33+** — AppCompat forwards to the framework's `LocaleManager`. The choice is stored by the
  system, and the in-app picker and Android's own language screen are two views of one value.
- **Below 33** — AppCompat backports it, persisting through the `AppLocalesMetadataHolderService`
  with `autoStoreLocales=true` declared in the app manifest. Without that service the picker works
  until the process dies and then quietly forgets.

`androidResources { generateLocaleConfig = true }` generates the `<locale-config>` and points the
manifest at it, which is what makes the app appear in the system list at all. It needs
`res/resources.properties` naming the language the unqualified `values/` folder is written in
(`unqualifiedResLocale=en-US`); AGP fails the build without it. `localeFilters` names the five
languages explicitly so a transitive dependency's translations cannot silently add a sixth.

Structurally:

| Piece | Where | Why there |
|---|---|---|
| `AppLanguage` — the five languages, tag parsing | `:core:model` | Pure Kotlin, so tag matching is JVM-testable |
| `AppLocales` — apply and read the selection | `:core:designsystem` | The one place that touches `AppCompatDelegate` |
| `SettingsScreen`, `SettingsFragment` | `:feature:settings` | A screen, so it is a feature module |

`SettingsFragment` deliberately has **no ViewModel**. There is no state to hold: the selection lives
in the delegate, and applying one recreates the activity. A ViewModel would only mirror a value it
does not own, and the mirror is exactly what goes stale.

The picker offers **"System default"** alongside the five languages, and each language is labelled
with its **endonym** — Español, Français, Português, Italiano — above its name in whatever language
is currently showing. A user who lands in Italian by accident cannot read "Italian"; they can find
"Italiano".

## Consequences

- The choice survives process death, restart and reinstall-from-backup without a line of persistence
  code, and it stays in step with the system screen.
- Applying a language **recreates the activity**. That is the framework's mechanism, not a bug; it
  is why the Compose picker holds the tapped value in local state, so the radio moves immediately
  rather than after the new activity reads the delegate back.
- The regional-variant behaviour is deliberate: `es-419`, `es-ES` and `pt-BR` all resolve to their
  primary language, so a device in Mexican Spanish gets Spanish rather than falling through to
  English. The Portuguese translations are European.
- **The round trip is not unit-testable.** Robolectric's sandbox has no per-app locale store, so on
  the API 33+ path `setApplicationLocales` is a genuine no-op there and reads back empty. Tests were
  written, confirmed to be testing the sandbox rather than the app, and deleted rather than
  weakened. What *is* tested is the tag logic in `:core:model` and the picker's behaviour in
  `:feature:settings`; the two-line delegate call between them is verified by running the app.
- Adding a sixth language is a `values-xx/` folder, one entry in `AppLanguage`, one entry in
  `localeFilters`, and one `settings_language_*` string in each of the six locales. Lint's
  `MissingTranslation` fails the build on any of those being forgotten — verified by deleting a
  string and watching it go red.
