@testable import QaApp
import XCTest

final class QaConfigJsonTests: XCTestCase {
    private let pin = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="

    private func field(_ text: String) -> String? {
        guard case let .invalid(problem) = QaConfigJson.decode(text) else { return nil }
        return problem.field
    }

    private func ok(_ text: String, file: StaticString = #filePath, line: UInt = #line) -> QaConfig {
        switch QaConfigJson.decode(text) {
        case let .ok(config):
            return config
        case let .invalid(problem):
            XCTFail("\(text) failed on \(problem.field): \(problem.message)", file: file, line: line)
            return QaConfig()
        }
    }

    func test_the_default_config_is_what_an_empty_object_decodes_to() {
        XCTAssertEqual(ok("{}"), QaConfig())
    }

    func test_the_default_config_survives_a_round_trip() {
        XCTAssertEqual(QaConfigJson.decode(QaConfigJson.encode(QaConfig())), .ok(QaConfig()))
    }

    func test_an_integral_number_is_read_by_value_whatever_its_spelling() {
        for spelling in ["100", "100.0", "1e2", "1E2", "1.00e2", "1e+2", "10000e-2"] {
            XCTAssertEqual(ok("{\"order\":{\"amount\":\(spelling)}}").order.amount, 100, spelling)
        }
    }

    func test_a_boolean_a_string_or_a_fraction_is_not_an_amount() {
        for value in ["true", "\"100\"", "100.5", "[]", "{}"] {
            XCTAssertEqual(field("{\"order\":{\"amount\":\(value)}}"), "order.amount", value)
        }
    }

    func test_an_absurd_exponent_is_rejected_without_exhausting_memory() {
        XCTAssertEqual(field("{\"order\":{\"amount\":1e999999999}}"), "order.amount")
        XCTAssertEqual(field("{\"order\":{\"amount\":1e-999999999}}"), "order.amount")
        XCTAssertEqual(ok("{\"order\":{\"amount\":0e999999999}}").order.amount, 0)
    }

    func test_an_exponent_at_the_top_of_the_int_range_is_a_wrong_value_and_not_a_crash() {
        let values = [
            "1e2147483647", "-1e2147483647", "12e2147483647", "100e2147483647", "1e2147483648", "1e99999999999",
            "-1e99999999999",
        ]
        for value in values {
            XCTAssertEqual(field("{\"order\":{\"amount\":\(value)}}"), "order.amount", value)
            XCTAssertEqual(field("{\"version\":\(value)}"), "version", value)
        }
        XCTAssertEqual(field("{\"order\":{\"amount\":1e-2147483648}}"), "order.amount")
        XCTAssertEqual(field("{\"order\":{\"amount\":0.5e-2147483647}}"), "order.amount")
    }

    func test_the_messages_of_a_wrong_amount_read_as_on_android() {
        XCTAssertEqual(
            QaConfigJson.decode("{\"order\":{\"amount\":100.5}}"),
            .invalid(Problem(field: "order.amount", message: "must be an integer"))
        )
        XCTAssertEqual(
            QaConfigJson.decode("{\"order\":{\"amount\":1e20}}"),
            .invalid(Problem(field: "order.amount", message: "must be between 0 and 999999999999"))
        )
        XCTAssertEqual(QaConfigJson.decode("{\"version\":2}"), .invalid(Problem(field: "version", message: "must be 1")))
        XCTAssertEqual(ok("{\"order\":{\"amount\":999999999999}}").order.amount, 999_999_999_999)
    }

    func test_an_explicit_null_is_the_same_as_an_absent_field() {
        let text = "{\"profile\":null,\"order\":{\"amount\":null,\"currency\":null,\"features\":null}," +
            "\"sdk\":{\"theme\":null,\"googlePay\":null}}"
        XCTAssertEqual(ok(text), QaConfig())
    }

    func test_a_blank_string_is_null_for_every_nullable_string() {
        let config = ok("{\"profile\":\"\",\"sdk\":{\"locale\":\" \",\"googlePay\":{\"environment\":\" \",\"merchantId\":\"\\t\"}}}")
        XCTAssertNil(config.profile)
        XCTAssertNil(config.sdk.locale)
        XCTAssertNil(config.sdk.googlePay.environment)
        XCTAssertNil(config.sdk.googlePay.merchantId)
    }

    func test_a_non_blank_string_is_never_trimmed() {
        XCTAssertEqual(ok("{\"order\":{\"clientId\":\" client \"}}").order.clientId, " client ")
    }

    func test_a_known_object_field_that_is_not_an_object_fails_with_its_own_path() {
        XCTAssertEqual(field("{\"order\":1}"), "order")
        XCTAssertEqual(field("{\"sdk\":[]}"), "sdk")
        XCTAssertEqual(field("{\"sdk\":{\"googlePay\":\"x\"}}"), "sdk.googlePay")
        XCTAssertEqual(field("{\"sdk\":{\"applePay\":true}}"), "sdk.applePay")
        XCTAssertEqual(field("{\"sdk\":{\"sslPinning\":1}}"), "sdk.sslPinning")
    }

