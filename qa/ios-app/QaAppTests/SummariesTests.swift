@testable import QaApp
import XCTest

final class SummariesTests: XCTestCase {
    private let pin = Pin(host: "dev.bpcbt.com", pin: "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
    private let utc = TimeZone(identifier: "UTC")!

    func test_the_order_row_names_the_source_and_what_it_starts_from() {
        XCTAssertEqual(orderSummary(OrderConfig()), "register · payment")
        XCTAssertEqual(orderSummary(OrderConfig(type: .aft)), "register · aft")
        XCTAssertEqual(orderSummary(OrderConfig(source: .mdOrder, mdOrder: "01ea8a09")), "mdOrder · 01ea8a09")
        XCTAssertEqual(orderSummary(OrderConfig(source: .mdOrder)), "mdOrder · empty")
        XCTAssertEqual(orderSummary(OrderConfig(source: .sessionId, sessionId: "ps_1")), "sessionId · ps_1")
    }

    func test_the_amount_row_reads_in_major_units_with_the_currency() {
        XCTAssertEqual(amountSummary(OrderConfig()), "100.00 EUR")
        XCTAssertEqual(amountSummary(OrderConfig(amount: 5, currency: "USD")), "0.05 USD")
    }

    func test_features_extra_and_the_client_id_read_none_when_empty() {
        XCTAssertEqual(featuresSummary(OrderConfig()), "none")
        XCTAssertEqual(featuresSummary(OrderConfig(features: ["VERIFY", "FORCE_TDS"])), "VERIFY, FORCE_TDS")
        XCTAssertEqual(extraSummary(OrderConfig()), "none")
        XCTAssertEqual(extraSummary(OrderConfig(extra: ["description": .string("QA")])), "description")
        XCTAssertEqual(clientIdSummary(OrderConfig()), "none")
        XCTAssertEqual(clientIdSummary(OrderConfig(clientId: "c1")), "c1")
    }

    func test_the_behaviour_row_lists_the_locale_and_the_switches_that_are_on() {
        XCTAssertEqual(behaviourSummary(SdkOptions()), "system locale")
        XCTAssertEqual(
            behaviourSummary(SdkOptions(locale: "en", showResultScreen: true, scanner: true, keyProviderUrl: "https://k")),
            "en · result · scanner · key provider"
        )
    }

    func test_apple_pay_and_pinning_read_off_until_switched_on() {
        XCTAssertEqual(applePaySummary(SdkOptions()), "off")
        XCTAssertEqual(applePaySummary(SdkOptions(applePay: ApplePayOptions(enabled: true))), "on")
        XCTAssertEqual(sslPinningSummary(SdkOptions()), "off")
        XCTAssertEqual(sslPinningSummary(SdkOptions(sslPinning: SslPinningOptions(enabled: true))), "0 pins")
        XCTAssertEqual(sslPinningSummary(SdkOptions(sslPinning: SslPinningOptions(enabled: true, pins: [pin]))), "1 pin")
        XCTAssertEqual(sslPinningSummary(SdkOptions(sslPinning: SslPinningOptions(enabled: true, pins: [pin, pin]))), "2 pins")
    }

    func test_a_stand_reads_as_its_host_and_api() {
        XCTAssertEqual(
            standSummary(StandProfile(name: "sandbox", gatewayUrl: "https://dev.bpcbt.com/payment", api: .v2)),
            "dev.bpcbt.com · API v2"
        )
    }

    func test_the_last_run_reads_as_its_minute_and_status() {
        let time = Date(timeIntervalSince1970: 10 * 3600 + 50 * 60 + 30)
        XCTAssertEqual(lastRunSummary(time, .known(orderStatus: 2, actionCode: 0, actionCodeDescription: nil), timeZone: utc), "10:50 · status 2 · action 0")
        XCTAssertEqual(lastRunSummary(time, .notQueried, timeZone: utc), "10:50 · status not queried")
        XCTAssertEqual(lastRunSummary(time, .known(orderStatus: nil, actionCode: 0, actionCodeDescription: nil), timeZone: utc), "10:50 · status - · action 0")
        XCTAssertEqual(lastRunSummary(time, .failed(httpStatus: 500, message: "oops"), timeZone: utc), "10:50 · status error")
    }

    func test_the_banner_details_the_status() {
        XCTAssertEqual(
            statusDetail(.known(orderStatus: 2, actionCode: 0, actionCodeDescription: "Success")),
            ["orderStatus 2 · actionCode 0", "Success"]
        )
        XCTAssertEqual(statusDetail(.notQueried), ["status not queried"])
        XCTAssertEqual(statusDetail(.failed(httpStatus: nil, message: "timeout")), ["status error: timeout"])
    }

    func test_a_launched_order_reads_as_its_type_and_amount() {
        XCTAssertEqual(launchedOrderSummary(OrderConfig()), "payment · 100.00 EUR")
        XCTAssertEqual(launchedOrderSummary(OrderConfig(source: .sessionId, sessionId: "ps_1")), "sessionId · ps_1")
    }

    func test_outcomes_have_a_title_and_a_tone() {
        XCTAssertEqual(outcomeTitle(.registrationFailed), "Registration failed")
        XCTAssertEqual(outcomeTone(.completed), .ok)
        XCTAssertEqual(outcomeTone(.sdkError), .bad)
        XCTAssertEqual(outcomeTone(.canceled), .neutral)
    }

    func test_a_problem_is_shown_on_the_row_whose_screen_fixes_it() {
        XCTAssertEqual(homeRow("profile"), .stand)
        XCTAssertEqual(homeRow("profile.apiKey"), .stand)
        XCTAssertEqual(homeRow("order.amount"), .amount)
        XCTAssertEqual(homeRow("order.currency"), .amount)
        XCTAssertEqual(homeRow("order.features"), .features)
        XCTAssertEqual(homeRow("order.mdOrder"), .order)
        XCTAssertEqual(homeRow("sdk.theme"), .theme)
        XCTAssertEqual(homeRow("sdk.applePay.merchantId"), .wallet)
        XCTAssertEqual(homeRow("sdk.sslPinning.pins"), .sslPinning)
        XCTAssertEqual(homeRow("sdk.locale"), .behaviour)
    }

    func test_a_row_problem_gets_its_fields_name_when_the_message_needs_a_subject() {
        XCTAssertEqual(rowProblem(Problem(field: "order.mdOrder", message: "is required")), "mdOrder is required")
        XCTAssertEqual(rowProblem(Problem(field: "profile", message: "select a stored profile")), "select a stored profile")
    }

    func test_the_order_screen_shows_the_fields_its_source_uses() {
        XCTAssertEqual(
            orderFields(.register),
            [Tags.orderType, Tags.orderAmount, Tags.orderCurrency, Tags.orderFeatures, Tags.orderClientId, Tags.orderExtra]
        )
        XCTAssertEqual(orderFields(.mdOrder), [Tags.orderMdOrder])
        XCTAssertEqual(orderFields(.sessionId), [Tags.orderSessionId])
    }

    func test_durations_keep_a_decimal_under_ten_seconds() {
        XCTAssertEqual(durationText(4321), "4.3 s")
        XCTAssertEqual(durationText(0), "0.0 s")
        XCTAssertEqual(durationText(20400), "20 s")
    }

    func test_a_duration_that_rounds_to_ten_seconds_has_no_decimal() {
        XCTAssertEqual(durationText(9949), "9.9 s")
        XCTAssertEqual(durationText(9950), "10 s")
        XCTAssertEqual(durationText(9999), "10 s")
        XCTAssertEqual(durationText(10000), "10 s")
    }
}
