import XCTest
import sdk_payment_scanner
@testable import SampleApp

/// A registrar that answers only when the test says so.
private final class HeldRegistrar: OrderRegistrar, @unchecked Sendable {
    private(set) var calls = 0
    private(set) var continuation: CheckedContinuation<OrderRegistration, Never>?

    func register(amountMinor: Int64, clientId: String?) async -> OrderRegistration {
        calls += 1
        return await withCheckedContinuation { continuation = $0 }
    }

    func answer(_ registration: OrderRegistration) { continuation?.resume(returning: registration) }
}

private struct FixedRegistrar: OrderRegistrar {
    let registration: OrderRegistration
    func register(amountMinor: Int64, clientId: String?) async -> OrderRegistration { registration }
}

@MainActor
final class CheckoutTests: XCTestCase {
    private let coverTheme = PaymentThemeLight.shared

    private func config(_ scenario: Scenario = Scenario(), _ override: SheetThemeOverride = .cover) -> SdkPaymentConfig {
        buildSdkConfig(baseUrl: "https://gw/payment", scenario: scenario, coverTheme: coverTheme, themeOverride: override,
                       locale: nil, applePayMerchantId: "merchant.test")
    }

    func test_scanner_off_gives_no_scanner() {
        XCTAssertNil(config(Scenario(scanner: false)).cardScannerProvider)
        XCTAssertNotNil(config(Scenario(scanner: true)).cardScannerProvider)
    }

    func test_wallet_off_gives_no_apple_pay() {
        XCTAssertNil(config(Scenario(wallet: false)).applePay)
        XCTAssertEqual(config(Scenario(wallet: true)).applePay?.merchantId, "merchant.test")
    }

    func test_sdk_result_screen_maps_through() {
        XCTAssertTrue(config(Scenario(sdkResultScreen: true)).showResultScreen)
    }

    func test_theme_override() {
        // Kotlin objects come to Swift as fresh wrappers, so compare kinds, not references.
        XCTAssertTrue(config(Scenario(), .cover).theme is PaymentThemeLight)
        XCTAssertTrue(config(Scenario(), .dark).theme is PaymentThemeDark)
        XCTAssertTrue(config(Scenario(), .system).theme is PaymentThemeSystem)
    }

    func test_outcomes() {
        XCTAssertEqual(outcome(of: CheckoutResultCompleted(orderId: "p-1")), .success(paymentId: "p-1"))
        XCTAssertEqual(outcome(of: CheckoutResultCanceled(orderId: "p-1")), .cancelled)
        XCTAssertEqual(
            outcome(of: CheckoutResultFailed(orderId: "p-1", error: SdkException(message: "Declined by issuer", cause: nil))),
            .declined(message: "Declined by issuer")
        )
        XCTAssertEqual(outcome(of: CheckoutResultUnknown(orderId: "p-1", cause: nil)), .unconfirmed(paymentId: "p-1"))
    }

    func test_pay_while_registering_is_ignored() async {
        let registrar = HeldRegistrar()
        var presented: [String] = []
        let checkout = StoreCheckout(registrar: registrar, present: { mdOrder, _ in presented.append(mdOrder) })
        checkout.pay(amountMinor: 100, clientId: nil)
        checkout.pay(amountMinor: 100, clientId: nil)
        XCTAssertEqual(checkout.state, .registering)
        // Wait until the registration is really in flight before answering it.
        while registrar.continuation == nil { await Task.yield() }
        registrar.answer(.registered(mdOrder: "md-1"))
        await checkout.waitUntilSettled()
        XCTAssertEqual(registrar.calls, 1)
        XCTAssertEqual(presented, ["md-1"])
        XCTAssertEqual(checkout.state, .inSheet)
    }

    func test_an_api_v2_session_is_presented_as_a_session_and_reported_by_its_mdOrder() async {
        var presented: [String?] = []
        var reported: [String] = []
        let checkout = StoreCheckout(
            registrar: FixedRegistrar(registration: .registered(mdOrder: "md-1", sessionId: "ps_1")),
            present: { _, sessionId in presented.append(sessionId) },
            onMdOrder: { reported.append($0) }
        )
        checkout.pay(amountMinor: 100, clientId: nil)
        await checkout.waitUntilSettled()
        XCTAssertEqual(presented, ["ps_1"])
        XCTAssertEqual(reported, ["md-1"])
    }

    func test_missing_api_key_does_not_present() async {
        var presented: [String] = []
        let checkout = StoreCheckout(registrar: FixedRegistrar(registration: .missingApiKey), present: { mdOrder, _ in presented.append(mdOrder) })
        checkout.pay(amountMinor: 100, clientId: nil)
        await checkout.waitUntilSettled()
        XCTAssertEqual(checkout.state, .registrationFailed(.missingApiKey))
        XCTAssertTrue(presented.isEmpty)
    }

    func test_missing_credentials_does_not_present() async {
        var presented: [String] = []
        let checkout = StoreCheckout(registrar: FixedRegistrar(registration: .missingCredentials), present: { mdOrder, _ in presented.append(mdOrder) })
        checkout.pay(amountMinor: 100, clientId: nil)
        await checkout.waitUntilSettled()
        XCTAssertEqual(checkout.state, .registrationFailed(.missingCredentials))
        XCTAssertTrue(presented.isEmpty)
    }

    func test_reset_clears_a_registration_error() async {
        let checkout = StoreCheckout(registrar: FixedRegistrar(registration: .missingCredentials), present: { _, _ in })
        checkout.pay(amountMinor: 100, clientId: nil)
        await checkout.waitUntilSettled()
        checkout.resetError()
        XCTAssertEqual(checkout.state, .idle)
    }
}
