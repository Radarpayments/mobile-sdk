# PAYRDR QA apps

Two native apps, Android (`qa/android-app`, Compose) and iOS (`qa/ios-app`, SwiftUI), that open the SDK's checkout with
any configuration a QA specialist chooses: order parameters, SDK parameters, theme. The application id and the bundle
id are `net.payrdr.mobile.payment.sdk.qa`, the display name is `PAYRDR QA`; the apps install next to a sample.

They are a test tool, **not a sample for integrators**. Where an integrator should see one clean integration, these apps
expose every knob, accept parameters from outside and keep test-stand credentials on the device. For the integration
reference use `samples/`.

The platforms share no code. They share this contract: one JSON format, one list of element identifiers, one set of
fixtures (`qa/fixtures/`) that both apps run in their unit tests. A change to the contract (a field, a rule, an
identifier) lands in this file, in `qa/fixtures/` and in both apps in the same change.

## Build

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"   # Gradle needs JDK 17-21
./gradlew :qa:android-app:assembleDebug     # an APK signed with the debug key
```

```bash
# iOS: a simulator build, zipped for a tester's simulator (the first build links the SDK through Gradle: minutes)
xcodebuild -project qa/ios-app/QaApp.xcodeproj -scheme QaApp -sdk iphonesimulator -configuration Debug \
  -derivedDataPath build/qa-ios build
ditto -c -k --keepParent build/qa-ios/Build/Products/Debug-iphonesimulator/QaApp.app build/QaApp-simulator.zip

# iOS unit tests
xcodebuild -project qa/ios-app/QaApp.xcodeproj -scheme QaApp \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro,OS=17.4' test -only-testing:QaAppTests
```

The Xcode build phase sets its own `JAVA_HOME` for Gradle. The two Xcode projects (this and the iOS sample) write to
the same `sdk-payment-scanner/build/XCFrameworks/debug`, and Gradle builds of the SDK write there too: never run two of
these builds at once.

## Passing input from outside

```bash
# Android: intent extras (raw JSON), or a link
adb shell am start -n net.payrdr.mobile.payment.sdk.qa/.MainActivity --es qaConfig '<json>' --ez qaLaunch true
adb shell am start -a android.intent.action.VIEW -d 'payrdrqa://config?data=…'

