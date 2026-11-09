import SwiftUI

/// The store's own screen after the sheet closes: the merchant app, not the SDK, decides what comes next.
/// The coffee shop shows its order number instead of a mark.
struct StoreResultView: View {
    let cover: CoverId
    let receipt: StoreModel.Receipt
    let orderNumber: Int
    let onDone: () -> Void
    let onRetry: () -> Void

    var body: some View {
        let style = coverStyle(cover)
        VStack(spacing: 0) {
            Spacer()
            if case let .success(paymentId) = receipt.outcome {
                if cover == .coffee {
                    Text("Your order is being prepared").font(style.body(17, .bold)).foregroundColor(style.muted)
                    Text("\(orderNumber)").font(style.heading(88, .heavy)).foregroundColor(style.text)
                } else {
                    mark(ok: true, tint: style.accent)
                    Text(successTitle).font(style.heading(30, .bold)).foregroundColor(style.text)
                        .multilineTextAlignment(.center).padding(.top, 18)
                }
                Text(successBody).font(style.body(15)).foregroundColor(style.muted).multilineTextAlignment(.center).padding(.top, 8)
                VStack(spacing: 6) {
                    row(String(localized: "Amount", bundle: AppLanguage.bundle), formatEur(receipt.amountMinor), style: style, mono: false)
                    row(String(localized: "result_payment_id", defaultValue: "Payment", bundle: AppLanguage.bundle), paymentId, style: style, mono: true)
                }
                .padding(14)
                .overlay(RoundedRectangle(cornerRadius: style.radius).strokeBorder(style.border))
                .padding(.top, 24)
            } else if case .unconfirmed = receipt.outcome {
                Image(systemName: "clock")
                    .font(.system(size: 28, weight: .semibold))
                    .foregroundColor(style.muted)
                    .frame(width: 72, height: 72)
                    .overlay(Circle().strokeBorder(style.muted, lineWidth: 1.5))
                Text(String(localized: "Checking your payment", bundle: AppLanguage.bundle))
                    .font(style.heading(28, .bold)).foregroundColor(style.text).multilineTextAlignment(.center).padding(.top, 18)
                Text(String(localized: "The bank has not answered yet. Please don’t pay again — we’ll let you know as soon as the payment is confirmed.",
                            bundle: AppLanguage.bundle))
                    .font(style.body(15)).foregroundColor(style.muted).multilineTextAlignment(.center).padding(.top, 8)
            } else {
                mark(ok: false, tint: Color(argb: 0xFFDC_2626))
                Text("Payment declined").font(style.heading(28, .bold)).foregroundColor(style.text).padding(.top, 18)
                Text("The bank did not confirm the payment.").font(style.body(15)).foregroundColor(style.muted).padding(.top, 8)
                if case let .declined(message?) = receipt.outcome {
                    Text(message).font(style.body(12)).foregroundColor(style.muted).multilineTextAlignment(.center).padding(.top, 12)
                }
            }
            Spacer()
            // Paying again while the first payment may have gone through could charge the buyer twice.
            if receipt.outcome.offersRetry {
                PayButton(cover: cover, label: String(localized: "Try again", bundle: AppLanguage.bundle), busy: false, enabled: true, action: onRetry)
                    .padding(.bottom, 10)
            }
            Button(action: onDone) {
                Text(receipt.outcome.isSuccess ? "Done" : "Back to the store")
                    .font(style.body(16, .bold))
                    .foregroundColor(style.text)
                    .frame(maxWidth: .infinity, minHeight: 52)
                    .overlay(style.buttonShape.stroke(style.border))
            }
            .buttonStyle(.plain)
        }
        .padding(20)
        .background(style.background.ignoresSafeArea())
        .preferredColorScheme(style.dark ? .dark : .light)
        .navigationBarBackButtonHidden()
    }

    private var successTitle: String {
        switch cover {
        case .furniture: return String(localized: "Thank you for your order", bundle: AppLanguage.bundle)
        case .coffee: return String(localized: "Your order is being prepared", bundle: AppLanguage.bundle)
        case .tickets: return String(localized: "You are going!", bundle: AppLanguage.bundle)
        case .invest: return String(localized: "Deposit", bundle: AppLanguage.bundle)
        }
    }

    private var successBody: String {
        switch cover {
        case .furniture: return String(localized: "We will be in touch to arrange delivery.", bundle: AppLanguage.bundle)
        case .coffee: return String(localized: "Show this number to the barista.", bundle: AppLanguage.bundle)
        case .tickets: return String(localized: "Tickets are in your account.", bundle: AppLanguage.bundle)
        case .invest: return String(localized: "AFT · to own account", bundle: AppLanguage.bundle)
        }
    }

    private func mark(ok: Bool, tint: Color) -> some View {
        Image(systemName: ok ? "checkmark" : "xmark")
            .font(.system(size: 28, weight: .semibold))
            .foregroundColor(tint)
            .frame(width: 72, height: 72)
            .overlay(Circle().strokeBorder(tint, lineWidth: 1.5))
    }

    private func row(_ label: String, _ value: String, style: CoverStyle, mono: Bool) -> some View {
        HStack {
            Text(label).font(style.body(13)).foregroundColor(style.muted)
            Spacer()
            Text(value).font(mono ? .system(size: 12, design: .monospaced) : style.body(13, .medium)).foregroundColor(style.text)
        }
    }
}

extension CheckoutOutcome {
    var isSuccess: Bool { if case .success = self { return true } else { return false } }

    /// Only after a decline: not after a payment, and not while one may still be going through.
    var offersRetry: Bool { if case .declined = self { return true } else { return false } }
}
