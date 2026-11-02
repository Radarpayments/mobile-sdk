import XCTest
import SwiftUI
@testable import SampleApp

final class SvgPathTests: XCTestCase {
    func test_svg_path_follows_relative_and_shorthand_commands() {
        let box = svgPath("M0 0h10v10H0z").boundingRect
        XCTAssertEqual(box.width, 10, accuracy: 0.001)
        XCTAssertEqual(box.height, 10, accuracy: 0.001)
    }

    func test_svg_arc_ends_where_it_says() {
        // The first arc of the contactless mark: from (10.54, 62.5) bulging right to (10.54, 119.5).
        let box = svgPath("M10.54 62.5 A49.81 49.81 0 0 1 10.54 119.5").boundingRect
        XCTAssertEqual(box.minY, 62.5, accuracy: 0.5)
        XCTAssertEqual(box.maxY, 119.5, accuracy: 0.5)
        XCTAssertGreaterThan(box.maxX, 18)
    }

    func test_svg_numbers_without_separators_are_read() {
        // "-0.217" and ".5.5" style runs, as the generated logos use.
        let box = svgPath("M1-1l.5.5l-.5-.5z").boundingRect
        XCTAssertEqual(box.minX, 1, accuracy: 0.001)
        XCTAssertEqual(box.maxY, -0.5, accuracy: 0.001)
    }
}
