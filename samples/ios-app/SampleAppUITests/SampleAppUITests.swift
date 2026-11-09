import XCTest

/// The demo app with no merchant credentials and no remembered business.
final class SampleAppUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        // Launch arguments override UserDefaults: empty credentials, start screen first, the three stores
        // (an AFT order type left on the simulator shows Northwind instead).
        app.launchArguments += [
            "-store_login", "", "-store_password", "", "-store_last_cover", "", "-store_order_type", "PURCHASE",
            "-AppleLanguages", "(en)",
        ]
        app.launch()
    }

    @MainActor
    func testStartShowsThreeCovers() {
        XCTAssertTrue(app.buttons["cover_furniture"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.buttons["cover_coffee"].exists)
        XCTAssertTrue(app.buttons["cover_tickets"].exists)
    }

    @MainActor
    func testGearOpensDeveloper() {
        app.buttons["start_settings"].tap()
        XCTAssertTrue(app.navigationBars["Developer settings"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.secureTextFields["dev_password"].exists)
    }

    @MainActor
    func testPayWithoutCredentialsShowsError() {
        app.buttons["cover_furniture"].tap()
        // The furniture store: product → "Add to cart" → cart.
        app.buttons["product_velvet_sofa"].tap()
        app.buttons["add_to_cart"].tap()
        let openCart = app.buttons["open_cart"]
        XCTAssertTrue(openCart.waitForExistence(timeout: 5))
        openCart.tap()
        let pay = app.buttons["store_pay"]
        XCTAssertTrue(pay.waitForExistence(timeout: 5))
        pay.tap()
        XCTAssertTrue(app.staticTexts["Merchant login and password are not set."].waitForExistence(timeout: 10))
        XCTAssertTrue(app.buttons["Open settings"].exists)
    }

    @MainActor
    func testScenarioTogglePersists() {
        app.buttons["cover_coffee"].tap()
        let scenario = app.buttons["store_scenario"]
        if !scenario.waitForExistence(timeout: 3) {
            app.buttons["add_flat_white"].tap()
            app.buttons["open_order"].tap()
        }
        app.buttons["store_scenario"].tap()
        app.segmentedControls.buttons["With cards"].tap()
        app.swipeDown(velocity: .fast)
        XCTAssertTrue(app.buttons["store_scenario"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["store_scenario"].label.contains("With cards"))
        // Restore for the next run.
        app.buttons["store_scenario"].tap()
        app.segmentedControls.buttons["Guest"].tap()
    }

    @MainActor
    func testCoreTileOpensSdkCoreAndShowsFieldErrors() {
        let tile = app.buttons["start_core"]
        XCTAssertTrue(tile.waitForExistence(timeout: 10))
        tile.tap()
        let generate = app.buttons["core_generate"]
        XCTAssertTrue(generate.waitForExistence(timeout: 5))
        app.swipeUp()
        generate.tap()
        XCTAssertTrue(app.staticTexts["core_error_PAN"].waitForExistence(timeout: 5))
    }

    @MainActor
    func testExpiryGetsItsSlashWhileTyping() {
        let tile = app.buttons["start_core"]
        XCTAssertTrue(tile.waitForExistence(timeout: 10))
        tile.tap()
        let expiry = app.textFields["core_field_EXPIRY"]
        XCTAssertTrue(expiry.waitForExistence(timeout: 5))
        expiry.tap()
        expiry.typeText("1230")
        XCTAssertEqual(expiry.value as? String, "12/30")
    }

    @MainActor
    func testCardNumberShowsInTheNetworksGroups() {
        let tile = app.buttons["start_core"]
        XCTAssertTrue(tile.waitForExistence(timeout: 10))
        tile.tap()
        XCTAssertTrue(app.buttons["New card"].waitForExistence(timeout: 5))
        app.buttons["New card"].tap()
        let pan = app.textFields["core_field_PAN"]
        pan.tap()
        pan.typeText("378282246310005999") // Amex: 15 digits at most, grouped 4-6-5
        XCTAssertEqual(pan.value as? String, "3782 822463 10005")
        let cvc = app.textFields["core_field_CVC"]
        cvc.tap()
        cvc.typeText("12345")
        XCTAssertEqual(cvc.value as? String, "1234")
    }
}
