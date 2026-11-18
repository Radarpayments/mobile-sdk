# QA apps — agent notes

Two native apps (`android-app`, Compose; `ios-app`, SwiftUI) that open the SDK's checkout with any configuration. They
are a test tool, not a sample: `samples/` stays the integration reference and is not modified from here. `README.md` is
the contract (JSON documents, gateway requests, theme presets, identifiers, external input); read it before you change
anything. The root `AGENTS.md` rules apply (explicit visibility, code style, no card data in logs, English, commits).

## Parts

The same on both platforms, one directory each (`config/`, `storage/`, ... under the app's package or `QaApp/`).

| Part | Responsibility | Depends on |
|---|---|---|
| `config` | `QaConfig` and `StandProfile` models; JSON reading, writing, validation, launch checks | nothing |
| `storage` | the current configuration and the profiles; credentials in encrypted storage (Keystore / Keychain) | `config` |
| `intake` | accepts a configuration or a profile from outside: deep link, launch argument, clipboard, QR | `config`, `storage` |
| `merchant` | registers an order, queries its status | `config` |
| `theme` | turns a preset name into a `PaymentTheme` | SDK |
| `launcher` | builds `SdkPaymentConfig` from `QaConfig`, opens the sheet through the turnkey checkout API | `config`, `theme`, SDK |
| `journal` | the app's `LogTransport`, entries in memory, gateway exchanges of `merchant` | SDK |
| `report` | builds the run report text | `config`, `journal` |
| `ui` | screens | all of the above |

## Rules

- **`QaConfig` is the single source of truth.** The form only edits it, `launcher` only reads it. A configuration that
  came by link and one typed by hand start the same run, and the report prints exactly what the sheet was opened with.
- **Platform parity.** The platforms share no code, only the contract: `README.md`, `fixtures/cases.json` and
  `fixtures/identifiers.txt`. Both apps run the same fixtures in their unit tests and check their identifiers against the
  list; that is the guard against drift. A change to a field, a rule or an identifier lands in the README, the fixtures
  and both apps together. Fixtures name only `https://dev.bpcbt.com`; the profile fixtures carry the placeholder secrets
  `login-1`, `password-1`, `key-1`.
- **Public SDK API only**, as an integrator uses it. No `@InternalPayrdrApi`, no reaching into the SDK's internals.
- **No baseline.** ktlint and detekt pass without one, and `:qa:android-app` builds in release (CI runs
  `assembleRelease` and `testReleaseUnitTest`). Every Kotlin declaration in the app is `internal` or `private`.
- **Secrets stay out.** A password or an API key never reaches the journal, the report, `toString()`, an exception or
  test output. `merchant` replaces them with `***` in what it journals; the report holds a profile's name and gateway
  URL, never its credentials. Types that carry a secret override `toString()`.
- **Stored data may be unreadable** (written by another build, a Keystore or Keychain entry gone after a restore): the
  app starts with the default config and no profiles instead of crashing.
- **Deliberately out** of the first version: a store front, the headless flow, the classic Activity, the SDK Core
  screen, a per-token theme editor, CI artifacts, Flutter and React Native, UI tests of the apps.
- **iOS compares text by scalars.** Swift's `String ==`, `Dictionary` keys and `Character` use canonical equivalence
  (`"\u{212A}"` equals `"K"`), Android compares chars. `QaApp/Config` reads JSON with its own parser into `JSONValue`
  (numbers keep their text, objects keep their order and scalar keys) and compares through `sameText`, `isBlank` and
  `equalsIgnoringCase` in `TextRules.swift`; keep new rules on those helpers, not on `==` or `Foundation`'s
  `JSONSerialization` / `NSRegularExpression`. The app target isolates to the main actor by default, so `config` types
  and functions are `nonisolated`.
- **Documents are strict JSON on both sides.** kotlinx's `Json.parseToJsonElement` reads `01`, `NaN`, bare words and
  raw control characters, so Android parses document text only through `parseObject` (or after `isStrictJson`, as
  `QaStore.readProfiles` does), never with kotlinx directly; iOS's `JSONValue.parse` is strict already. Text the app
  wrote itself is exempt. A fixture with a repeated key or text that is not JSON uses `inputText`.
- **Do not build the two Xcode projects in parallel** (this one and `samples/ios-app`): both write to
  `sdk-payment-scanner/build/XCFrameworks/debug`. No release linking of iOS frameworks, no whole-repo `./gradlew build`.
