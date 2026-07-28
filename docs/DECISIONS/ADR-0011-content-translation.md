# ADR-0011 — On-device translation of ad text, with the original as the fallback

**Status:** Accepted · 2026-07-28

## Context

The app's UI is localized into six languages. The **ad content** is not: `description` and
`propertyComment` come from the API in Spanish, always. An English user therefore got English
buttons around three thousand characters of Spanish real-estate prose — which is the part of the
screen they actually came to read.

Localizing the chrome and leaving the content is the more common bug of the two, and it is worse,
because it looks finished.

## Options

| Option | Why not |
|---|---|
| Bundle hand-written translations of the four mock ads | Works only because the mock API is frozen. It is a translation of *this* fixture, not a capability — the first real ad would arrive untranslated, and ~80 000 characters of generated prose in `values-*/` would imply otherwise |
| A cloud translation API | Needs a key. Same objection as Google Maps in ADR-0010 |
| Server-side localized content | Correct in production, and entirely outside this challenge's control |
| **ML Kit on-device translation** | Chosen |

## Decision

`AdTextTranslator` is an interface in `:core:data`; `MlKitAdTextTranslator` implements it with ML
Kit's on-device models. No API key, and it works offline once the model for a language has been
downloaded (~30 MB, once, per language).

Three properties matter more than the translation itself:

1. **The screen never waits for it.** The detail ViewModel emits `Content` with the original text
   first and re-emits with `translatedComment` when it arrives. The first use of a language downloads
   a model; a blank description for that long would be a worse bug than the one being fixed.
2. **Every failure means "show the original".** No model, no network on first use, an unsupported
   language, an exception — all return `null`, and `null` renders the Spanish. A listing the user can
   read in Spanish beats an error message where the description should be.
3. **A translation says it is one.** A "Translated from Spanish" note appears above translated text.
   Machine translation of a legal-ish document presented as the original is dishonest, and a Spanish
   speaker needs to know why the wording changed.

The language to translate into comes from `CurrentLanguage`, a `fun interface` declared in
`:core:data` and bound in `:app` to `AppLocales.current()`. That keeps `:core:data` free of AppCompat
while still letting it ask.

## Consequences

- Translation applies to whatever the API sends, not just the four fixtures — which is the point.
- `:core:data` gains a ~2 MB dependency and the app gains a first-run model download per language.
- Results are cached per (text, language) in memory, so re-opening a screen does not re-translate.
- **Never executed.** ML Kit needs a real device to fetch its models; there is no emulator here. The
  orchestration is tested against a fake translator — content shows while a translation is pending, a
  failure falls back, following the system language asks for nothing — but the ML Kit class itself is
  written and unrun. `TESTING.md` and the README both say so.
- Prices, dates and areas are **not** translated: they are formatted from structured fields, so they
  are already localized by `Formatters` and `java.time`. Only free text goes through the translator.
