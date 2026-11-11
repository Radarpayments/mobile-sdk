# sdk-payment — agent notes

Read the root `AGENTS.md` first. This is the largest module: the checkout an integrator opens with an mdOrder (API V1)
or a session id (API V2), the gateway client, the payment state machine, 3-D Secure, Google Pay / Apple Pay, and the
payment sheet: Compose on Android, SwiftUI on iOS. No Compose reaches the iOS frameworks (see "UI").

Paths below are relative to `src/<sourceSet>/kotlin/net/payrdr/mobile/payment/sdk/payment/`.

## Public API

Keep it to what an app needs to take a payment (the `MSDK-110 | Public API` commit explains the cut):

- `PayrdrCheckout`
  - Android (`android/PayrdrCheckout.kt`): `PayrdrCheckout.Builder(config, onResult).build(host)` before the host reaches
    STARTED (it registers an activity result; a `check` enforces it), or `rememberPayrdrCheckout` in Compose;
    `present(mdOrder)` / `presentSession(sessionId)`. `PayrdrCheckoutIntent` is the `startActivityForResult` path for
    plain Activities and the plugins.
  - iOS (`ios/PayrdrCheckout.kt`): `PayrdrCheckout(config:)`, `present(mdOrder:from:onResult:)` /
    `presentSession(...)` → `Bool` (false: nothing opened), `dismiss()`, `isPresenting`. Main thread only.
- Configs: `SdkPaymentConfig`, `CheckoutConfig` (`MdOrder` | `SessionId`), `GooglePayConfig` (all optional, the rest
  comes from the gateway), `ApplePayConfig` (`merchantId`), `SslPinningConfig`, `PaymentTheme`.
- `CheckoutResult` (`Completed` / `Failed(error)` / `Canceled` / `Unknown`, each with `orderId`) and the `Sdk*Exception`s
  in `exception/SdkException.kt`. The merchant's server is the source of truth; `Unknown` means "ask it".
- `LogTransport` (+ `ConsoleLogTransport`, `NoopLogTransport`, `LogEvent`, `LogLevel`); must be thread-safe.
- Headless: `SdkPayment.create(config)` → `newFlow(webChallengeHost, scope)`, `paymentFlow(...)`, `completePayment(...)`;
  `PaymentState`, `PaymentIntent`, `PaymentEffect`, `WebChallengeHost`, `AndroidWebChallengeHost`,
  `IosWebChallengeHost`, `iosMainScope()`.
- iOS factories for sdk-core params: `ios/IosSdkCoreSurface.kt` (Kotlin/Native exports only referenced types).

Swift-shaped details: `@ObjCName(swiftName = "__baseUrl" / "__merchantId")` on config constructors leaves room for the
Swift convenience inits in `src/iosMain/swift/`; SKIE turns suspend interface methods into
`__`-prefixed ones with a completion handler. That Swift code (the inits, async `PayrdrCheckout.present`, the SwiftUI
`.payrdrCheckout` modifier) goes into this module's klib, and SKIE compiles it into every framework that holds
`sdk-payment`, `RadarpaymentsSDKPayment` included. Change a public signature → check the Swift side and the sample apps.

## Map (commonMain)

