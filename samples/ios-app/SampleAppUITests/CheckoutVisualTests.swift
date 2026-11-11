import XCTest

/// Screenshots of the SDK checkout in each of its states, taken against `tools/visual/fake_gateway.py`.
/// Run through `tools/visual/capture.sh <set-name>`; the PNGs land in `VISUAL_OUT`.
final class CheckoutVisualTests: XCTestCase {
    private var app: XCUIApplication!

    private let gateway = ProcessInfo.processInfo.environment["VISUAL_GATEWAY"] ?? "http://127.0.0.1:8787"
    private let outDir = URL(fileURLWithPath: ProcessInfo.processInfo.environment["VISUAL_OUT"] ?? "/tmp/payrdr-visual/default")

    override func setUpWithError() throws {
        continueAfterFailure = false
        try FileManager.default.createDirectory(at: outDir, withIntermediateDirectories: true)
    }

    override func tearDown() {
        get("/__release/all")
        app?.terminate()
    }

    // MARK: - States

    @MainActor
    func test01Skeleton() {
        openCheckout(scenario: "slow_session.cards3.wallet", skeletonCards: 3, skeletonWallet: true)
        sleep(3)
        shot("01_skeleton_cards3_wallet", settle: 0)
        app.terminate()
        openCheckout(scenario: "slow_session.cards0")
        sleep(3)
        shot("02_skeleton_cards0", settle: 0)
    }

    @MainActor
    func test02Methods() {
        openCheckout(scenario: "cards3.wallet", skeletonCards: 3, skeletonWallet: true)
        waitForWallet()
        shot("03_methods_cards3_wallet")
        app.terminate()
        openCheckout(scenario: "cards1", skeletonCards: 1)
        waitForButton("New card")
        shot("04_methods_cards1")
    }

    @MainActor
    func test03CardList() {
        openCheckout(scenario: "cards8.wallet", skeletonCards: 3, skeletonWallet: true)
        waitForWallet()
        shot("05_methods_cards8_wallet")
        app.buttons["All cards"].tap()
        _ = app.buttons["Edit"].waitForExistence(timeout: 5)
        shot("06_card_list_cards8")
        app.buttons["Edit"].tap()
        shot("07_card_list_edit")
        let remove = app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'Remove card'")).firstMatch
        if remove.waitForExistence(timeout: 3) {
            remove.tap()
            shot("08_card_list_remove")
        }
    }

    @MainActor
    func test04CardForm() {
        openCheckout(scenario: "cards0")
        _ = pan.waitForExistence(timeout: 15)
        shot("10_form_empty")
        pan.tap()
        shot("11_form_focused")
        pan.typeText("4111")
        shot("12_form_typing_visa_focused")
        pan.typeText("111111111111")
        expiry.tap()
        expiry.typeText("1230")
        cvc.tap()
        cvc.typeText("123")
        shot("13_form_filled_focused")
        hideKeyboard()
        shot("14_form_filled")
        app.buttons["Where to find the CVV"].tap()
        shot("15_form_cvc_hint")
    }

    /// The simulator has no camera: past the permission prompt the scanner can only say so. Scanning itself needs a device.
    @MainActor
    func test21ScannerUnavailable() {
        openCheckout(scenario: "cards0", scanner: true)
        _ = pan.waitForExistence(timeout: 15)
        shot("90_scan_form")
        app.buttons["Scan card"].tap()
        shot("91_scan_intro")
        app.buttons["Turn on camera"].tap()
        answerCameraPrompt(allow: true)
        _ = app.staticTexts["Camera unavailable"].waitForExistence(timeout: 10)
        shot("92_scan_unavailable")
        app.buttons["Enter manually"].tap()
        _ = app.buttons["Scan card"].waitForExistence(timeout: 5)
        shot("94_scan_closed")
    }

    @MainActor
    func test22ScannerDenied() {
        openCheckout(scenario: "cards0", scanner: true)
        _ = pan.waitForExistence(timeout: 15)
        app.buttons["Scan card"].tap()
        _ = app.buttons["Turn on camera"].waitForExistence(timeout: 5)
        app.buttons["Turn on camera"].tap()
        answerCameraPrompt(allow: false)
        _ = app.staticTexts["Camera access required"].waitForExistence(timeout: 10)
        shot("93_scan_denied")
    }