    func test_an_element_of_pins_that_is_not_an_object_fails_as_the_pins_field() {
        XCTAssertEqual(field("{\"sdk\":{\"sslPinning\":{\"pins\":[\"x\"]}}}"), "sdk.sslPinning.pins")
        XCTAssertEqual(field("{\"sdk\":{\"sslPinning\":{\"pins\":\"x\"}}}"), "sdk.sslPinning.pins")
        XCTAssertEqual(field("{\"sdk\":{\"sslPinning\":{\"pins\":[{\"host\":\"a.example\"}]}}}"), "sdk.sslPinning.pins")
    }

    func test_a_pin_host_that_is_not_a_hostname_fails_as_the_pins_field_whether_or_not_pinning_is_on() {
        let hosts = [
            ".", "https://dev.bpcbt.com", "dev.bpcbt.com:443", "dev.bpcbt.com/x", "a*b.com", "dev..bpcbt.com",
            "dev.bpcbt.com.", ".dev.bpcbt.com", "*", "*.", "***.bpcbt.com", "dev.*.bpcbt.com", "a_b.com", "дев.рф",
            String(repeating: "a", count: 64) + ".com",
        ]
        for host in hosts {
            for enabled in [true, false] {
                let text = "{\"sdk\":{\"sslPinning\":{\"enabled\":\(enabled),\"pins\":[{\"host\":\"\(host)\",\"pin\":\"\(pin)\"}]}}}"
                XCTAssertEqual(field(text), "sdk.sslPinning.pins", host)
            }
        }
    }

    func test_a_pin_host_may_be_a_hostname_with_a_leading_one_or_any_depth_wildcard() {
        let hosts = [
            "dev.bpcbt.com", "localhost", "DEV-1.Bpcbt.com", "10.0.0.1", "*.bpcbt.com", "**.bpcbt.com",
            String(repeating: "a", count: 63) + ".com",
        ]
        for host in hosts {
            let text = "{\"sdk\":{\"sslPinning\":{\"enabled\":true,\"pins\":[{\"host\":\"\(host)\",\"pin\":\"\(pin)\"}]}}}"
            XCTAssertEqual(ok(text).sdk.sslPinning.pins, [Pin(host: host, pin: pin)], host)
        }
    }

    func test_a_url_scheme_is_compared_ignoring_case_and_a_bare_scheme_is_rejected() {
        XCTAssertEqual(
            ok("{\"sdk\":{\"keyProviderUrl\":\"HTTPS://dev.bpcbt.com/keys\"}}").sdk.keyProviderUrl,
            "HTTPS://dev.bpcbt.com/keys"
        )
        XCTAssertEqual(ok("{\"sdk\":{\"keyProviderUrl\":\"HTTPS://h\"}}").sdk.keyProviderUrl, "HTTPS://h")
        XCTAssertEqual(field("{\"sdk\":{\"keyProviderUrl\":\"http\\u017f://h\"}}"), "sdk.keyProviderUrl")
        XCTAssertEqual(field("{\"sdk\":{\"keyProviderUrl\":\"https://\"}}"), "sdk.keyProviderUrl")
        XCTAssertEqual(field("{\"sdk\":{\"keyProviderUrl\":\"ftp://dev.bpcbt.com\"}}"), "sdk.keyProviderUrl")
        XCTAssertEqual(field("{\"sdk\":{\"keyProviderUrl\":\"https://dev.bpcbt.com/a b\"}}"), "sdk.keyProviderUrl")
    }

    func test_the_first_failing_field_in_the_table_order_is_the_one_reported() {
        XCTAssertEqual(field("{\"sdk\":{\"theme\":\"nope\"},\"order\":{\"amount\":\"x\"}}"), "order.amount")
        XCTAssertEqual(field("{\"order\":{\"currency\":\"XXX\"},\"sdk\":{\"theme\":1}}"), "order.currency")
        XCTAssertEqual(field("{\"order\":{\"features\":{}},\"profile\":\"UPPER\"}"), "profile")
        XCTAssertEqual(field("{\"order\":{\"source\":\"nope\"},\"version\":2}"), "version")
    }

    func test_text_that_is_not_a_json_object_fails_at_the_root() {
        for text in ["", "{", "[]", "1", "null", "{\"a\":1,}", "{'a':1}", "{a:1}", "{}x", "\u{FEFF}{}"] {
            XCTAssertEqual(
                QaConfigJson.decode(text),
                .invalid(Problem(field: "$", message: "the document is not a JSON object")),
                text
            )
        }
    }

