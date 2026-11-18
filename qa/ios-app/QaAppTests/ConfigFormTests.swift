@testable import QaApp
import XCTest

@MainActor
final class ConfigFormTests: XCTestCase {
    private let form = ConfigForm(QaConfig())
    private let pin = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="

    func test_editing_the_amount_changes_the_config() {
        form.edit(Tags.orderAmount, "250")

        XCTAssertEqual(form.config.order.amount, 250)
        XCTAssertNil(form.problem)
    }

    func test_a_half_typed_amount_keeps_the_last_good_config_and_names_the_field() {
        form.edit(Tags.orderAmount, "25x")

        XCTAssertEqual(form.config.order.amount, 10000)
        XCTAssertEqual(form.text(Tags.orderAmount), "25x")
        XCTAssertEqual(form.problem?.field, "order.amount")
    }

    func test_an_amount_out_of_range_names_the_field() {
        form.edit(Tags.orderAmount, "1000000000000")

        XCTAssertEqual(form.config.order.amount, 10000)
        XCTAssertEqual(form.problem?.field, "order.amount")
    }

    func test_the_amount_takes_ascii_digits_only() {
        for text in ["+500", "-500", "５００", "٥٠٠", " 500", "5 00", "", "99999999999999999999"] {
            form.edit(Tags.orderAmount, text)

            XCTAssertEqual(form.config.order.amount, 10000, text)
            XCTAssertEqual(form.problem?.field, "order.amount", text)
        }
    }

    func test_correcting_the_text_clears_the_problem() {
        form.edit(Tags.orderAmount, "25x")
        form.edit(Tags.orderAmount, "25")

        XCTAssertNil(form.problem)
        XCTAssertEqual(form.config.order.amount, 25)
    }

    func test_the_text_keeps_what_was_typed_while_it_is_valid() {
        form.edit(Tags.orderFeatures, "VERIFY,")

        XCTAssertEqual(form.text(Tags.orderFeatures), "VERIFY,")
        XCTAssertEqual(form.config.order.features, ["VERIFY"])
    }

    func test_features_are_typed_as_a_comma_separated_line() {
        form.edit(Tags.orderFeatures, "VERIFY, FORCE_CREATE_BINDING")

        XCTAssertEqual(form.config.order.features, ["VERIFY", "FORCE_CREATE_BINDING"])
    }

    func test_a_feature_with_a_space_inside_names_the_field() {
        form.edit(Tags.orderFeatures, "VERIFY BINDING")

        XCTAssertEqual(form.config.order.features, [])
        XCTAssertEqual(form.problem?.field, "order.features")
    }

