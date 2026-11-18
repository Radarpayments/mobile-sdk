@testable import QaApp
import XCTest

final class IntakeTests: XCTestCase {
    private let configJson = #"{"profile":"sandbox","order":{"amount":500}}"#
    private var config: QaConfig {
        QaConfig(profile: "sandbox", order: OrderConfig(amount: 500))
    }

    // Two bytes of input leave one padding character, so the padded and unpadded spellings differ.
    private let paddedConfigJson = #"{"order":{"amount":5}}"#
    private var paddedConfig: QaConfig {
        QaConfig(order: OrderConfig(amount: 5))
    }

    private let profileJson = #"{"name":"sandbox","gatewayUrl":"https://dev.bpcbt.com/payment","api":"v2","apiKey":"key-1"}"#
    private let profile = StandProfile(name: "sandbox", gatewayUrl: "https://dev.bpcbt.com/payment", api: .v2, apiKey: "key-1")

    private func standardBase64(_ bytes: [UInt8]) -> String {
        Data(bytes).base64EncodedString()
    }

    private func b64(_ bytes: [UInt8]) -> String {
        standardBase64(bytes).replacingOccurrences(of: "+", with: "-").replacingOccurrences(of: "/", with: "_")
    }

    private func b64(_ text: String) -> String {
        b64(Array(text.utf8))
    }

    private func field(_ result: [Intake]) -> String? {
        guard result.count == 1, case let .rejected(problem) = result[0] else { return nil }
        return problem.field
    }

    private func arguments(config: String? = nil, profile: String? = nil, launch: Bool = false) -> [String] {
        var arguments = ["QaApp"]
        if let config { arguments += ["-qaConfig", config] }
        if let profile { arguments += ["-qaProfile", profile] }
        if launch { arguments += ["-qaLaunch", "1"] }
        return arguments
    }

    func test_a_config_link_yields_the_config() {
        XCTAssertEqual(intake(fromLink: "payrdrqa://config?data=\(b64(configJson))"), [.config(config)])
    }

    func test_launch_1_adds_a_launch_after_the_config() {
        XCTAssertEqual(intake(fromLink: "payrdrqa://config?data=\(b64(configJson))&launch=1"), [.config(config), .launch])
        XCTAssertEqual(intake(fromLink: "payrdrqa://config?launch=1&data=\(b64(configJson))"), [.config(config), .launch])
    }

    func test_a_launch_value_other_than_1_is_not_a_launch() {
        for query in ["launch=0", "launch=true", "launch=", "launch", "launch=11", "launch=1&launch=1", "LAUNCH=1", "launch=%EF%BC%91"] {
            XCTAssertEqual(intake(fromLink: "payrdrqa://config?data=\(b64(configJson))&\(query)"), [.config(config)], query)
        }
    }

    func test_a_profile_link_asks_for_confirmation() {
        XCTAssertEqual(intake(fromLink: "payrdrqa://profile?data=\(b64(profileJson))"), [.profile(profile, needsConfirmation: true)])
    }

    func test_a_profile_link_does_not_launch() {
        XCTAssertEqual(
            intake(fromLink: "payrdrqa://profile?data=\(b64(profileJson))&launch=1"),
            [.profile(profile, needsConfirmation: true)]
        )
    }

    func test_padded_and_unpadded_base64url_are_both_read() {
        let padded = b64(paddedConfigJson)
        XCTAssertTrue(padded.hasSuffix("="))
        XCTAssertEqual(intake(fromLink: "payrdrqa://config?data=\(padded)"), [.config(paddedConfig)])
        let unpadded = padded.replacingOccurrences(of: "=", with: "")
        XCTAssertEqual(intake(fromLink: "payrdrqa://config?data=\(unpadded)"), [.config(paddedConfig)])
    }

    func test_a_percent_encoded_data_value_is_read() {
        let padded = b64(paddedConfigJson)
        for escape in ["%3D", "%3d"] {
            let link = "payrdrqa://config?data=\(padded.replacingOccurrences(of: "=", with: escape))"
            XCTAssertEqual(intake(fromLink: link), [.config(paddedConfig)])
        }
    }

