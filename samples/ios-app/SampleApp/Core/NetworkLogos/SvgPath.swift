import SwiftUI

/// SVG path data (`d` attribute) as a SwiftUI `Path`: all commands, absolute and relative, arcs included.
func svgPath(_ data: String) -> Path {
    var parser = SvgPathParser(Array(data.utf8))
    return parser.parse()
}

private struct SvgPathParser {
    private let bytes: [UInt8]
    private var index = 0
    private var path = Path()
    private var current = CGPoint.zero
    private var start = CGPoint.zero
    /// Second control point of the last curve, for the S/T shorthands.
    private var lastControl: CGPoint?

    init(_ bytes: [UInt8]) { self.bytes = bytes }

    mutating func parse() -> Path {
        var command: UInt8 = 0
        while true {
            skipSeparators()
            guard index < bytes.count else { break }
            let byte = bytes[index]
            if isCommand(byte) {
                command = byte
                index += 1
            } else if command == 0 {
                break
            }
            run(command)
            // After a moveto, further coordinate pairs are linetos.
            if command == UInt8(ascii: "M") { command = UInt8(ascii: "L") }
            if command == UInt8(ascii: "m") { command = UInt8(ascii: "l") }
        }
        return path
    }

    private mutating func run(_ command: UInt8) {
        let relative = command >= UInt8(ascii: "a")
        let base = relative ? current : .zero
        switch Character(Unicode.Scalar(command)).uppercased() {
        case "M":
            guard let p = point(base) else { return }
            path.move(to: p); current = p; start = p; lastControl = nil
        case "L":
            guard let p = point(base) else { return }
            path.addLine(to: p); current = p; lastControl = nil
        case "H":
            guard let x = number() else { return }
            let p = CGPoint(x: relative ? current.x + x : x, y: current.y)
            path.addLine(to: p); current = p; lastControl = nil
        case "V":
            guard let y = number() else { return }
            let p = CGPoint(x: current.x, y: relative ? current.y + y : y)
            path.addLine(to: p); current = p; lastControl = nil
        case "C":
            guard let c1 = point(base), let c2 = point(base), let p = point(base) else { return }
            path.addCurve(to: p, control1: c1, control2: c2); current = p; lastControl = c2
        case "S":
            guard let c2 = point(base), let p = point(base) else { return }
            let c1 = reflected()
            path.addCurve(to: p, control1: c1, control2: c2); current = p; lastControl = c2
        case "Q":
            guard let c = point(base), let p = point(base) else { return }
            path.addQuadCurve(to: p, control: c); current = p; lastControl = c
        case "T":
            guard let p = point(base) else { return }
            let c = reflected()
            path.addQuadCurve(to: p, control: c); current = p; lastControl = c
        case "A":
            guard let rx = number(), let ry = number(), let rotation = number(),
                  let large = flag(), let sweep = flag(), let p = point(base) else { return }
            addArc(to: p, rx: rx, ry: ry, rotation: rotation, largeArc: large, sweep: sweep)
            current = p; lastControl = nil
        case "Z":
            path.closeSubpath(); current = start; lastControl = nil
        default:
            index = bytes.count
        }
    }

    private func reflected() -> CGPoint {
        guard let c = lastControl else { return current }
        return CGPoint(x: 2 * current.x - c.x, y: 2 * current.y - c.y)
    }

