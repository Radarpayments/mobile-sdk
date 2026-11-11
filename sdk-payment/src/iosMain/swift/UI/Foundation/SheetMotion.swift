import SwiftUI

/// Every duration, curve and distance the checkout sheet moves with: the numbers of the Android checkout's
/// `SheetMotion`, so both platforms move alike. Durations are in seconds.
enum SheetMotion {
    static let stepDuration = 0.27
    static let stepExitFadeDuration = 0.19
    static let heightDuration = 0.27
    static let sheetEnterDuration = 0.5
    static let sheetCloseDuration = 0.32
    static let sheetExpandDuration = 0.5

    /// Scale a step comes in from going forward, and leaves to going back.
    static let zoomIn: CGFloat = 0.96
    /// Scale a step leaves to going forward, and comes in from going back.
    static let zoomOut: CGFloat = 1.04
    static let blur: CGFloat = 2

    static let skeletonFadeDuration = 0.15
    static let pulseDuration = 1.4
    static let pulseMinOpacity = 0.45

    static let statusOutDuration = 0.12
    static let statusInDuration = 0.16

    static let badgeDamping = 0.55
    static let badgeStiffness = 520.0
    static let badgeDelay = 0.09
    static let markDuration = 0.32
    static let markDelay = 0.22
    static let shakeDuration = 0.42
    static let shakeDelay = 0.48
    static let shakeOffsets: [CGFloat] = [0, -6, 5, -3, 2, 0]
    static let cascadeDuration = 0.26
    static let cascadeStart = 0.32
    static let cascadeStep = 0.045
    static let cascadeRise: CGFloat = 4

    static let labelLeaveDuration = 0.22
    static let labelLeaveStep = 0.045
    static let labelArriveDelay = 0.14
    static let labelArriveStep = 0.07
    static let labelArriveFadeDuration = 0.26
    static let labelArriveMoveDuration = 0.38
    static let labelShift: CGFloat = 12
    static let labelKeepMoveDuration = 0.46

    static let labelSlideDuration = 0.38
    static let labelSlideOutFadeDuration = 0.18
    static let labelSlideInDelay = 0.06
    static let labelSlideInFadeDuration = 0.26
    static let labelSlide: CGFloat = 32

    static let reducedEnterDuration = 0.15
    static let reducedExitDuration = 0.1
    static let reducedSheetEnterDuration = 0.2
    static let reducedSheetCloseDuration = 0.15

    static func content(_ duration: Double = stepDuration) -> Animation {
        .timingCurve(0.26, 0.08, 0.25, 1, duration: duration)
    }

    static func height(_ duration: Double = heightDuration) -> Animation {
        .timingCurve(0.25, 1, 0.5, 1, duration: duration)
    }

    static func sheet(_ duration: Double) -> Animation {
        .timingCurve(0.32, 0.72, 0, 1, duration: duration)
    }

    static func mark(_ duration: Double) -> Animation {
        .timingCurve(0.2, 0.8, 0.2, 1, duration: duration)
    }

    static func label(_ duration: Double) -> Animation {
        .timingCurve(0.22, 0.9, 0.24, 1, duration: duration)
    }

    /// When the `index`-th line of the receipt starts to come in, after the mark.
    static func cascadeDelay(_ index: Int) -> Double {
        cascadeStart + Double(index) * cascadeStep
    }
}
