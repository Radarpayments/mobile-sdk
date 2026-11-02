import UIKit

/// Why `PayrdrCheckout.present(mdOrder:from:)` returned without a result.
public enum PayrdrCheckoutError: Error {
    /// Nothing opened: a checkout of this object is already open, or `from` is not on screen.
    case notPresented
}

extension PayrdrCheckout {
    /// Opens the checkout and returns its result once the sheet has closed.
    ///
    /// Cancelling the calling task closes the sheet; the result then says what the checkout had reached (a cancel,
    /// or unknown if a payment was already sent — check the order on your server).
    ///
    /// - Throws: `PayrdrCheckoutError.notPresented` when nothing opened.
    @MainActor
    public func present(mdOrder: String, from: UIViewController) async throws -> CheckoutResult {
        try await withTaskCancellationHandler {
            try await withCheckedThrowingContinuation { continuation in
                let opened = present(mdOrder: mdOrder, from: from) { result in
                    continuation.resume(returning: result)
                }
                if !opened { continuation.resume(throwing: PayrdrCheckoutError.notPresented) }
            }
        } onCancel: {
            Task { @MainActor in self.dismiss() }
        }
    }
}
