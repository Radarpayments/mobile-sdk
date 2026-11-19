@testable import QaApp
import XCTest

final class LaunchCheckTests: XCTestCase {
    private let v1 = StandProfile(
        name: "sandbox", gatewayUrl: "https://dev.bpcbt.com/payment", api: .v1, login: "login-1", password: "password-1"
    )
    private let v2 = StandProfile(name: "sandbox", gatewayUrl: "https://dev.bpcbt.com/payment", api: .v2, apiKey: "key-1")
    private let pinned = SslPinningOptions(enabled: true)

    private func config(
        source: OrderSource = .register,
        type: OrderType = .payment,
        mdOrder: String? = nil,
        sessionId: String? = nil,
        pinning: SslPinningOptions = SslPinningOptions()
    ) -> QaConfig {
        QaConfig(
            profile: "sandbox",
            order: OrderConfig(source: source, type: type, mdOrder: mdOrder, sessionId: sessionId),
            sdk: SdkOptions(sslPinning: pinning)
        )
    }

    private func with(_ profile: StandProfile, _ change: (inout StandProfile) -> Void) -> StandProfile {
        var copy = profile
        change(&copy)
        return copy
    }

    func test_a_v1_profile_with_credentials_and_a_default_config_has_no_problem() {
        XCTAssertNil(launchProblem(config: QaConfig(profile: "sandbox"), profile: v1))
    }

    func test_no_profile_selected_is_a_problem_of_the_profile_field() {
        XCTAssertEqual(
            launchProblem(config: QaConfig(profile: nil), profile: v1),
            Problem(field: "profile", message: "select a stored profile")
        )
    }

    func test_a_profile_that_is_not_stored_is_a_problem_of_the_profile_field() {
        XCTAssertEqual(launchProblem(config: QaConfig(profile: "gone"), profile: nil)?.field, "profile")
    }

    func test_aft_registration_on_a_v2_profile_is_a_problem_of_the_type_field() {
        XCTAssertEqual(
            launchProblem(config: config(type: .aft), profile: v2),
            Problem(field: "order.type", message: "a transfer is registered with API v1 only")
        )
    }

    func test_aft_with_an_existing_session_on_a_v2_profile_is_allowed() {
        XCTAssertNil(launchProblem(config: config(source: .sessionId, type: .aft, sessionId: "s"), profile: v2))
        XCTAssertNil(launchProblem(config: config(source: .mdOrder, type: .aft, mdOrder: "x"), profile: v2))
    }

    func test_registering_on_v1_needs_a_login_and_a_password() {
        XCTAssertEqual(
            launchProblem(config: config(), profile: with(v1) { $0.login = "" }),
            Problem(field: "profile.login", message: "registering an order needs a login and a password")
        )
        XCTAssertEqual(launchProblem(config: config(), profile: with(v1) { $0.password = " " })?.field, "profile.login")
    }

    func test_registering_on_v2_needs_an_api_key() {
        XCTAssertEqual(
            launchProblem(config: config(), profile: with(v2) { $0.apiKey = "" }),
            Problem(field: "profile.apiKey", message: "registering an order needs an API key")
        )
    }

    func test_opening_an_existing_order_needs_no_credentials() {
        let bare = with(v1) {
            $0.login = ""
            $0.password = ""
        }
        XCTAssertNil(launchProblem(config: config(source: .mdOrder, mdOrder: "x"), profile: bare))
        XCTAssertNil(launchProblem(config: config(source: .sessionId, sessionId: "x"), profile: with(v2) { $0.apiKey = "" }))
    }

    func test_an_md_order_source_without_an_md_order_is_a_problem_of_that_field() {
        XCTAssertEqual(
            launchProblem(config: config(source: .mdOrder), profile: v1),
            Problem(field: "order.mdOrder", message: "is required")
        )
    }

    func test_a_session_id_source_without_a_session_id_is_a_problem_of_that_field() {
        XCTAssertEqual(
            launchProblem(config: config(source: .sessionId), profile: v1),
            Problem(field: "order.sessionId", message: "is required")
        )
    }

    func test_pinning_without_pins_is_a_problem_of_the_pins_field() {
        XCTAssertEqual(
            launchProblem(config: config(pinning: pinned), profile: v1),
            Problem(field: "sdk.sslPinning.pins", message: "pinning is enabled without a pin")
        )
    }

    func test_pinning_with_a_pin_has_no_problem() {
        let pins = [Pin(host: "dev.bpcbt.com", pin: "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")]
        XCTAssertNil(launchProblem(config: config(pinning: SslPinningOptions(enabled: true, pins: pins)), profile: v1))
    }

    func test_the_first_failing_check_in_the_contract_order_is_reported() {
        let broken = config(type: .aft, pinning: pinned)
        XCTAssertEqual(launchProblem(config: broken, profile: with(v2) { $0.apiKey = "" })?.field, "order.type")
        XCTAssertEqual(launchProblem(config: broken, profile: nil)?.field, "profile")
        XCTAssertEqual(launchProblem(config: config(source: .mdOrder, pinning: pinned), profile: v1)?.field, "order.mdOrder")
    }

    func test_every_failing_check_is_listed_in_order_and_the_first_is_the_launch_problem() {
        let given = config(source: .mdOrder, pinning: pinned)
        let problems = launchProblems(config: given, profile: v1)

        XCTAssertEqual(problems.map(\.field), ["order.mdOrder", "sdk.sslPinning.pins"])
        XCTAssertEqual(launchProblem(config: given, profile: v1), problems.first)
        XCTAssertEqual(launchProblems(config: config(), profile: v1), [])
    }
}
