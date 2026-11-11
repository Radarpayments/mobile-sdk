import SwiftUI

/// SVG path data as a `CGPath`: the icons and network logos keep the path strings of the Compose UI.
enum SVGPath {
    static func path(_ data: String) -> Path {
        Path(parse(data))
    }

    static func parse(_ data: String) -> CGPath {
        var reader = Reader(data)
        let path = CGMutablePath()
        var command: Character = "M"
        var start = CGPoint.zero
        var current = CGPoint.zero
        var lastControl: CGPoint?
        var lastCommand: Character = " "

        while let next = reader.nextCommand(or: command) {
            command = next
            let relative = command.isLowercase
            let origin = relative ? current : .zero
            func point() -> CGPoint? {
                guard let x = reader.number(), let y = reader.number() else { return nil }
                return CGPoint(x: origin.x + x, y: origin.y + y)
            }
            var control: CGPoint?

            switch Character(command.uppercased()) {
            case "M":
                guard let to = point() else { return path }
                path.move(to: to)
                current = to
                start = to
                // Coordinates that follow a move without a letter are lines.
                command = relative ? "l" : "L"
            case "L":
                guard let to = point() else { return path }
                path.addLine(to: to)
                current = to
            case "H":
                guard let x = reader.number() else { return path }
                current = CGPoint(x: origin.x + x, y: current.y)
                path.addLine(to: current)
            case "V":
                guard let y = reader.number() else { return path }
                current = CGPoint(x: current.x, y: origin.y + y)
                path.addLine(to: current)
            case "C":
                guard let first = point(), let second = point(), let to = point() else { return path }
                path.addCurve(to: to, control1: first, control2: second)
                control = second
                current = to
            case "S":
                guard let second = point(), let to = point() else { return path }
                let first = "CS".contains(lastCommand) ? reflect(lastControl, around: current) : current
                path.addCurve(to: to, control1: first, control2: second)
                control = second
                current = to
            case "Q":
                guard let first = point(), let to = point() else { return path }
                path.addQuadCurve(to: to, control: first)
                control = first
                current = to
            case "T":
                guard let to = point() else { return path }
                let first = "QT".contains(lastCommand) ? reflect(lastControl, around: current) : current
                path.addQuadCurve(to: to, control: first)
                control = first
                current = to
            case "A":
                guard let rx = reader.number(), let ry = reader.number(), let rotation = reader.number(),
                      let large = reader.flag(), let sweep = reader.flag(), let to = point()
                else { return path }
                addArc(to: path, from: current, to: to, rx: rx, ry: ry, rotation: rotation, large: large, sweep: sweep)
                current = to
            case "Z":
                path.closeSubpath()
                current = start
            default:
                return path
            }
            lastControl = control
            lastCommand = Character(command.uppercased())
        }
        return path
    }

    private static func reflect(_ control: CGPoint?, around point: CGPoint) -> CGPoint {
        guard let control = control else { return point }
        return CGPoint(x: 2 * point.x - control.x, y: 2 * point.y - control.y)
    }