    func test_the_url_safe_characters_of_base64url_are_read() {
        // The text is chosen so that its base64 has both a '+' and a '/' that base64url spells '-' and '_'.
        let json = #"{"order":{"clientId":">>>???"}}"#
        let encoded = b64(json)
        XCTAssertTrue(encoded.contains("-") && encoded.contains("_"))
        XCTAssertEqual(
            intake(fromLink: "payrdrqa://config?data=\(encoded)"),
            [.config(QaConfig(order: OrderConfig(clientId: ">>>???")))]
        )
    }

    func test_standard_base64_is_rejected_because_the_contract_names_base64url_only() {
        let standard = standardBase64(Array(#"{"order":{"clientId":">>>???"}}"#.utf8))
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=\(standard)")), "data")
        let escaped = standard.replacingOccurrences(of: "+", with: "%2B").replacingOccurrences(of: "/", with: "%2F")
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=\(escaped)")), "data")
    }

    func test_wrong_padding_is_rejected() {
        let unpadded = b64(paddedConfigJson).replacingOccurrences(of: "=", with: "")
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=\(unpadded)===")), "data")
        let inside = "\(unpadded.dropLast())=\(unpadded.last!)"
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=\(inside)")), "data")
    }

    func test_an_invalid_config_link_is_rejected_with_its_field_and_does_not_launch() {
        let link = "payrdrqa://config?data=\(b64(#"{"order":{"amount":-1}}"#))&launch=1"
        XCTAssertEqual(field(intake(fromLink: link)), "order.amount")
    }

    func test_an_invalid_profile_link_is_rejected_with_the_decoders_own_problem() {
        let json = #"{"name":"Bad Name","gatewayUrl":"https://dev.bpcbt.com/payment","password":"password-1"}"#
        let result = intake(fromLink: "payrdrqa://profile?data=\(b64(json))")
        XCTAssertEqual(field(result), "name")
        XCTAssertFalse(String(describing: result).contains("password-1"))
    }

    func test_a_rejection_does_not_echo_the_payload() {
        let json = #"{"order":{"extra":{"password":"password-1"}}}"#
        let result = intake(fromLink: "payrdrqa://config?data=\(b64(json))")
        XCTAssertEqual(field(result), "order.extra")
        XCTAssertFalse(String(describing: result).contains("password-1"))
        XCTAssertFalse(String(describing: result).contains(b64(json)))
        let broken = intake(fromLink: "payrdrqa://config?data=\(b64(json))!")
        XCTAssertEqual(field(broken), "data")
        XCTAssertFalse(String(describing: broken).contains(b64(json)))
    }

    func test_an_accepted_profile_does_not_print_its_credentials() {
        let json = #"{"name":"sandbox","gatewayUrl":"https://dev.bpcbt.com/payment","password":"password-1","apiKey":"key-1"}"#
        let result = intake(fromText: json)
        for text in [String(describing: result), String(reflecting: result)] {
            XCTAssertFalse(text.contains("password-1"))
            XCTAssertFalse(text.contains("key-1"))
        }
    }

    func test_a_link_of_another_host_is_rejected() {
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://settings?data=\(b64(configJson))")), "link")
    }

    func test_a_link_of_another_shape_is_rejected() {
        for link in [
            "payrdrqa://config/extra?data=\(b64(configJson))",
            "payrdrqa://user@config?data=\(b64(configJson))",
            "payrdrqa://config:1?data=\(b64(configJson))",
            "payrdrqa://config?data=\(b64(configJson))#top",
            "https://config?data=\(b64(configJson))",
            "payrdrqa:config?data=\(b64(configJson))",
            "payrdrqa://",
            "",
        ] {
            XCTAssertEqual(field(intake(fromLink: link)), "link", link)
        }
    }

    func test_a_link_without_data_is_rejected() {
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config")), "data")
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?launch=1")), "data")
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://profile?")), "data")
    }

    func test_an_empty_data_is_rejected() {
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=")), "data")
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data")), "data")
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=&launch=1")), "data")
    }

    func test_a_repeated_data_is_rejected() {
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=\(b64(configJson))&data=\(b64(configJson))")), "data")
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=\(b64(configJson))&data=")), "data")
    }

    func test_data_that_is_not_base64_is_rejected() {
        for data in ["not base64", #"{"a":1}"#, "QUJD!", "%ZZ", "%4", "QUJD%"] {
            XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=\(data)")), "data", data)
        }
    }

    func test_data_whose_bytes_are_not_utf_8_is_rejected() {
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=\(b64([0x7B, 0xC3, 0x28, 0x7D]))")), "data")
    }

    func test_data_with_a_surrogate_is_rejected() {
        // ED A0 80 is U+D800 spelled in UTF-8, which strict UTF-8 forbids.
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=\(b64([0x7B, 0xED, 0xA0, 0x80, 0x7D]))")), "data")
    }

    func test_data_that_is_not_a_json_object_is_rejected_at_the_root() {
        for text in ["[]", "\"text\"", "1", "{"] {
            XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=\(b64(text))")), "$", text)
        }
    }

    // As on Android: a profile link's text is trimmed, a config link's is decoded as it is (JSON allows the whitespace).
    func test_the_decoded_text_of_a_profile_link_is_trimmed_and_that_of_a_config_link_is_not() {
        XCTAssertEqual(intake(fromLink: "payrdrqa://config?data=\(b64(" \n\(configJson)\r\n"))"), [.config(config)])
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=\(b64("\u{FEFF}\(configJson)"))")), "$")
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=\(b64(" \n"))")), "$")
        XCTAssertEqual(
            intake(fromLink: "payrdrqa://profile?data=\(b64("\u{FEFF}\t\(profileJson) \n"))"),
            [.profile(profile, needsConfirmation: true)]
        )
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://profile?data=\(b64(" \n"))")), "data")
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=\(b64("\u{85}\(configJson)"))")), "$")
    }

    func test_a_link_with_surrounding_whitespace_is_read() {
        XCTAssertEqual(
            intake(fromLink: "\n  payrdrqa://config?data=\(b64(configJson))&launch=1 \r\n"),
            [.config(config), .launch]
        )
    }

    func test_arguments_apply_the_profile_before_the_config_and_launch_last() {
        XCTAssertEqual(
            intake(fromArguments: ["QaApp", "-qaConfig", configJson, "-qaProfile", profileJson, "-qaLaunch", "1"]),
            [.profile(profile, needsConfirmation: false), .config(config), .launch]
        )
    }

    func test_qaLaunch_alone_launches_the_stored_config() {
        XCTAssertEqual(intake(fromArguments: arguments(launch: true)), [.launch])
    }

    func test_no_arguments_yield_nothing() {
        XCTAssertEqual(intake(fromArguments: ["QaApp"]), [])
        XCTAssertEqual(intake(fromArguments: []), [])
    }

    func test_a_qaLaunch_other_than_1_does_not_launch() {
        for value in ["0", "YES", "true", "", "11"] {
            XCTAssertEqual(intake(fromArguments: ["QaApp", "-qaLaunch", value]), [], value)
        }
        XCTAssertEqual(intake(fromArguments: ["QaApp", "-qaLaunch", " 1\n"]), [.launch])
    }

    func test_a_flag_without_a_value_is_ignored() {
        XCTAssertEqual(intake(fromArguments: ["QaApp", "-qaConfig", configJson, "-qaLaunch"]), [.config(config)])
        XCTAssertEqual(intake(fromArguments: ["QaApp", "-qaLaunch", "1", "-qaProfile"]), [.launch])
        XCTAssertEqual(intake(fromArguments: ["QaApp", "-qaConfig"]), [])
    }

    func test_unrelated_arguments_are_ignored() {
        XCTAssertEqual(
            intake(fromArguments: ["QaApp", "-NSDocumentRevisionsDebugMode", "YES", "-qaConfig", configJson, "-AppleLanguages", "(en)"]),
            [.config(config)]
        )
        XCTAssertEqual(intake(fromArguments: ["QaApp", "qaConfig", configJson, "--qaLaunch", "1"]), [])
    }

    func test_the_value_of_a_flag_is_the_next_argument_whatever_it_is() {
        XCTAssertEqual(field(intake(fromArguments: ["QaApp", "-qaConfig", "-qaLaunch", "1"])), "$")
    }

    func test_a_repeated_flag_takes_its_last_value() {
        XCTAssertEqual(
            intake(fromArguments: ["QaApp", "-qaConfig", "not json", "-qaConfig", configJson]),
            [.config(config)]
        )
    }

    func test_an_invalid_argument_rejects_every_argument_and_does_not_launch() {
        XCTAssertEqual(
            field(intake(fromArguments: arguments(config: #"{"order":{"amount":-1}}"#, profile: profileJson, launch: true))),
            "order.amount"
        )
        XCTAssertEqual(field(intake(fromArguments: arguments(config: configJson, profile: #"{"name":"sandbox"}"#, launch: true))), "gatewayUrl")
        XCTAssertEqual(field(intake(fromArguments: arguments(config: "not json", launch: true))), "$")
    }

    func test_when_both_documents_are_rejected_the_profiles_problem_is_reported() {
        let result = intake(fromArguments: arguments(config: #"{"order":{"amount":-1}}"#, profile: #"{"name":"sandbox"}"#, launch: true))
        XCTAssertEqual(field(result), "gatewayUrl")
    }

    func test_an_empty_argument_is_rejected_as_missing_data() {
        XCTAssertEqual(field(intake(fromArguments: arguments(config: "", launch: true))), "data")
        XCTAssertEqual(field(intake(fromArguments: arguments(profile: "  "))), "data")
    }

    func test_text_that_is_not_strict_json_is_rejected_at_the_root_on_every_path() {
        for text in [#"{"order":{"amount":01}}"#, #"{"order":{"amount":+1}}"#, #"{"order":{"extra":{"a":abc}}}"#, "{\"clientId\":\"a\tb\"}"] {
            XCTAssertEqual(field(intake(fromText: text)), "$", text)
            XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=\(b64(text))")), "$", text)
            XCTAssertEqual(field(intake(fromArguments: arguments(config: text, launch: true))), "$", text)
        }
        let profile = #"{"name":"sandbox","gatewayUrl":"https://dev.bpcbt.com/payment","version":01}"#
        XCTAssertEqual(field(intake(fromText: profile)), "$")
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://profile?data=\(b64(profile))")), "$")
        XCTAssertEqual(field(intake(fromArguments: arguments(profile: profile))), "$")
    }

    func test_pasted_json_with_a_gateway_url_is_a_profile_to_confirm() {
        XCTAssertEqual(intake(fromText: profileJson), [.profile(profile, needsConfirmation: true)])
    }

    func test_pasted_json_without_one_is_a_config() {
        XCTAssertEqual(intake(fromText: configJson), [.config(config)])
    }

    func test_pasted_text_is_trimmed() {
        XCTAssertEqual(intake(fromText: "\n  \(configJson)\n"), [.config(config)])
        XCTAssertEqual(intake(fromText: "\r\n\t\(profileJson) \n"), [.profile(profile, needsConfirmation: true)])
    }

    func test_a_pasted_link_is_read_as_a_link() {
        XCTAssertEqual(intake(fromText: "  payrdrqa://config?data=\(b64(configJson))&launch=1\n"), [.config(config), .launch])
        XCTAssertEqual(intake(fromText: "payrdrqa://profile?data=\(b64(profileJson))"), [.profile(profile, needsConfirmation: true)])
        XCTAssertEqual(field(intake(fromText: "payrdrqa://other?data=\(b64(configJson))")), "link")
    }

    func test_pasted_text_that_is_neither_is_rejected() {
        for text in ["", "   ", "hello", "[1,2]", "https://dev.bpcbt.com/payment"] {
            XCTAssertEqual(field(intake(fromText: text)), "$", text)
        }
    }

    func test_a_pasted_profile_that_is_invalid_is_rejected_with_its_field() {
        XCTAssertEqual(field(intake(fromText: #"{"name":"sandbox","gatewayUrl":"ftp://dev.bpcbt.com"}"#)), "gatewayUrl")
    }

    // The root object is level 1, so depth n is n objects nested in each other.
    private func nested(_ depth: Int) -> String {
        String(repeating: "{\"a\":", count: depth - 1) + "{}" + String(repeating: "}", count: depth - 1)
    }

    // 'é' is two bytes of UTF-8 and one character, so the byte limit is not mistaken for a character limit.
    private func sized(_ bytes: Int) -> String {
        let filler = bytes - 8
        return "{\"a\":\"" + String(repeating: "é", count: filler / 2) + String(repeating: "x", count: filler % 2) + "\"}"
    }

    func test_a_link_or_pasted_text_keeps_the_depth_limit() {
        XCTAssertEqual(intake(fromLink: "payrdrqa://config?data=\(b64(nested(32)))"), [.config(QaConfig())])
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=\(b64(nested(33)))&launch=1")), "$")
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://profile?data=\(b64(nested(33)))")), "$")
        XCTAssertEqual(intake(fromText: nested(32)), [.config(QaConfig())])
        XCTAssertEqual(field(intake(fromText: nested(33))), "$")
        XCTAssertEqual(field(intake(fromText: nested(100_000))), "$")
        XCTAssertEqual(field(intake(fromArguments: arguments(config: nested(100_000), launch: true))), "$")
    }

    func test_a_link_or_pasted_text_keeps_the_size_limit_in_bytes() {
        XCTAssertEqual(intake(fromLink: "payrdrqa://config?data=\(b64(sized(65536)))"), [.config(QaConfig())])
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=\(b64(sized(65537)))")), "$")
        XCTAssertEqual(intake(fromText: sized(65536)), [.config(QaConfig())])
        XCTAssertEqual(field(intake(fromText: sized(65537))), "$")
        XCTAssertEqual(field(intake(fromArguments: arguments(config: sized(65537)))), "$")
    }

    func test_the_limits_are_measured_after_trimming() {
        XCTAssertEqual(intake(fromText: "\u{FEFF}\n\(sized(65536)) \n"), [.config(QaConfig())])
        XCTAssertEqual(intake(fromArguments: arguments(config: "  \(sized(65536))\n")), [.config(QaConfig())])
        // A config link's text is not trimmed, so its whitespace counts.
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=\(b64(" \(sized(65536))\n"))")), "$")
    }

    func test_data_longer_than_base64url_of_the_largest_document_is_refused_before_it_is_decoded() {
        // 87388 characters would decode to 65541 bytes; the largest document, 65536 bytes, takes 87384.
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=\(String(repeating: "A", count: 87388))")), "data")
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=\(String(repeating: "%41", count: 87388))")), "data")
        XCTAssertEqual(field(intake(fromText: "payrdrqa://config?data=\(String(repeating: "A", count: 1_000_000))")), "data")
    }

    func test_data_of_exactly_87384_characters_is_decoded() {
        let encoded = b64(sized(65536))
        XCTAssertEqual(encoded.count, 87384)
        let escaped = encoded.replacingOccurrences(of: "A", with: "%41")
        XCTAssertEqual(intake(fromLink: "payrdrqa://config?data=\(escaped)"), [.config(QaConfig())])
    }

    func test_the_scheme_is_read_ignoring_ascii_case_and_the_host_is_exact() {
        XCTAssertEqual(intake(fromLink: "PAYRDRQA://config?data=\(b64(configJson))&launch=1"), [.config(config), .launch])
        XCTAssertEqual(intake(fromText: "PayRdrQa://config?data=\(b64(configJson))"), [.config(config)])
        XCTAssertEqual(intake(fromText: " PAYRDRQA://profile?data=\(b64(profileJson))"), [.profile(profile, needsConfirmation: true)])
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://CONFIG?data=\(b64(configJson))")), "link")
        XCTAssertEqual(field(intake(fromText: "PAYRDRQA://Profile?data=\(b64(profileJson))")), "link")
        // U+212A KELVIN SIGN folds to 'k' in Unicode, not in ASCII.
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://\u{212A}onfig?data=\(b64(configJson))")), "link")
    }

    func test_an_authority_other_than_the_bare_host_is_rejected() {
        for link in [
            "payrdrqa://config:?data=\(b64(configJson))",
            "payrdrqa:///config?data=\(b64(configJson))",
            "payrdrqa://config/?data=\(b64(configJson))",
            "payrdrqa:config?data=\(b64(configJson))",
            "payrdrqa://%63onfig?data=\(b64(configJson))",
            "payrdrqa://config.?data=\(b64(configJson))",
        ] {
            XCTAssertEqual(field(intake(fromLink: link)), "link", link)
        }
    }

    func test_a_host_that_only_looks_the_same_is_rejected() {
        // "config" with a combining mark after its last letter, which Swift's String would not tell from "config" plus it.
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://confi\u{0067}\u{0301}?data=\(b64(configJson))")), "link")
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://ｃonfig?data=\(b64(configJson))")), "link")
    }

    func test_trimming_removes_ascii_space_tab_cr_lf_and_one_leading_byte_order_mark() {
        XCTAssertEqual(intake(fromText: "\u{FEFF}\(configJson)"), [.config(config)])
        XCTAssertEqual(intake(fromText: "\u{FEFF} \t\r\n\(configJson) \t\r\n"), [.config(config)])
        XCTAssertEqual(intake(fromLink: "\u{FEFF}payrdrqa://config?data=\(b64(configJson))\n"), [.config(config)])
        XCTAssertEqual(
            intake(fromArguments: arguments(config: "\u{FEFF}\(configJson)\n", profile: "\u{FEFF}\(profileJson)")),
            [.profile(profile, needsConfirmation: false), .config(config)]
        )
        XCTAssertEqual(field(intake(fromArguments: arguments(config: "\u{FEFF} \n"))), "data")
    }

    func test_other_whitespace_and_a_second_byte_order_mark_are_not_trimmed() {
        XCTAssertEqual(field(intake(fromText: "\u{85}\(configJson)")), "$")
        XCTAssertEqual(field(intake(fromText: "\(configJson)\u{85}")), "$")
        XCTAssertEqual(field(intake(fromText: "\u{0B}\(configJson)")), "$")
        XCTAssertEqual(field(intake(fromText: "\u{A0}\(configJson)")), "$")
        XCTAssertEqual(field(intake(fromText: "\u{FEFF}\u{FEFF}\(configJson)")), "$")
        XCTAssertEqual(field(intake(fromText: "\(configJson)\u{FEFF}")), "$")
        XCTAssertEqual(field(intake(fromText: " \u{FEFF}\(configJson)")), "$")
        XCTAssertEqual(field(intake(fromText: "\u{85}payrdrqa://config?data=\(b64(configJson))")), "$")
        XCTAssertEqual(field(intake(fromLink: "\u{85}payrdrqa://config?data=\(b64(configJson))")), "link")
        XCTAssertEqual(field(intake(fromArguments: arguments(config: "\u{85}\(configJson)"))), "$")
        XCTAssertEqual(field(intake(fromArguments: arguments(config: "\u{85}"))), "$")
    }

    func test_a_crlf_at_the_end_is_trimmed_although_swift_reads_it_as_one_character() {
        XCTAssertEqual(intake(fromText: "\(configJson)\r\n"), [.config(config)])
        XCTAssertEqual(intake(fromLink: "payrdrqa://config?data=\(b64(configJson))\r\n"), [.config(config)])
    }

    func test_escapes_that_decode_to_nothing_usable_are_rejected() {
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=%00")), "data")
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=\(b64(configJson))%00")), "data")
        // C0 AE is an overlong spelling of '.', which a strict UTF-8 decoder refuses.
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=%C0%AE")), "data")
        XCTAssertEqual(intake(fromLink: "payrdrqa://config?data=\(b64(configJson))&launch=%C0%AE"), [.config(config)])
        XCTAssertEqual(intake(fromLink: "payrdrqa://config?%C0%AE=1&data=\(b64(configJson))"), [.config(config)])
        XCTAssertEqual(intake(fromLink: "payrdrqa://config?data=\(b64(configJson))&launch=%31"), [.config(config), .launch])
    }

    func test_an_escape_takes_ascii_hex_digits_only() {
        // U+0664 and U+0661 are Arabic-Indic four and one, which a Unicode digit reader would turn into %41, 'A'.
        let data = b64(configJson)
        let link = "payrdrqa://config?data=\(data.dropLast())%\u{0664}\u{0661}"
        XCTAssertEqual(field(intake(fromLink: link)), "data")
        XCTAssertEqual(intake(fromLink: "payrdrqa://config?data=\(data)&launch=%\u{FF13}\u{FF11}"), [.config(config)])
    }

    func test_an_overlong_utf_8_sequence_inside_data_is_rejected() {
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=\(b64([0x7B, 0xC0, 0xAE, 0x7D]))")), "data")
    }

    func test_base64url_of_the_wrong_length_with_pad_bits_or_a_line_break_is_rejected() {
        let unpadded = b64(configJson).replacingOccurrences(of: "=", with: "")
        let oneLeft = unpadded.count % 4 == 0 ? unpadded + "A" : String(unpadded.dropLast(unpadded.count % 4 - 1))
        XCTAssertEqual(oneLeft.count % 4, 1)
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=\(oneLeft)")), "data")
        // `{"a":1}` is eyJhIjoxfQ==; 'R' sets the pad bits that 'Q' leaves zero.
        XCTAssertEqual(intake(fromLink: "payrdrqa://config?data=eyJhIjoxfQ"), [.config(QaConfig())])
        XCTAssertEqual(intake(fromLink: "payrdrqa://config?data=eyJhIjoxfQ=="), [.config(QaConfig())])
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=eyJhIjoxfR")), "data")
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=eyJhIjoxfR==")), "data")
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=eyJhIjoxfQ=")), "data")
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=eyJhIjoxfQ===")), "data")
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=eyJhIjox====")), "data")
        let half = unpadded.count / 2
        for separator in ["\n", "%0A", "%20"] {
            let link = "payrdrqa://config?data=\(unpadded.prefix(half))\(separator)\(unpadded.dropFirst(half))"
            XCTAssertEqual(field(intake(fromLink: link)), "data", separator)
        }
    }

    func test_only_an_ampersand_separates_parameters_and_empty_or_nameless_pairs_are_skipped() {
        XCTAssertEqual(field(intake(fromLink: "payrdrqa://config?data=\(b64(configJson));launch=1")), "data")
        XCTAssertEqual(intake(fromLink: "payrdrqa://config?data=\(b64(configJson))&a=1;launch=1"), [.config(config)])
        XCTAssertEqual(intake(fromLink: "payrdrqa://config?=x&data=\(b64(configJson))&launch=1"), [.config(config), .launch])
        XCTAssertEqual(intake(fromLink: "payrdrqa://config?&&data=\(b64(configJson))&&launch=1&&"), [.config(config), .launch])
        XCTAssertEqual(intake(fromLink: "payrdrqa://config?data=\(b64(configJson))&other=%ZZ&x"), [.config(config)])
    }

    func test_pasted_bare_base64_is_not_json() {
        XCTAssertEqual(field(intake(fromText: b64(configJson))), "$")
    }

    func test_a_link_url_cannot_parse_is_read_from_its_text() {
        // Foundation's URL refuses or rewrites some of these, so the app reads the link from its text.
        XCTAssertEqual(intake(fromLink: "payrdrqa://config?data=\(b64(configJson))&note=a b|c"), [.config(config)])
        XCTAssertEqual(intake(fromLink: "payrdrqa://config?data=\(b64(configJson))&note=%"), [.config(config)])
    }

    func test_a_scanned_text_is_not_printed() {
        let scan = QrScan.text("payrdrqa://profile?data=secret")
        for text in [String(describing: scan), String(reflecting: scan), "\(scan)"] {
            XCTAssertFalse(text.contains("secret"), text)
        }
        XCTAssertNotEqual(scan, .cancelled)
        XCTAssertNotEqual(QrScan.cancelled, .unavailable)
    }
}
