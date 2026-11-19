import Combine
import SwiftUI

/// A short confirmation over the bottom of the screen (`Report copied`), gone after two seconds; Android's toast.
@MainActor
final class Toaster: ObservableObject {
    @Published private(set) var text: String?
    private var hide: Task<Void, Never>?

    func say(_ text: String) {
        self.text = text
        hide?.cancel()
        hide = Task { [weak self] in
            try? await Task.sleep(nanoseconds: 2_000_000_000)
            guard !Task.isCancelled else { return }
            self?.text = nil
        }
    }
}

/// Copies the text and says so: `mdOrder copied`.
@MainActor
func copyText(_ label: String, _ text: String, toaster: Toaster) {
    UIPasteboard.general.string = text
    toaster.say("\(label) copied")
}

struct ToastLayer: View {
    @ObservedObject var toaster: Toaster

    var body: some View {
        if let text = toaster.text {
            Text(text)
                .qaStyle(QaType.label)
                .foregroundStyle(QaColor.bg)
                .padding(.horizontal, QaSize.screen)
                .padding(.vertical, 12)
                .background(QaColor.ink)
                .padding(.horizontal, QaSize.screen)
                .padding(.bottom, 96)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
                .allowsHitTesting(false)
                .transition(.opacity)
                .accessibilityAddTraits(.updatesFrequently)
        }
    }
}
