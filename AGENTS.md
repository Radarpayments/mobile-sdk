# PAYRDR Mobile SDK — agent notes

The Kotlin Multiplatform payment SDK for Android and iOS (version 4.x), plus thin Flutter and React Native plugins and
four sample apps. `README.md` is the integrator's overview; this file is what you need to change the code safely. Every
module and sample has its own `AGENTS.md` — read the one for the directory you work in.

## Modules

```
sdk-core ◄── sdk-payment ◄── sdk-payment-scanner          (iOS: RadarpaymentsSDKPayment + the Swift scanner)
   ▲              ▲                  ▲
   └──────────────┴──── sdk-bridge ──┘ ◄── plugins/flutter, plugins/react-native  (JSON strings)
```

| Path | What it is | Notes |
|---|---|---|
| `sdk-core/` | headless tokenization: card validation, RSA cryptogram → `seToken` | `sdk-core/AGENTS.md` |
| `sdk-payment/` | the checkout: gateway client, payment flow, 3DS, Google Pay / Apple Pay, UI in Compose (Android) and SwiftUI (iOS) | `sdk-payment/AGENTS.md` |
| `sdk-payment-scanner/` | optional camera card scanner; on iOS it builds `RadarpaymentsSDKPayment` (re-exporting `sdk-payment` and `sdk-core`) and holds the Swift sources of `RadarpaymentsSDKScanner` | `sdk-payment-scanner/AGENTS.md` |
| `sdk-bridge/` | the SDK as a JSON contract for Flutter / React Native; `PayrdrSdkBridge.xcframework` | `sdk-bridge/AGENTS.md` |
| `plugins/` | `payrdr_sdk` (Flutter), `@payrdr/react-native-sdk` (TurboModule) | `plugins/AGENTS.md` |
| `samples/` | demo stores on Android, iOS, Flutter, React Native | `samples/AGENTS.md` |
| `build-logic/convention/` | convention plugins `payrdr.kmp.library`, `payrdr.kmp.compose.android` | below |
| `scripts/build-bridge.sh` | builds what the plugins consume (`build/maven`, the XCFramework, the scanner's Swift sources) | |

Package root: `net.payrdr.mobile.payment.sdk.<module>`. Android namespace is derived from the module name. Maven
coordinates are `io.github.radarpayments:<module>` (Android artifact `<module>-android`), as in SDK 3.x — the group is
not the package name. Source repository: https://github.com/Radarpayments/mobile-sdk.

## Toolchain

- **Gradle needs JDK 17–21.** A newer system JDK (26) fails with a bare "26" error. Use Android Studio's JBR:
  `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew …`.
- Gradle 8.10.2, Kotlin 2.2.21, AGP 8.7.2, Compose Multiplatform 1.10.3, compileSdk 35, minSdk 24, iOS 14.0,
  targets `androidTarget`, `iosX64`, `iosArm64`, `iosSimulatorArm64`. All versions are in `gradle/libs.versions.toml`.
- **The versions are pinned for integrators, not by neglect.** The Kotlin an integrator's app needs follows the Kotlin
  the CMP libraries were built with: CMP 1.10 → apps need Kotlin 2.1+. CMP 1.11 needs Kotlin 2.2+ in apps and drops
  `iosX64`; 1.12 forces compileSdk 37 + AGP 9.1 on every app. Do not bump Kotlin, CMP, AGP, compileSdk, minSdk or the iOS
  minimum without asking; when a bump is agreed, write the reason and "why not newer" as a comment next to the version
  in `libs.versions.toml` (see `kotlin`, `compose-mp`, `androidx-camerax` there).
- The plugins and the Flutter/RN samples build with their own toolchains (newer AGP/Kotlin); that is expected.

## Build setup

- `payrdr.kmp.library` (`build-logic/convention/.../KmpLibraryConventionPlugin.kt`): KMP + Android library, the four
  targets, `explicitApi()`, JVM 17, opt-ins `kotlin.time.ExperimentalTime` and `InternalPayrdrApi`, kotest +
  kotlin-test in commonTest, release-only Android publication. commonTest runs as Android unit tests, not on devices;
  instrumented tests live only in `androidInstrumentedTest`, whose APK targets compileSdk (with minSdk as target,
  recent Android puts a permission screen over the test activity and Compose finds no hierarchy).
- `payrdr.kmp.compose.android`: Compose on the Android target only — the Compose compiler is limited to `androidJvm`
  and the Compose libraries are androidMain dependencies, so the iOS frameworks carry no Compose or Skia. The UI
  texts are `src/commonMain/strings/values*/strings.xml`; `GeneratePaymentStringsTask` turns them into
  Kotlin tables for both platforms and, on Android, `Res.string.<name>` keys in
  `net.payrdr.mobile.payment.sdk.<module.with.dots>.generated.resources` (hence `…sdk.sdk.payment…`). No resources
  ship next to the frameworks.
- Each module declares its own XCFramework and applies BCV (`klib` enabled) and, for the iOS-facing ones, SKIE.
- The root `build.gradle.kts` applies ktlint and detekt to every subproject and publishes the four SDK libraries to
  `build/maven` (`./gradlew publishAndroidToRepo`) for the plugins. It also gives every publication the POM, a javadoc
  jar and, with `SIGNING_KEY` set, signatures for Maven Central; the `staging` repository is `build/staging-deploy`.
- Espresso 3.7 / androidx.test 1.7: older Espresso calls `InputManager.getInstance`, gone on recent Android.

## Commands

Check narrowly: the affected module, debug, one iOS target.

```bash
./gradlew :<module>:testDebugUnitTest            # commonTest + androidUnitTest on the JVM
./gradlew :<module>:iosSimulatorArm64Test        # commonTest + iosTest on the simulator (macOS)
./gradlew :<module>:compileKotlinIosSimulatorArm64
./gradlew :<module>:ktlintCheck :<module>:detekt # :<module>:ktlintFormat fixes formatting
./gradlew :<module>:apiCheck                     # :<module>:apiDump after an intended public API change
./gradlew :sdk-payment:connectedDebugAndroidTest # needs a running emulator/device
./gradlew koverHtmlReportSdk                     # JVM test coverage of the four SDK modules → build/reports/kover/htmlSdk
scripts/build-bridge.sh                          # before running a plugin or Flutter/RN sample
```

CI (`.github/workflows/build.yml`) runs `ktlintCheck detekt verifyVersions`, `assembleDebug assembleRelease`,
`testDebugUnitTest testReleaseUnitTest`, `apiCheck` on Linux and `linkDebugFrameworkIosSimulatorArm64`,
`iosSimulatorArm64Test` on macOS. It does not run instrumented tests, the plugins' tests or the samples' Flutter/RN
tests. Release frameworks are linked only by `release.yml`: three modules linking release at once in one Gradle run
exhaust the 4 GB heap on the 7 GB macOS runner (`OutOfMemoryError: GC overhead limit exceeded`).

**Do not start heavy runs without being asked**: `iosSimulatorArm64MainBinaries`, release XCFrameworks
(`build-bridge.sh release`), whole-repo `./gradlew build`, a fresh-clone CI replay. Release linking of the iOS frameworks
takes minutes and loads the machine. Never run two Gradle builds in parallel; stop emulators and background
tasks you no longer need.

## Rules that apply everywhere

### Public API

- Every module compiles in **explicit API mode**. Default to `internal`; a new `public` declaration must be meant for
  integrators. What modules share with each other and nobody else is `@InternalPayrdrApi`
  (`sdk-core/.../InternalPayrdrApi.kt`: opt-in at error level, hidden from Swift).
- The public surface is guarded by binary-compatibility dumps `<module>/api/<module>.api` (Android) and
  `<module>.klib.api` (iOS). Any change to them is a public API change: run `apiDump`, commit the dumps, and (once
  4.0.0 is out) add a `CHANGELOG.md` entry. Compose compiler output (`ComposableSingletons…`, Android only) and
  serializers also show up in the klib dump after moving code between files — that is expected and goes in the same
  commit.
- detekt requires KDoc on public classes and functions. KDoc states the contract (what the caller gets, when), not the
  implementation.

### Security and data

- **Card data (PAN, CVC, expiry, holder) never go into logs, exceptions, error messages, analytics or test output.**
  Param classes are data classes whose `toString()` prints them. The bridge never echoes a request back (it carries card
  data). The scanner logs no digits.
- Logging goes through `LogTransport` (`ConsoleLogTransport` / `NoopLogTransport` by default). There is no Sentry or
  other crash reporter in the SDK, by decision; an integrator plugs in their own `LogTransport`.
- What the buyer sees on a failure comes from the gateway (decline reason, error code, HTTP status) — never the SDK's
  own exception text, which is developer English with ids and hosts in it.
- **No credentials in the repo**: no merchant logins, passwords, API keys, Google Pay/Apple Pay merchant secrets. The
  public sandbox `https://dev.bpcbt.com` may appear in code and docs. Hosts, merchants and names of banks' or customers'
  test stands must not appear anywhere in the repo (code, tests, samples, docs, commit messages); keep them in local,
  gitignored settings and use anonymised vectors in tests.

### Product decisions

- No new `@Experimental…` Compose API in the SDK's UI: the integrator's app resolves its own Compose version, and an
  experimental API that changed there fails at run time with `NoSuchMethodError` (it happened with
  `SharedTransitionLayout`). The existing opt-ins are deliberate and confined to one file each (a few
  foundation/material ones); a new one needs a reason the user agreed to.
- An order `REVERSED` with feature `VERIFY` and `actionCode 0` is a success (zero-amount payment plus reversal when a card
  is saved). Without VERIFY, REVERSED is a failure. Keep this when touching outcome classification.
- AFT (one-leg P2P transfer, `registerP2P.do`, feature `WITHOUT_TO_CARD`): wallets pay one-off only
  (`bindingNotNeeded=true`); saved cards for AFT are DEBIT bindings only. Do not add wallet bindings.

### Version

SemVer with dotted pre-releases (`beta.10` sorts after `beta.9`; `beta10` would not). The first release is SDK
`4.0.0-beta.1` with plugins `1.0.0-beta.1`. Branches hold `X.Y.Z-SNAPSHOT`: released versions exist only in release
commits.

**Releases** come from `.github/workflows/release.yml` (modelled on the 3.x `android-sdk` and `ios-sdk` pipelines), run
by hand from the branch to release with a stage, an optional bump and a mode:
- Stages: `alpha` from any branch; `beta`, `rc` and `stable` from `main` only. A version goes through them forward only
  (any may be skipped): once 4.1.0 has a beta, other branches' alphas go to 4.2.0. A stable release after an rc is
  built from the commit that rc was built from.
- `scripts/release-version.sh <stage> [major|minor|patch]` computes the versions from the tags (`4.1.0-rc.2`, no `v`,
  as in 3.x) and enforces those rules. The stage counters are shared by all branches. The plugins take the same bump
  and suffix (SDK `4.1.0-rc.2` ↔ plugins `1.1.0-rc.2`).
- Modes: `dry-run` (default) builds, signs and packs everything into the workflow artifacts; `validate` also has
  Maven Central validate the bundle and drops it; `publish` releases. Pull requests touching the pipeline run a dry run.
- The workflow runs `build.yml` on the commit, and `release` starts only after it passed: if the macOS runner never
  came and the build was cancelled, re-run the workflow. Then on macOS: release XCFrameworks (`RadarpaymentsSDKPayment`,
  `RadarpaymentsSDKCore`, the bridge), every publication signed into `build/staging-deploy`
  (`publishAllPublicationsToStagingRepository`; POM and signing in the root `build.gradle.kts`),
  `scripts/package-release.sh` (the Central bundle, `Package.swift` with the three products and the two zips'
  checksums, AARs, plugin packages, notes), and `.github/spm-check` — a Swift package that links the products like an
  integrator and checks the version, the SwiftUI checkout, the Swift conveniences and that the scanner plugs in; SDK
  Core alone has a test bundle of its own, since an app holds one of the two frameworks.
- `publish`: Maven Central (`.github/scripts/central-upload.sh`, Central Portal API; not for alphas, cannot be undone),
  then the release commit and its tag; the commit stays off the branches, only the tag is pushed. Then the GitHub
  release (XCFramework zips, the scanner's sources, AARs, the Flutter and React Native packages; an alpha also the
  Maven repository as a zip) and the SPM check against the released tag. For a stable version the workflow also opens
  a pull request
  `release/<version>` → `main` with the release commit (the dated CHANGELOG section) and the next patch `-SNAPSHOT`
  (opened with the workflow's token, it runs no checks until someone closes and reopens it), and deletes the GitHub
  releases of the version's pre-releases (tags stay).
- `main` is protected: changes come only through pull requests whose `android` and `ios` checks (`build.yml`) pass,
  for admins too; no force pushes. Work on a feature branch.
- Secrets, as in 3.x: `SIGNING_KEY` (armored, fingerprint checked against `SIGNING_KEY_FINGERPRINT`),
  `SIGNING_PASSWORD`, `MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_PASSWORD` (Central Portal user token). The namespace
  `io.github.radarpayments` is verified on Central since 3.x.
- npm and pub.dev are not used yet. When they are: beta, rc and stable only (npm dist-tags `beta`, `next` for rc,
  `latest` for stable), and a published version there cannot be deleted either.

**Change versions only with `scripts/set-version.sh <sdk-version> [<plugin-version>]`**:
- The SDK version lives in `gradle.properties` (`version`) alone. The `payrdr.kmp.library` convention generates
  `internal const val SDK_VERSION` into each module (`build/generated/sdkVersion`), which `SdkCore.VERSION` and
  `SdkPayment.VERSION` return, and writes it without the pre-release (`4.0.0`, all Apple accepts) into the iOS
  frameworks' `Info.plist`.
- The plugins depend on the SDK version as text (`payrdrSdkVersion` in `plugins/*/android/build.gradle*`): they are
  built in the app, outside this build. The plugins' own version is `plugins/flutter/pubspec.yaml` (the plugin's
  Gradle file and podspec read it) and `plugins/react-native/package.json` (its podspec reads it), plus the lockfiles
  that record them.
- `./gradlew verifyVersions` (in CI) fails when the plugins' SDK version or their two versions disagree.

## Code style

- ktlint 1.3.1 (`android = true`), `.editorconfig`: 4 spaces (2 for yaml/json/md), max line 140, no wildcard imports.
  detekt with `config/detekt/detekt.yml`, `maxIssues: 0`; only `samples/android-app` has a baseline. Do not grow a
  baseline to silence a new finding — fix it.
- English everywhere: code, comments, tests, docs, commits.
- Comments say **why** — the platform bug, the gateway quirk, the consequence — in plain sentences. No comments that
  restate the code, no section dividers, no commented-out code, no TODO without context. Match the density of the file
  you are in.
- One concern per file. Large files are split by responsibility (see recent `MSDK-110` commits), behaviour unchanged.
- Constants in a `private companion object`, UPPER_SNAKE. `require` / `check` / `error()` for contract violations.
- Platform files: `X.android.kt` / `X.ios.kt` (or the same name in each source set); `expect`/`actual` only where the
  platforms really differ.
- Tests: `kotlin.test` `@Test` + kotest matchers (`shouldBe`), names are backtick sentences
  (`` `an empty card number is required` ``). In `iosTest` use snake_case names: Kotlin/Native rejects some characters
  (commas) in backtick names. commonTest is not run on devices: names with spaces cannot be dexed below API 30.

## Changelog and commits

- `CHANGELOG.md` follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and SemVer, and records changes
  **between released versions** only. 4.0.0-beta.1 is the first release of this SDK: until it is tagged, do not add
  entries — everything so far is part of it, and git history keeps the details.
- From then on, add each change an integrator would notice to `## [Unreleased]` under `### Added`, `Changed`,
  `Deprecated`, `Removed`, `Fixed` or `Security`. Internal refactors, tests and sample-only changes stay out. Each entry
  is a `- ` bullet wrapped at ~120 columns: what changed for the app, why (the symptom and its cause for a fix), and
  what the app must do. Names in backticks; dependency bumps as `A → B`. On release `[Unreleased]` becomes
  `## [x.y.z] - YYYY-MM-DD`.
- Commits: `<TICKET> | <Area>: <one sentence about the outcome>` (e.g. `MSDK-110 | Payment flow: the order outcome, the
  wallets and the card form leave the controller`), then a prose body explaining why. Commit only when asked.

## Where things are documented

- Integrator docs: `README.md`, `sdk-payment-scanner/README.md`, `sdk-bridge/README.md` (the JSON contract),
  `plugins/*/README.md`, `samples/README.md` (the demo and developer settings). Keep them true when behaviour changes.
- Agent docs: the `AGENTS.md` in each directory. When you learn something non-obvious that the next agent would trip
  over, add it to the nearest one.
