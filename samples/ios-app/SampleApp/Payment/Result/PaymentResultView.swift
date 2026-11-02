import SwiftUI
import sdk_payment_scanner

struct PaymentResultView: View {
    @EnvironmentObject var pathHolder: PathHolder

    var body: some View {
        let captured = LastPaymentResult.readAndClear()

        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                NavTopBar(title: "Result", onBack: nil)
                VStack(spacing: 14) {
                    if let (result, duration, method) = captured {
                        let summary = Self.summary(of: result)
                        let rows: [(String, String)] = [
                            ("Status", summary.status),
                            ("Payment ID", result.orderId),
                            ("Method", method),
                            ("Duration", String(format: "%.0fms", duration * 1000)),
                        ]
                        ResultHero(
                            isSuccess: result is CheckoutResultCompleted,
                            title: summary.title,
                            subtitle: summary.subtitle,
                            rows: rows
                        )
                    } else {
                        ResultHero(isSuccess: false, title: "No result",
                                   subtitle: "Start a payment first", rows: [])
                    }
                    PrimaryCta(text: "Start another payment") {
                        pathHolder.popToPaymentRoot()
                    }
                }.padding(16)
            }
        }
        .navigationBarHidden(true)
        .background(Color(red: 0.949, green: 0.957, blue: 0.973))
    }

    private static func summary(of result: CheckoutResult) -> (status: String, title: String, subtitle: String) {
        switch onEnum(of: result) {
        case .completed: return ("APPROVED", "Payment approved", "Completed")
        case .canceled: return ("CANCELED", "Payment cancelled", "The buyer closed the checkout")
        case .failed(let failed): return ("DECLINED", "Payment failed", failed.error.message ?? "Failed")
        case .unknown: return ("UNKNOWN", "Outcome unknown — check the order status", "The checkout could not learn how the payment ended")
        }
    }
}