| Package | What it does |
|---|---|
| `SdkPayment.kt` | entry point; builds the API client and key provider lazily on a shared HTTP client. `SdkPaymentNetwork` (internal) lets tests inject them |
| `api/` | `PaymentApi` (internal) / `PaymentApiImpl` (Ktor), `Endpoints.kt` (every gateway path), `PayerDataFormParams`; `dto/request`, `dto/response` wire models |
| `flow/` | `PaymentFlowController` (the state machine), `OrderOutcome` (what an order status means), `CardForm.kt` (form + order summary from a session), `WalletPayments` / `WalletResolver`, `Transfer.kt` (AFT), `PayerDataMapper`, `Countries.kt` |
| `config/` | the configs; `RestorableCheckout` (config + checkout as a string for Android process-death restore; theme typography and corners, log transport and scanner are not restored) |
| `result/` | `CheckoutResult`, `CheckoutResultCodec` (result ↔ Intent extras, `cause` dropped), `PaymentState`, `PaymentIntent`, `PaymentEffect`, `PayerData` |
| `threeds/` | `ThreeDsChallenge`, `ThreeDsResult`, `WebChallengeHost`, `ThreeDsRedirect` (return URL), `AcsForm` (POST body encoding) |
| `gpay/`, `applepay/` | token provider interfaces + no-ops, `GooglePayRequestBuilder`, enums |
| `http/` | `HttpClientFactory`, `SharedHttpClients` (one client per factory + pin set per process: a client per checkout leaked a `URLSession` per payment on iOS), `PlatformLock` |
| `keyprovider/` | `RemoteKeyProvider`: GET `se/keys.do`, first key |
| `logger/` | `LogTransport` (public), `Logger` (`@InternalPayrdrApi`) |
| `scanner/` | the scanner contract; the implementation is `sdk-payment-scanner` |
| `ssl/` | `SslPinningConfig`, `expect sha256` |
| `utils/` | `SessionIdConverter` (`ps_` + Base58(`ps:{mdOrder}`)), `OrderStatus`, `MoneyFormatter` (amounts per language: `€1,234.50`, `1.234,50 €`) |
| `ui/` | the sheet — see "UI" below |

Platform code:
- androidMain `android/` (`PayrdrCheckout`, `PayrdrCheckoutIntent`, `SdkPaymentContract`, internal `SdkPaymentActivity`
  — the run spec travels through the in-memory `SdkPaymentRegistry`, restored from `RestorableCheckout` after process
  death; `AndroidWebChallengeHost` + `ThreeDsWebActivity`), `gpay/` (`AndroidGooglePayTokenProvider`,
  `GooglePayAvailability` — reflection, because `play-services-wallet` is `compileOnly`; `consumer-rules.pro` lets R8
  in an app without Google Pay leave those references unresolved), `http/OkHttpClientFactory`
  (CertificatePinner; connect 15 s, request/socket 60 s).
- iosMain `ios/` (`PayrdrCheckout`, `SdkPaymentViewControllerFactory` — runs the flow and asks the Swift
  `CheckoutRenderer` for the view controller, `IosWebChallengeHost` — WKWebView),
  `applepay/IosApplePayTokenProvider` (keeps a strong reference to its delegate: PassKit holds it weakly and the sheet
  hangs without it), `http/DarwinPinningDelegate` (system chain validation first, then any certificate in the chain may
  match, as OkHttp does).

## The payment flow

`PaymentFlowController`: `state: StateFlow<PaymentState>` (`Initializing → Processing → CardFormShown → Processing /
ThreeDsChallenge → Completed`), `effects` (`ShowThreeDs`, `Dismiss`; not replayed), `order: OrderSummary`. A second pay
intent while one is in flight is ignored.

1. **Start.** A session id is decoded to its mdOrder; both API generations then use the same mdOrder endpoints.
   `getSessionStatus.do`; a closed session (`remainingSecs == null`) is settled from `getFinishedPaymentInfo.do`
   (`OrderOutcome.ofClosedSession`). Wallets come from `getPaymentSettings.do` with a 3 s timeout; if it fails, cards
   still work.
