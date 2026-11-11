import SwiftUI

/// A card step in the sheet: the header, the scrolling fields, and the Pay button under a hairline. While the keyboard
/// is open the footer is hidden so the fields keep the room, unless `footerOverKeyboard`.
struct FormScaffold<Header: View, Content: View>: View {
    var footerOverKeyboard = false
    let payLabel: String
    let onPay: () -> Void
    var hintText = ""
    @Binding var hintShown: Bool
    @ViewBuilder let header: () -> Header
    @ViewBuilder let content: () -> Content

    @State private var contentHeight: CGFloat?
    @State private var canScrollForward = false
    @State private var hintAnchor = CvcHintAnchor()
    @EnvironmentObject private var insets: SheetInsets
    @Environment(\.checkoutTheme) private var theme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private static var headerHeight: CGFloat { 2 + stepHeaderHeight + 8 }
    private static var footerHeight: CGFloat { 1 + 12 + checkoutButtonHeight }
    /// A keyboard lower than this is only a bar over a hardware keyboard.
    private static var minKeyboardHeight: CGFloat { 120 }
    private static var scrollSpace: String { "form-scroll" }

    var body: some View {
        let showFooter = footerOverKeyboard || insets.keyboard < Self.minKeyboardHeight
        let limit = max(UIScreen.main.bounds.height - sheetChrome - Self.headerHeight - (showFooter ? Self.footerHeight : 0), 0)
        let fits = (contentHeight ?? 0) <= limit
        VStack(spacing: 0) {
            header()
                .padding(.horizontal, sidePadding)
                .padding(.top, 2)
                .padding(.bottom, 8)
            VStack(spacing: 0) {
                ScrollView(fits ? [] : .vertical, showsIndicators: false) {
                    VStack(spacing: 16) { content() }
                        .padding(.horizontal, sidePadding)
                        .padding(.top, 6)
                        .padding(.bottom, showFooter ? 4 : 20)
                        .background(GeometryReader { proxy in
                            Color.clear.preference(key: ContentFrameKey.self, value: proxy.frame(in: .named(Self.scrollSpace)))
                        })
                        .fixedSize(horizontal: false, vertical: true)
                }
                .coordinateSpace(name: Self.scrollSpace)
                .frame(height: min(contentHeight ?? limit, limit))
                .preference(key: SheetContentScrollsKey.self, value: !fits)
                .onPreferenceChange(ContentFrameKey.self) { frame in
                    contentHeight = frame.height
                    canScrollForward = frame.maxY > min(frame.height, limit) + 0.5
                }
                if showFooter {
                    VStack(spacing: 0) {
                        theme.colors.outline
                            .frame(height: 1)
                            .opacity(canScrollForward ? 1 : 0)
                            .animation(.linear(duration: 0.15), value: canScrollForward)
                        PayButton(label: payLabel, action: onPay)
                            .padding(.horizontal, sidePadding)
                            .padding(.top, 12)
                    }
                    .transition(.opacity)
                }
            }
            .stepBody()
        }
        .animation(reduceMotion ? nil : SheetMotion.content(0.25), value: showFooter)
        // Over the sheet's main button, which the sheet draws above its steps, as the Compose popup is.
        .anchorPreference(key: SheetPopupsKey.self, value: .bounds) { anchor in
            hintShown ? [SheetPopup(anchor: anchor, content: AnyView(hint))] : []
        }
        .coordinateSpace(name: CvcHintAnchor.space)
        .environment(\.cvcHintAnchor, hintAnchor)
    }

    @ViewBuilder private var hint: some View {
        if hintShown {
            let rect = hintAnchor.frame
            ZStack(alignment: .topLeading) {
                Color.clear.contentShape(Rectangle()).onTapGesture { hintShown = false }
                CvcHintBubble(text: hintText)
                    .frame(width: max(rect.midX + CvcHintBubble.arrowInset, 0), alignment: .trailing)
                    .offset(y: rect.maxY + CvcHintBubble.anchorGap)
                    .allowsHitTesting(false)
            }
        }
    }

    /// What `SheetHost` puts around a step: the gap above the sheet, its paddings, the grabber and the spacing under it.
    private var sheetChrome: CGFloat {
        insets.top + 24 + 8 + 14 + 14 + 12 + insets.sheetBottom
    }
}

private struct ContentFrameKey: PreferenceKey {
    static let defaultValue: CGRect = .zero

    static func reduce(value: inout CGRect, nextValue: () -> CGRect) {
        let next = nextValue()
        if next != .zero { value = next }
    }
}
