import Combine
import SwiftUI
import UIKit
import sdk_payment_scanner

// Everything this demo does with the payment SDK lives in this file. To take payments in your app:
//
//   1. Describe the checkout:         SdkPaymentConfig(baseUrl: ..., theme: ..., applePay: ...)
//   2. Get the order.                 Your backend registers it at the gateway — an mdOrder through API V1
//      (register.do), a session id through API V2 (POST /api2/sessions); this demo does it in-app through
//      OrderRegistrar only so that it runs without a backend.
//   3. Open the payment sheet:        PayrdrCheckout(config:).present(mdOrder:from:) { result in ... }
//                                     or .presentSession(sessionId:from:) { result in ... }
//                                     (SwiftUI: .payrdrCheckout(isPresented:mdOrder:config:) { result in ... })
//   4. Handle the result:             completed, failed, canceled or unknown — see outcome(of:).

/// What the store does after the payment sheet closes.
enum CheckoutOutcome: Equatable {
    /// Paid; `paymentId` identifies the payment at the gateway.
    case success(paymentId: String)
    /// The bank or the gateway refused the payment.
    case declined(message: String?)
    /// The buyer closed the sheet.
    case cancelled
    /// The SDK could not learn how the payment ended; the store must not offer to pay again until it knows.
    case unconfirmed(paymentId: String)
}

/// Where the store's "Pay" button is in the payment path.
enum CheckoutState: Equatable {
    case idle, registering, inSheet
    case registrationFailed(OrderRegistration)
}

/// Step 1: the SDK config for the current demo panel, cover theme and developer settings.
func buildSdkConfig(baseUrl: String, scenario: Scenario, coverTheme: any PaymentTheme, themeOverride: SheetThemeOverride,
                    locale: String?, applePayMerchantId: String) -> SdkPaymentConfig {
    let theme: any PaymentTheme
    switch themeOverride {
    case .cover: theme = coverTheme
    case .light: theme = PaymentThemeLight.shared
    case .dark: theme = PaymentThemeDark.shared
    case .system: theme = PaymentThemeSystem.shared
    }
    // Country and currency come from the order.
    let applePay = ApplePayConfig(merchantId: applePayMerchantId)
    return SdkPaymentConfig(
        baseUrl: baseUrl,
        locale: locale,
        theme: theme,
        applePay: scenario.wallet ? applePay : nil,
        cardScannerProvider: scenario.scanner ? cardScanner : nil,
        showResultScreen: scenario.sdkResultScreen
    )
}

// One provider for every config.
private let cardScanner = MlKitVisionCardScannerProvider(logTransport: nil)

/// Step 4: turns the SDK result into what the store shows next.
func outcome(of result: CheckoutResult) -> CheckoutOutcome {
    switch onEnum(of: result) {
    case .completed(let completed): return .success(paymentId: completed.orderId)
    case .canceled: return .cancelled
    case .failed(let failed): return .declined(message: failed.error.message)
    // How the payment ended is not known yet. A real store checks the order on its server (or waits for the
    // gateway's callback) before telling the buyer anything, and never offers to pay again meanwhile.
    case .unknown(let unknown): return .unconfirmed(paymentId: unknown.orderId)
    }
}

/// Steps 2 and 3 for the store's "Pay" button: registers the order, then opens the sheet. A second tap
/// while the order is being registered or the sheet is open does nothing.
@MainActor
final class StoreCheckout: ObservableObject {
    @Published private(set) var state: CheckoutState = .idle
    private let registrar: OrderRegistrar
    private let present: (_ mdOrder: String, _ sessionId: String?) -> Void
    private let onMdOrder: (String) -> Void
    private var task: Task<Void, Never>?

    /// `present` opens the sheet for a registered order: its session when there is one (API V2), its mdOrder otherwise.
    /// `onMdOrder` gets the mdOrder of every registered order, API V2 sessions included.
    init(registrar: OrderRegistrar, present: @escaping (_ mdOrder: String, _ sessionId: String?) -> Void,
         onMdOrder: @escaping (String) -> Void = { _ in }) {
        self.registrar = registrar
        self.present = present
        self.onMdOrder = onMdOrder
    }

    /// Pays `amountMinor` euro cents; `clientId` = the demo buyer with saved cards, nil = guest.
    func pay(amountMinor: Int64, clientId: String?) {
        guard state != .registering, state != .inSheet else { return }
        state = .registering
        task = Task {
            switch await registrar.register(amountMinor: amountMinor, clientId: clientId) {
            case let .registered(mdOrder, sessionId):
                onMdOrder(mdOrder)
                state = .inSheet
                present(mdOrder, sessionId)
            case let other:
                state = .registrationFailed(other)
            }
        }
    }

    /// The sheet returned a result.
    func sheetClosed() { state = .idle }

    /// Forgets a registration error, e.g. once the buyer comes back from the settings.
    func resetError() {
        if case .registrationFailed = state { state = .idle }
    }

    /// Waits for the current registration to finish (tests).
    func waitUntilSettled() async { await task?.value }
}

/// Step 3 from UIKit: the checkout opens over whatever the key window shows and closes itself. An API V2 order
/// opens by its `sessionId`, an API V1 order by its `mdOrder`.
@MainActor
func presentCheckout(config: SdkPaymentConfig, mdOrder: String, sessionId: String?,
                     onResult: @escaping (CheckoutResult) -> Void) {
    let opened = UIApplication.shared.keyWindowRoot.map { root in
        let checkout = PayrdrCheckout(config: config)
        if let sessionId { return checkout.presentSession(sessionId: sessionId, from: root, onResult: onResult) }
        return checkout.present(mdOrder: mdOrder, from: root, onResult: onResult)
    } ?? false
    // Nothing on screen to present over: the store treats it as a cancel and can try again.
    if !opened { onResult(CheckoutResultCanceled(orderId: mdOrder)) }
}

extension UIApplication {
    /// The root controller of the foreground key window.
    var keyWindowRoot: UIViewController? {
        connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)?
            .rootViewController
    }
}
