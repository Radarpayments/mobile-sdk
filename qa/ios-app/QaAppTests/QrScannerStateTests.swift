@testable import QaApp
import XCTest

final class QrScannerStateTests: XCTestCase {
    func test_the_first_appearance_configures_the_camera() {
        var state = QrScannerState()
        XCTAssertEqual(state.appear(), .configure)
    }

    func test_a_screen_that_appears_again_before_a_scan_resumes_the_camera() {
        var state = QrScannerState()
        _ = state.appear()
        XCTAssertEqual(state.appear(), .resume)
        XCTAssertEqual(state.appear(), .resume)
    }

    func test_a_screen_that_appears_after_the_scan_was_reported_does_nothing() {
        var state = QrScannerState()
        _ = state.appear()
        _ = state.end(.text("x"))
        XCTAssertEqual(state.appear(), .none)
    }

    func test_a_camera_granted_after_the_screen_closed_during_the_prompt_does_not_start() {
        var state = QrScannerState()
        _ = state.appear()
        XCTAssertTrue(state.mayStart)
        _ = state.disappear()
        XCTAssertFalse(state.mayStart)
    }

    func test_a_code_is_reported_and_nothing_after_it() {
        var state = QrScannerState()
        _ = state.appear()
        XCTAssertEqual(state.end(.text("x")), .text("x"))
        XCTAssertNil(state.end(.text("y")))
        XCTAssertNil(state.end(.unavailable))
        XCTAssertNil(state.disappear())
    }

    func test_a_screen_gone_without_a_code_is_a_cancel() {
        var state = QrScannerState()
        _ = state.appear()
        XCTAssertEqual(state.disappear(), .cancelled)
        XCTAssertNil(state.disappear())
        XCTAssertNil(state.end(.text("x")))
    }

    func test_an_unavailable_camera_is_reported_once_and_the_closing_screen_adds_no_cancel() {
        var state = QrScannerState()
        _ = state.appear()
        XCTAssertEqual(state.end(.unavailable), .unavailable)
        XCTAssertNil(state.disappear())
    }

    func test_a_swipe_down_that_is_abandoned_keeps_scanning() {
        var state = QrScannerState()
        _ = state.appear()
        // UIKit calls viewWillDisappear and then viewDidAppear again; the state hears only the second.
        XCTAssertEqual(state.appear(), .resume)
        XCTAssertEqual(state.end(.text("x")), .text("x"))
    }
}