# iOS simulator: launch arguments (raw JSON), or a link
xcrun simctl launch booted net.payrdr.mobile.payment.sdk.qa -qaConfig '<json>' -qaLaunch 1
xcrun simctl openurl booted 'payrdrqa://config?data=…'
```

The exact rules are under "External input". On Android the extras arrive on the exported launcher activity, so any app
on the device can pass them. This is accepted: the app is a test tool that holds test-stand credentials on test devices.

## The contract

### Documents

Two JSON documents. `StandProfile` holds the stand and its credentials, the only place with secrets. `QaConfig` holds
everything else and no secrets, so it can be pasted into a bug report. The app keeps the config in its preferences
(DataStore / `UserDefaults`) and the list of profiles encrypted (Keystore / Keychain). Uninstalling the Android app
removes both; an iOS Keychain item survives deleting the app, so after a reinstall on iOS the profiles and their
credentials are back while the config starts from its defaults. Both platforms model **both** documents in full,
the wallet fields of the other platform included: they are carried through import and export and have no form element
on the other platform, so one file round-trips on both.

#### `StandProfile`

| Field | Type, default | Rule |
|---|---|---|
| `version` | int, 1 | must be 1 |
| `name` | string, required | `[a-z0-9_-]{1,32}` |
| `gatewayUrl` | string, required | absolute `http` / `https` URL |
| `api` | `v1` \| `v2`, `v1` | `v1` uses `login` and `password`, `v2` the `apiKey` |
| `login`, `password`, `apiKey` | string, `""` | |

#### `QaConfig`

| Path | Type, default | Rule |
|---|---|---|
| `version` | int, 1 | must be 1 |
| `profile` | string?, null | the profile name pattern |
| `order.source` | `register` \| `mdOrder` \| `sessionId`, `register` | `register`: the app registers the order; the others open an existing one |
| `order.type` | `payment` \| `aft`, `payment` | |
| `order.amount` | integer, 10000 | integral number (see Decoding), 0…999999999999, in minor units (0 is a VERIFY order) |
| `order.currency` | string, `EUR` | a key of "Currencies" |
| `order.clientId`, `order.mdOrder`, `order.sessionId` | string?, null | blank is read as null |
| `order.features` | string[], [] | each non-blank, no comma, no whitespace |
| `order.extra` | object, {} | no key equal, ignoring case, to `username`, `password`, `token` |
| `sdk.locale` | string?, null | `en ru ro el bg de fr es ar`; null is the system language |
| `sdk.theme` | string, `system` | `system light dark contrast rounded square largeText debug` |
| `sdk.showResultScreen`, `sdk.scanner` | bool, false | |
| `sdk.keyProviderUrl` | string?, null | absolute `http` / `https` URL (see Decoding) |
| `sdk.sslPinning.enabled` | bool, false | |
| `sdk.sslPinning.pins` | `{host,pin}[]`, [] | `host` is a plain hostname, matching `^(\*\*?\.)?[A-Za-z0-9-]{1,63}(\.[A-Za-z0-9-]{1,63})*$`: labels of ASCII letters, digits and hyphens, 1–63 characters, separated by single dots, at most 253 characters in all (the prefix included); no scheme, port, path, trailing dot or empty label. A leading `*.` covers one label below the domain, `**.` the domain and any depth below (as OkHttp's `CertificatePinner`; the SDK matches the same way on iOS); a `*` anywhere else is rejected. `pin` matches `^[A-Za-z0-9+/]{43}=$`, the padded standard base64 of 32 bytes (the SHA-256 of the certificate's SPKI). Error field: `sdk.sslPinning.pins`, also for an element that is not an object |
| `sdk.googlePay.enabled`, `.billingAddressRequired` | bool, false | |
| `sdk.googlePay.merchantId`, `.merchantFullName`, `.gateway`, `.gatewayMerchantId`, `.countryCode` | string?, null | |
| `sdk.googlePay.environment` | `test` \| `production` \| null, null | |
| `sdk.googlePay.allowedCardNetworks` | string[], `["MASTERCARD","VISA"]` | subset of `AMEX DISCOVER INTERAC JCB MASTERCARD VISA` |
| `sdk.googlePay.allowedAuthMethods` | string[], `["PAN_ONLY","CRYPTOGRAM_3DS"]` | subset of `PAN_ONLY CRYPTOGRAM_3DS`, in this order (`PAN_ONLY` first) |
| `sdk.applePay.enabled` | bool, false | |
| `sdk.applePay.merchantId` | string, `merchant.net.payrdr.samples.test` | non-blank |
| `sdk.applePay.supportedNetworks` | string[], `["MASTERCARD","VISA"]` | subset of `AMEX DISCOVER JCB MASTERCARD VISA` |
| `sdk.applePay.merchantCapabilities` | string[], `["CREDIT","DEBIT","THREE_D_SECURE"]` | subset of `CREDIT DEBIT EMV THREE_D_SECURE` |
| `sdk.applePay.countryCode` | string?, null | |

`sdk.applePay.merchantId` defaults to the placeholder the iOS sample commits, which is also the one in the committed
entitlements; a real merchant id needs a local change of the entitlements and is not committed.

`order.features` is a list of gateway feature names, sent as set. The app forces nothing beyond `WITHOUT_TO_CARD` for
AFT, so QA can also try combinations the product does not support; `VERIFY` and `FORCE_CREATE_BINDING` are typed into
`order.features` by QA. `order.extra` is the escape hatch for gateway parameters the form does not have; its keys
`username`, `password` and `token` are rejected so that `QaConfig` stays free of secrets.

**Decoding.** The root must be a JSON object, else the error field is `$`. A known field of the wrong JSON type fails
with its path; so does a known object field (`order`, `sdk`, `sdk.googlePay`, `sdk.applePay`, `sdk.sslPinning`) that is
not an object, and an element of `pins` that is not an object fails with the path of `pins`. Unknown fields are
ignored, missing fields take their defaults, so a link with two fields is a valid configuration. The first failing
field in the table's order is the one reported. A malformed document or an invalid value is never applied in part: the
app names the field and keeps the previous state. Further rules, so that two decoders cannot read a document
differently:

- A document is JSON as RFC 8259 spells it, and the whole text is checked before any field is read; text that is not
  JSON fails on `$` (the form's `order.extra` text on `order.extra`), however lenient a platform's parser would be.
  Only space, tab, line feed and carriage return may stand around and between the tokens, and nothing may follow the
  value. A number is `-?(0|[1-9][0-9]*)(\.[0-9]+)?([eE][+-]?[0-9]+)?` in ASCII digits, so `01`, `+1`, `1.`, `.5` and
  `NaN` are not numbers, and `true`, `false` and `null` are the only bare words. A string holds no raw character below
  U+0020 and no escape but `\"`, `\\`, `\/`, `\b`, `\f`, `\n`, `\r`, `\t` and `\uXXXX`; a surrogate, raw or escaped, is
  only valid as the first or second half of a pair (RFC 8259 leaves a lone one to the parser; here it is not JSON).
  A repeated key is JSON: the member keeps the place of its first occurrence and the value of its last.
- An explicit JSON `null` is the same as the field being absent: the default applies.
- An integral number (`version`, `order.amount`) is a JSON number whose value has no fractional part, whatever its
  spelling: `100`, `100.0` and `1e2` are all 100. A JSON boolean or string is a type error on that field, and so is a
  number with a fractional part.
- A string is blank when it is empty or whitespace only. Every nullable string field (`profile`, `order.clientId`,
  `order.mdOrder`, `order.sessionId`, `sdk.locale`, `sdk.keyProviderUrl`, the string fields of `sdk.googlePay` and
  `sdk.applePay.countryCode` among them) reads a blank string as null. A non-blank string is stored as given, never trimmed.
  Whitespace here, in `order.features` and in the form's trimming is Kotlin's `Char.isWhitespace`: Unicode's White_Space
  without U+0085, plus U+001C–U+001F. The URL rule below has its own set.
