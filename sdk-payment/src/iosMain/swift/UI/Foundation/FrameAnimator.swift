import SwiftUI
import UIKit

/// Runs a value along a timing curve one display frame at a time, each frame a plain state change.
///
/// A SwiftUI animation of an offset or a frame moves only the views that existed when it started: a step inserted
/// while the sheet slides in is placed where the sheet will end up and shows through the moving clip. Motion that
/// other views appear in during its run goes through here instead.
final class FrameAnimator: NSObject {
    private var link: CADisplayLink?
    private var start: CFTimeInterval?
    private var duration: Double = 0
    private var curve = CubicBezier.content
    private var step: ((CGFloat) -> Void)?
    private var done: (() -> Void)?

    /// Calls `step` with the eased fraction 0…1 on every frame for `duration` seconds, then `done`.
    func run(duration: Double, curve: CubicBezier, step: @escaping (CGFloat) -> Void, done: @escaping () -> Void = {}) {
        stop()
        guard duration > 0 else {
            Self.instantly { step(1) }
            done()
            return
        }
        self.duration = duration
        self.curve = curve
        self.step = step
        self.done = done
        start = nil
        let link = CADisplayLink(target: self, selector: #selector(tick(_:)))
        link.add(to: .main, forMode: .common)
        self.link = link
    }

    func stop() {
        link?.invalidate()
        link = nil
        step = nil
        done = nil
    }

    @objc private func tick(_ link: CADisplayLink) {
        let begin = start ?? link.timestamp
        start = begin
        let fraction = min((link.targetTimestamp - begin) / duration, 1)
        let value = CGFloat(curve.value(at: fraction))
        let step = self.step
        Self.instantly { step?(value) }
        guard fraction >= 1 else { return }
        let done = self.done
        stop()
        done?()
    }

    private static func instantly(_ change: () -> Void) {
        var transaction = Transaction()
        transaction.disablesAnimations = true
        withTransaction(transaction, change)
    }
}

extension CubicBezier {
    static let sheet = CubicBezier(x1: 0.32, y1: 0.72, x2: 0, y2: 1)
    static let linear = CubicBezier(x1: 0, y1: 0, x2: 1, y2: 1)
}
