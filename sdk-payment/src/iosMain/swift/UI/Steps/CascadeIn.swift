import SwiftUI

/// The `index`-th line of the receipt comes in after the mark: it fades in, rising a little. Disabled, it stands.
private struct CascadeIn: ViewModifier {
    let index: Int
    let enabled: Bool

    @State private var shown = false

    func body(content: Content) -> some View {
        let progress: CGFloat = !enabled || shown ? 1 : 0
        return content
            .opacity(Double(progress))
            .offset(y: SheetMotion.cascadeRise * (1 - progress))
            .onAppear {
                guard enabled else { return }
                withAnimation(SheetMotion.mark(SheetMotion.cascadeDuration).delay(SheetMotion.cascadeDelay(index))) { shown = true }
            }
    }
}

extension View {
    func cascadeIn(_ index: Int, enabled: Bool) -> some View { modifier(CascadeIn(index: index, enabled: enabled)) }
}
