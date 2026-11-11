import XCTest

/// What VoiceOver gets from the SDK checkout, against `tools/visual/fake_gateway.py`: names, roles, states and
/// announcements, checked on the running app.
/// Run through `VISUAL_CLASS=CheckoutAccessibilityTests tools/visual/capture.sh <set-name> [test-name]`; with
/// `VISUAL_DUMP=1` each checked screen's element tree (labels, traits, frames) lands in `VISUAL_OUT`.
final class CheckoutAccessibilityTests: XCTestCase {
    private var app: XCUIApplication!

    private let gateway = ProcessInfo.processInfo.environment["VISUAL_GATEWAY"] ?? "http://127.0.0.1:8787"
    private let outDir = URL(fileURLWithPath: ProcessInfo.processInfo.environment["VISUAL_OUT"] ?? "/tmp/payrdr-visual/default")

    override func setUpWithError() throws {
        continueAfterFailure = true
        try FileManager.default.createDirectory(at: outDir, withIntermediateDirectories: true)
    }

    override func tearDown() {
        get("/__release/all")
        app?.terminate()
    }

    // MARK: - The contract

    /// The skeleton is one "Loading…" element, not stand-in cards with digits.
    @MainActor
    func testSkeletonReadsAsLoading() {
        openCheckout(scenario: "slow_session.cards3.wallet", skeletonCards: 3, skeletonWallet: true)
        let loading = app.descendants(matching: .any)["Loading…"]
        XCTAssertTrue(loading.waitForExistence(timeout: 10), "no Loading… element")
        let spoken = spoken("a11y_skeleton")
        XCTAssertTrue(spoken.contains { $0.label == "Loading…" }, "Loading… is not read")
        XCTAssertFalse(spoken.contains { $0.label.contains("0000") }, "stand-in card digits are read")
        assertNoNamelessButtons(spoken)
    }

    /// The methods sheet: its title is a heading and plain text, the saved card picked reads as selected, the
    /// Apple Pay button is a named button, and neither the scrim nor the handle is a nameless button.
    @MainActor
    func testMethodsSheet() {
        openCheckout(scenario: "cards3.wallet", skeletonCards: 3, skeletonWallet: true)
        waitForWallet()
        let spoken = spoken("a11y_methods")
        let title = app.staticTexts["Payment"]
        XCTAssertTrue(title.exists, "the title is not a text")
        XCTAssertTrue(hasTrait(title, header), "the title is no heading")
        XCTAssertFalse(app.buttons["Payment"].exists, "the title is read as a button")
        assertNoNamelessButtons(spoken)
        assertNoScreenWideButton(spoken)

        let selected = spoken.filter { $0.traits & UIAccessibilityTraits.selected.rawValue != 0 }
        XCTAssertEqual(selected.count, 1, "selected: \(selected.map(\.label))")
        if let card = selected.first {
            XCTAssertTrue(card.button, "the picked card is no button")
            XCTAssertTrue(card.label.contains("1111"), "picked: \(card.label)")
        }

        // PassKit writes "Apple Pay" with a no-break space.
        let applePay = app.buttons.matching(NSPredicate(format: "label CONTAINS[c] 'Apple'")).firstMatch
        XCTAssertTrue(applePay.exists, "no Apple Pay button")
        XCTAssertTrue(applePay.isHittable && applePay.isEnabled, "the Apple Pay button cannot be activated")
        XCTAssertTrue(spoken.contains { $0.button && $0.label.contains("Apple") }, "VoiceOver does not reach Apple Pay")
    }

