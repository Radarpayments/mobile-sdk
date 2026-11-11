import SwiftUI

/// An icon's strokes scaled to the view and drawn one after another up to `drawn` of their whole length.
struct DrawnIconShape: Shape {
    static let grid: CGFloat = 24

    let icon: PaymentIcon
    var drawn: CGFloat = 1

    var animatableData: CGFloat {
        get { drawn }
        set { drawn = newValue }
    }

    func path(in rect: CGRect) -> Path {
        let scale = min(rect.width, rect.height) / DrawnIconShape.grid
        let transform = CGAffineTransform(translationX: rect.minX, y: rect.minY).scaledBy(x: scale, y: scale)
        var whole = Path()
        icon.paths.forEach { whole.addPath(Path(SVGPath.parse($0)), transform: transform) }
        return drawn >= 1 ? whole : whole.trimmedPath(from: 0, to: max(drawn, 0))
    }
}
