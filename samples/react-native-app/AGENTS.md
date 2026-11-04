# React Native sample — agent notes

Read `../AGENTS.md` (all samples) and `README.md` here first. The module it uses is `plugins/react-native`
(`file:` dependency); see `plugins/AGENTS.md`.

This app **mirrors the Flutter sample file by file** (`x.dart` → `x.ts`/`X.tsx`, same names for functions and types).
When you change one, change the other, or say why not.

## Map (`src/`)

| Path | What it is |
|---|---|
| `App.tsx`, `SampleApp.tsx` | loads `AppPrefs`; screen registry; RTL through the `direction` style (no `forceRTL`) |
| `AppScope.tsx` | React context: prefs, store, `registrarFactory`, `transferRegistrarFactory`, `presentSheet` (test seam); hooks `usePrefs`, `useStore`, `useL10n` |
| `listenable.ts` | a `ChangeNotifier` clone used with `useSyncExternalStore` — the state model of the whole app |
| `navigation.tsx` | a hand-made stack. Not React Navigation: react-native-screens would need changes to `MainActivity` |
| `checkout/checkout.ts` | **the only place the payment sheet is called**: `buildSdkConfig`, `toOutcome`, `presentSheet`, `StoreCheckout` |
| `core/` | SDK Core screen and `coreToken.ts` (`PayrdrCore.generateToken`) |
| `merchant/` | `orderRegistrar.ts` ("your server", same design as Flutter, `fetch` + `AbortController`, unions on `kind`), `sessionId.ts`, `utf8.ts` |
| `prefs/appPrefs.ts` | `Listenable` over AsyncStorage, read once at launch, served from memory; same keys as Flutter |
| `store/` | `CoverScreen.tsx`, the three stores, `model/`, `theme/`; `store/invest/` — Northwind Invest (AFT) |
| `l10n/` | `l10n.ts` (system language — iOS reads `AppleLanguages` — `formatEur`, `isRtl`); `messages.ts` is **generated, do not edit** |
| `ui/` | small shared pieces |

Amounts are euro cents; Hermes on iOS has no `Intl.NumberFormat.formatToParts`, so do not rely on it.

## Plugin link and native setup

- `"@payrdr/react-native-sdk": "file:../../plugins/react-native"` → a symlink in `node_modules`; the plugin ships TS
  source. `metro.config.js` watches the plugin folder and blocks its own `node_modules` (otherwise two copies of
  react-native). Keep those three settings together.
- Run `scripts/build-bridge.sh` from the repo root after any SDK or bridge change, then `pod install` in `ios/` (the
  podspec vendors the XCFramework the script copies). Android reads `build/maven`.
- iOS `Podfile` `post_install` raises each `*_resources` bundle to the app's minimum iOS (AsyncStorage's privacy
  bundle). `Pods/` is ignored, `Podfile.lock` is committed.
- Android: New Architecture + Hermes; fonts via `assets.srcDirs += ["../../assets"]`.
- Toolchain: Node ≥ 22.11, RN 0.88, React 19, CocoaPods ≥ 1.13 (not 1.15.0/1.15.1), see `Gemfile`.

## Texts and fonts

`src/l10n/messages.ts` is made from the Flutter sample's `.arb` files, which are made from the Android sample's
resources. Edit texts in `samples/android-app/src/main/res`, then:

```bash
(cd ../flutter-app && python3 tool/android_strings_to_arb.py && flutter gen-l10n)
node tool/arb_to_ts.mjs
```

`tool/make_fonts.py` (needs fonttools) cuts static weights from the Flutter sample's variable fonts and a Material Icons
subset. A new weight also goes into `UIAppFonts` in `ios/PayrdrSample/Info.plist` and into the Xcode project.

## Tests and lint

```bash
npm test             # jest: app, appPrefs, cart, checkout, invest, l10n, orderRegistrar, sessionId, utf8
npm run lint         # eslint (@react-native)
npx tsc --noEmit
npx prettier --check .
```

- `jest.setup.js` mocks AsyncStorage, clipboard, safe-area, `@payrdr/react-native-sdk` (`PayrdrCheckout`, `PayrdrCore`
  as jest functions) and `Settings` (`AppleLanguages` → `en-US`).
- A new native dependency needs an entry in `transformIgnorePatterns` (`jest.config.js`) and a mock in `jest.setup.js`.
- `l10n.test.ts` checks that every language has every key with the same placeholders.
- Tests fake the registrar and `presentSheet` through `AppScope`; no global HTTP mocking. Test names are lowercase
  behaviour sentences; `testID`s are snake_case and match the Flutter keys.

## Conventions

Prettier: printWidth 120, 2 spaces (the repo `.editorconfig` says 4 — `.prettierrc.js` explains the exception), single
quotes, `arrowParens: avoid`, trailing commas. PascalCase `.tsx` for components, camelCase `.ts` for models,
discriminated unions on `kind`/`step` where Dart uses sealed classes. Doc comments in plain English saying why.
