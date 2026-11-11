import SwiftUI

/// The details of a payment that went through: lines with hairlines between them and no frame.
struct ReceiptView: View {
    let lines: [ReceiptLineSpec]
    let cascade: Bool

    @Environment(\.checkoutTheme) private var theme

    var body: some View {
        VStack(spacing: 0) {
            ForEach(lines.indices, id: \.self) { index in
                VStack(spacing: 0) {
                    if index > 0 {
                        theme.colors.outline.frame(height: 1)
                    }
                    ReceiptLine(line: lines[index])
                }
                // After the title (0) and the amount (1).
                .cascadeIn(index + 2, enabled: cascade)
            }
        }
        .frame(maxWidth: .infinity)
    }
}

private struct ReceiptLine: View {
    let line: ReceiptLineSpec

    @Environment(\.checkoutTheme) private var theme

    var body: some View {
        HStack(spacing: 12) {
            Text(line.label)
                .line(theme.label.weight(theme.body.weight), color: theme.colors.onSurfaceVariant)
            HStack(spacing: 8) {
                Spacer(minLength: 0)
                if line.card {
                    NetworkLogoView(logo: line.logo)
                }
                Text(line.value)
                    .line(theme.label.tabular(), color: theme.colors.onBackground)
            }
            .frame(maxWidth: .infinity)
        }
        .padding(.vertical, 11)
        .accessibilityElement(children: .combine)
    }
}