    /// The SVG endpoint arc as cubic curves (SVG 1.1, F.6.5), one per quarter turn at most.
    private static func addArc(
        to path: CGMutablePath, from: CGPoint, to: CGPoint, rx: CGFloat, ry: CGFloat, rotation: CGFloat, large: Bool, sweep: Bool
    ) {
        guard from != to else { return }
        var rx = abs(rx)
        var ry = abs(ry)
        guard rx > 0, ry > 0 else {
            path.addLine(to: to)
            return
        }
        let phi = rotation * .pi / 180
        let cosPhi = cos(phi)
        let sinPhi = sin(phi)
        let dx = (from.x - to.x) / 2
        let dy = (from.y - to.y) / 2
        let x1 = cosPhi * dx + sinPhi * dy
        let y1 = -sinPhi * dx + cosPhi * dy
        let lambda = (x1 * x1) / (rx * rx) + (y1 * y1) / (ry * ry)
        if lambda > 1 {
            rx *= sqrt(lambda)
            ry *= sqrt(lambda)
        }
        let numerator = rx * rx * ry * ry - rx * rx * y1 * y1 - ry * ry * x1 * x1
        let denominator = rx * rx * y1 * y1 + ry * ry * x1 * x1
        var factor = denominator == 0 ? 0 : sqrt(max(numerator / denominator, 0))
        if large == sweep { factor = -factor }
        let cx1 = factor * rx * y1 / ry
        let cy1 = -factor * ry * x1 / rx
        let center = CGPoint(
            x: cosPhi * cx1 - sinPhi * cy1 + (from.x + to.x) / 2,
            y: sinPhi * cx1 + cosPhi * cy1 + (from.y + to.y) / 2
        )
        let startAngle = atan2((y1 - cy1) / ry, (x1 - cx1) / rx)
        var delta = atan2((-y1 - cy1) / ry, (-x1 - cx1) / rx) - startAngle
        if sweep, delta < 0 { delta += 2 * .pi }
        if !sweep, delta > 0 { delta -= 2 * .pi }

        let segments = max(Int((abs(delta) / (.pi / 2)).rounded(.up)), 1)
        let step = delta / CGFloat(segments)
        let handle = 4 / 3 * tan(step / 4)
        func onEllipse(_ angle: CGFloat) -> CGPoint {
            let x = rx * cos(angle)
            let y = ry * sin(angle)
            return CGPoint(x: center.x + cosPhi * x - sinPhi * y, y: center.y + sinPhi * x + cosPhi * y)
        }
        func tangent(_ angle: CGFloat) -> CGPoint {
            let x = -rx * sin(angle)
            let y = ry * cos(angle)
            return CGPoint(x: cosPhi * x - sinPhi * y, y: sinPhi * x + cosPhi * y)
        }
        var angle = startAngle
        var point = from
        for index in 0..<segments {
            let next = angle + step
            let end = index == segments - 1 ? to : onEllipse(next)
            let out = tangent(angle)
            let into = tangent(next)
            path.addCurve(
                to: end,
                control1: CGPoint(x: point.x + handle * out.x, y: point.y + handle * out.y),
                control2: CGPoint(x: end.x - handle * into.x, y: end.y - handle * into.y)
            )
            angle = next
            point = end
        }
    }

    private struct Reader {
        private let scalars: [Unicode.Scalar]
        private var index = 0

        init(_ data: String) {
            scalars = Array(data.unicodeScalars)
        }

        private mutating func skipSeparators() {
            while index < scalars.count, scalars[index] == "," || scalars[index].properties.isWhitespace { index += 1 }
        }

        /// The next command letter, or `previous` again when numbers follow directly; nil at the end.
        mutating func nextCommand(or previous: Character) -> Character? {
            skipSeparators()
            guard index < scalars.count else { return nil }
            let scalar = scalars[index]
            if scalar.properties.isAlphabetic, scalar != "e", scalar != "E" {
                index += 1
                return Character(scalar)
            }
            return "Zz".contains(previous) ? nil : previous
        }

        mutating func number() -> CGFloat? {
            skipSeparators()
            let begin = index
            var seenDot = false
            var seenExponent = false
            while index < scalars.count {
                let scalar = scalars[index]
                let afterExponent = index > begin && (scalars[index - 1] == "e" || scalars[index - 1] == "E")
                if scalar == "-" || scalar == "+" {
                    guard index == begin || afterExponent else { break }
                } else if scalar == "." {
                    guard !seenDot, !seenExponent else { break }
                    seenDot = true
                } else if scalar == "e" || scalar == "E" {
                    guard !seenExponent, index > begin else { break }
                    seenExponent = true
                } else if !("0"..."9").contains(scalar) {
                    break
                }
                index += 1
            }
            guard index > begin else { return nil }
            var text = String.UnicodeScalarView()
            text.append(contentsOf: scalars[begin..<index])
            return Double(String(text)).map { CGFloat($0) }
        }

        /// An arc flag is one digit and may be glued to what follows (`a2 2 0 012 2`).
        mutating func flag() -> Bool? {
            skipSeparators()
            guard index < scalars.count, scalars[index] == "0" || scalars[index] == "1" else { return nil }
            defer { index += 1 }
            return scalars[index] == "1"
        }
    }
}
