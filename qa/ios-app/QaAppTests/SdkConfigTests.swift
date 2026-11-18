@testable import QaApp
import RadarpaymentsSDKPayment
import XCTest

private final class StubScanner: NSObject, CardScannerProvider {}

@MainActor
final class SdkConfigTests: XCTestCase {
    private let profile = StandProfile(
        name: "sandbox", gatewayUrl: "https://dev.bpcbt.com/payment", api: .v1, login: "login-1", password: "password-1"
    )
    private let journal = Journal()
    private let scanner = StubScanner()
    private let pin = Pin(host: "dev.bpcbt.com", pin: "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")

    private func build(_ sdk: SdkOptions = SdkOptions()) -> SdkPaymentConfig {
        sdkPaymentConfig(config: QaConfig(profile: "sandbox", sdk: sdk), profile: profile, journal: journal, scanner: scanner)
    }

    private func withoutPinning(_ config: SdkPaymentConfig) -> SdkPaymentConfig {
        config.doCopy(
            __baseUrl: config.baseUrl,
            keyProviderUrl: config.keyProviderUrl,
            sslPinning: nil,
            logTransport: config.logTransport,
            locale: config.locale,
            theme: config.theme,
            googlePay: config.googlePay,
            applePay: config.applePay,
            cardScannerProvider: config.cardScannerProvider,
            showResultScreen: config.showResultScreen
        )
    }

    func test_the_default_config_opens_the_profiles_gateway_with_nothing_optional() {
        let config = build()
        XCTAssertEqual(config.baseUrl, "https://dev.bpcbt.com/payment")
        XCTAssertNil(config.locale)
        XCTAssertNil(config.keyProviderUrl)
        XCTAssertNil(config.sslPinning)
        XCTAssertNil(config.googlePay)
        XCTAssertNil(config.applePay)
        XCTAssertNil(config.cardScannerProvider)
        XCTAssertTrue(PaymentThemeSystem.shared.isEqual(config.theme))
        XCTAssertFalse(config.showResultScreen)
        XCTAssertTrue(config.logTransport === journal)
    }

    func test_the_sdk_options_are_passed_as_set() {
        let config = build(
            SdkOptions(
                locale: "ro",
                theme: .dark,
                showResultScreen: true,
                keyProviderUrl: "https://dev.bpcbt.com/payment/se/keys.do"
            )
        )
        XCTAssertEqual(config.locale, "ro")
        XCTAssertTrue(PaymentThemeDark.shared.isEqual(config.theme))
        XCTAssertTrue(config.showResultScreen)
        XCTAssertEqual(config.keyProviderUrl, "https://dev.bpcbt.com/payment/se/keys.do")
    }

    func test_apple_pay_is_passed_only_when_enabled() {
        let options = ApplePayOptions(enabled: true)
        XCTAssertEqual(
            build(SdkOptions(applePay: options)).applePay,
            ApplePayConfig(
                merchantId: "merchant.net.payrdr.samples.test",
                supportedNetworks: [.mastercard, .visa],
                merchantCapabilities: [.credit, .debit, .threeDSecure],
                countryCode: nil
            )
        )
        var disabled = options
        disabled.enabled = false
        XCTAssertNil(build(SdkOptions(applePay: disabled)).applePay)
    }

    func test_every_apple_pay_field_is_passed() {
        let options = ApplePayOptions(
            enabled: true,
            merchantId: "merchant.net.payrdr.qa",
            supportedNetworks: ["AMEX", "DISCOVER", "JCB", "MASTERCARD", "VISA"],
            merchantCapabilities: ["EMV"],
            countryCode: "CY"
        )
        XCTAssertEqual(
            build(SdkOptions(applePay: options)).applePay,
            ApplePayConfig(
                merchantId: "merchant.net.payrdr.qa",
                supportedNetworks: Set(ApplePayNetwork.allCases),
                merchantCapabilities: [.emv],
                countryCode: "CY"
            )
        )
    }

    func test_google_pay_is_never_passed_on_ios() {
        XCTAssertNil(build(SdkOptions(googlePay: GooglePayOptions(enabled: true, environment: "test"))).googlePay)
    }

    func test_the_scanner_is_passed_only_when_asked_for() {
        XCTAssertTrue(build(SdkOptions(scanner: true)).cardScannerProvider === scanner)
        XCTAssertNil(build(SdkOptions(scanner: false)).cardScannerProvider)
    }

    func test_pinning_is_passed_only_when_enabled() {
        XCTAssertNotNil(build(SdkOptions(sslPinning: SslPinningOptions(enabled: true, pins: [pin]))).sslPinning)
        XCTAssertNil(build(SdkOptions(sslPinning: SslPinningOptions(enabled: false, pins: [pin]))).sslPinning)
    }

    // The SDK's builder rejects what it cannot pin with a Kotlin exception, which would end the app; every host the
    // validation lets through must build.
    func test_the_most_exotic_pin_hosts_the_validation_accepts_build() throws {
        let hosts = ["**.A-1.bpcbt.com", "*.bpcbt.com", String(repeating: "a", count: 63) + ".x--y.com", "localhost", "10.0.0.1"]
        let pinning = SslPinningOptions(enabled: true, pins: hosts.map { Pin(host: $0, pin: pin.pin) })
        let text = QaConfigJson.encode(QaConfig(sdk: SdkOptions(sslPinning: pinning)))
        guard case let .ok(config) = QaConfigJson.decode(text) else { return XCTFail("the pins do not validate: \(text)") }

        XCTAssertNotNil(build(config.sdk).sslPinning)
    }

    func test_equal_configs_build_equal_sdk_configs() {
        let sdk = SdkOptions(theme: .largeText, applePay: ApplePayOptions(enabled: true), scanner: true)
        XCTAssertEqual(build(sdk), build(sdk))
        for preset in ThemePreset.allCases {
            XCTAssertEqual(build(SdkOptions(theme: preset)), build(SdkOptions(theme: preset)), "\(preset)")
        }
    }

    // SslPinningConfig has no equality: two builds of one pin list are different objects, so an sdk config with pinning
    // never equals another, and the caller has to key the checkout on the QaConfig rather than on this result.
    func test_with_pinning_only_the_pinning_config_differs_between_two_builds() {
        let sdk = SdkOptions(sslPinning: SslPinningOptions(enabled: true, pins: [pin]))
        let first = build(sdk)
        let second = build(sdk)
        XCTAssertNotEqual(first.sslPinning, second.sslPinning)
        XCTAssertNotEqual(first, second)
        XCTAssertEqual(withoutPinning(first), withoutPinning(second))
    }
}
