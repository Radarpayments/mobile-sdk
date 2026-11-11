import PassKit
import SwiftUI

/// Apple's own "Buy with Apple Pay" button in the checkout's corners: white on dark themes, black on light ones.
struct ApplePayButton: View {
    let action: () -> Void

    @Environment(\.checkoutTheme) private var theme

    var body: some View {
        NativePayButton(dark: theme.isDark, radius: theme.buttonCorner, action: action)
            .id(theme.isDark)
    }
}

private struct NativePayButton: UIViewRepresentable {
    let dark: Bool
    let radius: CGFloat
    let action: () -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(action: action)
    }

    func makeUIView(context: Context) -> PKPaymentButton {
        let button = PKPaymentButton(paymentButtonType: .buy, paymentButtonStyle: dark ? .white : .black)
        button.cornerRadius = radius
        button.addTarget(context.coordinator, action: #selector(Coordinator.tapped), for: .touchUpInside)
        return button
    }

    func updateUIView(_ button: PKPaymentButton, context: Context) {
        button.cornerRadius = radius
        context.coordinator.action = action
    }

    final class Coordinator: NSObject {
        var action: () -> Void

        init(action: @escaping () -> Void) {
            self.action = action
        }

        @objc func tapped() {
            action()
        }
    }
}
