import SwiftUI
import UIKit

/// The SwiftUI screens as the Kotlin side finds them: by this class's Objective-C name, see `CheckoutRenderers`.
@objc(PayrdrSwiftUICheckoutRenderer)
final class PayrdrSwiftUICheckoutRenderer: NSObject, CheckoutRenderer {
    @objc func install() {
        CheckoutRenderers.shared.swiftUi = self
    }

    func viewController(session: CheckoutSession) -> UIViewController {
        CheckoutHostingController(session: session)
    }
}

/// What the screens draw from: the model's latest value, republished for SwiftUI.
final class CheckoutStore: ObservableObject {
    let session: CheckoutSession
    @Published private(set) var model: CheckoutModel?
    @Published private(set) var ui: CheckoutUi?

    init(session: CheckoutSession) {
        self.session = session
    }

    func start(sheetPresentation: Bool) {
        guard model == nil else { return }
        let model = session.start(sheetPresentation: sheetPresentation)
        self.model = model
        ui = model.ui.value
        session.observe(model: model) { [weak self] in self?.ui = $0 }
    }
}

final class CheckoutHostingController: UIHostingController<CheckoutRoot> {
    private let store: CheckoutStore
    private let insets = SheetInsets()
    private var hiddenUnder: [UIView] = []
    private var closed = false

    init(session: CheckoutSession) {
        store = CheckoutStore(session: session)
        super.init(rootView: CheckoutRoot(store: store, insets: insets))
        view.backgroundColor = .clear
        // The merchant screen stays visible under the transparent sheet; VoiceOver must not reach it.
        view.accessibilityViewIsModal = true
        modalPresentationStyle = .overFullScreen
        modalTransitionStyle = .crossDissolve
        if !session.theme.followsSystem {
            overrideUserInterfaceStyle = CheckoutTheme(spec: session.theme, systemIsDark: false).isDark ? .dark : .light
        }
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        insets.update(from: view)
        // Presented directly the controller covers the merchant screen transparently and the checkout draws its own
        // sheet; embedded in another controller the container is already a sheet, so the steps fill it. The hierarchy
        // is only settled once the view is on screen.
        store.start(sheetPresentation: parent == nil)
        if hiddenUnder.isEmpty, let under = presentingViewController?.view, !under.accessibilityElementsHidden {
            under.accessibilityElementsHidden = true
            hiddenUnder = [under]
        }
    }

    override func viewSafeAreaInsetsDidChange() {
        super.viewSafeAreaInsetsDidChange()
        insets.update(from: view)
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        // The bank's page is presented over the checkout and takes it off screen without ending it.
        guard isBeingDismissed || isMovingFromParent || presentingViewController == nil && parent == nil else { return }
        close()
    }

    deinit {
        close()
    }

    private func close() {
        guard !closed else { return }
        closed = true
        hiddenUnder.forEach { $0.accessibilityElementsHidden = false }
        store.session.closed()
    }
}

struct CheckoutRoot: View {
    @ObservedObject var store: CheckoutStore
    @ObservedObject var insets: SheetInsets
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        let session = store.session
        // The root takes its size from the container: a frame that only allows growing shrinks with a step that gets
        // shorter in place, and the sheet then stands above the bottom edge.
        GeometryReader { proxy in
            ZStack {
                if let model = store.model, let ui = store.ui {
                    CheckoutScreen(model: model, ui: ui)
                }
            }
            .frame(width: proxy.size.width, height: proxy.size.height)
        }
        .environment(\.checkoutTheme, CheckoutTheme(spec: session.theme, systemIsDark: colorScheme == .dark))
        .environment(\.checkoutStrings, session.strings)
        .environment(\.cardScanner, session.cardScannerProvider as? NativeCardScanner)
        .environment(\.layoutDirection, session.rightToLeft ? .rightToLeft : .leftToRight)
        .environmentObject(insets)
        .edgesIgnoringSafeArea(.all)
    }
}

private struct CheckoutStringsKey: EnvironmentKey {
    static let defaultValue: PaymentStrings? = nil
}

extension EnvironmentValues {
    var checkoutStrings: PaymentStrings? {
        get { self[CheckoutStringsKey.self] }
        set { self[CheckoutStringsKey.self] = newValue }
    }
}