    /// Endpoint arc → centre parameterisation, then cubic Béziers of at most 90° each (SVG 1.1, F.6).
    private mutating func addArc(to end: CGPoint, rx rxIn: Double, ry ryIn: Double, rotation: Double, largeArc: Bool, sweep: Bool) {
        var rx = abs(rxIn), ry = abs(ryIn)
        let p0 = current
        if rx == 0 || ry == 0 || p0 == end { path.addLine(to: end); return }
        let phi = rotation * .pi / 180
        let cosPhi = cos(phi), sinPhi = sin(phi)
        let dx = (p0.x - end.x) / 2, dy = (p0.y - end.y) / 2
        let x1 = cosPhi * dx + sinPhi * dy
        let y1 = -sinPhi * dx + cosPhi * dy
        let lambda = (x1 * x1) / (rx * rx) + (y1 * y1) / (ry * ry)
        if lambda > 1 { rx *= lambda.squareRoot(); ry *= lambda.squareRoot() }
        let num = rx * rx * ry * ry - rx * rx * y1 * y1 - ry * ry * x1 * x1
        let den = rx * rx * y1 * y1 + ry * ry * x1 * x1
        var coef = (max(0, num) / den).squareRoot()
        if largeArc == sweep { coef = -coef }
        let cx1 = coef * rx * y1 / ry
        let cy1 = -coef * ry * x1 / rx
        let cx = cosPhi * cx1 - sinPhi * cy1 + (p0.x + end.x) / 2
        let cy = sinPhi * cx1 + cosPhi * cy1 + (p0.y + end.y) / 2
        func angle(_ ux: Double, _ uy: Double, _ vx: Double, _ vy: Double) -> Double {
            atan2(ux * vy - uy * vx, ux * vx + uy * vy)
        }
        let theta1 = angle(1, 0, (x1 - cx1) / rx, (y1 - cy1) / ry)
        var delta = angle((x1 - cx1) / rx, (y1 - cy1) / ry, (-x1 - cx1) / rx, (-y1 - cy1) / ry)
        if !sweep && delta > 0 { delta -= 2 * .pi }
        if sweep && delta < 0 { delta += 2 * .pi }
        let segments = max(1, Int(ceil(abs(delta) / (.pi / 2))))
        let step = delta / Double(segments)
        let t = 4.0 / 3.0 * tan(step / 4)
        var a = theta1
        func onEllipse(_ angle: Double) -> CGPoint {
            CGPoint(x: cx + rx * cos(angle) * cosPhi - ry * sin(angle) * sinPhi,
                    y: cy + rx * cos(angle) * sinPhi + ry * sin(angle) * cosPhi)
        }
        func derivative(_ angle: Double) -> CGPoint {
            CGPoint(x: -rx * sin(angle) * cosPhi - ry * cos(angle) * sinPhi,
                    y: -rx * sin(angle) * sinPhi + ry * cos(angle) * cosPhi)
        }
        for _ in 0..<segments {
            let b = a + step
            let pA = onEllipse(a), pB = onEllipse(b)
            let dA = derivative(a), dB = derivative(b)
            path.addCurve(to: pB,
                          control1: CGPoint(x: pA.x + t * dA.x, y: pA.y + t * dA.y),
                          control2: CGPoint(x: pB.x - t * dB.x, y: pB.y - t * dB.y))
            a = b
        }
    }

    // MARK: Tokens

    private func isCommand(_ b: UInt8) -> Bool {
        switch b {
        case UInt8(ascii: "M"), UInt8(ascii: "m"), UInt8(ascii: "L"), UInt8(ascii: "l"), UInt8(ascii: "H"), UInt8(ascii: "h"),
             UInt8(ascii: "V"), UInt8(ascii: "v"), UInt8(ascii: "C"), UInt8(ascii: "c"), UInt8(ascii: "S"), UInt8(ascii: "s"),
             UInt8(ascii: "Q"), UInt8(ascii: "q"), UInt8(ascii: "T"), UInt8(ascii: "t"), UInt8(ascii: "A"), UInt8(ascii: "a"),
             UInt8(ascii: "Z"), UInt8(ascii: "z"):
            return true
        default:
            return false
        }
    }

    private mutating func skipSeparators() {
        while index < bytes.count, bytes[index] == UInt8(ascii: " ") || bytes[index] == UInt8(ascii: ",")
            || bytes[index] == 9 || bytes[index] == 10 || bytes[index] == 13 {
            index += 1
        }
    }

    private mutating func point(_ base: CGPoint) -> CGPoint? {
        guard let x = number(), let y = number() else { return nil }
        return CGPoint(x: base.x + x, y: base.y + y)
    }

    /// An arc flag: a single 0 or 1, which may be written without a separator ("a1 1 0 01 2 2").
    private mutating func flag() -> Bool? {
        skipSeparators()
        guard index < bytes.count else { return nil }
        let b = bytes[index]
        guard b == UInt8(ascii: "0") || b == UInt8(ascii: "1") else { return nil }
        index += 1
        return b == UInt8(ascii: "1")
    }

    private mutating func number() -> Double? {
        skipSeparators()
        let begin = index
        if index < bytes.count, bytes[index] == UInt8(ascii: "-") || bytes[index] == UInt8(ascii: "+") { index += 1 }
        var seenDot = false, seenDigit = false
        while index < bytes.count {
            let b = bytes[index]
            if b >= UInt8(ascii: "0") && b <= UInt8(ascii: "9") {
                seenDigit = true
            } else if b == UInt8(ascii: "."), !seenDot {
                seenDot = true
            } else {
                break
            }
            index += 1
        }
        if index < bytes.count, seenDigit, bytes[index] == UInt8(ascii: "e") || bytes[index] == UInt8(ascii: "E") {
            index += 1
            if index < bytes.count, bytes[index] == UInt8(ascii: "-") || bytes[index] == UInt8(ascii: "+") { index += 1 }
            while index < bytes.count, bytes[index] >= UInt8(ascii: "0") && bytes[index] <= UInt8(ascii: "9") { index += 1 }
        }
        guard seenDigit, let value = Double(String(decoding: bytes[begin..<index], as: UTF8.self)) else {
            index = begin
            return nil
        }
        return value
    }
}
