import SwiftUI

/// Two strokes meeting at a right angle, the corner pointing to [direction]: the row's chevron, Back, a menu's arrow.
struct Chevron: View {
    enum Direction {
        case right
        case left
        case down
    }

    var direction = Direction.right
    var size: CGFloat = 8
    var stroke: CGFloat = 1.5
    var color = QaColor.muted

    var body: some View {
        CornerShape()
            .stroke(color, style: StrokeStyle(lineWidth: stroke, lineCap: .square))
            .frame(width: size, height: size)
            .rotationEffect(angle)
            .frame(width: size * 1.5, height: size * 1.5)
            .accessibilityHidden(true)
    }

    private var angle: Angle {
        switch direction {
        case .right: .degrees(-45)
        case .left: .degrees(135)
        case .down: .degrees(45)
        }
    }
}

/// The right and bottom edges of a square: rotated, it points right, left or down.
private struct CornerShape: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.maxX, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
        path.addLine(to: CGPoint(x: rect.minX, y: rect.maxY))
        return path
    }
}

/// Three squares one over the other: the menu's button.
struct MenuDots: View {
    var body: some View {
        VStack(spacing: 3) {
            ForEach(0 ..< 3, id: \.self) { _ in
                Rectangle().fill(QaColor.ink).frame(width: 4, height: 4)
            }
        }
        .padding(.horizontal, 4)
        .accessibilityHidden(true)
    }
}