- Strings are compared character for character, without Unicode normalisation: `KZT` (KELVIN SIGN) is not `KZT`,
  and `extra` keys `K` and `K` are two keys. "Ignoring case" in the `order.extra` rule is Java's `equalsIgnoreCase`,
  one character at a time, so `toKen` and `paſsword` are forbidden too.
- An absolute `http` / `https` URL is a string that matches `^https?://[^\s/?#]+\S*$`, the scheme compared ignoring
  ASCII case only. `\s` is Unicode's White_Space: U+0009–U+000D, U+0020, U+0085, U+00A0, U+1680, U+2000–U+200A,
  U+2028, U+2029, U+202F, U+205F and U+3000 (U+200B and U+FEFF are not in it).
- A document is at most 65536 bytes of UTF-8 and nests objects and arrays at most 32 levels deep, the root object
  being level 1. Both are checked on the text before it is parsed, brackets inside strings not counting, and either
  excess fails on `$` (the parsers recurse, and a deeply nested document would overflow the stack). The form's
  `order.extra` text is checked the same way with 30 levels, its place in the document, and fails on `order.extra`.

**Encoding.** Every field is written, nulls included, `version` as 1, in the order of the tables. A string that is read
as null (a blank nullable string, see Decoding) is written as null. A set (the Google Pay and Apple Pay lists) is
written without duplicates in the order of its allowed values in the table, whatever order it was read in; the order
of the "subset of" lists is the output order, and the defaults are written in it. This keeps two
implementations, one of which has no ordered set, write the same document. `order.features` has no allowed list: it
keeps the order it was read in, with duplicates removed.

**Launch checks.** They are not document validation (a half-edited form and a two-field link stay valid documents).
They run when Launch is pressed, in this order, and each reports one field:

| Condition | Field |
|---|---|
| `profile` is null or names no stored profile | `profile` |
| `order.type=aft` and `order.source=register` and the profile has `api=v2` | `order.type` |
| `source=register`, `api=v1`, blank login or password | `profile.login` |
| `source=register`, `api=v2`, blank API key | `profile.apiKey` |
| `source=mdOrder`, `order.mdOrder` is null | `order.mdOrder` |
| `source=sessionId`, `order.sessionId` is null | `order.sessionId` |
| `sslPinning.enabled` and `pins` is empty | `sdk.sslPinning.pins` |

Opening an existing order needs the profile only for its `gatewayUrl`; its credentials may be empty, and the status
query is then skipped.

### Currencies

Alphabetic code to numeric: `EUR 978, USD 840, GBP 826, CHF 756, RUB 643, KZT 398, BGN 975, RON 946, PLN 985,
CZK 203, TRY 949, AED 784, UAH 980, BYN 933, GEL 981, AMD 051, AZN 944, UZS 860`.

### Gateway requests

`base` is `gatewayUrl` without a trailing `/`. `api2` is `api2Url(gatewayUrl)` exactly as in the samples' `OrderRegistrar`
(`…/payment` becomes `…/api2`). `orderNumber` is `qa-` and 20 hex characters. The timeout is 20 s. `order.extra` is merged
last, at the top level, and replaces a value of the same name whole (no deep merge); a `null` value removes the parameter
of that name from the request, form or JSON alike (a `null` for a name the request does not have changes nothing). In a
form request a string goes without its quotes, a number or a boolean as its JSON text (`5`, `true`), and an object or an
array as its JSON text (`{"a":1}`).

| Endpoint | Shape | Fields in order |
|---|---|---|
| `POST {base}/rest/register.do` (`payment`, v1) | form | `userName`, `password`, `amount`, `currency` (numeric), `orderNumber`, `returnUrl=sdk://done`, `clientId` if set, `features` = names joined by `,` if non-empty, extra |
| `POST {base}/rest/api/p2p/registerP2P.do` (`aft`, v1) | JSON | `username`, `password`, `amount` (number), `currency` (numeric string), `orderNumber`, `returnUrl`, `transactionTypeIndicator:"A"`, `features:{feature:[…]}` = `WITHOUT_TO_CARD` first, then `order.features` without duplicates, `clientId` if set, extra |
| `POST {api2}/sessions` (`payment`, v2) | JSON, header `X-API-KEY` | `amount` (number), `currency` (alphabetic), `successUrl`, `failureUrl` (`sdk://done`), `merchantCustomerId` if `clientId` is set, `features` array if non-empty ⚠, extra |
| `POST {base}/rest/getOrderStatusExtended.do` (v1 with credentials) | form | `userName`, `password`, `orderId` |

