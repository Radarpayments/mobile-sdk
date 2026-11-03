import SwiftUI
import sdk_payment_scanner

struct AdvancedView: View {
    @EnvironmentObject var store: SampleStore
    @EnvironmentObject var pathHolder: PathHolder
    @AppStorage(AppPrefsKeys.mdOrder) var mdOrder: String = AppPrefsDefaults.mdOrder
    @Environment(\.dismiss) private var dismiss
    @State private var trigger = false
    // The pinning demo's own config: the rest of the app keeps paying without the broken pin.
    @State private var pinning: SslPinningConfig?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                NavTopBar(title: "Advanced", onBack: { dismiss() })
                VStack(alignment: .leading, spacing: 10) {
                    InfoTile(title: "🔤 Custom button text",
                             message: "buttonText config is not in 4.x SdkPaymentConfig. Control happens gateway-side.")
                    NavigationLink(value: SampleRoute.paymentHeadless) {
                        ActionTile(title: "🔀 Headless payment flow",
                                   message: "Sample collects PaymentState stream and renders FSM states live.",
                                   buttonText: "Open")
                    }.buttonStyle(.plain)
                    SslPinningTile(mdOrder: mdOrder, pinning: $pinning, trigger: $trigger)
                }
                .padding(16)
            }
        }
        .navigationBarHidden(true)
        .background(Color(red: 0.949, green: 0.957, blue: 0.973))
        .payrdrCheckout(isPresented: $trigger, mdOrder: mdOrder, config: store.config(sslPinning: pinning)) { r in
            LastPaymentResult.set(r)
            pathHolder.push(.paymentResult)
        }
    }
}

private struct InfoTile: View {
    let title: String
    let message: String
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title).font(.system(size: 12, weight: .bold)).foregroundColor(Color(white: 0.34))
            Text(message).font(.system(size: 10)).foregroundColor(Color(white: 0.42))
        }.padding(14).background(Color(white: 0.95)).cornerRadius(12)
    }
}

private struct ActionTile: View {
    let title: String
    let message: String
    let buttonText: String
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title).font(.system(size: 13, weight: .bold))
            Text(message).font(.system(size: 10)).foregroundColor(Color(white: 0.42))
            Text(buttonText).font(.system(size: 12, weight: .bold)).foregroundColor(.white)
                .frame(maxWidth: .infinity).padding(.vertical, 10)
                .background(BrandGradient.ctaPrimary.linear()).cornerRadius(10)
        }.padding(14).background(Color.white).cornerRadius(12)
    }
}

private struct SslPinningTile: View {
    let mdOrder: String
    @Binding var pinning: SslPinningConfig?
    @Binding var trigger: Bool
    @EnvironmentObject var store: SampleStore

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 6) {
                Image(systemName: "lock")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(Color(white: 0.1))
                Text("SSL pinning (hash invalid — intentional)")
                    .font(.system(size: 13, weight: .bold))
            }
            Text("Pin is a placeholder — sample demonstrates fail-fast behavior on mismatch.")
                .font(.system(size: 10)).foregroundColor(Color(white: 0.42))
            PrimaryCta(text: "Buy (expect pin error)") {
                let host = URL(string: store.config().baseUrl)?.host ?? "dev.bpcbt.com"
                pinning = SslPinningConfig.companion.builder()
                    .addPin(hostname: host, sha256Pin: "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
                    .build()
                LastPaymentResult.begin(method: "CARD")
                trigger = true
            }
        }.padding(14).background(Color.white).cornerRadius(12)
    }
}
