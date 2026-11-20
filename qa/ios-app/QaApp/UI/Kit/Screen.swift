import SwiftUI
import UIKit

/// A whole screen: the bar, the content scrolling under it with 24 at its end, and the footer fixed at the bottom above
/// the keyboard. With `scrolls` off the content gets the height left, for a screen that lays out a list of its own.
struct QaScreen<Actions: View, Footer: View, Content: View>: View {
    let title: String
    var back: (() -> Void)?
    var scrolls = true
    @ViewBuilder let actions: () -> Actions
    @ViewBuilder let footer: () -> Footer
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(spacing: 0) {
            AppBar(title: title, back: back, actions: actions)
            if scrolls {
                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        content()
                        Spacer().frame(height: 24)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .scrollDismissesKeyboard(.interactively)
            } else {
                VStack(alignment: .leading, spacing: 0, content: content).frame(maxHeight: .infinity, alignment: .top)
            }
            footer()
        }
        .foregroundStyle(QaColor.ink)
        .background(QaColor.bg.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
    }
}

extension QaScreen where Actions == EmptyView {
    init(
        title: String,
        back: (() -> Void)? = nil,
        scrolls: Bool = true,
        @ViewBuilder footer: @escaping () -> Footer,
        @ViewBuilder content: @escaping () -> Content
    ) {
        self.init(title: title, back: back, scrolls: scrolls, actions: { EmptyView() }, footer: footer, content: content)
    }
}

extension QaScreen where Footer == EmptyView {
    init(
        title: String,
        back: (() -> Void)? = nil,
        scrolls: Bool = true,
        @ViewBuilder actions: @escaping () -> Actions,
        @ViewBuilder content: @escaping () -> Content
    ) {
        self.init(title: title, back: back, scrolls: scrolls, actions: actions, footer: { EmptyView() }, content: content)
    }
}

extension QaScreen where Actions == EmptyView, Footer == EmptyView {
    init(title: String, back: (() -> Void)? = nil, @ViewBuilder content: @escaping () -> Content) {
        self.init(title: title, back: back, scrolls: true, actions: { EmptyView() }, footer: { EmptyView() }, content: content)
    }
}

// The bar is the app's own, so the system's is hidden; this keeps the edge swipe that goes back with it.
extension UINavigationController: @retroactive UIGestureRecognizerDelegate {
    override open func viewDidLoad() {
        super.viewDidLoad()
        interactivePopGestureRecognizer?.delegate = self
    }

    public func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
        viewControllers.count > 1
    }
}
