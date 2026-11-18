@testable import QaApp
import UIKit
import XCTest

@MainActor
final class ThemePresetsTests: XCTestCase {
    private func custom(_ preset: ThemePreset, file: StaticString = #filePath, line: UInt = #line) throws -> PaymentThemeCustom {
        try XCTUnwrap(preset.paymentTheme() as? PaymentThemeCustom, "\(preset)", file: file, line: line)
    }

    private func large(_ style: PaymentTextStyle) -> PaymentTextStyle {
        style.doCopy(
            size: style.size * 1.5,
            weight: style.weight,
            lineHeight: style.lineHeight * 1.5,
            letterSpacing: style.letterSpacing,
            tabularFigures: style.tabularFigures,
            fontFamily: PaymentFontFamily(name: "Georgia")
        )
    }

    // Each read of a Kotlin object gives Swift a new wrapper, so identity says nothing; `isEqual` is Kotlin's equality.
    func test_system_light_and_dark_are_the_sdks_themes() {
        XCTAssertTrue(PaymentThemeSystem.shared.isEqual(ThemePreset.system.paymentTheme()))
        XCTAssertTrue(PaymentThemeLight.shared.isEqual(ThemePreset.light.paymentTheme()))
        XCTAssertTrue(PaymentThemeDark.shared.isEqual(ThemePreset.dark.paymentTheme()))
        XCTAssertFalse(PaymentThemeSystem.shared.isEqual(ThemePreset.dark.paymentTheme()))
    }

    func test_rounded_makes_every_corner_a_pill() throws {
        let pill = PaymentCornersPill.shared
        let theme = try custom(.rounded)
        XCTAssertEqual(theme.shapes, PaymentShapes(field: pill, button: pill, sheet: pill, card: pill))
        XCTAssertEqual(theme.typography, PaymentTypography.companion.Default)
        XCTAssertEqual(theme.colors.primary, PaymentColor(argb: 0xFF1F_5FFF))
    }

    func test_square_makes_every_corner_zero() throws {
        let zero = PaymentCornersRadius(points: 0)
        let theme = try custom(.square)
        XCTAssertEqual(theme.shapes, PaymentShapes(field: zero, button: zero, sheet: zero, card: zero))
        XCTAssertEqual(theme.typography, PaymentTypography.companion.Default)
        XCTAssertEqual(theme.colors, try custom(.rounded).colors)
    }

    func test_the_base_palette_keeps_the_sdks_defaults_for_the_four_optional_tokens() throws {
        let colors = try custom(.rounded).colors
        XCTAssertEqual(colors.onBackground, PaymentColor(argb: 0xFF14_161A))
        XCTAssertEqual(colors.onSurfaceVariant, colors.onBackground.withAlpha(alpha: 0.7))
        XCTAssertEqual(colors.placeholder, colors.onBackground.withAlpha(alpha: 0.5))
        XCTAssertEqual(colors.outlineStrong, colors.outline)
        XCTAssertEqual(colors.scrim, PaymentColor.companion.Black.withAlpha(alpha: 0.45))
    }

    func test_large_text_scales_all_eight_styles_by_one_and_a_half_and_uses_a_serif_family() throws {
        let theme = try custom(.largeText)
        let typography = theme.typography
        let base = PaymentTypography.companion.Default
        XCTAssertEqual(typography.title, large(base.title))
        XCTAssertEqual(typography.body, large(base.body))
        XCTAssertEqual(typography.label, large(base.label))
        XCTAssertEqual(typography.button, large(base.button))
        XCTAssertEqual(typography.input, large(base.input))
        XCTAssertEqual(typography.headline, large(base.headline))
        XCTAssertEqual(typography.amount, large(base.amount))
        XCTAssertEqual(typography.caption, large(base.caption))
        XCTAssertEqual(typography.title.size, 25.5)
        XCTAssertEqual(typography.amount.lineHeight, 51)
        XCTAssertEqual(theme.shapes, PaymentShapes.companion.Default)
        XCTAssertEqual(theme.colors, try custom(.rounded).colors)
    }

    // The SDK falls back to the system font when UIFont knows no faces of the family, so a typo here would go unnoticed.
    func test_the_large_text_family_is_installed_on_ios() throws {
        let family = try XCTUnwrap(try custom(.largeText).typography.body.fontFamily?.name)
        XCTAssertFalse(UIFont.fontNames(forFamilyName: family).isEmpty, family)
    }

    func test_contrast_is_black_on_white() throws {
        let colors = try custom(.contrast).colors
        XCTAssertEqual(colors.background.argb, 0xFFFF_FFFF)
        XCTAssertEqual(colors.onBackground.argb, 0xFF00_0000)
        XCTAssertEqual(colors.placeholder.argb, 0xFF59_5959)
        XCTAssertEqual(colors.scrim.argb, 0xCC00_0000)
    }

    func test_the_debug_palette_has_fourteen_different_colours_and_the_legend_lists_them() throws {
        XCTAssertEqual(Set(debugLegend.map(\.color.argb)).count, 14)
        XCTAssertEqual(debugLegend.first?.token, "primary")
        XCTAssertEqual(debugLegend.first?.color, PaymentColor(argb: 0xFFFF_00FF))
        XCTAssertEqual(
            debugLegend.map(\.token),
            [
                "primary", "onPrimary", "background", "onBackground", "surface", "onSurface", "error", "onError", "outline",
                "success", "onSurfaceVariant", "placeholder", "outlineStrong", "scrim",
            ]
        )
        let colors = try custom(.debug).colors
        XCTAssertEqual(
            debugLegend.map(\.color),
            [
                colors.primary, colors.onPrimary, colors.background, colors.onBackground, colors.surface, colors.onSurface,
                colors.error, colors.onError, colors.outline, colors.success, colors.onSurfaceVariant, colors.placeholder,
                colors.outlineStrong, colors.scrim,
            ]
        )
        XCTAssertEqual(colors.scrim.argb, 0x80FF_0080)
    }
}
