import SwiftUI

private struct PlaceholderKey: EnvironmentKey {
    static let defaultValue = false
}

extension EnvironmentValues {
    /// True inside a skeleton: the same layout draws blocks instead of content and takes no taps.
    var isPlaceholder: Bool {
        get { self[PlaceholderKey.self] }
        set { self[PlaceholderKey.self] = newValue }
    }
}

/// A block of a skeleton; the caller gives it its size.
struct PlaceholderBlock: View {
    var corner: CGFloat = 6

    @Environment(\.checkoutTheme) private var theme

    var body: some View {
        RoundedRectangle(cornerRadius: corner, style: .circular).fill(theme.colors.surface)
    }
}

private struct PlaceholderText: ViewModifier {
    @Environment(\.isPlaceholder) private var isPlaceholder
    @Environment(\.checkoutTheme) private var theme

    func body(content: Content) -> some View {
        if isPlaceholder {
            content
                .opacity(0)
                .background(RoundedRectangle(cornerRadius: 6, style: .circular).fill(theme.colors.surface))
        } else {
            content
        }
    }
}

extension View {
    /// In a skeleton, a block exactly the size of this text in its place.
    func placeholderText() -> some View {
        modifier(PlaceholderText())
    }
}