    func test_extra_is_typed_as_a_json_object() {
        form.edit(Tags.orderExtra, #"{"description":"QA"}"#)

        XCTAssertEqual(form.config.order.extra, ["description": .string("QA")])
    }

    func test_extra_with_a_forbidden_key_is_not_applied() {
        form.edit(Tags.orderExtra, #"{"password":"x"}"#)

        XCTAssertEqual(form.config.order.extra, JSONObject())
        XCTAssertEqual(form.problem?.field, "order.extra")
    }

    func test_pins_are_typed_one_per_line() {
        form.edit(Tags.sdkSslPinningPins, "a.example=\(pin)\nb.example=\(pin)")

        XCTAssertEqual(form.config.sdk.sslPinning.pins, [Pin(host: "a.example", pin: pin), Pin(host: "b.example", pin: pin)])
    }

    func test_a_pin_that_is_not_a_pin_names_the_field() {
        form.edit(Tags.sdkSslPinningPins, "a.example=AAAA")

        XCTAssertEqual(form.config.sdk.sslPinning.pins, [])
        XCTAssertEqual(form.problem?.field, "sdk.sslPinning.pins")
    }

    func test_an_empty_optional_text_is_null() {
        form.edit(Tags.orderClientId, "client-1")
        form.edit(Tags.orderClientId, "")

        XCTAssertNil(form.config.order.clientId)
    }

    func test_a_selector_takes_the_json_value() {
        form.edit(Tags.orderSource, "mdOrder")
        form.edit(Tags.sdkTheme, "largeText")

        XCTAssertEqual(form.config.order.source, .mdOrder)
        XCTAssertEqual(form.config.sdk.theme, .largeText)
        XCTAssertEqual(form.text(Tags.sdkTheme), "largeText")
    }

    func test_a_value_a_selector_does_not_offer_names_the_field() {
        form.edit(Tags.sdkLocale, "xx")

        XCTAssertNil(form.config.sdk.locale)
        XCTAssertEqual(form.problem?.field, "sdk.locale")
    }

    func test_a_switch_takes_true_and_false() {
        form.edit(Tags.sdkScanner, "true")
        form.edit(Tags.sdkSslPinningEnabled, "true")
        form.edit(Tags.sdkApplePayEnabled, "true")

        XCTAssertTrue(form.config.sdk.scanner)
        XCTAssertTrue(form.config.sdk.sslPinning.enabled)
        XCTAssertTrue(form.config.sdk.applePay.enabled)
    }

    func test_a_switch_takes_nothing_but_true_and_false() {
        form.edit(Tags.sdkScanner, "yes")

        XCTAssertFalse(form.config.sdk.scanner)
        XCTAssertEqual(form.problem?.field, "sdk.scanner")
    }

    func test_a_wallet_set_is_kept_in_the_order_of_the_contract() {
        form.edit(Tags.sdkApplePaySupportedNetworks, "VISA, AMEX")

        XCTAssertEqual(form.config.sdk.applePay.supportedNetworks, ["AMEX", "VISA"])
    }

    func test_a_wallet_set_with_an_unknown_name_names_the_field() {
        form.edit(Tags.sdkApplePayMerchantCapabilities, "CREDIT, OTHER")

        XCTAssertEqual(form.config.sdk.applePay.merchantCapabilities, ["CREDIT", "DEBIT", "THREE_D_SECURE"])
        XCTAssertEqual(form.problem?.field, "sdk.applePay.merchantCapabilities")
    }

    func test_a_blank_apple_pay_merchant_id_names_the_field() {
        form.edit(Tags.sdkApplePayMerchantId, " ")

        XCTAssertEqual(form.config.sdk.applePay.merchantId, ApplePayOptions().merchantId)
        XCTAssertEqual(form.problem?.field, "sdk.applePay.merchantId")
    }

    func test_a_profile_name_outside_the_pattern_names_the_field() {
        form.edit(Tags.profile, "Dev Stand")

        XCTAssertNil(form.config.profile)
        XCTAssertEqual(form.problem?.field, "profile")
    }

    func test_the_first_pending_problem_is_the_first_field_of_the_form() {
        form.edit(Tags.sdkLocale, "xx")
        form.edit(Tags.orderAmount, "x")

        XCTAssertEqual(form.problem?.field, "order.amount")
    }

    func test_setting_a_config_drops_the_drafts() {
        form.edit(Tags.orderAmount, "x")
        form.set(QaConfig())

        XCTAssertNil(form.problem)
        XCTAssertEqual(form.text(Tags.orderAmount), "10000")
    }

    func test_every_field_of_the_contract_can_be_edited() {
        let populated = QaConfig(
            profile: "dev",
            order: OrderConfig(clientId: "c", features: ["VERIFY"], extra: ["a": .number("1")]),
            sdk: SdkOptions(
                locale: "de",
                applePay: ApplePayOptions(enabled: true, supportedNetworks: ["VISA"], countryCode: "CY"),
                sslPinning: SslPinningOptions(enabled: true, pins: [Pin(host: "dev.bpcbt.com", pin: pin)])
            )
        )
        for start in [QaConfig(), populated] {
            let form = ConfigForm(start)
            for tag in Tags.fields {
                form.edit(tag, form.text(tag))

                XCTAssertNil(form.problem, tag)
                XCTAssertEqual(form.config, start, tag)
            }
        }
    }
}
