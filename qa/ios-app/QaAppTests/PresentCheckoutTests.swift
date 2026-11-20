@testable import QaApp
import RadarpaymentsSDKPayment
import XCTest

private final class StubScanner: NSObject, CardScannerProvider {}

@MainActor
final class PresentCheckoutTests: XCTestCase {
    private let profile = StandProfile(
        name: "sandbox", gatewayUrl: "https://dev.bpcbt.com/payment", api: .v1, login: "login-1", password: "password-1"
    )

    private func checkout() -> PayrdrCheckout {
        let config = sdkPaymentConfig(
            config: QaConfig(profile: "sandbox"), profile: profile, journal: Journal(), scanner: StubScanner()
        )
        return PayrdrCheckout(config: config)
    }

    func test_no_window_opens_nothing_and_reports_no_result() {
        var results = 0
        let opened = presentCheckout(checkout(), order: OpenOrder(mdOrder: "md-1", sessionId: nil), from: nil) { _ in
            results += 1
        }

        XCTAssertFalse(opened)
        XCTAssertEqual(results, 0)
    }

    func test_a_controller_outside_any_window_opens_nothing_and_reports_no_result() {
        var results = 0
        let opened = presentCheckout(checkout(), order: OpenOrder(mdOrder: "md-1", sessionId: nil), from: UIViewController()) { _ in
            results += 1
        }

        XCTAssertFalse(opened)
        XCTAssertEqual(results, 0)
    }
}
