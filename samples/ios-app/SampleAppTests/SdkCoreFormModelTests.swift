import XCTest
@testable import SampleApp
import RadarpaymentsSDKPayment

final class SdkCoreFormModelTests: XCTestCase {
    func test_newCardVariant_buildsParams() {
        let m = SdkCoreFormModel()
        m.variant = .newCard
        m.pan = "5391119268214792"
        m.expiry = "12/28"
        m.cvc = "123"
        m.holder = "Roy Band"
        m.mdOrder = "c400b41a-aa3d-43db-8727-ac4ca9e8f701"
        m.pubKeyPem = "-----BEGIN PUBLIC KEY-----\nabc\n-----END PUBLIC KEY-----"
        let params = m.buildParams()
        XCTAssertNotNil(params)
    }

    func test_instantVariant_buildsParams() {
        let m = SdkCoreFormModel()
        m.variant = .instant
        m.pan = "5391119268214792"
        m.expiry = "12/28"
        m.cvc = "123"
        m.pubKeyPem = "-----BEGIN PUBLIC KEY-----\nabc\n-----END PUBLIC KEY-----"
        XCTAssertNotNil(m.buildParams())
    }

    func test_storedVariant_buildsParams() {
        let m = SdkCoreFormModel()
        m.variant = .stored
        m.bindingId = "513b17f4-e32e-414f-8c74-936fd7027baa"
        m.mdOrder = "c400b41a-aa3d-43db-8727-ac4ca9e8f701"
        m.pubKeyPem = "-----BEGIN PUBLIC KEY-----\nabc\n-----END PUBLIC KEY-----"
        XCTAssertNotNil(m.buildParams())
    }

    func test_expiry_input_gets_its_slash_after_the_month() {
        XCTAssertEqual(formatExpiryInput("1"), "1")
        XCTAssertEqual(formatExpiryInput("12"), "12")
        XCTAssertEqual(formatExpiryInput("123"), "12/3")
        XCTAssertEqual(formatExpiryInput("12/305"), "12/30")
    }

    func test_empty_card_is_reported_at_the_card_number() {
        let m = SdkCoreFormModel()
        m.variant = .newCard
        m.mdOrder = AppPrefsDefaults.mdOrder
        m.pubKeyPem = AppPrefsDefaults.pubKeyPem
        m.submit()
        XCTAssertEqual(m.errors, [.pan: "required"])
        XCTAssertNil(m.token)
    }

    func test_card_data_turn_into_a_token() {
        let m = SdkCoreFormModel()
        m.variant = .newCard
        m.pan = "4111111111111111"
        m.expiry = "12/30"
        m.cvc = "123"
        m.mdOrder = AppPrefsDefaults.mdOrder
        m.pubKeyPem = AppPrefsDefaults.pubKeyPem
        m.submit()
        XCTAssertEqual(m.errors, [:])
        XCTAssertFalse((m.token ?? "").isEmpty)
    }
}