2. **Pay.**
   - New card: key from `KeyProvider` → `SdkCore.generate` (`MSDK_PAYMENT`) → `processform.do` with `seToken`,
     `bindingNotNeeded = !saveCard`, `threeDSSDK=false`, payer params.
   - Saved card: `BindingParams(bindingId, cvc)` → `processBindingForm.do`; removal `unbindcardanon.do`.
   - Google Pay `google/paymentOrder.do`, Apple Pay `applepay/paymentOrder.do`; the answer is mapped to the
     `ProcessFormResponse` shape. The Google Pay token is sent **base64-encoded**, as the gateway's own payment page does.
   - AFT (P2P order with `WITHOUT_TO_CARD`, `flow/Transfer.kt`): `p2p/perform.do` (JSON, `fromCard` = seToken |
     bindingId | wallet token), saved card via `p2p/performByBinding.do`; sender `payer*` / recipient `recipient*` fields;
     only DEBIT bindings (`bindingCategory == "D"`). Save-card mode HIDDEN / OFFERED / LOCKED / IMPLIED from
     `FORCE_CREATE_BINDING`, `VERIFY`, `BINDING_NOT_NEEDED`, `DEBIT_BINDING_ALLOWED`. Never send
     `bindingNotNeeded=true` for forced or VERIFY transfers, and never send the amount to p2p. The cardholder sealed into
     the token must not be empty (typed name → sender's name in Latin, ≤30 → `CARDHOLDER`).
3. **Answer.** `threeDSMethodURL` → `SdkNotConfiguredException` (native 3DS2 SDK is not supported; `threeDSSDK=false` is
   always sent). `acsUrl` + `paReq` + `termUrl` → web challenge (`Succeeded` → finalize, `Failed` → failed, `TimedOut` /
   `Interrupted` → recheck the order, `Cancelled` → canceled). A refusal without ACS → `SdkDeclinedException`.
   Otherwise finalize with `getFinishedPaymentInfo.do`.
4. **Outcome** (`OrderOutcome.settled`, in this order): session expired → failed; APPROVED/DEPOSITED or VERIFY + REVERSED +
   actionCode 0 → completed; actionCode -100 / -30001 / 90 / 998 / 151019 → not settled yet; a decline reason →
   `SdkDeclinedException`; DECLINED → failed; REVERSED/REFUNDED → `SdkTransactionException`. Not settled → recheck after
   1, 2, 4 s → `Unknown`.

Invariants:
- **A lost answer is not a failure.** After the payment was sent, `SdkNetworkException` / `SdkPaymentApiException` mean
  "ask the order" (`completeAfterLostAnswer`), never "declined".
- The first outcome wins (`complete()`); closing the sheet mid-payment or during 3DS gives `Unknown`, otherwise
  `Canceled`.
- The bank page closes when the checkout moves on and never opens over a finished checkout. On Android, a recreated
  3DS activity closes as `Interrupted` rather than re-posting the one-time request.
- 3DS return: a URL starting with `sdk://done`, or with `sessionId=<ps_…>` in the query (API V2 success/failure URLs);
  `error` in the query means failed. `AcsForm` escapes everything but RFC 3986 unreserved characters — a `+` in the
  base64 PaReq must not become a space.
- Buyer-facing failure text is the gateway's `displayErrorMessage`, else `error` / `actionCodeDescription`. The gateway
  writes missing values as blank or the string `"null"` (`orNullIfBlank`).
- Wallets show only when the gateway enables them (`merchantOptions`, not hidden by `ecom2.hidden.payments`) and country,
  currency and amount are known. No guessing.
- Logging: intents log their type only; `PaymentIntent`, `P2PPerformRequest` and token classes override `toString` to mask
  card data. Keep that for every new class that holds card data, payer data or tokens.
- SSL pinning is opt-in; pins are raw 44-char base64 SHA-256 of the SPKI (no `sha256/` prefix); unpinned hosts use the
  system trust store.

## UI

Two UIs over one model. commonMain `presentation/` (`CheckoutModel` — sheet, steps, result hand-over; `StringCatalog`)
and the pure parts of `ui/` (`CheckoutStep`, `PayerFieldsState`, formatters, validation, `LoadingPhases`) are shared.
The Compose UI is androidMain `ui/`; the SwiftUI UI is `src/iosMain/swift/UI` (SKIE compiles it into the frameworks),
reading iosMain `presentation/` (`@PayrdrUiApi`: `CheckoutSession`, `PaymentStrings`, `ThemeSpec`, `CardEntrySpec`,
`NativeCardScanner`, …; kept out of the API dumps). iosMain code must not use Compose: the build applies the Compose
compiler to Android only, so it would not compile. Platform hooks a shared file needs are `expect`s in commonMain with
an Android and an iOS actual (`deviceLanguage`, `observableValueOf`, `hasPlatformWallet`, …).

### Android (Compose, androidMain `ui/`)

Entry: internal `ui/PaymentCheckout.kt` `PaymentCheckout(config, controller, onResult, presentation)`, hosted by
`SdkPaymentActivity`. It wraps `PaymentLocaleProvider` →
`PaymentThemeProvider` → system bars, collects `controller.state` / `order`, and delivers the result on
`PaymentEffect.Dismiss`. Card-step navigation is local to the UI; the state machine only sees submitted intents.

| Path | What it does |
|---|---|
| `CheckoutSheet.kt` | scrim, sheet host, slide-in, `smoothHeight`, `LocalSheetEntered` |
| `SheetSteps.kt`, `CheckoutStep.kt`, `CheckoutStepContent.kt` | step transitions; the step stack (pure, tested); step bodies and the first step's skeleton |
| `ResultDelivery.kt` | one result per run; in sheet mode the sheet slides away first |
| `PaymentLocale.kt` | `PaymentLocaleProvider`, `SupportedPaymentLanguages`, RTL for ar/fa/he/ur |
| `components/` | buttons (`PrimaryButtonLayer`, `CascadeLabel`, the Google Pay button; `ApplePayButton` is an empty stub), fields (`NativeTextField`, `ComposeTextField`, `FieldChrome`, `FieldGroup`, `CardDetailsGroup`), sheet chrome, logos, platform hooks (haptics, reduce motion, screen-change announcements) |
| `screens/` | `PaymentMethodsSheet`, `CardFormScreen`, `SavedCardScreen`, `CardListScreen`, `ProcessingScreen`, `ThreeDsChallengeScreen` (waiting only — the WebView is outside Compose), `ResultScreen`, `CheckoutSkeleton`, `PageScaffold` |
| `payer/`, `payer/phone/` | payer and AFT transfer fields, pickers, phone parsing/formatting |
| `loading/` | loading phases; `SavedCardCountCache` so the skeleton matches the last session |
| `motion/SheetMotion.kt` | all motion specs, `stepAlpha` (the SwiftUI copy is `swift/UI/Foundation/SheetMotion.swift`) |
| `theme/` | `PaymentTheme` (System / Light / Dark / Custom), `PaymentColors` of `PaymentColor`, `PaymentTypography` of `PaymentTextStyle`, `PaymentShapes` of `PaymentCorners` — all Compose-free, one API for both platforms; androidMain `ComposeTheme.kt` resolves them to Compose types (`ThemeColors` + derived tints) for `PaymentThemeProvider`, `ComposeThemeInterop.kt` takes Compose colours and text styles; iOS reads them through `ThemeSpec`; `PaymentFontFamily` is expect/actual (Android `FontFamily`, iOS family name) |
| `util/` | `ltr()` (Bidi), card formatting / validation / network resolver, payer field validation, gateway dates |

### UI rules

- **Motion is simple.** A step change zooms and fades the whole sheet content (forward in, back out); no per-element
  morphs or shared-element transitions. The exception is the main button: it stays put (`PrimaryButtonLayer`); its label
  flips as a whole on a screen change and cascades only the changed words on the same screen (`CascadeLabel`), never per
  character. All screens share one side padding (`SIDE_PADDING`, 20dp) so the button keeps its width. Skeletons copy
  the real screen's layout and pulse (no shimmer).
- Both UIs follow the same rules and the same numbers; a change of look or motion goes into both (the iOS screenshots
  in `tools/visual` compare against `golden/compose`).
- The main button, drawn outside the steps by `PrimaryButtonLayer`, takes its alpha from `stepAlpha()`
  (`LocalStepAlpha`). **If you change the step fade specs, keep `stepAlpha` in sync.** Autofocus waits for
  `LocalStepSettled`.
- **Autofill**: every field sets a content type. A masked field without one reads as a password, and Google Password
  Manager offered to save the CVC as one.
- Accessibility: `tapsOnly` instead of `clickable` for scrim/handle/sheet; `OverlayCover` hides content under pickers;
  errors are `liveRegion` (polite), the result assertive; pass `label`, not only a placeholder; validate when the buyer
  leaves a field, not while typing; every error shown in words.
- The footer with Pay hides while the keyboard is open; Return/Done leaves the field.
- Dragging the sheet down on a pushed step goes back (typed card data survive) instead of closing.
- No ripple anywhere (`LocalIndication` is off in the theme).
- **Card masks, amounts and codes go through `ltr()`** (LRI/PDI isolation) so they read correctly in Arabic. Amounts
  are formatted in `flow/` with `MoneyFormatter` and arrive in the UI as strings.
- The SDK's own exception text never reaches the buyer: no failure chip for SDK-internal errors (`FailureChipTest`).

### Strings

`src/commonMain/strings/values{,-ar,-bg,-de,-el,-es,-fr,-ro,-ru}/strings.xml`. `generatePaymentStrings`
(`GeneratePaymentStringsTask` in build-logic) writes them as Kotlin tables (`presentation/PaymentStringTables`, common)
and, for Android, `Res.string.<name>` keys in `net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources` (the
doubled `sdk` is real) read by that package's `stringResource` in the language of `LocalSdkLanguage`
(`PaymentLocaleProvider`). iOS reads them through `PaymentStrings`. **A new string goes into all nine files** in the same
section; nothing checks this automatically, so compare the key counts. Placeholders are `%1$s`; a missing translation
falls back to English.

## Tests

```bash
./gradlew :sdk-payment:testDebugUnitTest            # commonTest + androidUnitTest
./gradlew :sdk-payment:iosSimulatorArm64Test        # commonTest + iosTest
./gradlew :sdk-payment:connectedDebugAndroidTest    # UI tests on a device/emulator
./gradlew :sdk-payment:apiCheck                     # apiDump after an intended change
```

- commonTest: api, config, flow (`PaymentFlowControllerTest`, `AftFlowTest`, `OrderOutcomeTest`, …), threeds, ssl,
  result, utils, and pure UI logic (steps, skeleton, payer fields, formatters). Helpers in `support/`:
  `FakePaymentApi` (programmable `next…` answers and errors, gates, recorded calls), `FakeKeyProvider`,
  `FakeWebChallengeHost`, fake wallet token providers, `Fixtures` (test RSA key, mdOrder, PAN), `mockJsonClient` for
  Ktor `MockEngine`. Add to these rather than writing new fakes.
- androidUnitTest: `PayrdrCheckoutTest`, `PayrdrCheckoutIntentTest` (no Robolectric); the Compose-typed UI helpers
  (`LocalizationSmokeTest`, `ResultTitleTest`, `FailureChipTest`, `PaymentThemeTypesTest`, `PaymentNetworkLogosTest`,
  `SheetMotionTest`, `ComposeThemeInteropTest`). `CertificatePinningTest` checks the OkHttp pinner
  against `ssl/TestCertificates` (each key type, a CA-issued chain, pins as `openssl` computes them). `live/` talks to a real gateway and
  is skipped unless environment variables are set (`PAYRDR_API2_KEY`; `PAYRDR_AFT_BASE_URL`, `PAYRDR_AFT_LOGIN`,
  `PAYRDR_AFT_PASSWORD`, …). Credentials only ever come from the environment.
- iosTest: `ThemeSpecTest`, `ApplePayRequestBuilderTest`, `ApplePayNetworkMappingTest`, `DarwinPinningDelegateTest`.
  The SwiftUI screens are checked by the sample app's XCUITests (`CheckoutVisualTests`, `CheckoutAccessibilityTests`
  through `tools/visual`); the result hand-over is `CheckoutModelTest` (commonTest). The simulator test
  process cannot reach the trust daemon (`SecTrustEvaluateWithError` fails with errSecInternal, -26276), so the pinning
  test checks the SPKI pins and the chain matching, not the system chain check. snake_case test names.
- androidInstrumentedTest: `CardFormScreenTest`, `CheckoutSkeletonTest`, `ProcessingScreenTest`, `ResultScreenTest`;
  assume an English device. Match amounts and masks with `ltr(…)`; the expiry hint has a word joiner (`MM/⁠YY`); call
  `Espresso.closeSoftKeyboard()` before tapping Pay; `SuccessIcon`/`FailureIcon` need `useUnmergedTree = true`; screens
  carry test tags named after them (`CardFormScreen`, `ResultScreen`, …), payer fields `payer_<apiName>`.