    @MainActor
    func test05CardFormErrors() {
        openCheckout(scenario: "cards0.binding")
        _ = pan.waitForExistence(timeout: 15)
        shot("16_form_empty_save_card")
        pan.tap()
        pan.typeText("4111111111111112")
        expiry.tap()
        expiry.typeText("1220")
        cvc.tap()
        cvc.typeText("1")
        hideKeyboard()
        shot("17_form_errors")
        payButton.tap()
        shot("18_form_errors_after_pay")
    }

    /// Takes no shots: it is recorded on video to compare how the sheet closes.
    @MainActor
    func test16Close() {
        openCheckout(scenario: "cards1", skeletonCards: 1)
        waitForButton("New card")
        sleep(1)
        app.buttons["Close"].firstMatch.tap()
        sleep(2)
        app.terminate()
        openCheckout(scenario: "cards1", skeletonCards: 1)
        waitForButton("New card")
        sleep(1)
        let start = app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'New card'")).firstMatch.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
        start.press(forDuration: 0.05, thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.98)))
        sleep(2)
    }

    @MainActor
    func test06PayerContact() {
        openCheckout(scenario: "cards0.payer_contact")
        _ = pan.waitForExistence(timeout: 15)
        fillCard()
        hideKeyboard()
        shot("20_form_payer_contact")
        let code = app.buttons.matching(NSPredicate(format: "label CONTAINS '+357'")).firstMatch
        if code.waitForExistence(timeout: 3) {
            code.tap()
            sleep(1)
            shot("21_phone_country_picker")
        }
    }

    /// The form scrolls the focused field above the keyboard, the lowest billing field included.
    @MainActor
    func test20FocusScroll() {
        openCheckout(scenario: "cards0.payer_billing")
        _ = pan.waitForExistence(timeout: 15)
        fillCard()
        hideKeyboard()
        let state = app.descendants(matching: .any)["payer_BILLING_STATE"].firstMatch
        XCTAssertTrue(state.waitForExistence(timeout: 5))
        state.tap()
        sleep(2)
        shot("80_focus_billing_state")
        let email = app.descendants(matching: .any)["payer_EMAIL"].firstMatch
        email.tap()
        sleep(2)
        shot("81_focus_email")
    }

    /// Video only: the saved card step and the new card form go back to the methods step.
    @MainActor
    func test19Back() {
        openCheckout(scenario: "cards1.cvc", skeletonCards: 1)
        waitForButton("New card")
        payButton.tap()
        _ = cvc.waitForExistence(timeout: 5)
        hideKeyboard()
        sleep(1)
        app.buttons["Back"].firstMatch.tap()
        sleep(2)
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'New card'")).firstMatch.tap()
        sleep(2)
        hideKeyboard()
        sleep(1)
        app.buttons["Back"].firstMatch.tap()
        sleep(2)
    }

    /// Video only: picking another saved card changes the Pay label in place.
    @MainActor
    func test18SelectCard() {
        openCheckout(scenario: "cards3.cvc", skeletonCards: 3)
        waitForButton("New card")
        sleep(1)
        let mastercard = app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS '4444'")).firstMatch
        XCTAssertTrue(mastercard.waitForExistence(timeout: 5))
        mastercard.tap()
        sleep(2)
        app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS '0005'")).firstMatch.tap()
        sleep(2)
    }

    /// Video only: the sheet grows when the payer fields show their errors and shrinks when one is fixed.
    @MainActor
    func test17PayerErrors() {
        openCheckout(scenario: "cards0.payer_contact")
        _ = pan.waitForExistence(timeout: 15)
        fillCard()
        hideKeyboard()
        sleep(1)
        payButton.tap()
        sleep(2)
        let email = app.descendants(matching: .any)["payer_EMAIL"].firstMatch
        XCTAssertTrue(email.waitForExistence(timeout: 5))
        email.tap()
        email.typeText("buyer@example.com")
        hideKeyboard()
        sleep(2)
    }

    @MainActor
    func test07PayerBilling() {
        openCheckout(scenario: "cards0.payer_billing")
        _ = pan.waitForExistence(timeout: 15)
        fillCard()
        hideKeyboard()
        shot("22_form_payer_billing")
        let country = app.buttons["payer_BILLING_COUNTRY"]
        XCTAssertTrue(country.waitForExistence(timeout: 5))
        country.tap()
        sleep(1)
        shot("23_billing_country_picker")
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'Albania'")).firstMatch.tap()
        sleep(1)
        // The shot is of the form's end: one swipe falls short of it in UIScrollView, and the fling has to come to rest.
        app.swipeUp(velocity: .fast)
        app.swipeUp(velocity: .fast)
        sleep(1)
        shot("24_form_payer_billing_bottom")
    }

    @MainActor
    func test08SavedCardCvc() {
        openCheckout(scenario: "cards1.cvc", skeletonCards: 1)
        waitForButton("New card")
        payButton.tap()
        _ = cvc.waitForExistence(timeout: 5)
        shot("30_saved_card_cvc_empty_focused")
        cvc.typeText("123")
        shot("31_saved_card_cvc_filled_focused")
        hideKeyboard()
        shot("32_saved_card_cvc_filled")
    }

    @MainActor
    func test09Processing() {
        openCheckout(scenario: "cards1.slow_process", resultScreen: true, skeletonCards: 1)
        waitForButton("New card")
        payButton.tap()
        sleep(3)
        shot("40_processing", settle: 0)
    }

    @MainActor
    func test10ThreeDs() {
        openCheckout(scenario: "cards1.threeds", resultScreen: true, skeletonCards: 1)
        waitForButton("New card")
        payButton.tap()
        _ = app.webViews.staticTexts["Fake Bank"].waitForExistence(timeout: 15)
        shot("41_threeds_challenge")
    }

    @MainActor
    func test11Result() {
        openCheckout(scenario: "cards1", resultScreen: true, skeletonCards: 1)
        waitForButton("New card")
        payButton.tap()
        waitForButton("Done")
        shot("50_result_success")
        app.terminate()
        openCheckout(scenario: "cards1.decline", resultScreen: true, skeletonCards: 1)
        waitForButton("New card")
        payButton.tap()
        waitForButton("Close")
        sleep(2)
        shot("51_result_failure")
    }

    @MainActor
    func test12Dark() {
        openCheckout(scenario: "cards3.wallet", theme: "dark", skeletonCards: 3, skeletonWallet: true)
        waitForWallet()
        shot("60_dark_methods_cards3_wallet")
        app.terminate()
        openCheckout(scenario: "cards0", theme: "dark")
        _ = pan.waitForExistence(timeout: 15)
        fillCard()
        hideKeyboard()
        shot("61_dark_form_filled")
        app.terminate()
        openCheckout(scenario: "cards1", theme: "dark", resultScreen: true, skeletonCards: 1)
        waitForButton("New card")
        payButton.tap()
        waitForButton("Done")
        shot("62_dark_result_success")
    }

    @MainActor
    func test13Languages() {
        openCheckout(scenario: "cards3.wallet", locale: "ar", skeletonCards: 3, skeletonWallet: true)
        waitForWallet()
        shot("70_ar_methods_cards3_wallet")
        app.terminate()
        openCheckout(scenario: "cards0", locale: "ar")
        _ = pan.waitForExistence(timeout: 15)
        fillCard()
        hideKeyboard()
        shot("71_ar_form_filled")
        app.terminate()
        openCheckout(scenario: "cards3.wallet", locale: "ru", skeletonCards: 3, skeletonWallet: true)
        waitForWallet()
        shot("72_ru_methods_cards3_wallet")
    }

    @MainActor
    func test14Transfer() {
        openCheckout(scenario: "cards1", transfer: true, skeletonCards: 1)
        sleep(5)
        shot("80_aft_methods_cards1")
        let newCard = app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'New card'")).firstMatch
        if newCard.exists {
            newCard.tap()
            _ = pan.waitForExistence(timeout: 5)
            shot("81_aft_form_empty")
        }
    }

    // MARK: - Elements

    // By position, not by label: the labels are translated.
    private var pan: XCUIElement { app.textFields.element(boundBy: 0) }
    private var expiry: XCUIElement { app.textFields.element(boundBy: 1) }
    private var cvc: XCUIElement { app.secureTextFields.firstMatch }
    private var payButton: XCUIElement { app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'Pay'")).firstMatch }

    @MainActor
    private func waitForButton(_ label: String) {
        let button = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", label)).firstMatch
        XCTAssertTrue(button.waitForExistence(timeout: 20), "No button \(label)")
    }

    /// The methods sheet with its Apple Pay button, which UIKit draws a moment after the sheet has settled.
    @MainActor
    private func waitForWallet() {
        _ = app.textFields.firstMatch.waitForNonExistence(timeout: 1)
        let row = app.buttons.element(boundBy: 3)
        XCTAssertTrue(row.waitForExistence(timeout: 20), "No methods sheet")
        // The button is not in the accessibility tree, so there is nothing to wait on.
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

    /// The checkmark of the bar above the keyboard.
    @MainActor
    private func hideKeyboard() {
        let done = app.toolbars.buttons.firstMatch
        if done.waitForExistence(timeout: 2) { done.tap() }
        _ = app.keyboards.firstMatch.waitForNonExistence(timeout: 3)
    }

    // MARK: - Driving the sample

    /// Launches the sample with a payment started: the coffee shop with one flat white, or a 50 € top-up at Northwind
    /// for a transfer. `scenario` becomes the merchant login, which is how the fake gateway picks its answers.
    @MainActor
    private func openCheckout(scenario: String, theme: String = "light", locale: String = "en", resultScreen: Bool = false,
                              transfer: Bool = false, skeletonCards: Int = 0, skeletonWallet: Bool = false, scanner: Bool = false) {
        get("/__release/all")
        app = XCUIApplication()
        let base = gateway + "/payment"
        let flags = "{\"buyerWithCards\":true,\"wallet\":true,\"scanner\":\(scanner),\"sdkResultScreen\":\(resultScreen)}"
        app.launchArguments += [
            "-base_url", base,
            "-store_login", scenario, "-store_password", "visual",
            "-store_api_version", "V1", "-store_order_type", transfer ? "AFT" : "PURCHASE",
            "-store_last_cover", transfer ? "invest" : "coffee",
            "-store_sheet_theme", theme, "-store_locale", locale,
            "-store_client_id", "demo-visual",
            // Data in the old property-list form, which is how the argument domain reads a value.
            "-store_scenario", "<" + flags.utf8.map { String(format: "%02x", $0) }.joined() + ">",
            // What the SDK remembers of the last session, so the skeleton draws the same rows on every run.
            "-payrdr.savedCardCount.\(base)", String(skeletonCards),
            "-payrdr.walletOffered.\(base)", skeletonWallet ? "1" : "0",
            "-AppleLanguages", "(en)", "-AppleLocale", "en_US",
        ]
        // Every scanner test starts from the system's camera prompt.
        if scanner { app.resetAuthorizationStatus(for: .camera) }
        app.launch()
        if transfer {
            let topUp = app.buttons["Top up"]
            XCTAssertTrue(topUp.waitForExistence(timeout: 15))
            topUp.tap()
            let go = app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'Continue'")).firstMatch
            XCTAssertTrue(go.waitForExistence(timeout: 5))
            if !go.isEnabled {
                let amount = app.textFields["invest_amount"]
                amount.tap()
                amount.typeText("50")
            }
            go.tap()
            return
        }
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

    /// Taps the system's camera permission prompt by position: its labels follow the simulator's language, and "Don't
    /// Allow" comes first.
    @MainActor
    private func answerCameraPrompt(allow: Bool) {
        let alert = XCUIApplication(bundleIdentifier: "com.apple.springboard").alerts.firstMatch
        guard alert.waitForExistence(timeout: 10) else { return }
        alert.buttons.element(boundBy: allow ? 1 : 0).tap()
    }

    // MARK: - Shots

    /// Saves the screen once two consecutive screenshots are the same, i.e. once nothing moves any more.
    /// `settle: 0` is for the screens that never stop moving (the skeleton's pulse, the processing ring).
    @MainActor
    private func shot(_ name: String, settle: TimeInterval = 6) {
        usleep(600_000)
        var last = XCUIScreen.main.screenshot().pngRepresentation
        let deadline = Date().addingTimeInterval(settle)
        while Date() < deadline {
            usleep(350_000)
            let next = XCUIScreen.main.screenshot().pngRepresentation
            if next == last { break }
            last = next
        }
        try? last.write(to: outDir.appendingPathComponent(name + ".png"))
    }

    private func get(_ path: String) {
        guard let url = URL(string: gateway + path) else { return }
        let done = expectation(description: path)
        URLSession.shared.dataTask(with: url) { _, _, _ in done.fulfill() }.resume()
        wait(for: [done], timeout: 5)
    }
}