The `features` of `register.do` and the status request's parameters are not taken from the samples; both were checked
against the sandbox. `register.do` reads `features` as names split at `,`, ignores a name it does not know without a
word (the order registers as if it were absent) and rejects `FORCE_TDS` together with `FORCE_SSL` (`errorCode` 13).
⚠ marks the one format not checked yet: the `features` array of sessions (no API key for the sandbox's API v2). Unlike
the samples, the app adds nothing for a zero amount. On both platforms the app follows no redirect, since following one
would post the password again or carry the API key to another host: a 30x is read like any other answer that is not 2xx
and fails as `HTTP <status>: …`, so a profile must name the final URL. It sends no cookies and keeps none between calls.

Answers. `register.do` and `registerP2P.do`: `orderId`, else `errorMessage` or `Gateway error <errorCode>`. Sessions:
`id` (the `mdOrder` is decoded from it as the samples' `mdOrderOfSession` does, and is null when it does not decode,
which is not a failure), else `error.errorMessage` / `error.errorCode`. Status: `orderStatus`, `actionCode`,
`actionCodeDescription`, or a failure when `errorCode` is present and not `"0"`. A body that is none of these fails with
`HTTP <status>: <first 300 characters of the body>`. Every failure carries the HTTP status when there was a response.

Journal lines of an exchange (tag `merchant`): `→ POST <url> <body>` (`→ POST <url> X-API-KEY: *** <body>` when the
profile sends an API key) and `← <status> <body>`, with the values of
`password` and `X-API-KEY` replaced by `***`. The request line and a 2xx answer are `INFO`, any other answer `WARNING`. A
line is cut at 4000 characters (UTF-16 units) and ends in `…`; secrets are masked before the cut, in failure messages
too. With no answer within the timeout, the body included, the result fails with `The gateway did not answer in <whole
seconds> s`; a network error fails with `<the platform's error type>: <its message>`, at most 300 characters. Neither carries an HTTP status, and the journal shows either as `← <message>` at `WARNING`. A caller that is
cancelled gets no failure and no `←` line.

### Theme presets

`system`, `light` and `dark` are the SDK's own themes. The others are `PaymentTheme.Custom` over one palette each,
default typography and shapes unless listed. A custom preset does not follow the system's light or dark mode.

- Base palette (for `rounded`, `square`, `largeText`): primary `FF1F5FFF`, onPrimary `FFFFFFFF`, background `FFFFFFFF`,
  onBackground `FF14161A`, surface `FFF3F4F6`, onSurface `FF14161A`, error `FFD92D20`, onError `FFFFFFFF`, outline
  `FFD0D5DD`, success `FF12B76A`; the four optional tokens keep the SDK's defaults.
- `contrast`: primary `FF000000`, onPrimary `FFFFFFFF`, background `FFFFFFFF`, onBackground `FF000000`, surface
  `FFFFFFFF`, onSurface `FF000000`, error `FFB00020`, onError `FFFFFFFF`, outline `FF000000`, success `FF006400`,
  onSurfaceVariant `FF000000`, placeholder `FF595959`, outlineStrong `FF000000`, scrim `CC000000`.
- `rounded`: all four shapes `Pill`. `square`: all four `Radius(0)`.
- `largeText`: each of the eight styles of `PaymentTypography.Default` with `size` and `lineHeight` times 1.5 and a
  serif family: Android `FontFamily.Serif`, iOS `PaymentFontFamily(name: "Georgia")`. No font files are bundled.
- `debug`: primary `FFFF00FF`, onPrimary `FF00FF00`, background `FFFFFF00`, onBackground `FF0000FF`, surface `FF00FFFF`,
  onSurface `FFFF0000`, error `FFFF8000`, onError `FF800080`, outline `FF008000`, success `FF0080FF`, onSurfaceVariant
  `FF804000`, placeholder `FF808080`, outlineStrong `FF000000`, scrim `80FF0080`. The Theme group shows a legend of
  token and swatch while `debug` is selected, so QA can name the token in a bug report.

### Identifiers

`qa/fixtures/identifiers.txt` lists them, one per line. A `# android` or `# ios` after an identifier marks the ones a
single platform has; everything from `#` on is a comment. Android exposes them as test tags (as resource ids), iOS as
accessibility identifiers. A line with `<name>` or `<value>` is a pattern, not an identifier: `<name>` is a stored
profile's name (one identifier per profile), `<value>` an option of a choice, a chip of a set or a journal filter. An
option's identifier is its value in snake case: a lower-case letter followed by an upper-case one gets a `_` between
them, the whole is lower-cased, every run of other characters than `a-z` and `0-9` becomes one `_`, and no value
(`null` or empty) is `none` — `qa_order_currency_eur`, `qa_order_source_md_order`, `qa_sdk_theme_large_text`,
`qa_sdk_locale_none`, `qa_order_feature_force_tds`, `qa_sdk_google_pay_allowed_auth_methods_cryptogram_3ds`.

- Config fields: `qa_profile`, `qa_order_source`, `qa_order_type`, `qa_order_amount`, `qa_order_currency`,
  `qa_order_client_id`, `qa_order_features`, `qa_order_extra`, `qa_order_md_order`, `qa_order_session_id`,
  `qa_sdk_locale`, `qa_sdk_theme`, `qa_sdk_show_result_screen`, `qa_sdk_scanner`, `qa_sdk_key_provider_url`,
  `qa_sdk_ssl_pinning_enabled`, `qa_sdk_ssl_pinning_pins`.
- Android only: `qa_sdk_google_pay_enabled`, `_merchant_id`, `_merchant_full_name`, `_gateway`, `_gateway_merchant_id`,
  `_environment`, `_allowed_card_networks`, `_allowed_auth_methods`, `_country_code`, `_billing_address_required`.
- iOS only: `qa_sdk_apple_pay_enabled`, `_merchant_id`, `_supported_networks`, `_merchant_capabilities`,
  `_country_code`.
- Options (patterns): `qa_order_source_<value>`, `qa_order_type_<value>`, `qa_order_currency_<value>` (the items of
  the currency menu), `qa_order_feature_<value>` (a chip, a custom name included), `qa_sdk_theme_<value>`,
  `qa_sdk_locale_<value>`, `qa_profile_api_<value>`; Android only `qa_sdk_google_pay_environment_<value>`,
  `qa_sdk_google_pay_allowed_card_networks_<value>`, `qa_sdk_google_pay_allowed_auth_methods_<value>`. The field's own
  identifier stays on the control that holds the choice and its text is the value.
- Home rows, each opening a screen: `qa_profiles_open` (the stand, its text-bearing child is `qa_profile`),
  `qa_group_order` (Order screen), `qa_home_amount`, `qa_home_features` (both open the Order screen),
  `qa_group_theme` (Theme), `qa_group_sdk` (Behaviour), `qa_group_wallets` (the wallet's screen), `qa_group_security`
  (SSL pinning), `qa_home_last_run` (the result of the last run, shown once a run has finished); `qa_theme_legend`.
- Order screen: `qa_order_features_other` (+ Other), `qa_order_features_other_name`, `qa_order_features_other_add`,
  `qa_order_client_id_open`, `qa_order_extra_open` (open the field's own screen), `qa_order_md_order_paste`,
  `qa_order_session_id_paste`.
- Actions: `qa_launch`, `qa_menu`, `qa_import` (clipboard), `qa_import_qr`, `qa_export` (copies the `QaConfig` JSON),
  `qa_reset`, `qa_report`, `qa_run_again`.
- Stands (profiles): `qa_profile_name`, `qa_profile_gateway_url`, `qa_profile_api`, `qa_profile_login`,
  `qa_profile_password`, `qa_profile_password_show`, `qa_profile_api_key`, `qa_profile_api_key_show`, `qa_profile_add`,
  `qa_profile_enter`, `qa_profile_paste`, `qa_profile_scan`, `qa_profile_save`, `qa_profile_delete`,
  `qa_profile_select_<name>`, `qa_profile_edit_<name>`, `qa_profile_confirm`, `qa_profile_reject`.
- Asserted on: `qa_validation_error` (text `<field>: <message>`), `qa_result_type`, `qa_result_md_order`,
  `qa_result_md_order_copy`, `qa_result_session_id`, `qa_result_session_id_copy`, `qa_result_order_status`,
  `qa_result_error`, `qa_result_stand`, `qa_result_order`, `qa_result_theme`, `qa_journal_open`, `qa_journal_text`,
  `qa_journal_level` (text `all`, `gateway`, `sdk` or `errors`, the filter shown), `qa_journal_filter_<value>`,
  `qa_journal_search`, `qa_journal_clear`, `qa_journal_copy`.
- Text of a collection element: `qa_order_features` and the wallet sets are names separated by `,`; `qa_order_extra` is a
  JSON object; `qa_sdk_ssl_pinning_pins` is `host=pin` per line, split at the first `=` (pins end in `=`).
- `qa_result_type` text: `completed`, `failed`, `canceled`, `unknown`, `registration_failed`, `sdk_error`.
  `qa_result_order_status` text: `orderStatus=<n>, actionCode=<n>, <description>`, `not queried`, or `error: <message>`.
  A missing `orderStatus` or `actionCode` prints `-`, a missing description is left out together with its `, `. This
  text is exactly `not queried` here; only the run report adds the reason (see "Run report").
- On iOS a toolbar button's identifier (`qa_profiles_open`, `qa_journal_open`, `qa_menu`) can match two elements, the
  toolbar item and its button; the button-typed match is the one to use.

Each `QaConfig` field has exactly one form element on the platform that shows it (the other platform's wallet fields
have none), named by the field's path in snake_case with the `qa_` prefix. A
collection is one text element, not a row per item, so that identifiers do not depend on content. The profile name is
limited to `[a-z0-9_-]`, 1 to 32 characters, because it is part of an identifier.

### External input

Four ways in: a link, launch arguments, pasted text and a scanned QR code. Each yields either a list of steps applied
in order (apply a profile, apply a config, launch) or a single rejection naming one field; a rejection applies nothing,
not even the part that was valid, and launches nothing.

**Trimming.** Before anything else a link, pasted or scanned text and each launch argument or extra lose one leading
U+FEFF (a byte order mark), then ASCII space, tab, CR and LF at both ends, repeatedly. Nothing else is trimmed: a
U+0085, a U+00A0, a vertical tab, a second U+FEFF or a U+FEFF after the whitespace stays and makes the text unreadable.

**Link.** `payrdrqa://config?data=<base64url>[&launch=1]` or `payrdrqa://profile?data=<base64url>`, read in this order:

1. Field `link` unless the trimmed text is `payrdrqa://` (the scheme compared ignoring ASCII case only, so
   `PAYRDRQA://` is a link), then exactly `config` or `profile` (compared exactly: `CONFIG` is not), then either the
   end or `?` and the query. So these are `link`: any `#` anywhere (a fragment), a user-info (`user@config`), a port,
   even an empty one (`config:`), a path, even `/` alone (`config/`), an empty host (`payrdrqa:///config`), the opaque
   form without `//` (`payrdrqa:config`), a percent-escape in the host.
2. The query is split at `&` only (`;` is part of a value); empty pairs (`&&`, a leading or trailing `&`) are skipped.
   A pair is split at its first `=`: the name before it, the value after it (a pair without `=` has an empty value, a
   pair `=x` has an empty name). Names and values are percent-decoded strictly: `%` must be followed by two hex digits,
   and the decoded bytes must be UTF-8 (an overlong form like `%C0%AE` is not); `+` stays a plus. A pair whose name
   cannot be decoded is dropped; a value that cannot be decoded counts as an unusable value. Parameters other than
   `data` and `launch` are ignored.
3. Field `data` unless `data` occurs exactly once with a usable, non-empty value, and that value, after
   percent-decoding, is at most 87384 characters (padded base64url of the largest document; a longer one is refused
   before it is decoded), is base64url and decodes to UTF-8. Base64url is the alphabet `A–Z a–z 0–9 - _` only: `+`, `/`,
   whitespace or a line break anywhere is rejected. Padding is all or nothing: a length that is a multiple of 4 with the
   `=` it needs, or no `=` at all; `eyJhIjoxfQ=` (one of two) is rejected, and so are a length of 4n+1 characters and
   non-zero pad bits (`eyJhIjoxfR`). The bytes must be strict UTF-8 (no overlong forms, no surrogates). Bare base64 is
   never read as anything else.
4. The text is decoded as the document the host names (see Documents); an invalid document reports its own field
   unchanged. The decoded text of a profile link is trimmed as above first (field `data` when nothing is left); that
   of a config link is not, so a leading U+FEFF there makes the document unreadable.
5. A config link is followed by Launch when the query has exactly one `launch` and its decoded value is exactly `1`.
   Any other value, a repeated `launch` or an undecodable value is silently no launch, not an error. A profile link
   never launches, whatever its query says.

**Launch arguments**, raw JSON (not base64): Android extras `qaConfig`, `qaProfile` (strings) and `qaLaunch`
(boolean); iOS `-qaConfig <json>`, `-qaProfile <json>` and `-qaLaunch 1`, read from
`ProcessInfo.processInfo.arguments` (not `UserDefaults`, which would parse a `{…}` argument as a property list). On
iOS a flag's value is the argument after it, whatever it is (`-qaConfig -qaLaunch 1` is the config `-qaLaunch`), a
repeated flag keeps its last value, a flag with nothing after it is ignored, and `-qaLaunch` launches only when its
value is `1` after trimming (any other value is silently no launch). An argument that is empty after trimming is
field `data`; otherwise it is decoded as its document and reports its own field (`$` when it is not a JSON object).
The steps are: the profile, then the config, then Launch; `qaLaunch` without a config launches the stored one. When
either document is rejected the result is that one rejection (the profile's when both are), and nothing is applied or
launched. The launch arguments are the first input of the process, before a link that started it.

**Pasted or scanned text.** After trimming, text whose first 11 characters are `payrdrqa://` ignoring ASCII case is read
as a link, as above. Any other text must be a JSON object (field `$` otherwise, bare base64 included): a root object
with a `gatewayUrl` key, whatever its value, is a profile, any other object a config. iOS 16 and later asks "Allow
Paste" when `qa_import` reads text another app copied; declining reads no text, the `$` problem of an empty clipboard.

**Confirmation.** A config applies at once. A profile from a link, the clipboard or a QR waits for confirmation (its
name, its gateway URL, and "replaces the existing profile" when the name is taken), so that a link cannot silently
replace the stand and the credentials; from a launch argument it is saved directly. A rejection changes nothing and
shows `qa_validation_error` with its field. The limits of Documents (size and depth, field `$`) apply to every way in.

QR scanning uses the device camera and is hidden on the simulator. On Android the Google code scanner (Play services)
reads the code; when Play services cannot provide it the app says so, and a cancelled scan changes nothing. On iOS the
app reads the code with the camera itself and says so when there is no camera or access to it is denied. Neither is a
problem of a field: `qa_validation_error` does not change.

### Screens

What both apps do around a run, beyond the documents above.

- **One session.** The app has one form, one run and one journal per process. A launch while a run is busy (from
  registering the order to the end of the status query) is ignored, whether it comes from the Launch button,
  `launch=1` or `qaLaunch`; the other steps of that input (its profile, its config) are still applied, the open
  sheet stays open, its result is still shown, and the running attempt's journal is kept.
- **Launch and the form.** Launch is refused while a field holds text that does not parse (the field shows the
  problem): the last good value is not launched. An external launch in that state is ignored too, and the problem
  stays on screen. The form takes `order.amount` in major units of the currency: ASCII digits `0`–`9` with at most
  one `.` or `,` and at most as many digits after it as the currency has minor digits (2 for every currency of the
  app), no sign, no space and no other script's digits (`+5`, `2.505`, `５` and `٥` are problems on `order.amount`).
  The document keeps minor units: `10.5` EUR is `1050`. The field's text is the major amount (`10.00`), and the
  helper under it names the minor units.
- **Profiles from outside.** A profile that arrives from outside is saved, never selected; a config that names it, in
  the same input or later, selects it. Declining the confirmation shows no error and drops the rest of that input
  (its config and its launch). Inputs are applied one after another in the order they arrived: a second profile
  link while a confirmation is open waits, and its own confirmation is shown once the first one is answered. A
  confirmation that arrives while the SDK sheet is up waits under it and is answered once the sheet closes.
- **`qa_validation_error`** shows one problem, the first of: the form's problem with the text of a field; the
  notice about the last external input (its rejection) or about a failed save (field `store`, the message names
  what could not be saved, never a value); the refusal of the last launch. The notice clears with the next input or
  launch; the refusal clears once the config changes. A profile that cannot be saved or deleted from the profile editor
  shows the same `store` problem in the editor's error element.
- **Reset** returns every field to its default but keeps the selected profile. **Run again** on the result screen
  returns to the home screen with the form unchanged, Back to the screen before it; the result screen is opened once
  per run and stays reachable from the home screen's Last run row.
- **The screens.** Home lists the stand, the order, the checkout settings and the last run, one row per section with
  its state in words; a section with a problem shows it in red on its row, and Launch sits in the footer. Each row
  opens its screen: Order (source and type as segments, the amount with the currency menu, features as chips with
  + Other, the client id and extra parameters on screens of their own; for mdOrder or Session id only that field with
  Paste), Theme, Behaviour (locale, result screen, scanner, key provider URL), the wallet, SSL pinning, Stands (the
  stored stands with Edit, and adding one by hand, from the clipboard or from a QR code) and the stand editor. The
  journal shows a gateway request and its answer as one entry; its filters and the text of `qa_journal_text` do not
  change what the report prints.
- **Process end.** A run lives as long as the app process. When the process is killed while the sheet is open, the
  sheet's result is lost and the app starts Idle with the stored form.

### Run report

`qa_report` copies the report of the finished run as plain text. It is built from the finished run alone: the profile
and the configuration are the ones the run was launched with, not what the form holds when the report is copied.

Every line is the same text on both platforms except JSON. The Configuration section holds the same document on both
platforms as JSON, not as text: every field is present, but key order, spacing, escaping and the spelling of numbers
are the platform's own. The same holds for a JSON request or response body quoted in a journal line. The example below
is Android's output; the Android test `RunReportTest` compares its report with this block character for character, the
iOS test `RunReportTests` every line of it but the Configuration document, which it compares as JSON.

- Lines end in `\n`, the report ends in one `\n`, sections are separated by one empty line. No line is ever left out:
  a missing value prints `-`.
- Sections, in this order, each under a `== <Title> ==` line:
  - `== Versions ==`: `QA app: <app version>`, `SDK: <sdk version>`.
  - `== Device ==`: the device on one line, the OS on the next, with no label. The platform's own wording; the example
    shows Android's. iOS prints `Apple <model identifier>` (`Apple iPhone16,1`; a simulator, the model it simulates) and
    `iOS <version>`.
  - `== Profile ==`: `Name: <name>`, `Gateway URL: <gatewayUrl>`, `API: v1|v2`, nothing else. Passwords and API keys
    are masked (`***`) everywhere in the report; the login is not a secret and can appear in journal lines
    (`userName=<login>`).
  - `== Configuration ==`: the run's `QaConfig` document, every field, nulls and defaults included (JSON, see above).
  - `== Result ==`: `qa_result_type: <id>`, `mdOrder: <mdOrder>`, `Session id: <session id>`, `Error: <error>`,
    `HTTP status: <status>`. The error is the SDK error's message for `failed` (and the cause's message for `unknown`),
    the gateway's answer for `registration_failed`, `<SimpleClassName>: <message>` of what the sheet threw for
    `sdk_error` (`IllegalStateException: boom`). A missing or blank message prints the class name alone. A class with
    no simple name (anonymous) is named by its binary name after the last `.` on Android (e.g. `QaRunTest$run$1`), by
    its type name on iOS. On iOS the sheet throws nothing: `PayrdrCheckout.present` returns `false` when nothing opened
    (no key window, or a checkout already on screen), and the run finishes `sdk_error` with the error
    `PayrdrCheckoutError`, the type name of the SDK's own `PayrdrCheckoutError.notPresented`, which has no message. The
    HTTP status is the gateway's for `registration_failed` and the SDK's for an API error, `-` otherwise.
  - `== Order status ==`: the `qa_result_order_status` text, except that `not queried` is followed by `: <reason>`.
    The reasons are tried in this order and the first that applies is printed:
    - `not queried: the checkout did not run` for `registration_failed` and `sdk_error`;
    - `not queried: API v2 has no status query`;
    - `not queried: the profile has no login and password`;
    - `not queried: the checkout named no order` otherwise (the sheet closed before an order was known).
  - `== Journal ==`: first `Time zone: UTC±hh:mm`, the device's offset at the first line's time (at the time of the
    report when the journal is empty), then every line of the run as `HH:mm:ss.SSS LEVEL tag: message` in that zone.
    `LEVEL` is `DEBUG`, `INFO`, `WARNING` or `ERROR`. Digits and signs are written by hand, never by a locale-dependent
    formatter. Merchant lines are the ones of "Gateway requests", secrets already masked. When the sheet throws (on
    iOS: does not open), the journal gets `ERROR qa: The sheet threw <error>` (the error text of `sdk_error`), also when
    the sheet had already delivered its result and the run finishes with that result.
- A message, an error or a description that contains line breaks is printed as is, so one entry can span several lines.
- An SDK event with a cause journals `<message> (<SimpleName>: <cause message>)`, or `<message> (<SimpleName>)` when
  the cause has no message. The simple name is the Kotlin class's: Android's `simpleName` (the binary name when it is
  empty, an anonymous class); iOS reads it from the cause's Kotlin `toString()` (`description()` in Swift, which is
  `<qualified name>` or `<qualified name>: <message>`): without the `: <message>` suffix, cut at the first `: ` when a
  class overrides `toString()` after the default text, then what follows the last `.`, `Throwable` when nothing does.
  The Objective-C class cannot name it: a class the framework does not export arrives as a synthetic class or as its
  nearest exported superclass. Nested classes read the same on both platforms (`Inner`); an anonymous class reads
  `pkg.Outer$f$1` on Android and `Throwable` on iOS, where Kotlin/Native has no name for it. A Swift subclass of a
  Kotlin throwable is named by its Swift type name.
- The journal search (`qa_journal_search`) keeps the entries whose tag or message contains the text ignoring case as
  Kotlin's `contains(ignoreCase = true)`: Java's rule one UTF-16 char at a time (`İ` and `ı` match `i`, `ſ` matches
  `s`, `ß` matches `ẞ` but not `ss`; a character beyond the basic plane matches only itself). An empty search keeps
  every entry the chosen filter keeps.

Example, Android's output (`completed`, a registered order, v1 profile):

```text
== Versions ==
QA app: 1.0.0-beta.1 (7)
SDK: 4.0.0-beta.1

== Device ==
Google Pixel 8
Android 15 (API 35)

== Profile ==
Name: dev
Gateway URL: https://dev.bpcbt.com/payment
API: v1

== Configuration ==
{
    "version": 1,
    "profile": "dev",
    "order": {
        "source": "register",
        "type": "payment",
        "amount": 10000,
        "currency": "EUR",
        "clientId": null,
        "mdOrder": null,
        "sessionId": null,
        "features": [],
        "extra": {}
    },
    "sdk": {
        "locale": null,
        "theme": "system",
        "showResultScreen": false,
        "scanner": false,
        "keyProviderUrl": null,
        "sslPinning": {
            "enabled": false,
            "pins": []
        },
        "googlePay": {
            "enabled": false,
            "billingAddressRequired": false,
            "merchantId": null,
            "merchantFullName": null,
            "gateway": null,
            "gatewayMerchantId": null,
            "countryCode": null,
            "environment": null,
            "allowedCardNetworks": [
                "MASTERCARD",
                "VISA"
            ],
            "allowedAuthMethods": [
                "PAN_ONLY",
                "CRYPTOGRAM_3DS"
            ]
        },
        "applePay": {
            "enabled": false,
            "merchantId": "merchant.net.payrdr.samples.test",
            "supportedNetworks": [
                "MASTERCARD",
                "VISA"
            ],
            "merchantCapabilities": [
                "CREDIT",
                "DEBIT",
                "THREE_D_SECURE"
            ],
            "countryCode": null
        }
    }
}

== Result ==
qa_result_type: completed
mdOrder: 8f1e4c2a-0b3d-4e5f-9a6b-7c8d9e0f1a2b
Session id: -
Error: -
HTTP status: -

== Order status ==
orderStatus=2, actionCode=0, Approved

== Journal ==
Time zone: UTC+00:00
12:34:56.789 INFO merchant: → POST https://dev.bpcbt.com/payment/rest/register.do userName=login-1&password=***
12:34:57.102 INFO merchant: ← 200 {"orderId":"8f1e4c2a-0b3d-4e5f-9a6b-7c8d9e0f1a2b"}
```

## Fixtures

`qa/fixtures/cases.json` is a JSON array that both apps' unit tests read. Each element is

```json
{ "name": "…", "kind": "config" | "profile", "input": <any JSON> | "inputText": "<raw string>",
  "expect": { "document": <object> } | { "errorField": "<path>" } }
```

`input` is the parsed document; `inputText` is a raw string, for text that is not JSON, is too deep to be `input` or
repeats a key (a parsed `input` would keep only one). `kind` selects `QaConfig` or `StandProfile`. `expect.document` is
the full document as the encoder writes it (every field, nulls and defaults included): a test decodes `input`, encodes
the result and compares it with `document` structurally (key order does not matter; array order does, so a set must come
out in the order of its allowed values). `expect.errorField` is the field the decoder reports. Each failing input fails
on that field only. The profile fixtures carry the placeholder secrets `login-1`, `password-1` and `key-1`.

`qa/fixtures/identifiers.txt` is the identifier list above; a test checks that every identifier of the platform is
present in the app and that no other `qa_` identifier is.

## Not in the first version

A store front, the headless flow, the classic Activity, the SDK Core screen (use the samples for these), a per-token
theme editor, CI artifacts, Flutter and React Native, UI tests of the QA apps.
