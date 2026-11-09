import sdk_payment_scanner
import XCTest

final class SPMCheckTests: XCTestCase {

    func testVersion() {
        XCTAssertFalse(SdkPayment.companion.VERSION.isEmpty)
        if let expected = ProcessInfo.processInfo.environment["EXPECTED_SDK_VERSION"] {
            XCTAssertEqual(SdkPayment.companion.VERSION, expected)
        }
    }

    // Without them the first checkout screen throws MissingResourceException.
    func testComposeResourcesAreInTheFramework() {
        XCTAssertNotNil(Bundle(for: SdkPayment.self).url(forResource: "composeResources", withExtension: nil))
    }

    func testSwiftConveniencesAreInTheFramework() {
        XCTAssertEqual(SdkPaymentConfig(baseUrl: "https://dev.bpcbt.com/payment").baseUrl, "https://dev.bpcbt.com/payment")
        XCTAssertEqual(ApplePayConfig(merchantId: "merchant.com.example").merchantId, "merchant.com.example")
    }
}
