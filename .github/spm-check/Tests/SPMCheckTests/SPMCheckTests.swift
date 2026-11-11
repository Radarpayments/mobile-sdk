import sdk_payment_scanner
import XCTest

final class SPMCheckTests: XCTestCase {

    func testVersion() {
        XCTAssertFalse(SdkPayment.companion.VERSION.isEmpty)
        if let expected = ProcessInfo.processInfo.environment["EXPECTED_SDK_VERSION"] {
            XCTAssertEqual(SdkPayment.companion.VERSION, expected)
        }
    }

    // The checkout is drawn by the framework's Swift code; without it presenting the checkout fails.
    func testSwiftUICheckoutIsInTheFramework() {
        XCTAssertNotNil(NSClassFromString("PayrdrSwiftUICheckoutRenderer"))
    }

    func testSwiftConveniencesAreInTheFramework() {
        XCTAssertEqual(SdkPaymentConfig(baseUrl: "https://dev.bpcbt.com/payment").baseUrl, "https://dev.bpcbt.com/payment")
        XCTAssertEqual(ApplePayConfig(merchantId: "merchant.com.example").merchantId, "merchant.com.example")
    }
}
