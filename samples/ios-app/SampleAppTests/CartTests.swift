import XCTest
@testable import SampleApp

final class CartTests: XCTestCase {
    private let sofa = Product(id: "sofa", nameKey: "Velvet sofa", subtitleKey: nil, priceMinor: 124_900, image: nil)
    private let chair = Product(id: "chair", nameKey: "Cloud chair", subtitleKey: nil, priceMinor: 32_900, image: nil)

    func test_add_twice_increments_quantity() {
        var cart = Cart()
        cart.add(sofa)
        cart.add(sofa)
        XCTAssertEqual(cart.lines, [CartLine(product: sofa, quantity: 2)])
        XCTAssertEqual(cart.count, 2)
    }

    func test_total_of_sofa_and_chair_is_157800() {
        var cart = Cart()
        cart.add(sofa)
        cart.add(chair)
        XCTAssertEqual(cart.totalMinor, 157_800)
    }

    func test_setQuantity_zero_removes_line() {
        var cart = Cart()
        cart.add(sofa)
        cart.add(chair)
        cart.setQuantity("sofa", 0)
        XCTAssertEqual(cart.lines, [CartLine(product: chair, quantity: 1)])
    }

    func test_empty_cart_is_empty_and_zero() {
        var cart = Cart()
        cart.add(sofa)
        cart.clear()
        XCTAssertTrue(cart.isEmpty)
        XCTAssertEqual(cart.totalMinor, 0)
    }

    func test_formatEur_english() {
        XCTAssertEqual(formatEur(157_800, locale: Locale(identifier: "en_US")), "€1,578.00")
    }

    func test_formatEur_russian() {
        let text = formatEur(157_800, locale: Locale(identifier: "ru_RU"))
            .replacingOccurrences(of: "\u{00A0}", with: " ")
            .replacingOccurrences(of: "\u{202F}", with: " ")
        XCTAssertEqual(text, "1 578,00 €")
    }

    func test_catalog_prices_match_android() {
        func price(_ cover: CoverId, _ id: String) -> Int64? { Catalog.products(cover).first { $0.id == id }?.priceMinor }
        XCTAssertEqual(price(.furniture, "velvet_sofa"), 124_900)
        XCTAssertEqual(Catalog.products(.furniture).count, 6)
        XCTAssertEqual(Catalog.products(.coffee).count, 5)
        XCTAssertEqual(Catalog.products(.tickets).map(\.priceMinor), [4_500, 6_500, 350])
    }
}