    func test_sets_are_written_once_and_in_the_canonical_order() {
        let config = ok(
            "{\"sdk\":{\"googlePay\":{\"allowedCardNetworks\":[\"VISA\",\"AMEX\",\"VISA\"]," +
                "\"allowedAuthMethods\":[\"CRYPTOGRAM_3DS\",\"PAN_ONLY\"]}}}"
        )
        XCTAssertEqual(config.sdk.googlePay.allowedCardNetworks, ["AMEX", "VISA"])
        XCTAssertEqual(config.sdk.googlePay.allowedAuthMethods, ["PAN_ONLY", "CRYPTOGRAM_3DS"])
        var unordered = QaConfig()
        unordered.sdk.googlePay.allowedCardNetworks = ["VISA", "AMEX", "VISA"]
        unordered.sdk.applePay.merchantCapabilities = ["THREE_D_SECURE", "CREDIT", "CREDIT"]
        let written = ok(QaConfigJson.encode(unordered))
        XCTAssertEqual(written.sdk.googlePay.allowedCardNetworks, ["AMEX", "VISA"])
        XCTAssertEqual(written.sdk.applePay.merchantCapabilities, ["CREDIT", "THREE_D_SECURE"])
    }

    func test_features_keep_the_read_order_without_duplicates() {
        XCTAssertEqual(ok("{\"order\":{\"features\":[\"B\",\"A\",\"B\"]}}").order.features, ["B", "A"])
    }

    func test_every_field_is_written_including_nulls() throws {
        let written = try XCTUnwrap(JSONValue.parse(QaConfigJson.encode(QaConfig())))
        guard case let .object(root) = written, case let .object(order) = root["order"],
              case let .object(sdk) = root["sdk"], case let .object(googlePay) = sdk["googlePay"]
        else { return XCTFail("not an object") }
        XCTAssertEqual(root["version"], .number("1"))
        XCTAssertEqual(root["profile"], .null)
        XCTAssertEqual(order["clientId"], .null)
        XCTAssertEqual(order["amount"], .number("10000"))
        XCTAssertEqual(sdk["keyProviderUrl"], .null)
        XCTAssertEqual(googlePay["environment"], .null)
        XCTAssertEqual(googlePay["allowedCardNetworks"], .array([.string("MASTERCARD"), .string("VISA")]))
    }

    func test_extra_is_written_as_it_was_read() {
        let config = ok("{\"order\":{\"extra\":{\"a\":{\"b\":[1,2.50,\"x\",null,true]},\"c\":1e400}}}")
        XCTAssertEqual(QaConfigJson.decode(QaConfigJson.encode(config)), .ok(config))
        XCTAssertEqual(config.order.extra["c"], .number("1e400"))
    }

    // Swift's String equality treats U+212A KELVIN SIGN as "K"; Android compares chars, so these are other values.
    func test_text_is_compared_by_scalars_as_on_android() {
        XCTAssertEqual(field("{\"order\":{\"currency\":\"\u{212A}ZT\"}}"), "order.currency")
        XCTAssertEqual(field("{\"sdk\":{\"theme\":\"dar\u{212A}\"}}"), "sdk.theme")
        let extra = ok("{\"order\":{\"extra\":{\"K\":1,\"\u{212A}\":2}}}").order.extra
        XCTAssertEqual(extra.count, 2)
        XCTAssertEqual(QaConfigJson.decode(QaConfigJson.encode(QaConfig(order: OrderConfig(extra: extra)))).map(\.order.extra), .ok(extra))
    }

    // Android's equalsIgnoreCase matches U+212A to "k" and U+017F to "s"; the forbidden keys follow it.
    func test_a_forbidden_key_is_matched_as_java_ignores_case() {
        XCTAssertEqual(field("{\"order\":{\"extra\":{\"TOKEN\":1}}}"), "order.extra")
        XCTAssertEqual(field("{\"order\":{\"extra\":{\"to\u{212A}en\":1}}}"), "order.extra")
        XCTAssertEqual(field("{\"order\":{\"extra\":{\"pa\u{17F}sword\":1}}}"), "order.extra")
        XCTAssertNil(field("{\"order\":{\"extra\":{\"tokens\":1}}}"))
    }

    // Blank and whitespace follow Kotlin's isWhitespace: White_Space without U+0085, with U+001C–U+001F.
    func test_blank_and_whitespace_are_what_android_calls_them() {
        XCTAssertNil(ok("{\"profile\":\"\u{3000}\\u001f\"}").profile)
        XCTAssertEqual(field("{\"profile\":\"\u{85}\"}"), "profile")
        XCTAssertEqual(field("{\"order\":{\"features\":[\"a\\u001cb\"]}}"), "order.features")
        XCTAssertEqual(ok("{\"order\":{\"features\":[\"a\u{85}b\"]}}").order.features, ["a\u{85}b"])
    }

    func test_problem_of_judges_a_typed_value_by_the_same_rules_as_decode() {
        var amount = QaConfig()
        amount.order.amount = -1
        XCTAssertEqual(QaConfigJson.problem(of: amount)?.field, "order.amount")
        var features = QaConfig()
        features.order.features = ["a b"]
        XCTAssertEqual(QaConfigJson.problem(of: features)?.field, "order.features")
        var locale = QaConfig()
        locale.sdk.locale = "xx"
        XCTAssertEqual(QaConfigJson.problem(of: locale)?.field, "sdk.locale")
        XCTAssertNil(QaConfigJson.problem(of: QaConfig()))
    }
}
