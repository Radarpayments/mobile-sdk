import SwiftUI

/// A small sheet a step raises over the whole checkout, with a scrim under it.
struct SheetOverlayItem: Equatable {
    let id: String
    let onDismiss: () -> Void
    let content: AnyView

    static func == (lhs: SheetOverlayItem, rhs: SheetOverlayItem) -> Bool {
        lhs.id == rhs.id
    }
}

private struct SheetOverlayKey: PreferenceKey {
    static let defaultValue: SheetOverlayItem? = nil

    static func reduce(value: inout SheetOverlayItem?, nextValue: () -> SheetOverlayItem?) {
        value = nextValue() ?? value
    }
}

extension View {
    /// Raises `content` over the checkout while `id` is not nil. The steps are clipped to the sheet, so the
    /// overlay is drawn by `sheetOverlayHost()` on the checkout screen.
    func sheetOverlay<Content: View>(id: String?, onDismiss: @escaping () -> Void, @ViewBuilder content: () -> Content) -> some View {
        preference(
            key: SheetOverlayKey.self,
            value: id.map { SheetOverlayItem(id: $0, onDismiss: onDismiss, content: AnyView(content())) }
        )
    }

    /// Takes the content out of VoiceOver's reach while something covers it. `accessibilityHidden(false)` on a
    /// stack would override the `accessibilityHidden(true)` of the views inside, and `accessibilityHidden(true)`
    /// misses its UIKit fields, so the stack becomes one container that drops its children when covered.
    func coveredFromVoiceOver(_ covered: Bool) -> some View {
        accessibilityElement(children: covered ? .ignore : .contain)
            .accessibilityHidden(covered)
    }

    /// Draws the overlay sheets of the views inside over this view.
    func sheetOverlayHost(reduceMotion: Bool) -> some View {
        overlayPreferenceValue(SheetOverlayKey.self) { item in
            SheetOverlayLayer(item: item, reduceMotion: reduceMotion)
        }
    }
}

private struct SheetOverlayLayer: View {
    let item: SheetOverlayItem?
    let reduceMotion: Bool

    @State private var shown: SheetOverlayItem?
    @State private var visible = false
    @State private var sheetHeight: CGFloat = 0
    @State private var changes = 0
    @Environment(\.checkoutTheme) private var theme

    private static let enterDuration = 0.28
    private static let exitDuration = 0.22

    var body: some View {
        ZStack(alignment: .bottom) {
            if let shown = shown {
                theme.colors.scrim
                    .opacity(visible ? 1 : 0)
                    .edgesIgnoringSafeArea(.all)
                    .onTapGesture(perform: shown.onDismiss)
                    .accessibilityHidden(true)
                OverlaySheet(onDismiss: shown.onDismiss, onHeight: { sheetHeight = $0 }) { shown.content }
                    .offset(y: visible || reduceMotion ? 0 : max(sheetHeight, 600))
                    .opacity(visible || !reduceMotion ? 1 : 0)
                    .accessibilityAddTraits(.isModal)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
        .allowsHitTesting(item != nil)
        .onAppear { change(to: item) }
        .onChange(of: item) { change(to: $0) }
    }

    private func change(to item: SheetOverlayItem?) {
        changes += 1
        if let item = item {
            shown = item
            DispatchQueue.main.async {
                withAnimation(SheetMotion.content(reduceMotion ? SheetMotion.reducedEnterDuration : Self.enterDuration)) { visible = true }
            }
        } else if shown != nil {
            let duration = reduceMotion ? SheetMotion.reducedExitDuration : Self.exitDuration
            withAnimation(SheetMotion.content(duration)) { visible = false }
            // `onChange` runs the closure of the render before the change, whose `item` is still the sheet; the count
            // tells whether the sheet was opened again meanwhile. Left in place under the screen, its shadow showed.
            let change = changes
            DispatchQueue.main.asyncAfter(deadline: .now() + duration) {
                if changes == change { shown = nil }
            }
        }
    }
}

private struct OverlaySheet<Content: View>: View {
    let onDismiss: () -> Void
    let onHeight: (CGFloat) -> Void
    @ViewBuilder let content: () -> Content

    @State private var drag: CGFloat = 0
    @Environment(\.checkoutTheme) private var theme
    @EnvironmentObject private var insets: SheetInsets

    var body: some View {
        VStack(spacing: 14) {
            SheetGrabber().onTapGesture(perform: onDismiss)
            VStack(alignment: .leading, spacing: 14) { content() }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.bottom, insets.sheetBottom)
        }
        .padding(.horizontal, sidePadding)
        .padding(.top, 8)
        .padding(.bottom, 12)
        .background(SheetBackground(corner: theme.sheetCorner))
        .background(GeometryReader { proxy in Color.clear.preference(key: OverlayHeightKey.self, value: proxy.size.height) })
        .onPreferenceChange(OverlayHeightKey.self, perform: onHeight)
        .offset(y: max(drag, 0))
        .gesture(
            DragGesture(minimumDistance: 4)
                .onChanged { drag = $0.translation.height }
                .onEnded { value in
                    let velocity = (value.predictedEndTranslation.height - value.translation.height) * 4
                    if value.translation.height > 120 || velocity > 2400 / UIScreen.main.scale { onDismiss() }
                    withAnimation(SheetMotion.height()) { drag = 0 }
                }
        )
    }
}

private struct OverlayHeightKey: PreferenceKey {
    static let defaultValue: CGFloat = 0

    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = max(value, nextValue())
    }
}