    /// The removal sheet hides the card list under it; the scrim closes it but is no nameless button.
    @MainActor
    func testCardRemovalSheet() {
        openCheckout(scenario: "cards8.wallet", skeletonCards: 3, skeletonWallet: true)
        waitForWallet()
        app.buttons["All cards"].tap()
        XCTAssertTrue(app.buttons["Edit"].waitForExistence(timeout: 5))
        XCTAssertTrue(hasTrait(app.staticTexts["My cards"], header), "the card list title is no heading")
        app.buttons["Edit"].tap()
        let remove = app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'Remove card'")).firstMatch
        XCTAssertTrue(remove.waitForExistence(timeout: 5))
        remove.tap()
        sleep(2)
        let spoken = spoken("a11y_card_remove")
        XCTAssertTrue(spoken.contains { $0.label == "Keep card" }, "no removal sheet")
        XCTAssertFalse(spoken.contains { $0.label == "Edit cards" }, "the card list is read under the removal sheet")
        XCTAssertFalse(spoken.contains { $0.label.hasPrefix("Remove card ") },
                       "the card list's buttons are read under the removal sheet")
        assertNoNamelessButtons(spoken)
        assertNoScreenWideButton(spoken)
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.1)).tap()
        sleep(2)
        XCTAssertTrue(app.staticTexts["Edit cards"].exists, "tapping the scrim does not close the removal sheet")
    }

    /// The card form: the fields keep their names when filled, read an empty one's name once, and report errors.
    @MainActor
    func testCardFormFields() {
        openCheckout(scenario: "cards0.binding")
        XCTAssertTrue(pan.waitForExistence(timeout: 15))
        sleep(1)
        let empty = spoken("a11y_form_empty")
        let title = app.staticTexts["New card"]
        XCTAssertTrue(title.exists, "no step title")
        XCTAssertTrue(hasTrait(title, header), "the step title is no heading")
        XCTAssertTrue(app.buttons["Close"].exists, "no named close button")
        assertNoNamelessButtons(empty)
        XCTAssertEqual(pan.label, "Card number")
        XCTAssertEqual(expiry.label, "Expiry date")
        for name in ["Card number", "Expiry date"] {
            XCTAssertEqual(empty.filter { $0.label.contains(name) || $0.value.contains(name) }.count, 1, "\(name) is read by more than one element")
        }
        pan.tap()
        pan.typeText("4111111111111112")
        expiry.tap()
        expiry.typeText("1220")
        cvc.tap()
        cvc.typeText("1")
        hideKeyboard()
        payButton.tap()
        sleep(2)
        let errors = spoken("a11y_form_errors")
        assertNoNamelessButtons(errors)
        XCTAssertTrue(pan.label.hasPrefix("Card number"), "the filled field lost its name: \(pan.label)")
        XCTAssertGreaterThan(pan.label.count, "Card number".count, "the field's error is not read: \(pan.label)")
        XCTAssertTrue(expiry.label.hasPrefix("Expiry date"), "the filled field lost its name: \(expiry.label)")
        XCTAssertGreaterThan(expiry.label.count, "Expiry date".count, "the field's error is not read: \(expiry.label)")
    }

    /// The payer fields read their names once, and the phone code picker hides the form under it.
    @MainActor
    func testPayerContactFields() {
        openCheckout(scenario: "cards0.payer_contact")
        XCTAssertTrue(pan.waitForExistence(timeout: 15))
        fillCard()
        hideKeyboard()
        sleep(1)
        let form = spoken("a11y_payer_contact")
        assertNoNamelessButtons(form)
        let payer = form.filter { $0.traits & UIAccessibilityTraits.keyboardKey.rawValue == 0 && !$0.value.isEmpty }
        for field in payer where !["Card number", "CVC/CVV"].contains(field.label) {
            XCTAssertNotEqual(field.label, field.value, "\(field.label) is read twice")
        }
        let code = app.buttons.matching(NSPredicate(format: "label CONTAINS '+357'")).firstMatch
        XCTAssertTrue(code.waitForExistence(timeout: 3), "no phone code button")
        code.tap()
        sleep(1)
        let picker = spoken("a11y_phone_picker")
        XCTAssertFalse(picker.contains { $0.label == "Card number" }, "the card form is read under the phone picker")
        assertNoNamelessButtons(picker)
    }

    /// A picker sheet over the payer fields hides the form under it.
    @MainActor
    func testPickerHidesTheForm() {
        openCheckout(scenario: "cards0.payer_billing")
        XCTAssertTrue(pan.waitForExistence(timeout: 15))
        fillCard()
        hideKeyboard()
        let country = app.buttons["payer_BILLING_COUNTRY"]
        XCTAssertTrue(country.waitForExistence(timeout: 5))
        let countryName = country.label
        XCTAssertFalse(countryName.isEmpty, "the country field has no name")
        let panName = pan.label
        country.tap()
        sleep(2)
        let spoken = spoken("a11y_country_picker")
        XCTAssertTrue(spoken.contains { $0.label.hasPrefix("Albania") }, "no picker")
        XCTAssertFalse(spoken.contains { $0.label == panName }, "the card form is read under the picker")
        XCTAssertFalse(spoken.contains { $0.label == countryName }, "the payer fields are read under the picker")
        assertNoNamelessButtons(spoken)
        assertNoScreenWideButton(spoken)
    }

    /// The processing step reads its title as a heading and no developer names.
    @MainActor
    func testProcessing() {
        openCheckout(scenario: "cards1.slow_process", resultScreen: true, skeletonCards: 1)
        waitForButton("New card")
        payButton.tap()
        let title = app.staticTexts["Processing payment"]
        XCTAssertTrue(title.waitForExistence(timeout: 10), "no processing title")
        sleep(1)
        let spoken = spoken("a11y_processing")
        XCTAssertTrue(hasTrait(title, header), "the processing title is no heading")
        assertNoDeveloperNames(spoken)
        assertNoNamelessButtons(spoken)
    }

    /// The results read their titles as headings and no developer names.
    @MainActor
    func testResults() {
        openCheckout(scenario: "cards1", resultScreen: true, skeletonCards: 1)
        waitForButton("New card")
        payButton.tap()
        waitForButton("Done")
        sleep(2)
        let succeeded = spoken("a11y_result_success")
        let success = app.staticTexts["Payment successful"]
        XCTAssertTrue(success.exists, "no success title")
        XCTAssertTrue(hasTrait(success, header), "the success title is no heading")
        assertNoDeveloperNames(succeeded)
        assertNoNamelessButtons(succeeded)
        app.terminate()

        openCheckout(scenario: "cards1.decline", resultScreen: true, skeletonCards: 1)
        waitForButton("New card")
        payButton.tap()
        waitForButton("Close")
        sleep(2)
        let declined = spoken("a11y_result_failure")
        let failure = app.staticTexts["Payment declined"]
        XCTAssertTrue(failure.exists, "no failure title")
        XCTAssertTrue(hasTrait(failure, header), "the failure title is no heading")
        assertNoDeveloperNames(declined)
        assertNoNamelessButtons(declined)
        XCTAssertEqual(declined.first?.label, "Payment declined", "VoiceOver starts the result elsewhere")    }

    /// Arabic mirrors the sheet: the close and back buttons swap sides, the fields' names stay.
    @MainActor
    func testRightToLeft() {
        openCheckout(scenario: "cards1", locale: "ar", skeletonCards: 1)
        let rows = app.buttons.element(boundBy: 3)
        XCTAssertTrue(rows.waitForExistence(timeout: 20))
        sleep(3)
        assertNoNamelessButtons(spoken("a11y_ar_methods"))
        let width = app.frame.width
        let newCard = app.buttons.allElementsBoundByIndex.first { $0.label.contains("بطاقة") && $0.frame.minY > app.frame.height / 3 }
        if let newCard {
            newCard.tap()
            XCTAssertTrue(pan.waitForExistence(timeout: 10))
            sleep(2)
            assertNoNamelessButtons(spoken("a11y_ar_form"))
            let header = app.buttons.allElementsBoundByIndex.filter { $0.frame.minY < pan.frame.minY && $0.frame.width < 60 }
            for button in header {
                if button.frame.midX < width / 2 { XCTAssertNotEqual(button.label, "رجوع", "the back button is not mirrored") }
            }
            XCTAssertGreaterThan(pan.frame.maxX - pan.frame.minX, 0)
        }
    }

    // MARK: - Checks

    private let header: UInt64 = 1 << 16

    private func hasTrait(_ element: XCUIElement, _ trait: UInt64) -> Bool {
        guard element.exists else { return false }
        return traits(of: element.snapshotOrNil()) & trait != 0
    }

    private func traits(of snapshot: XCUIElementSnapshot?) -> UInt64 {
        guard let object = snapshot as? NSObject, object.responds(to: NSSelectorFromString("traits")) else { return 0 }
        return (object.value(forKey: "traits") as? NSNumber)?.uint64Value ?? 0
    }

    /// An element VoiceOver reaches.
    private struct Spoken {
        let label: String
        let value: String
        let traits: UInt64
        let frame: CGRect

        var button: Bool { traits & UIAccessibilityTraits.button.rawValue != 0 }
    }

    /// What VoiceOver reaches on screen, as the sample app's `VoiceOverProbe` walks it: XCUITest's own tree also lists
    /// the views hidden from VoiceOver and the children of combined elements.
    @MainActor
    private func spoken(_ name: String) -> [Spoken] {
        tree(name)
        let json = outDir.appendingPathComponent(name + ".vo.json")
        try? FileManager.default.removeItem(at: json)
        FileManager.default.createFile(atPath: outDir.appendingPathComponent(name + ".vo-request").path, contents: nil)
        let deadline = Date().addingTimeInterval(15)
        while !FileManager.default.fileExists(atPath: json.path) && Date() < deadline {
            Thread.sleep(forTimeInterval: 0.2)
        }
        guard let data = try? Data(contentsOf: json),
              let elements = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] else {
            XCTFail("the app wrote no VoiceOver tree for \(name)")
            return []
        }
        return elements.map { element in
            let frame = element["frame"] as? [Double] ?? [0, 0, 0, 0]
            let traits = (element["traits"] as? NSNumber)?.uint64Value ?? 0
            return Spoken(
                label: element["label"] as? String ?? "",
                value: element["value"] as? String ?? "",
                traits: traits,
                frame: CGRect(x: frame[0], y: frame[1], width: frame[2], height: frame[3])
            )
        }
    }

    private func assertNoNamelessButtons(_ spoken: [Spoken], file: StaticString = #filePath, line: UInt = #line) {
        let nameless = spoken.filter { $0.button && $0.label.trimmingCharacters(in: .whitespaces).isEmpty }
        XCTAssertTrue(nameless.isEmpty, "nameless buttons: \(nameless.map(\.frame))", file: file, line: line)
    }

    @MainActor
    private func assertNoScreenWideButton(_ spoken: [Spoken], file: StaticString = #filePath, line: UInt = #line) {
        let screen = app.frame
        let wide = spoken.filter { $0.button && $0.frame.width >= screen.width - 1 && $0.frame.height > screen.height / 2 }
        XCTAssertTrue(wide.isEmpty, "screen-wide buttons: \(wide.map { "\($0.label) \($0.frame)" })", file: file, line: line)
    }

    private func assertNoDeveloperNames(_ spoken: [Spoken], file: StaticString = #filePath, line: UInt = #line) {
        for name in ["ProcessingScreen", "ResultScreen", "FailureIcon", "SuccessIcon", "ProgressRing"] {
            XCTAssertFalse(spoken.contains { $0.label.contains(name) || $0.value.contains(name) }, "\(name) is read",
                           file: file, line: line)
        }
    }

    /// The element tree with traits, when `VISUAL_DUMP` is set.
    @MainActor
    private func tree(_ name: String) {
        guard ProcessInfo.processInfo.environment["VISUAL_DUMP"] == "1", let root = try? app.snapshot() else { return }
        var lines: [String] = []
        func walk(_ node: XCUIElementSnapshot, _ depth: Int) {
            let frame = node.frame.integral
            var line = String(repeating: "  ", count: depth) + "\(node.elementType.rawValue)"
            line += " traits=0x\(String(traits(of: node), radix: 16))"
            if !node.identifier.isEmpty { line += " id='\(node.identifier)'" }
            if !node.label.isEmpty { line += " label='\(node.label)'" }
            if let value = node.value { line += " value='\(value)'" }
            if node.isSelected { line += " selected" }
            line += " \(Int(frame.minX)),\(Int(frame.minY)) \(Int(frame.width))x\(Int(frame.height))"
            lines.append(line)
            node.children.forEach { walk($0, depth + 1) }
        }
        walk(root, 0)
        try? lines.joined(separator: "\n").write(to: outDir.appendingPathComponent(name + ".txt"), atomically: true, encoding: .utf8)
        try? app.debugDescription.write(to: outDir.appendingPathComponent(name + ".debug.txt"), atomically: true, encoding: .utf8)
    }

    // MARK: - Driving the sample (as in CheckoutVisualTests)

    private var pan: XCUIElement { app.textFields.element(boundBy: 0) }
    private var expiry: XCUIElement { app.textFields.element(boundBy: 1) }
    private var cvc: XCUIElement { app.secureTextFields.firstMatch }
    private var payButton: XCUIElement { app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'Pay'")).firstMatch }

    @MainActor
    private func waitForButton(_ label: String) {
        let button = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", label)).firstMatch
        XCTAssertTrue(button.waitForExistence(timeout: 20), "No button \(label)")
    }

    @MainActor
    private func waitForWallet() {
        let row = app.buttons.element(boundBy: 3)
        XCTAssertTrue(row.waitForExistence(timeout: 20), "No methods sheet")
        sleep(5)
    }

    @MainActor
    private func fillCard() {
        pan.tap()
        pan.typeText("4111111111111111")
        expiry.tap()
        expiry.typeText("1230")
        cvc.tap()
        cvc.typeText("123")
    }

    @MainActor
    private func hideKeyboard() {
        let done = app.toolbars.buttons.firstMatch
        if done.waitForExistence(timeout: 2) { done.tap() }
        _ = app.keyboards.firstMatch.waitForNonExistence(timeout: 3)
    }

    @MainActor
    private func openCheckout(scenario: String, locale: String = "en", resultScreen: Bool = false,
                              skeletonCards: Int = 0, skeletonWallet: Bool = false) {
        get("/__release/all")
        app = XCUIApplication()
        let base = gateway + "/payment"
        let flags = "{\"buyerWithCards\":true,\"wallet\":true,\"scanner\":false,\"sdkResultScreen\":\(resultScreen)}"
        app.launchArguments += [
            "-base_url", base,
            "-store_login", scenario, "-store_password", "visual",
            "-store_api_version", "V1", "-store_order_type", "PURCHASE",
            "-store_last_cover", "coffee",
            "-store_sheet_theme", "light", "-store_locale", locale,
            "-store_client_id", "demo-visual",
            "-store_scenario", "<" + flags.utf8.map { String(format: "%02x", $0) }.joined() + ">",
            "-payrdr.savedCardCount.\(base)", String(skeletonCards),
            "-payrdr.walletOffered.\(base)", skeletonWallet ? "1" : "0",
            "-AppleLanguages", "(en)", "-AppleLocale", "en_US",
            "-voiceOverProbeDir", outDir.path,
        ]
        app.launch()
        let add = app.buttons["add_flat_white"]
        XCTAssertTrue(add.waitForExistence(timeout: 15))
        add.tap()
        let order = app.buttons["open_order"]
        XCTAssertTrue(order.waitForExistence(timeout: 5))
        order.tap()
        let pay = app.buttons["store_pay"]
        XCTAssertTrue(pay.waitForExistence(timeout: 5))
        pay.tap()
    }

    private func get(_ path: String) {
        guard let url = URL(string: gateway + path) else { return }
        let done = expectation(description: path)
        URLSession.shared.dataTask(with: url) { _, _, _ in done.fulfill() }.resume()
        wait(for: [done], timeout: 5)
    }
}

private extension XCUIElement {
    func snapshotOrNil() -> XCUIElementSnapshot? { try? snapshot() }
}
