import SwiftUI
import UIKit

extension View {
    /// Opens the payment sheet for `mdOrder` when `isPresented` becomes `true`, and sets it back to `false` once the
    /// sheet has closed, whatever the outcome. Setting `isPresented` to `false` while it is open closes it; `onResult`
    /// then gets what the checkout had reached.
    ///
    /// ```swift
    /// Button("Pay") { paying = true }
    ///     .payrdrCheckout(isPresented: $paying, mdOrder: mdOrder, config: config) { result in
    ///         switch onEnum(of: result) { … }
    ///     }
    /// ```
    ///
    /// The sheet is presented with UIKit over the current screen (in a SwiftUI `.sheet` it would be drawn as a
    /// full-height page with empty space under the short payment sheet).
    public func payrdrCheckout(
        isPresented: Binding<Bool>,
        mdOrder: String,
        config: SdkPaymentConfig,
        onResult: @escaping (CheckoutResult) -> Void
    ) -> some View {
        background(PayrdrCheckoutAnchor(isPresented: isPresented, mdOrder: mdOrder, config: config, onResult: onResult))
    }
}

/// An invisible controller in the view's place: the checkout is presented from it.
private struct PayrdrCheckoutAnchor: UIViewControllerRepresentable {
    @Binding var isPresented: Bool
    let mdOrder: String
    let config: SdkPaymentConfig
    let onResult: (CheckoutResult) -> Void

    final class Coordinator {
        var checkout: PayrdrCheckout?
    }

    func makeCoordinator() -> Coordinator { Coordinator() }

    func makeUIViewController(context: Context) -> UIViewController { UIViewController() }

    func updateUIViewController(_ anchor: UIViewController, context: Context) {
        let coordinator = context.coordinator
        if !isPresented {
            // Closed from outside while open: close the sheet too (its result still comes).
            coordinator.checkout?.dismiss()
            return
        }
        // Once per opening: a later update while the sheet is up opens nothing.
        guard coordinator.checkout == nil else { return }
        let checkout = PayrdrCheckout(config: config)
        coordinator.checkout = checkout
        let isPresented = $isPresented
        let onResult = onResult
        let mdOrder = mdOrder
        // Not during a view update; the anchor is in the window by then.
        DispatchQueue.main.async {
            let opened = checkout.present(mdOrder: mdOrder, from: anchor) { result in
                coordinator.checkout = nil
                isPresented.wrappedValue = false
                onResult(result)
            }
            if !opened {
                // Nothing on screen (the view is not in a window yet): back to closed, so it can open later.
                coordinator.checkout = nil
                isPresented.wrappedValue = false
            }
        }
    }
}
