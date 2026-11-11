import SwiftUI

/// The camera screen a form raises over the whole checkout: like the bank's page, not the sheet, so no rounded top and
/// no sheet height.
struct ScannerCoverItem: Equatable {
    let id: Int
    let scanner: NativeCardScanner
    let onResult: (CardScannerResult) -> Void
    let onClose: () -> Void

    static func == (lhs: ScannerCoverItem, rhs: ScannerCoverItem) -> Bool {
        lhs.id == rhs.id
    }
}

private struct ScannerCoverKey: PreferenceKey {
    static let defaultValue: ScannerCoverItem? = nil

    static func reduce(value: inout ScannerCoverItem?, nextValue: () -> ScannerCoverItem?) {
        value = nextValue() ?? value
    }
}

private struct CardScannerKey: EnvironmentKey {
    static let defaultValue: NativeCardScanner? = nil
}

extension EnvironmentValues {
    /// The integrator's card scanner when it can be shown without Compose; the card form offers scanning then.
    var cardScanner: NativeCardScanner? {
        get { self[CardScannerKey.self] }
        set { self[CardScannerKey.self] = newValue }
    }
}

extension View {
    /// Raises the camera screen over the checkout while `item` is not nil.
    func scannerCover(_ item: ScannerCoverItem?) -> some View {
        preference(key: ScannerCoverKey.self, value: item)
    }

    /// Draws the camera screen of the views inside over this view, which VoiceOver then skips.
    func scannerCoverHost(reduceMotion: Bool) -> some View {
        modifier(ScannerCoverHost(reduceMotion: reduceMotion))
    }
}

private struct ScannerCoverHost: ViewModifier {
    let reduceMotion: Bool

    @State private var covered = false

    func body(content: Content) -> some View {
        content
            // The whole checkout, the Pay button too: the card form alone cannot hide what lies outside it.
            .coveredFromVoiceOver(covered)
            .overlayPreferenceValue(ScannerCoverKey.self) { item in
                ScannerCoverLayer(item: item, reduceMotion: reduceMotion)
            }
            .onPreferenceChange(ScannerCoverKey.self) { covered = $0 != nil }
    }
}

/// Whether the buyer has read the camera's intro: once per app run, as on Android.
enum ScanIntroMemory {
    static var seen = false
}

private struct ScannerCoverLayer: View {
    let item: ScannerCoverItem?
    let reduceMotion: Bool

    @State private var shown: ScannerCoverItem?
    @State private var visible = false
    @State private var changes = 0
    @EnvironmentObject private var insets: SheetInsets

    /// The Compose screen's slide: `tween(300)`, fast-out-slow-in.
    private static let slideDuration = 0.3

    var body: some View {
        GeometryReader { proxy in
            if let shown = shown {
                ScannerCoverContent(item: shown)
                    .padding(.top, insets.top)
                    .padding(.leading, insets.leading)
                    .padding(.trailing, insets.trailing)
                    // The bottom card and buttons end where the sheet's Pay button does.
                    .padding(.bottom, max(insets.bottom - SheetInsets.bottomReach, 0))
                    .frame(width: proxy.size.width, height: proxy.size.height)
                    .background(Color.black)
                    .environment(\.colorScheme, .dark)
                    .offset(y: visible || reduceMotion ? 0 : proxy.size.height)
                    .opacity(visible || !reduceMotion ? 1 : 0)
                    // As the overlay sheets: VoiceOver stays on the camera screen.
                    .accessibilityAddTraits(.isModal)
            }
        }
        .allowsHitTesting(item != nil)
        .onAppear { change(to: item) }
        .onChange(of: item) { change(to: $0) }
    }

    private func change(to item: ScannerCoverItem?) {
        changes += 1
        let duration = reduceMotion ? SheetMotion.reducedEnterDuration : Self.slideDuration
        let animation = Animation.timingCurve(0.4, 0, 0.2, 1, duration: duration)
        if let item = item {
            shown = item
            DispatchQueue.main.async {
                withAnimation(animation) { visible = true }
            }
        } else if shown != nil {
            withAnimation(animation) { visible = false }
            let change = changes
            DispatchQueue.main.asyncAfter(deadline: .now() + duration) {
                if changes == change { shown = nil }
            }
        }
    }
}

/// The intro first, then the scanner itself.
private struct ScannerCoverContent: View {
    let item: ScannerCoverItem

    @State private var cameraOn = ScanIntroMemory.seen
    @Environment(\.checkoutStrings) private var strings

    var body: some View {
        if cameraOn {
            CardScannerView(scanner: item.scanner, language: strings?.language ?? "en", onResult: item.onResult)
        } else {
            ScanIntroView(
                onTurnOn: {
                    ScanIntroMemory.seen = true
                    cameraOn = true
                },
                onManual: item.onClose
            )
        }
    }
}
