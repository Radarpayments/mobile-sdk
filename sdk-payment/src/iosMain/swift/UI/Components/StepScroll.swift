import SwiftUI

/// A step's body that takes its content's height until the sheet has reached its full height, and scrolls from there.
struct StepScroll<Content: View>: View {
    /// Height of the step's own parts outside the body: its header and footer.
    let chrome: CGFloat
    @ViewBuilder let content: () -> Content

    @EnvironmentObject private var insets: SheetInsets
    @State private var contentHeight: CGFloat?

    var body: some View {
        let limit = max(UIScreen.main.bounds.height - sheetChrome - chrome, 0)
        let fits = (contentHeight ?? 0) <= limit
        ScrollView(fits ? [] : .vertical, showsIndicators: false) {
            content()
                .background(GeometryReader { proxy in
                    Color.clear
                        .onAppear { contentHeight = proxy.size.height }
                        .onChange(of: proxy.size.height) { contentHeight = $0 }
                })
                // Without an axis the scroll view offers its own height, and the content would be squeezed into it.
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(height: min(contentHeight ?? limit, limit))
        .preference(key: SheetContentScrollsKey.self, value: !fits)
        .stepHeightUnmeasured(contentHeight == nil)
    }

    /// What `SheetHost` puts around a step: the gap above the sheet, its paddings, the grabber and the spacing under it.
    private var sheetChrome: CGFloat {
        insets.top + 24 + 8 + 14 + 14 + 12 + insets.sheetBottom
    }
}
