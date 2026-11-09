import XCTest
@testable import SampleApp

final class InvestModeTests: XCTestCase {
    func test_the_AFT_demo_is_API_V1_with_AFT_orders() {
        XCTAssertTrue(isAftDemo(apiVersion: .v1, orderType: .aft))
        XCTAssertFalse(isAftDemo(apiVersion: .v2, orderType: .aft))
        XCTAssertFalse(isAftDemo(apiVersion: .v1, orderType: .purchase))
    }

    func test_the_AFT_demo_shows_only_Northwind_otherwise_the_stores() {
        XCTAssertEqual(visibleCovers(aftDemo: true), [.invest])
        XCTAssertEqual(visibleCovers(aftDemo: false), [.furniture, .coffee, .tickets])
    }

    func test_the_last_business_reopens_only_when_the_mode_shows_it() {
        XCTAssertNil(reopenCover(.furniture, aftDemo: true))
        XCTAssertNil(reopenCover(.invest, aftDemo: false))
        XCTAssertEqual(reopenCover(.invest, aftDemo: true), .invest)
        XCTAssertEqual(reopenCover(.coffee, aftDemo: false), .coffee)
        XCTAssertNil(reopenCover(nil, aftDemo: true))
    }

    @MainActor func test_a_success_credits_once_and_saves() {
        var saved: [InvestAccount] = []
        let model = InvestModel(load: { .seed }, save: { saved.append($0) }, clock: { 1000 })
        model.onOutcome(.success(paymentId: "md-1"), amountMinor: 10000)
        model.onOutcome(.success(paymentId: "md-1"), amountMinor: 10000)
        XCTAssertEqual(model.account.freeCashMinor, 11240)
        XCTAssertEqual(saved.last?.freeCashMinor, 11240)
        XCTAssertEqual(model.status, .done(orderId: "md-1", amountMinor: 10000, freeCashBefore: 1240, freeCashAfter: 11240, at: 1000))
    }

    @MainActor func test_declined_and_unconfirmed_keep_the_balance() {
        let model = InvestModel(load: { .seed }, save: { _ in XCTFail("nothing to save") }, clock: { 1000 })
        model.onOutcome(.declined(message: "Issuer declined"), amountMinor: 10000)
        XCTAssertEqual(model.status, .declined(message: "Issuer declined"))
        model.onOutcome(.unconfirmed(paymentId: "md-2"), amountMinor: 10000)
        XCTAssertEqual(model.status, .unconfirmed)
        XCTAssertEqual(model.account.freeCashMinor, 1240)
        model.onOutcome(.cancelled, amountMinor: 10000)
        XCTAssertEqual(model.status, .editing)
    }

    @MainActor func test_a_saved_card_credits_nothing_and_the_top_up_may_go_on() {
        let model = InvestModel(load: { .seed }, save: { _ in XCTFail("nothing to save") }, clock: { 1000 })
        model.onCardOutcome(.success(paymentId: "md-verify"))
        XCTAssertEqual(model.status, .cardAdded)
        XCTAssertEqual(model.account, .seed)
        XCTAssertTrue(model.status.canContinue(model.amount))
    }

    @MainActor func test_a_card_the_bank_refused_or_did_not_confirm_leaves_the_top_up_open() {
        let model = InvestModel(load: { .seed }, save: { _ in XCTFail("nothing to save") }, clock: { 1000 })
        model.onCardOutcome(.declined(message: "Issuer declined"))
        XCTAssertEqual(model.status, .cardDeclined(message: "Issuer declined"))
        XCTAssertTrue(model.status.canContinue(model.amount))
        model.onCardOutcome(.unconfirmed(paymentId: "md-verify"))
        XCTAssertEqual(model.status, .cardUnconfirmed)
        XCTAssertTrue(model.status.canContinue(model.amount))
        model.onCardOutcome(.cancelled)
        XCTAssertEqual(model.status, .editing)
    }

    func test_an_open_business_stays_only_while_the_mode_still_shows_it() {
        XCTAssertTrue(coverFits(.invest, aftDemo: true))
        XCTAssertFalse(coverFits(.invest, aftDemo: false))
        XCTAssertFalse(coverFits(.tickets, aftDemo: true))
        XCTAssertTrue(coverFits(.tickets, aftDemo: false))
    }

    func test_back_from_the_settings_in_the_other_mode_leaves_the_business() {
        XCTAssertEqual(pathFitting([.cover(.invest)], aftDemo: false), [])
        XCTAssertEqual(pathFitting([.cover(.coffee), .result(.coffee)], aftDemo: true), [])
        XCTAssertEqual(pathFitting([.cover(.invest), .developer], aftDemo: false), [.cover(.invest), .developer])
        XCTAssertEqual(pathFitting([.cover(.invest)], aftDemo: true), [.cover(.invest)])
        XCTAssertEqual(pathFitting([.developer], aftDemo: true), [.developer])
    }
}
