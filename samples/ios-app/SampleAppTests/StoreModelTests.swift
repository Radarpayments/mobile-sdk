import XCTest
@testable import SampleApp

@MainActor
final class StoreModelTests: XCTestCase {
    private let mug = Product(id: "mug", nameKey: "Mug", subtitleKey: nil, priceMinor: 1_800, image: nil)

    func test_carts_are_separate_per_cover() {
        let store = StoreModel()
        store.add(.furniture, mug)
        XCTAssertEqual(store.cart(.furniture).count, 1)
        XCTAssertTrue(store.cart(.coffee).isEmpty)
    }

    func test_success_clears_the_cart_and_keeps_the_receipt() {
        let store = StoreModel()
        store.add(.furniture, mug)
        store.onOutcome(.furniture, .success(paymentId: "p-1"), amountMinor: 1_800)
        XCTAssertTrue(store.cart(.furniture).isEmpty)
        XCTAssertEqual(store.lastReceipt, StoreModel.Receipt(outcome: .success(paymentId: "p-1"), amountMinor: 1_800))
    }

    func test_cancel_keeps_the_cart_and_shows_no_result() {
        let store = StoreModel()
        store.add(.furniture, mug)
        store.onOutcome(.furniture, .cancelled, amountMinor: 1_800)
        XCTAssertEqual(store.cart(.furniture).count, 1)
        XCTAssertNil(store.lastReceipt)
    }

    func test_a_paid_order_can_be_repeated_and_numbers_start_at_42() {
        let store = StoreModel()
        store.add(.coffee, mug)
        XCTAssertNil(store.lastPaid(.coffee))
        store.onOutcome(.coffee, .success(paymentId: "p-1"), amountMinor: 1_800)
        XCTAssertEqual(store.lastOrderNumber, 42)
        store.repeatLast(.coffee)
        XCTAssertEqual(store.cart(.coffee).count, 1)
        store.onOutcome(.coffee, .success(paymentId: "p-2"), amountMinor: 1_800)
        XCTAssertEqual(store.lastOrderNumber, 43)
    }

    func test_furniture_filter_shows_only_lamps() {
        XCTAssertEqual(furnitureFilter(.light).map(\.id), ["arc_lamp"])
        XCTAssertEqual(furnitureFilter(.all).count, 6)
    }

    func test_ticket_order_total_and_limits() {
        XCTAssertEqual(TicketOrder(categoryId: "balcony", quantity: 2).totalMinor, 13_350)
        XCTAssertEqual(TicketOrder(quantity: 6).withQuantity(9).quantity, 6)
        XCTAssertEqual(TicketOrder(quantity: 1).withQuantity(0).quantity, 1)
    }
}
