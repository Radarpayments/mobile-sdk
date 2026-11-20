@testable import QaApp
import XCTest

final class StandProfileTests: XCTestCase {
    private let profile = StandProfile(
        name: "s", gatewayUrl: "https://dev.bpcbt.com/payment", login: "login-1", password: "password-1", apiKey: "key-1"
    )

    func test_the_description_hides_the_credentials() {
        for text in [String(describing: profile), String(reflecting: profile), "\(profile)"] {
            XCTAssertFalse(text.contains("login-1"))
            XCTAssertFalse(text.contains("password-1"))
            XCTAssertFalse(text.contains("key-1"))
            XCTAssertEqual(text, "StandProfile(name=s, gatewayUrl=https://dev.bpcbt.com/payment, api=V1)")
        }
    }

    func test_the_encoded_profile_carries_them() {
        let encoded = StandProfileJson.encode(profile)
        XCTAssertTrue(encoded.contains("password-1"))
        XCTAssertEqual(StandProfileJson.decode(encoded), .ok(profile))
    }

    func test_the_encoded_profile_carries_version_1() throws {
        let written = try XCTUnwrap(JSONValue.parse(StandProfileJson.encode(profile)))
        guard case let .object(fields) = written else { return XCTFail("not an object") }
        XCTAssertEqual(fields["version"], .number("1"))
        XCTAssertEqual(fields["api"], .string("v1"))
        XCTAssertEqual(fields.count, 7)
    }

    func test_a_problem_about_a_profile_never_contains_its_credentials() {
        var bad = profile
        bad.gatewayUrl = "not a url"
        let problem = StandProfileJson.problem(of: bad)
        XCTAssertEqual(problem, Problem(field: "gatewayUrl", message: "must be an absolute http or https URL"))
        XCTAssertFalse(String(describing: problem).contains("password-1"))
        XCTAssertFalse(String(describing: problem).contains("key-1"))
    }

    func test_a_profile_of_the_other_api_version_round_trips() {
        var v2 = profile
        v2.api = .v2
        XCTAssertEqual(StandProfileJson.decode(StandProfileJson.encode(v2)), .ok(v2))
    }

    func test_a_missing_name_or_gateway_is_required() {
        XCTAssertEqual(
            StandProfileJson.decode("{\"gatewayUrl\":\"https://h\"}"),
            .invalid(Problem(field: "name", message: "is required"))
        )
        XCTAssertEqual(
            StandProfileJson.decode("{\"name\":\"s\"}"),
            .invalid(Problem(field: "gatewayUrl", message: "is required"))
        )
        XCTAssertEqual(
            StandProfileJson.decode("{\"name\":\"S\",\"gatewayUrl\":\"https://h\"}"),
            .invalid(Problem(field: "name", message: "must be 1 to 32 characters of a-z, 0-9, _ and -"))
        )
        XCTAssertEqual(
            StandProfileJson.decode("{\"name\":\"s\",\"gatewayUrl\":\"https://h\",\"api\":\"v3\"}"),
            .invalid(Problem(field: "api", message: "must be one of v1, v2"))
        )
    }
}
