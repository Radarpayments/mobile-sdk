import XCTest
@testable import SampleApp

final class InvestAccountTests: XCTestCase {
    private let seed = InvestAccount.seed
    private let now: Int64 = 1_793_280_000_000

    func test_seed_totals_4812_40() {
        XCTAssertEqual(seed.totalMinor, 481_240)
        XCTAssertEqual(seed.freeCashMinor, 1240)
    }

    func test_credit_adds_to_free_cash_and_prepends_a_top_up() {
        let account = seed.credit(orderId: "md-1", amountMinor: 10000, at: now)
        XCTAssertEqual(account.freeCashMinor, 11240)
        XCTAssertEqual(account.totalMinor, 491_240)
        XCTAssertEqual(account.operations.first, InvestOperation(kind: .topUp, amountMinor: 10000, atEpochMillis: now, orderId: "md-1"))
    }

    func test_credit_twice_with_the_same_order_credits_once() {
        let account = seed.credit(orderId: "md-1", amountMinor: 10000, at: now).credit(orderId: "md-1", amountMinor: 10000, at: now + 1)
        XCTAssertEqual(account.freeCashMinor, 11240)
        XCTAssertEqual(account.operations.count, seed.operations.count + 1)
    }

    func test_invest_moves_cash_into_GLB() {
        let account = seed.credit(orderId: "md-1", amountMinor: 10000, at: now).invest(amountMinor: 10000, at: now + 1)
        XCTAssertEqual(account.freeCashMinor, 1240)
        XCTAssertEqual(account.holdings.first { $0.ticker == "GLB" }?.valueMinor, 300_000)
        XCTAssertEqual(account.totalMinor, 491_240)
        XCTAssertEqual(account.operations.first, InvestOperation(kind: .buy, amountMinor: 10000, atEpochMillis: now + 1, ticker: "GLB"))
    }

    func test_invest_more_than_free_cash_invests_all_free_cash() {
        let account = seed.invest(amountMinor: 1_000_000, at: now)
        XCTAssertEqual(account.freeCashMinor, 0)
        XCTAssertEqual(account.holdings.first { $0.ticker == "GLB" }?.valueMinor, 291_240)
    }

    func test_invest_with_under_one_euro_does_nothing() {
        var poor = seed
        poor.freeCashMinor = 99
        XCTAssertFalse(poor.canInvest)
        XCTAssertEqual(poor.invest(amountMinor: 99, at: now), poor)
        XCTAssertTrue(seed.canInvest)
    }

    func test_recent_operations_are_newest_first_and_capped() {
        var account = seed
        for i in 0..<6 { account = account.credit(orderId: "md-\(i)", amountMinor: 1000, at: now + Int64(i)) }
        XCTAssertEqual(account.recent().count, 5)
        XCTAssertEqual(account.recent().first?.orderId, "md-5")
    }

    func test_seed_operations_are_a_buy_and_a_top_up_newest_first() {
        XCTAssertEqual(seed.operations.map(\.kind), [.buy, .topUp])
        XCTAssertEqual(seed.operations.map(\.amountMinor), [25000, 25000])
    }

    func test_json_round_trip() {
        let account = seed.credit(orderId: "md-1", amountMinor: 10000, at: now).invest(amountMinor: 5000, at: now + 1)
        XCTAssertEqual(InvestAccount.fromJson(account.toJson()), account)
    }

    func test_corrupt_json_gives_the_seed() {
        for json in [nil, "", "{", #"{"freeCashMinor":"x"}"#, "[]", #"{"freeCashMinor":1}"#] {
            XCTAssertEqual(InvestAccount.fromJson(json), seed, "json: \(String(describing: json))")
        }
    }
}
