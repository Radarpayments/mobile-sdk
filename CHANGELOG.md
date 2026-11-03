# Changelog

## [Unreleased]

### Public API

- The public API is what an app needs to take a payment: `PayrdrCheckout` (Android and iOS), the configs
  (`SdkPaymentConfig`, `CheckoutConfig`, Google Pay and Apple Pay configs, `SslPinningConfig`, `PaymentTheme`),
  `CheckoutResult` and the `Sdk*Exception`s, logging through `LogTransport`, the scanner's `CardScannerProvider`, the
  headless `SdkPayment` flow and SDK Core's token generation. The rest is `internal`, and every module is built in
  Kotlin's explicit API mode.
- No longer public: the gateway client and its DTOs (`PaymentApi`, `PaymentApiImpl`), HTTP client factories, the key
  source (`KeyProvider`, `RemoteKeyProvider`), the wallet token providers and request builders, `Logger`, the UI
  components, screens and formatters (`CardFormatters`, `CardNetworkResolver`, `CardNetworkIcon`,
  `PaymentThemeProvider`, `PaymentCheckout`), `MoneyFormatter`, `SessionIdConverter`, SDK Core's validators, rules,
  cipher and string helpers, and the scanner's camera, parser and state classes.
- Removed: `SdkPaymentNetwork` and `SdkPayment.create(config, network)`, the wallet parameters of `SdkPayment.newFlow`,
  `CardNumberField`, `ExpiryField`, `CvvField`, `CardHolderField`, `NativeToggle`, `BottomSheetContainer`,
  `CheckoutUiAction` with `reduceUiAction` and `rememberPaymentCheckoutState`, `AndroidApplePayTokenProvider`,
  `IosGooglePayTokenProvider`, `PaymentEffect.ShowError`, `PaymentIntent.ThreeDsCompleted`,
  `installIosCrashLogger`, and the unused `finish3dsVer2PaymentAnonymous` call.
- `MlKitVisionCardScannerProvider` takes a `logTransport` instead of a `Logger`.

### Flutter and React Native

- New module `sdk-bridge`: the SDK for apps that are not written in Kotlin or Swift. It takes the checkout config and
  SDK Core requests as JSON and gives the results back as JSON (contract and tests in `sdk-bridge/README.md`), so the
  plugins hold no mapping of their own. Android: `sdk-bridge-android`; iOS: `PayrdrSdkBridge.xcframework`, one framework
  with all of the SDK.
