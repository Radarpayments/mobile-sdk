import Foundation
import sdk_payment_scanner

/// Process-scoped capture of the most recent payment outcome, consumed once by PaymentResultView.
enum LastPaymentResult {
    static var value: CheckoutResult?
    static var startedAt: Date?
    static var method: String = "CARD"

    static func begin(method: String) {
        self.value = nil
        self.startedAt = Date()
        self.method = method
    }

    static func set(_ r: CheckoutResult) { value = r }

    static func readAndClear() -> (CheckoutResult, TimeInterval, String)? {
        guard let v = value else { return nil }
        let dur = startedAt.map { Date().timeIntervalSince($0) } ?? 0
        let m = method
        value = nil
        return (v, dur, m)
    }
}
