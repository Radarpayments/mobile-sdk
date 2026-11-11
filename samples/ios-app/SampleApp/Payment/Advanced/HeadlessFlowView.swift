import SwiftUI
import RadarpaymentsSDKPayment

struct HeadlessFlowView: View {
    @EnvironmentObject var store: SampleStore
    @Environment(\.dismiss) private var dismiss
    @State private var trace: [String] = []

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                NavTopBar(title: "Headless flow", onBack: { dismiss() })
                VStack(alignment: .leading, spacing: 12) {
                    PrimaryCta(text: "Start headless flow") {
                        trace.removeAll()
                        Task {
                            // A fresh order each run: an mdOrder can be paid only once.
                            let registration = await PrefsRegistrar().register(amountMinor: 100, clientId: nil)
                            guard case let .registered(freshOrder, sessionId) = registration else {
                                trace = ["Order not registered: \(registration)"]
                                return
                            }
                            let host = NoopWebHost()
                            // An API V2 session opens by its id, an API V1 order by its mdOrder.
                            let checkout: any CheckoutConfig = sessionId.map { CheckoutConfigSessionId(value: $0) as any CheckoutConfig }
                                ?? CheckoutConfigMdOrder(value: freshOrder)
                            // A Kotlin Flow is an AsyncSequence in Swift.
                            let states = SdkPayment.companion.create(config: store.config()).paymentFlow(
                                checkout: checkout,
                                webChallengeHost: host,
                                scope: IosCoroutineScopeKt.iosMainScope()
                            )
                            for await state in states {
                                trace.append("\(type(of: state))")
                            }
                        }
                    }
                    VStack(alignment: .leading, spacing: 6) {
                        Text("PaymentState trace").foregroundColor(.white).font(.system(size: 11, weight: .bold))
                        if trace.isEmpty {
                            Text("(no events yet)").foregroundColor(Color(white: 0.4)).font(.system(size: 10))
                        } else {
                            ForEach(Array(trace.enumerated()), id: \.offset) { (i, s) in
                                Text("\(i + 1). \(s)")
                                    .font(.system(size: 11, design: .monospaced))
                                    .foregroundColor(Color(red: 0.647, green: 0.706, blue: 0.988))
                            }
                        }
                    }
                    .padding(12).frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(red: 0.043, green: 0.071, blue: 0.125)).cornerRadius(10)
                }.padding(16)
            }
        }
        .navigationBarHidden(true)
        .background(Color(red: 0.949, green: 0.957, blue: 0.973))
    }
}

/// Swallow-all 3DS host for headless demo — does not present any UI. If a challenge is
/// raised, we cancel it so the flow terminates cleanly.
private final class NoopWebHost: NSObject, WebChallengeHost {
    // SKIE exposes the Kotlin suspend function to Swift implementers under this name.
    func __performChallenge(
        challenge: ThreeDsChallenge,
        completionHandler: @escaping @Sendable ((any ThreeDsResult)?, (any Error)?) -> Void
    ) {
        completionHandler(ThreeDsResultCancelled(), nil)
    }
}
