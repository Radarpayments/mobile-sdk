import SwiftUI

/// A card network's logo in its brand colours, centred in a slot of one size for every network.
struct NetworkLogoView: View {
    let logo: NetworkLogoSpec?

    private let slot = CGSize(width: 40, height: 26)

    var body: some View {
        ZStack {
            if let logo = logo {
                let shapes = LogoShapes(logo)
                let ratio = shapes.bounds.width / max(shapes.bounds.height, 1)
                let width = min(slot.width, CGFloat(logo.height) * ratio)
                ZStack {
                    ForEach(shapes.layers.indices, id: \.self) { index in
                        let layer = shapes.layers[index]
                        LogoLayerShape(path: layer.path, bounds: shapes.bounds).fill(layer.fill)
                    }
                }
                .frame(width: width, height: width / ratio)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(Text(logo.name))
            }
        }
        .frame(width: slot.width, height: slot.height)
    }
}

private struct LogoShapes {
    struct Layer {
        let path: Path
        let fill: LinearGradient
    }

    let layers: [Layer]
    let bounds: CGRect

    init(_ logo: NetworkLogoSpec) {
        let paths = logo.layers.map { Path(SVGPath.parse($0.path)) }
        let box = paths.map(\.boundingRect).reduce(CGRect.null) { $0.union($1) }
        bounds = box.isNull ? .zero : box
        let size = bounds.size
        let origin = bounds.origin
        layers = zip(logo.layers, paths).map { spec, path in
            guard let gradient = spec.gradient, size.width > 0, size.height > 0 else {
                let color = ARGB(spec.color).color
                return Layer(path: path, fill: LinearGradient(gradient: Gradient(colors: [color]), startPoint: .leading, endPoint: .trailing))
            }
            let stops = LogoShapes.stops(
                offsets: gradient.offsets.map(\.doubleValue),
                colors: gradient.colors.map { ARGB($0.int64Value) }
            )
            // The gradient's points are in the logo's coordinates; a fill takes them as shares of the drawn box.
            func unit(_ x: Double, _ y: Double) -> UnitPoint {
                UnitPoint(x: (CGFloat(x) - origin.x) / size.width, y: (CGFloat(y) - origin.y) / size.height)
            }
            return Layer(
                path: path,
                fill: LinearGradient(
                    gradient: Gradient(stops: stops),
                    startPoint: unit(gradient.startX, gradient.startY),
                    endPoint: unit(gradient.endX, gradient.endY)
                )
            )
        }
    }

    /// SwiftUI blends between two stops perceptually, the logos were drawn for a plain sRGB blend: the stops in
    /// between are laid out here, close enough for the two to agree.
    private static func stops(offsets: [Double], colors: [ARGB]) -> [Gradient.Stop] {
        guard offsets.count > 1 else {
            return zip(offsets, colors).map { Gradient.Stop(color: $1.color, location: CGFloat($0)) }
        }
        var stops: [Gradient.Stop] = []
        for index in 0..<(offsets.count - 1) {
            let from = colors[index]
            let to = colors[index + 1]
            for step in 0..<stopsPerSpan {
                let share = Double(step) / Double(stopsPerSpan)
                func mix(_ start: Double, _ end: Double) -> Double { start + (end - start) * share }
                stops.append(
                    Gradient.Stop(
                        color: Color(
                            .sRGB,
                            red: mix(from.red, to.red),
                            green: mix(from.green, to.green),
                            blue: mix(from.blue, to.blue),
                            opacity: mix(from.alpha, to.alpha)
                        ),
                        location: CGFloat(mix(offsets[index], offsets[index + 1]))
                    )
                )
            }
        }
        stops.append(Gradient.Stop(color: colors[colors.count - 1].color, location: CGFloat(offsets[offsets.count - 1])))
        return stops
    }

    private static let stopsPerSpan = 16
}

/// One layer scaled from the logo's box to the view. The shape spans the whole view so that a gradient fill, which
/// is laid over the shape's frame, lines up with the logo's box.
private struct LogoLayerShape: Shape {
    let path: Path
    let bounds: CGRect

    func path(in rect: CGRect) -> Path {
        guard bounds.width > 0, bounds.height > 0 else { return Path() }
        let scale = min(rect.width / bounds.width, rect.height / bounds.height)
        let dx = rect.minX + (rect.width - bounds.width * scale) / 2
        let dy = rect.minY + (rect.height - bounds.height * scale) / 2
        let transform = CGAffineTransform(translationX: dx, y: dy)
            .scaledBy(x: scale, y: scale)
            .translatedBy(x: -bounds.minX, y: -bounds.minY)
        return path.applying(transform)
    }
}
