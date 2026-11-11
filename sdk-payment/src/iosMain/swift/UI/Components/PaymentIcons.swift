import SwiftUI

/// Line icons of the checkout: a 24×24 grid, round caps and joins, tinted by the caller.
struct PaymentIcon: Equatable {
    let paths: [String]
    var stroke: CGFloat = 1.8
    /// Points the other way in right-to-left layouts.
    var mirrored = false

    private static let lockPaths = [
        "M7 11h10a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-6a2 2 0 0 1 2-2z",
        "M8 11V8a4 4 0 0 1 8 0v3",
    ]

    static let lock = PaymentIcon(paths: lockPaths)
    /// On the Pay button: the heavier stroke matches the semibold label next to it.
    static let lockBold = PaymentIcon(paths: lockPaths, stroke: 2.4)
    static let back = PaymentIcon(paths: ["M15 18l-6-6 6-6"], mirrored: true)
    static let close = PaymentIcon(paths: ["M6 6l12 12M18 6L6 18"])
    static let chevron = PaymentIcon(paths: ["M9 6l6 6-6 6"], mirrored: true)
    static let chevronDown = PaymentIcon(paths: ["M6 9l6 6 6-6"])
    static let search = PaymentIcon(paths: ["M10.5 4a6.5 6.5 0 1 0 0 13a6.5 6.5 0 1 0 0-13", "M15.5 15.5L20 20"])
    static let plus = PaymentIcon(paths: ["M12 5v14M5 12h14"])
    static let scan = PaymentIcon(paths: [
        "M4 8V6a2 2 0 0 1 2-2h2M16 4h2a2 2 0 0 1 2 2v2M20 16v2a2 2 0 0 1-2 2h-2M8 20H6a2 2 0 0 1-2-2v-2",
        "M8 9h8a1 1 0 0 1 1 1v4a1 1 0 0 1-1 1H8a1 1 0 0 1-1-1v-4a1 1 0 0 1 1-1z",
    ])
    static let check = PaymentIcon(paths: ["M5 12.5l4.5 4.5L19 7.5"], stroke: 2.4)
    static let cross = PaymentIcon(paths: ["M6 6l12 12M18 6L6 18"], stroke: 2.4)
    static let shield = PaymentIcon(paths: ["M12 3l7 3v6c0 4.5-3 7.5-7 9-4-1.5-7-4.5-7-9V6z", "M9 12l2 2 4-4"])
    static let info = PaymentIcon(paths: ["M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18", "M12 11v5", "M12 7.75v.01"])
    static let trash = PaymentIcon(paths: ["M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3"])
    static let mail = PaymentIcon(paths: [
        "M4.5 5.5h15A1.5 1.5 0 0 1 21 7v10a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 17V7a1.5 1.5 0 0 1 1.5-1.5z",
        "M3.6 6.6l7.5 5.6a1.5 1.5 0 0 0 1.8 0l7.5-5.6",
    ])
    static let phone = PaymentIcon(paths: [
        "M8.5 2.75h7a1.75 1.75 0 0 1 1.75 1.75v15a1.75 1.75 0 0 1-1.75 1.75h-7a1.75 1.75 0 0 1-1.75-1.75v-15A1.75 1.75 0 0 1 8.5 2.75z",
        "M11 18h2",
    ])
    static let pin = PaymentIcon(paths: [
        "M12 21.25s-6.75-5.7-6.75-11.25a6.75 6.75 0 0 1 13.5 0c0 5.55-6.75 11.25-6.75 11.25z",
        "M12 7.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0-5",
    ])
}

/// An icon drawn in a square of `size` points; decorative, so hidden from accessibility.
struct IconView: View {
    let icon: PaymentIcon
    let size: CGFloat
    let color: Color

    @Environment(\.layoutDirection) private var layoutDirection

    var body: some View {
        IconShape(icon: icon, flipped: icon.mirrored && layoutDirection == .rightToLeft)
            .stroke(color, style: StrokeStyle(lineWidth: icon.stroke * size / 24, lineCap: .round, lineJoin: .round))
            .frame(width: size, height: size)
            .accessibilityHidden(true)
    }
}

private struct IconShape: Shape {
    let icon: PaymentIcon
    let flipped: Bool

    func path(in rect: CGRect) -> Path {
        let scale = rect.width / 24
        var transform = CGAffineTransform(translationX: rect.minX, y: rect.minY).scaledBy(x: scale, y: scale)
        if flipped {
            transform = transform.translatedBy(x: 24, y: 0).scaledBy(x: -1, y: 1)
        }
        var path = Path()
        for data in icon.paths {
            path.addPath(Path(SVGPath.parse(data)), transform: transform)
        }
        return path
    }
}