- Flutter plugin `plugins/flutter` (`payrdr_sdk`): `PayrdrCheckout(PayrdrConfig(...)).present(mdOrder)` →
  `CheckoutResult` (sealed), `PayrdrCore.generateToken(...)`. iOS through Swift Package Manager (Flutter's default) and
  CocoaPods. `MainActivity` stays a `FlutterActivity`.
- React Native module `plugins/react-native` (`@payrdr/react-native-sdk`): a TurboModule (RN 0.76+) with the same API in
  TypeScript. `MainActivity` and `AppDelegate` need no code.
- Neither needs Android manifest entries: the bridge declares Google Pay (and depends on `play-services-wallet`), the
  scanner declares the camera.
- `scripts/build-bridge.sh` builds what the plugins take from this repository: the Android libraries into `build/maven`
  (`./gradlew publishAndroidToRepo`) and the XCFramework into each plugin.
- Sample apps `samples/flutter-app` and `samples/react-native-app`: the same three stores, SDK Core screen and developer
  settings as the native samples.

### Accessibility: screen readers

- iOS: VoiceOver reaches the card number, expiry and CVC fields (they were left out of its reach), and the Apple Pay
  button, named by PassKit ("Buy with Apple Pay"). The card fields follow the system text size.
- The merchant screen under the checkout is hidden from VoiceOver, and the screen under an open picker, the camera or a
  confirmation is hidden from both screen readers.
- A sheet's text is read line by line: it was one nameless button with all its text, and so were the scrim and handle.
- Every field keeps its name once filled and carries its error; errors are read out as they appear, and the card group
  shows each of them, not only the first.
- Steps announce themselves (pane title on Android, "screen changed" on iOS); the result and each processing stage are
  read out. The loading skeleton reads as loading, not as stand-in cards.
- Saved cards, countries and transfer options read as selected or not. Flag emoji are no longer read on Android.
- The scanner's camera intro has a named close button; the scanner says when it found a card and reads out why it
  stopped.
- No developer names in screen-reader output: `"CardFormScreen"`, `"ResultScreen"`, `"SuccessIcon"` and the like are
  test tags now (`onNodeWithTag`).
- `NativeTextField` takes `label` and `errorMessage` for screen readers.

### Android: checkout from any activity

- `PayrdrCheckoutIntent.create(context, config, mdOrder)` / `parseResult(resultCode, data, mdOrder)`: the sheet with
  `startActivityForResult`, for a screen that cannot register an activity result (a plain `Activity`, Flutter, React
  Native). `PayrdrCheckout` stays the way for `ComponentActivity` and Compose.

### Android: no "save password" prompt for the card form

- With a CVV and an e-mail in the form, Google Password Manager offered to save a "password" when the checkout closed.
  Since Compose UI 1.8 autofill reads the fields from their semantics: the masked CVV had no type there and read as a
  password, the e-mail as its user name, while the old autofill nodes the SDK still added carried the types but no
  values. The fields now declare their type in their semantics (card number, expiry, CVV, e-mail, phone, address), and
  the old nodes are gone: autofill sees the CVV as card data and offers no password to save.

### AFT transfers

- The checkout pays one-leg AFT transfers: an order registered with `registerP2P.do` and the feature `WITHOUT_TO_CARD`
  (the buyer's card is debited, the money goes to the recipient the merchant registered). Nothing changes for the app:
  `present(mdOrder)` as for a payment; the SDK reads the order from `getSessionStatus.do`
  (`orderPaymentWay = P2P`, `orderFeatures`).
  - The card goes to `p2p/perform.do` (a saved debit card to `p2p/performByBinding.do`, by its id when
    `cvcBindingNotRequired`) instead of `processform.do`; 3-D Secure and the result as for a payment
    (`getFinishedPaymentInfo.do`).
  - The sheet asks for the sender's and the recipient's details the gateway lists in `payerDataParamsNeedToBeFilled`
    (`PAYER_*`, `RECIPIENT_*`, now in `PayerField`): names, birthday (typed `DD.MM.YYYY`, sent `YYYYMMDD`; the input
    takes only digits that can still make a real day from 1900 to today: `32`, `31.04`, `29.02.2023` or a future day do
    not go in, `4` becomes `04`, `1.` becomes `01`), account and its type, document and its type, the recipient's
    address and country; values the merchant registered are filled in.
    An account type registered as a code comes back from `getSessionStatus.do` as a name (`7` → `WALLET_ID`); both
    forms now select it in the picker. Under the contacts come the billing address, then the sender, then the
    recipient.
    The button says "Transfer".
  - Google Pay and Apple Pay pay a transfer too: the wallet's token goes to `p2p/perform.do` as
    `fromCard: {vendor: GOOGLE | APPLE, paymentToken}`, with `bindingNotNeeded: true` — a wallet card is never saved.
    The wallets are not offered when the order demands a saved card (`FORCE_CREATE_BINDING`, `VERIFY`) or asks the
    buyer for a mandatory detail (a wallet has no form to send it). On the gateway the merchant needs `USE_APPLEPAY`
    for Apple Pay and `GOOGLE_PAY_CARD_ALLOWED` / `GOOGLE_PAY_TOKENIZED_ALLOWED` for Google Pay.
  - Saved cards, as the gateway's own payment page has them (UI-6467): only debit cards (`bindingCategory = D`) of a
    buyer with `bindingEnabled` are offered, also with `BINDING_NOT_NEEDED`, which forbids saving a new card only.
    "Save card" is offered to a merchant with `DEBIT_BINDING_ALLOWED`, ticked and locked with `FORCE_CREATE_BINDING`.
    `bindingNotNeeded` is always sent, and is false whenever the order demands a saved card (`FORCE_CREATE_BINDING`,
    `VERIFY`), even with no box to show: true would cancel what the merchant asked for.
  - An AFT order that only saves a card (`VERIFY` with `FORCE_CREATE_BINDING`, a zero amount): the sheet asks for a new
    card only, with no saved cards, no "Save card" box and no amount; the button says "Save card", the result "Card
    saved". The gateway makes a zero payment and takes it back, so `REVERSED` with action code 0 is
    `CheckoutResult.Completed` for such an order (also when it is opened again). `PaymentState.CardFormShown.saveCardOnly`
    and `FinishedPaymentInfoResponse.orderFeatures` are new; `CardFormScreen` takes `saveCardOnly`.
  - A transfer the bank pays without its page answers `errorCode 0` with `errorMessage` "Successful": only a non-zero
    code is a refusal, the outcome comes from `getFinishedPaymentInfo.do` (it was shown as a decline reading
    "Successful" while the money had gone).
  - Other P2P orders (OCT, two-leg) fail with `SdkNotConfiguredException`.
  - New in the API: `PaymentApi.p2pPerform` / `p2pPerformByBinding`, `P2PPerformRequest`, `P2PFromCard`,
    `PaymentState.CardFormShown.transfer` / `saveCardByDefault` / `saveCardLocked`, `CardFormScreen(transfer, …)`,
    `SessionStatusResponse.orderPaymentWay` / `orderFeatures` / `cvcBindingNotRequired` / `billingRecipientData`,
    `BindingItem.bindingCategory`. A transfer's seToken always carries a cardholder (the P2P gateway refuses an empty
    one): the typed name, else the sender's, else `CARDHOLDER`.
  - The Flutter and React Native plugins get it with the rebuilt bridge (`scripts/build-bridge.sh`).
- sdk-core: the seToken writes the expiry month with a leading zero (`202802`, not `20282`): the P2P gateway reads it
  by position and answered "System error".
- Sample apps (all four): developer settings → API V1 → Order: "Payment" / "AFT transfer" (`registerP2P.do`). With
  AFT the start screen shows Northwind Invest instead of the stores: an investment account topped up through the
  sheet, registered with the investor's details so the sheet asks only for the card; the balance grows only on a
  completed transfer.
- `AftLiveTest` runs a transfer against a real gateway when `PAYRDR_AFT_BASE_URL`, `PAYRDR_AFT_LOGIN` and
  `PAYRDR_AFT_PASSWORD` are set.

### API V2 sessions

- The bank page of an API V2 session (`CheckoutConfig.SessionId`) whose `successUrl`/`failureUrl` are the merchant's own
  pages did not end: after the challenge the gateway sends the buyer to `{successUrl|failureUrl}?sessionId=ps_…`, not
  to `sdk://done`, so the page opened the merchant's site inside the checkout and waited five minutes; closing it
  reported "canceled" for a payment that had gone through. A page whose query names the session now ends the challenge
  like `sdk://done` does: new `ThreeDsChallenge.sessionId` and `ThreeDsChallenge.isReturnUrl(url)` (use it in your own
  `WebChallengeHost`).
- A session the merchant expired (`POST /sessions/{id}/expire`) or an order otherwise declined before it was opened
  fails with the gateway's code and wording (`SdkDeclinedException.declineCode = "DECLINED_BY_MERCHANT"`) instead of a
  bare "Order is declined".
- `ApiV2LiveTest` checks a session against a real gateway when `PAYRDR_API2_KEY` holds the merchant's API key.
- Sample apps (Android, iOS, Flutter, React Native): developer settings switch between API V1 (`register.do`, login
  and password → `present(mdOrder)`) and API V2 (`POST /api2/sessions`, API key → `presentSession(sessionId)`; the
  buyer with cards goes as `merchantCustomerId`).

### Expired session

- A payment sent after the order's session expired ended as "unknown" ("the bank has not answered yet"), although the
  gateway had declined the order and charged nothing. On an expired session `processform.do` answers with only
  `errorCode: 0`, and `getFinishedPaymentInfo.do` with only `{"sessionExpired": true}`, with no `status`, so the SDK asked
  again and gave up. The checkout now fails with the new `SdkSessionExpiredException` (`session_expired` in the bridge,
  Flutter and React Native), says the time to pay ran out and nothing was charged, and offers no "try again": the order
  cannot be paid any more.
- An order opened after its session expired fails the same way, instead of "order not found".

### Google Pay token

- The Google Pay token went to `google/paymentOrder.do` as Google returns it, a JSON string, but the gateway decodes
  `paymentToken` from base64 (its own payment page and the previous Android SDK encode it), so the payment could not be
  decrypted. The SDK now encodes it. Apple Pay's token was already sent in base64.

### 3-D Secure page on iOS

- The bank page no longer answered "bad pareq": the form was encoded with Foundation's query character set, which leaves
  `+` as is, and the bank read the `+` of base64 PaReq as a space. The form is now built by `AcsForm` (everything but
  RFC 3986's unreserved characters escaped), tested in common code.
- The page stays below the bars when scrolled: the navigation bar is see-through on iOS 26, and the full-screen WebView
  scrolled under the status bar and the close button. It is now pinned to the safe area.

### 3-D Secure page on Android

- The bank page no longer slides under the status bar: the system bar insets were set as the WebView's padding, which a
  WebView ignores for its page; they now go on a container around it.
- The bank's one-time code is no longer offered for saving to the password manager: the page's WebView is excluded from
  autofill.

### Dependencies

- Compose Multiplatform 1.7.0 → 1.9.3 (Kotlin 2.1.0 → 2.1.21, kotlinx-datetime 0.6.1 → 0.7.1). From 1.9 Compose finds a
  framework's resources inside it (`App.app/Frameworks/*.framework/composeResources`), which is what lets the bridge
  framework carry the sheet's strings and images without a build phase in the app. kotlinx-datetime moved `Instant` and
  `Clock` to `kotlin.time`, experimental until Kotlin 2.3: the library modules opt in to `kotlin.time.ExperimentalTime`.
- `ComposeUIViewControllerConfiguration.opaque` (the transparent layer under the iOS sheet) is now
  `@ExperimentalComposeUiApi` instead of `@ExperimentalComposeApi`; the opt-in moved with it, the API is the same.
- Build logic: `-Xskip-metadata-version-check`, because Gradle 8.10 compiles it with Kotlin 1.9 and the Compose plugin is
  built with Kotlin 2.x.
- Compose Multiplatform 1.9.3 → 1.10.3, Kotlin 2.1.21 → 2.2.21: the 1.10 libraries are built with Kotlin 2.2.20, and an
  older compiler cannot read them. What it means for apps:
  - Android: the SDK's libraries carry Kotlin 2.2 metadata; an app that compiles Kotlin against them needs Kotlin 2.1
    or newer (Kotlin 2.0 fails with "incompatible version of Kotlin"). compileSdk 35 and AGP 8.6+ are still enough.
  - iOS: nothing changes; the frameworks keep the x86_64 simulator slice.
  - Not 1.11: its libraries are built with Kotlin 2.3 and need Kotlin 2.2 in the app, and they have no `iosX64`
    builds. Not 1.12: its Android libraries need compileSdk 37 and AGP 9.1, and so would every app using the SDK.
- Compose libraries are named in the version catalog: the plugin's `compose.*` accessors are deprecated since 1.10.
  Material is versioned on its own (material3 1.9.0, material-icons-extended 1.7.3, the versions the 1.10.3 plugin pairs
  with).
- The API dumps also changed where the compilers did: Kotlin 2.2 lists the `$default` bridges of interface methods with
  default arguments, and the Compose compiler no longer emits some `$stableprop` fields and `ComposableSingletons`
  classes.

### sdk-core

- `SdkCore.generate` validated fields through a map keyed by their values, so fields with equal values
  (several empty ones) were checked by one validator and reported under one field — an empty card number
  came back as `CVC: required`. Each field is now checked by its own validator, in form order
  (card number, expiry, CVC, holder, mdOrder, key; saved cards: bindingId, CVC, mdOrder, key).

### Sheet and keyboard

- Going back from a step with the keyboard up (the saved card's CVV) closes the keyboard at once, so it goes
  down with the step sliding back instead of after it.
- iOS: the sheet follows the keyboard as it slides; Compose's ime inset changes only once the keyboard has
  stopped, so the sheet used to stay up and then drop in one frame.
- Steps in the sheet sit on its bottom edge: a taller step rises above a shorter one, and the pay button of the
  step underneath no longer shows below the one sliding over it.
- Switching steps no longer tears the sheet: steps are clipped to its rounded top (no square corner riding over
  it), the sheet's height animates with the slide instead of on its own, and a field's keyboard opens only once
  the step has stopped (it used to cut the slide short).
- Height changes inside a step (loading → payment methods, an error line) animate with the content held under
  the sheet's top edge, so nothing jumps inside the sheet; a long form that fills the screen still follows the
  keyboard frame by frame.
- Opening the keyboard on the new-card form no longer lifts the sheet over the keys and drops it back: the pay
  button gives its height back as the keyboard rises, and the sheet follows it.
- iOS: the payer's fields (contact, billing, transfer sender and recipient) are Compose fields, as on Android; only the
  card fields stay UITextFields. The autofill type reaches the keyboard through `PlatformImeOptions` (experimental in
  Compose Multiplatform, and the way JetBrains documents for autofill on iOS), and the number and phone pads get the
  same system "Done" bar as the card fields. They draw with the sheet while a step animates, with no stand-in, and the
  date of birth now gets the number pad on iOS too (it had the letter keyboard). Focus passes between a card field and
  a payer field with one caret: Compose 1.10 gives up its focus when a UIKit field takes the keyboard.

### Sheet motion

- One step change for the whole checkout: the sheet's content zooms as one — going deeper it grows out of 0.96 as
  the old step fades and grows to 1.04, going back the other way — while the sheet's height follows. This replaces
  the sideways slides above. Durations and curves live in one place (`SheetMotion`).
- The sheet rises at once with its real header. A session that comes in within 300 ms skips the skeleton; a slower
  one shows a pulsing skeleton built from the same components as the payment sheet (wallet only when configured,
  as many card rows as the last session on this device had — only the count is kept), held at least 400 ms and
  filled in place.
- Processing, the 3-D Secure wait and the result open inside the same sheet instead of full-screen pages; the
  processing stage reads under the headline. For 3-D Secure the sheet opens to the whole screen first and the bank
  page appears over it with no transition of its own (and goes the same way).
- The result badge springs in and draws its check or cross; a declined one shakes once, a successful receipt comes
  in line by line. The system's success / error haptic plays with the mark (Android 11+, iOS).
- The sheet rises and closes on the iOS sheet curve. With the system's reduced motion every change is a short fade.
- iOS: card fields draw a Compose copy of themselves while their step animates (UIKit fields ignore Compose's
  alpha and scale), and become the native field once it settles.
- Fields of the billing address and of a transfer's sender and recipient name themselves with a floating label: the
  name sits in the middle of an empty field and rises over the value, shrinking to 12sp, once the field has focus or a
  value (150 ms); it takes the outline's colour while focused and the error colour on an error. Before, the name was
  only the hint and went away as the user typed. A date's format is the hint under the risen label, and the country,
  account type and document type pickers keep the name over what was picked. Fields with a label of their own (email,
  phone) and the card fields keep it.
- Every input is 54dp high instead of 48dp: single fields, the card fields and the rows of field groups, so that a
  group with floating labels lines up with the fields around it.
- iOS: the sheet rises without stutter. The native card fields and the Apple Pay button were made while it rose, each
  costing a long frame: the fields turned native as their step settled, before the sheet stood, and the button was made
  as the checkout opened even when the first step does not show it. Both now wait for the sheet to stand; a step that
  shows the Apple Pay button while the sheet rises still has it made at once.
- iOS: a checkout with Apple Pay that opens on the card form (an AFT transfer) no longer shows a faint "Buy with Apple
  Pay" through the form. The button is made as the sheet opens, nearly transparent so that PassKit draws it in time,
  and waits for a step that shows it; it waited where the sheet's first, short frame had put it, mid-form once the
  sheet grew. It now waits under the main button, which hides it.
- No public API change.

### Languages

- The checkout is translated into Romanian, Greek, Bulgarian, German, French, Spanish and Arabic, besides English and
  Russian (the card scanner too). `SdkPaymentConfig.locale` — a tag such as `"de"` or `"ar"` — now sets the language
  of the screens (it was not applied before); null follows the device. `PaymentLocaleProvider` does the same for the
  standalone screens, `SupportedPaymentLanguages` lists the languages.
- Arabic is laid out right to left: back and row chevrons are mirrored; card number, expiry, CVV, email, phone and
  address fields keep their text left to right (aligned to the right), so a card or phone number is not reversed;
  amounts, card masks and receipt codes stay left-to-right runs inside Arabic text.
- The order amount follows the language: `€1,234.50` (English), `1.234,50 €` (German, Romanian, Greek),
  `1 234,50 €` (Russian, Bulgarian, French), `1234,50 €` (Spanish), `1,234.50 €` (Arabic). `MoneyFormatter.format`
  has an overload with the language; the old one keeps the English look. The receipt amount from the gateway is
  written the same way.
- Country names in the billing and phone pickers follow the checkout language instead of the device's.
- The email error no longer says what the email is for ("Enter your email").
- Compose resources 1.7 has no public way to pick the language of a composition; the SDK reaches its internal
  environment (`PaymentLocale.kt`), to be replaced when the library opens it.

### Tests

- iOS unit tests of `sdk-payment` compile again: test names with commas, which Kotlin/Native rejects, were
  renamed.

### Card preview

- The live card above the new-card form (`CardPreview`, the mrbin.io bank lookup and its SVG decoding,
  the contactless mark) left `sdk-payment`; the form shows the fields alone. The saved-card mini card stays.
  `androidsvg` is no longer a dependency.
- `CardNetworkIcon` with a `tint` no longer needs `PaymentThemeProvider` around it (it crashed outside one).

### Card networks

- `CardNetwork` gains `JCB`, `DISCOVER`, `DINERS` and `MAESTRO`, recognised by BIN and by the gateway's
  `paymentSystem` of saved cards (Maestro used to be reported as `MASTERCARD`).
- `CardNetworkIcon` draws brand-colour logos (light variants on dark sheets); passing `tint` keeps the
  monochrome look. The card number field shows the detected network's logo.
- Logos from [svg-credit-card-payment-icons](https://github.com/aaronfagan/svg-credit-card-payment-icons)
  (Apache License 2.0).

### Done (Plan 5b — iOS sample app)

- **New module** `samples/ios-app/` — native SwiftUI application consuming `sdk-payment`
  (and transitively `sdk-core`) via XCFramework produced by Gradle.
- **XCFramework task** added to `:sdk-payment` via
  `org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework`; Xcode Run Script phase
  invokes `./gradlew :sdk-payment:assembleSdk_paymentDebugXCFramework` automatically
  on build. `binaryOption("bundleId", ...)` overrides CFBundleIdentifier so the
  embedded framework passes Apple's codesign validation.
- **Swift-callable iOS surface** added to `:sdk-payment` iosMain:
  `iosMainScope()` exposes `kotlinx.coroutines.MainScope()`; `iosSdkCore()`,
  `iosSdkCoreConfig(...)`, `iosCardParams(...)`, `iosCardInstantParams(...)`,
  `iosBindingParams(...)` expose sdk-core factory surface that wouldn't otherwise
  emit into the generated Obj-C header.
- **Root screen** mirrors Android — two large cards for sdk-core and sdk-payment,
  with SF Symbol badges in gradient frames.
- **sdk-core demo** with 3-variant chips (SF Symbol + label), inline card form and
  mdOrder / PEM inputs. Generated tokens appear on a dedicated result screen
  (`SdkCoreResultView`) reached via a process-scoped `LastTokenResult` holder.
- **sdk-payment demo** with shared config + 5 category drill-downs: Basic, Saved
  cards, Wallets (Apple Pay inline config · Google Pay disabled), Theme & locale,
  Advanced (scanner / button placeholders + headless flow + SSL pinning intentional-
  fail).
- **Swift bridges** (`Bridge/SdkPaymentBridge.swift`, `Bridge/SdkPaymentDeps.swift`) —
  `AsyncStream<PaymentState>` adapter over Kotlin `Flow<PaymentState>` via a
  `Kotlinx_coroutines_coreFlowCollector` adapter; deps builder using the default
  iOS HTTP client factory.
- **Visual language**: SF Symbols for all iconography (no third-party SPM); solid
  `#0284C7` primary CTA; edge-to-edge safe-area layout.
- **Persistence** via `@AppStorage` mirrors Android's DataStore keys.
- **Apple Pay entitlements** with placeholder merchant ID
  `merchant.net.payrdr.samples.test`. Real merchant ID registration is an out-of-
  band manual step outside Phase 8 scope.
- **shared sdk-payment fix**: `CardFormScreen.onBack` adds a status-bar-safe close
  (X) button wired to `PaymentIntent.Cancel` via `PaymentCheckout`; `expiryMMYY` is
  now formatted with `CardFormatters.formatExpiry` before validation/submit. These
  ship via the XCFramework to iOS consumers automatically.

### Done (Plan 5a — Android sample app)

- **New module** `:samples:android-app` — native Jetpack Compose application consuming
  `:sdk-core` and `:sdk-payment` as project dependencies.
- **Root screen** with two large cards linking to sdk-core and sdk-payment demos.
- **sdk-core demo** with 3-variant chips (NewCard / Instant / Stored), live credit-card
  visual (BIN-driven gradient), collapsible Settings drawer, and inline success/error
  result hero.
- **sdk-payment demo** with shared config card + 5 category drilldowns: Basic, Saved
  cards (backend-driven), Wallets (Google Pay inline config, Apple Pay disabled), Theme
  & locale (4 preset tiles), and Advanced (placeholders + headless flow + SSL pinning
  intentional-fail demo).
- **Persistence** via DataStore Preferences — all editable fields (baseUrl, mdOrder, PAN,
  holder, GPay config) are persisted across launches; defaults match legacy test values.
- **Composition root** via `rememberSdkPayment` + `buildSdkPaymentDeps` helpers — one
  `SdkPayment` instance per distinct config.
- **Result propagation** through `LastPaymentResult` — captures outcome from launcher
  callback, surfaces in result screen with kv-table (Status / Payment ID / Method /
  Duration).

### Done (Plan 4)
- Google Pay (Android) — real integration:
  - Expanded `GooglePayConfig` with merchantId / merchantFullName / gateway / gatewayMerchantId / environment / allowedCardNetworks / allowedAuthMethods / countryCode / currencyCode / billingAddressRequired
  - Supporting enums: `GooglePayAuthMethod`, `GooglePayCardNetwork`, `GooglePayEnvironment`
  - `GooglePayTokenProvider` interface + `NoOpGooglePayTokenProvider` + Android `AndroidGooglePayTokenProvider` using `PaymentsClient` + `ActivityResultLauncher<IntentSenderRequest>` pattern
  - `GooglePayRequestBuilder` — pure commonMain `config → PaymentDataRequest JSON` (API-version-2 envelope)
  - `GooglePayButton.android.kt` — real `com.google.android.gms.wallet.button.PayButton` via `AndroidView`, reflective availability check, Material fallback if `play-services-wallet` missing at runtime
  - `play-services-wallet:19.4.0` declared `compileOnly` — merchants with GPay add `implementation(...)` themselves
- Apple Pay (iOS) — real integration:
  - Expanded `ApplePayConfig` with merchantId / supportedNetworks / merchantCapabilities / countryCode / currencyCode
  - Supporting enums: `ApplePayNetwork`, `ApplePayMerchantCapability`
  - `ApplePayTokenProvider` interface + `NoOpApplePayTokenProvider` + iOS `IosApplePayTokenProvider` using `PKPaymentAuthorizationController`
  - `ApplePayRequestBuilder` — `ApplePayConfig → PKPaymentRequest` mapping (iosMain), unit-tested via iosTest
  - `ApplePayNetworkMapping.ios.kt` — enum → `PKPaymentNetwork` string constants, `ApplePayMerchantCapability` set → bitmask
  - `ApplePayButton.ios.kt` — real `PKPaymentButton` (type `.buy`, style `.automatic`) via Compose `UIKitView`
- `PaymentIntent.TapGooglePay(config)` + `PaymentIntent.TapApplePay(config)` variants; `PaymentFlowController.handleTapGooglePay` / `handleTapApplePay` handlers call the token provider then POST to `api.googlePayProcessForm` / `api.applePayProcessForm` with 3DS pass-through
- `SdkGooglePayException`, `SdkApplePayException` added to the exception hierarchy
- SSL pinning (both platforms):
  - `SslPinningConfig` in commonMain — builder accepting pre-computed `"sha256/<base64>"` SPKI pins via `addPin(hostname, sha256Pin)`; host normalization (lowercased, trailing dot stripped); `build()` requires at least one entry
  - `expect fun sha256(data: ByteArray): ByteArray` + JVM/CoreCrypto actuals
  - `HttpClientFactory` interface + `DefaultHttpClientFactory` unpinned baseline (commonMain)
  - Android `OkHttpClientFactory` using Ktor OkHttp engine + `okhttp3.CertificatePinner`
  - iOS `DarwinHttpClientFactory` using Ktor Darwin engine + `NSURLSessionAuthenticationMethodServerTrust` challenge handler + SPKI-based public-key pin evaluation
  - iOS: ASN.1 SPKI header prepending (TrustKit approach) so `SecKeyCopyExternalRepresentation` raw-key bytes hash to the same value as OkHttp's `CertificatePinner` + `openssl x509 -pubkey -noout | openssl dgst -sha256` recipe. Key-shape detection covers RSA-2048/3072/4096 + EC P-256/P-384.
  - iOS: host-scope short-circuit — hosts without pins fall through to Apple's default trust evaluation, matching OkHttp semantics.
  - Wired into `SdkPaymentConfig.sslPinning: SslPinningConfig?`
- `SdkPayment.newDefaultFlow(...)` convenience overload — builds `PaymentApi`, `RemoteKeyProvider`, and the pinned `HttpClient` internally from `SdkPaymentConfig`
- Tests:
  - commonTest additions: `GooglePayRequestBuilderTest`, `SslPinningConfigTest`, `DefaultHttpClientFactoryTest`, new `PaymentFlowControllerTest` methods for `TapGooglePay` / `TapApplePay`
  - iosTest additions: `ApplePayRequestBuilderTest`
  - Full suite: 86 tests on Android (`testDebugUnitTest`) + 93 tests on iOS (`iosSimulatorArm64Test`), all passing
- Breaking changes (pre-release, no deprecation cycle — still 4.0.0-SNAPSHOT):
  - `GooglePayConfig(enabled)` → `GooglePayConfig(merchantId, merchantFullName, gateway, gatewayMerchantId, ...)`
  - `ApplePayConfig(enabled)` → `ApplePayConfig(merchantId, supportedNetworks, ...)`
  - `PaymentFlowController(...)` constructor gains `googlePayTokenProvider` and `applePayTokenProvider` parameters
  - `SdkPayment.newFlow(...)` signature gains two optional parameters (backward-compatible at call sites using named args)
- Sentry — explicitly dropped from Plan 4 scope; consumers can implement custom `LogTransport` if needed.

### Done (Plan 3)
- Compose Multiplatform UI layer in `sdk-payment/commonMain`:
  - `PaymentTheme` sealed type (System/Light/Dark/Custom) + `PaymentColors` / `PaymentTypography` / `PaymentShapes` data classes with Material 3 integration
  - `LocalPaymentColors` / `LocalPaymentTypography` / `LocalPaymentShapes` CompositionLocals + `PaymentThemeProvider` composable
  - Screens: `CardFormScreen`, `CardListScreen`, `ThreeDsChallengeScreen`, `ProcessingScreen`, `ResultScreen`, root `PaymentCheckout`
  - Components: `CardNumberField`, `ExpiryField`, `CvvField`, `CardHolderField`, `CardNetworkIcon`, `PayButton`, `BottomSheetContainer`, `ApplePayButton` (expect), `GooglePayButton` (expect)
  - `NativeTextField` expect/actual: Android `BasicTextField` + `Modifier.autofill`; iOS `UIKitView(UITextField)` + `textContentType`
  - `CardNetworkResolver` (BIN-prefix → brand) + `CardFormatters` (pan/expiry/mask)
  - State bridge: `CheckoutUiAction` + pure `reduceUiAction` + `rememberPaymentCheckoutState` composable
- Localization: en + ru through Compose Multiplatform Resources (`composeResources/values/*/strings.xml`)
- Real `WebChallengeHost` platform implementations:
  - `AndroidWebChallengeHost` + internal `ThreeDsWebActivity` (WebView + redirect intercept + 5-min timeout)
  - `IosWebChallengeHost` + `WKNavigationDelegate` + `UINavigationController` modal
- Android turnkey entry points: `SdkPaymentActivity`, `SdkPaymentContract`, `SdkPaymentLauncher`, `rememberSdkPaymentLauncher`, manifest-registered Activities
- iOS turnkey entry point: `SdkPaymentViewControllerFactory` returning `ComposeUIViewController`
- Headless helpers on `SdkPayment`: `paymentFlow()` + `completePayment()`
- Config: `GooglePayConfig` + `ApplePayConfig` opt-in flags (integration stubs — real wiring in Plan 4), `SdkPaymentConfig.theme`
- Build-logic: `payrdr.kmp.compose.library` convention plugin for Compose MP opt-in on KMP libraries
- Apple Pay / Google Pay buttons: stub actuals in Plan 3 (hidden on the wrong platform; plain Material button on the right platform). Real PKPaymentButton / PayButton wiring in Plan 4.
- Tests:
  - commonTest: `CardNetworkResolverTest` (8), `CardFormattersTest` (11), `PaymentCheckoutStateReducerTest` (7), `LocalizationSmokeTest` (1)
  - androidInstrumentedTest: `CardFormScreenTest` (2), `ProcessingScreenTest` (2), `ResultScreenTest` (2)
  - iOS UI tests deferred to Plan 7 (Maestro / XCUITest)

### Done (Plan 2)
- sdk-payment headless business logic (commonMain only):
  - `SdkException` hierarchy (13 types: base + network/API, transactional, session/order, crypto, config/state, 3DS)
  - `Logger` facade + `LogTransport` pluggable sinks (`ConsoleLogTransport`, `NoopLogTransport`)
  - Public API models: `SdkPaymentConfig`, `CheckoutConfig` (MdOrder/SessionId), `PaymentResult`, `PaymentState`, `PaymentIntent`, `PaymentEffect`
  - `OrderStatus` enum + case-insensitive status matcher
  - `SessionIdConverter` ported to commonMain with KMP-safe exceptions
  - `KeyProvider` + `RemoteKeyProvider` with Ktor-backed HTTP and MockEngine tests
  - 18 `@Serializable` DTOs covering 9 PaymentApi endpoints
  - `PaymentApi` interface with all 9 methods: getSessionStatus, processForm, processBindingForm, googlePayProcessForm, applePayProcessForm, finish3dsVer2PaymentAnonymous, getFinishedPaymentInfo, getPaymentSettings, unbindCardAnonymous
  - `PaymentApiImpl` using Ktor Client 3.x with form-urlencoded + JSON payloads
  - `ThreeDsChallenge` + `ThreeDsResult` + `WebChallengeHost` (interface, not expect class)
  - `PaymentFlowController` MVI state machine: StateFlow<PaymentState> + dispatch(PaymentIntent) + Flow<PaymentEffect>
  - `SdkPayment` public entry point with headless `newFlow(...)` factory
- 34 tests passing on both JVM and iosSimulatorArm64 targets

### Done (Plan 1)
- Foundation: repo layout, Gradle 8.10 wrapper, version catalog, build-logic convention plugins, CI workflow, ktlint + detekt + binary-compatibility-validator setup
- Empty module scaffolds for `sdk-payment` and `sdk-payment-scanner`
- Full `sdk-core` implementation:
  - All 13 domain models ported to `commonMain` (no `android.os.Parcelable`)
  - All 6 validation rules + 7 field validators (Context-free: error localization via stable `errorCode`)
  - `PemKeyParser`, `Base58Coder` (BigInteger-free), `BindingUtils`
  - `DefaultPaymentStringProcessor` using `kotlinx-datetime`
  - `Rsa` expect/actual: JVM via `javax.crypto.Cipher`, iOS via `SecKeyCreateEncryptedData`
  - `RsaCryptogramCipher` + `SdkCore` public entry point
  - Tests ported from android-sdk 3.x, running on both JVM and iosSimulatorArm64 targets

## 4.0.0 (planned)

Full redesign on Kotlin Multiplatform. Breaking changes — see `docs/migration-guide-from-3.x.md`.
