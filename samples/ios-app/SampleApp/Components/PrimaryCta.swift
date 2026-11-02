import SwiftUI

/// Solid primary CTA (#0284C7). `enabled = false` fades the button and swallows taps.
struct PrimaryCta: View {
    let text: String
    let action: () -> Void
    var enabled: Bool = true

    var body: some View {
        Button(action: action) {
            Text(text)
                .font(.system(size: 15, weight: .bold))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(Color(red: 0.008, green: 0.518, blue: 0.780))
                .cornerRadius(12)
                .opacity(enabled ? 1 : 0.5)
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}
