import SwiftUI
import UIKit

/// The window's safe area and the keyboard, as the checkout lays itself out against them: the whole checkout ignores
/// SwiftUI's own safe area, because the sheet reaches under the home indicator and follows the keyboard on its own curve.
final class SheetInsets: ObservableObject {
    @Published var top: CGFloat = 0
    @Published var bottom: CGFloat = 0
    @Published var leading: CGFloat = 0
    @Published var trailing: CGFloat = 0
    @Published private(set) var keyboard: CGFloat = 0

    /// How far content may go into the home indicator's area.
    static let bottomReach: CGFloat = 20

    private var observers: [NSObjectProtocol] = []

    init() {
        let center = NotificationCenter.default
        observers = [
            center.addObserver(forName: UIResponder.keyboardWillChangeFrameNotification, object: nil, queue: .main) { [weak self] in
                self?.keyboardChanged($0, hides: false)
            },
            center.addObserver(forName: UIResponder.keyboardWillHideNotification, object: nil, queue: .main) { [weak self] in
                self?.keyboardChanged($0, hides: true)
            },
        ]
    }

    deinit {
        observers.forEach(NotificationCenter.default.removeObserver)
    }

    var keyboardOpen: Bool { keyboard > 0 }

    /// Under the sheet's content: the keyboard when it is up, otherwise what is left of the home indicator's area.
    var sheetBottom: CGFloat { max(bottom - SheetInsets.bottomReach, 0, keyboard) }

    func update(from view: UIView) {
        let insets = view.window?.safeAreaInsets ?? view.safeAreaInsets
        let rtl = view.effectiveUserInterfaceLayoutDirection == .rightToLeft
        let values = (insets.top, insets.bottom, rtl ? insets.right : insets.left, rtl ? insets.left : insets.right)
        if values != (top, bottom, leading, trailing) {
            (top, bottom, leading, trailing) = values
        }
    }

    private func keyboardChanged(_ notification: Notification, hides: Bool) {
        let info = notification.userInfo
        let frame = (info?[UIResponder.keyboardFrameEndUserInfoKey] as? NSValue)?.cgRectValue
        let duration = (info?[UIResponder.keyboardAnimationDurationUserInfoKey] as? NSNumber)?.doubleValue ?? 0.25
        let height = hides ? 0 : max(UIScreen.main.bounds.height - (frame?.origin.y ?? UIScreen.main.bounds.height), 0)
        guard height != keyboard else { return }
        withAnimation(duration > 0 ? .timingCurve(0.2, 0.8, 0.2, 1, duration: duration) : nil) {
            keyboard = height
        }
    }
}
