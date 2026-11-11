import XCTest
import UIKit
import RadarpaymentsSDKPayment
@testable import SampleApp

final class CoverStyleTests: XCTestCase {
    private func luminance(_ argb: UInt32) -> Double {
        func channel(_ shift: UInt32) -> Double {
            let c = Double((argb >> shift) & 0xFF) / 255
            return c <= 0.03928 ? c / 12.92 : pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    private func contrast(_ a: UInt32, _ b: UInt32) -> Double {
        let la = luminance(a) + 0.05, lb = luminance(b) + 0.05
        return max(la, lb) / min(la, lb)
    }

    func test_paymentColor_keeps_the_argb_value() {
        XCTAssertEqual(PaymentColor(0xFF1C_1917).argb, 0xFF1C_1917)
    }

    func test_paymentTheme_uses_cover_primary() {
        let style = coverStyle(.tickets)
        XCTAssertEqual(style.paymentTheme().colors.primary, PaymentColor(0xFFEA_580C))
        XCTAssertEqual(style.paymentTheme().colors.onPrimary, PaymentColor(0xFF00_0000))
    }

    func test_paymentTheme_takes_cover_corners() {
        let shapes = coverStyle(.tickets).paymentTheme().shapes
        XCTAssertEqual((shapes.field as? PaymentCornersRadius)?.points, 14)
        XCTAssertEqual((shapes.button as? PaymentCornersRadius)?.points, 14)
        XCTAssertTrue(coverStyle(.coffee).paymentTheme().shapes.button is PaymentCornersPill)
    }

    func test_sheet_placeholder_takes_the_cover_text_colour() {
        for cover in CoverId.allCases {
            let s = coverStyle(cover)
            let placeholder = UInt32(s.paymentTheme().colors.placeholder.argb)
            XCTAssertEqual(placeholder & 0x00FF_FFFF, s.textArgb & 0x00FF_FFFF, "\(cover) placeholder hue")
            XCTAssertLessThan(placeholder >> 24, 0xFF, "\(cover) placeholder is dimmer than the text")
        }
    }

    func test_only_coffee_is_dark() {
        XCTAssertTrue(coverStyle(.coffee).dark)
        XCTAssertFalse(coverStyle(.furniture).dark)
        XCTAssertFalse(coverStyle(.tickets).dark)
    }

    func test_text_is_readable_in_every_cover() {
        for cover in CoverId.allCases {
            let s = coverStyle(cover)
            XCTAssertGreaterThanOrEqual(contrast(s.primaryArgb, s.onPrimaryArgb), 4.5, "\(cover) onPrimary")
            XCTAssertGreaterThanOrEqual(contrast(s.backgroundArgb, s.textArgb), 4.5, "\(cover) text")
            XCTAssertGreaterThanOrEqual(contrast(s.backgroundArgb, s.mutedArgb), 4.5, "\(cover) muted")
        }
    }

    func test_brand_fonts_are_registered() {
        for family in ["Cormorant", "Montserrat", "Nunito", "Inter", "Playfair Display"] {
            XCTAssertTrue(UIFont.familyNames.contains(family), "\(family) is not in the bundle")
        }
    }
}
