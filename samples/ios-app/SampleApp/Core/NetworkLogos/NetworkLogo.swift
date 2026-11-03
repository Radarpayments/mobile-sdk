import SwiftUI
import sdk_payment_scanner

/// Card network logo in its brand colours, as the SDK's card number field shows it; on a dark background the
/// colours that would vanish switch to light ones (Visa, Amex and Discover turn white).
struct ColorNetworkLogo: View {
    let network: CardNetwork
    let dark: Bool

    var body: some View {
        if let shapes = ColorLogoShapes.of(network) {
            let height = NetworkLogoShapesHeight.mark(network)
            let ratio = shapes.bounds.width / shapes.bounds.height
            let width = min(40, height * ratio)
            Canvas { context, size in
                let scale = min(size.width / shapes.bounds.width, size.height / shapes.bounds.height)
                context.translateBy(x: (size.width - shapes.bounds.width * scale) / 2, y: (size.height - shapes.bounds.height * scale) / 2)
                context.scaleBy(x: scale, y: scale)
                context.translateBy(x: -shapes.bounds.minX, y: -shapes.bounds.minY)
                for (layer, path) in zip(shapes.logo.layers, shapes.paths) {
                    if let gradient = layer.gradient {
                        let stops = gradient.stops.map { Gradient.Stop(color: color($0.1), location: $0.0) }
                        context.fill(path, with: .linearGradient(Gradient(stops: stops), startPoint: gradient.start, endPoint: gradient.end))
                    } else {
                        context.fill(path, with: .color(color(layer.color)))
                    }
                }
            }
            .frame(width: width, height: width / ratio)
            .frame(width: 40, height: 26)
            .accessibilityLabel(Text(verbatim: network.displayName))
        } else {
            Color.clear.frame(width: 40, height: 26)
        }
    }

    private func color(_ argb: UInt32) -> Color {
        guard let shapes = ColorLogoShapes.of(network) else { return .clear }
        return Color(argb: dark ? shapes.logo.onDark[argb] ?? argb : argb)
    }
}

private final class ColorLogoShapes {
    let logo: NetworkColorLogo
    let paths: [Path]
    let bounds: CGRect

    private init(_ logo: NetworkColorLogo) {
        self.logo = logo
        paths = logo.layers.map { svgPath($0.path) }
        bounds = paths.dropFirst().reduce(paths.first?.boundingRect ?? .zero) { $0.union($1.boundingRect) }
    }

    private static var cache: [String: ColorLogoShapes] = [:]

    static func of(_ network: CardNetwork) -> ColorLogoShapes? {
        let name = network.rawValue
        if let cached = cache[name] { return cached }
        let logo: NetworkColorLogo
        switch name {
        case "VISA": logo = NetworkColorLogoData.visa
        case "MASTERCARD": logo = NetworkColorLogoData.mastercard
        case "AMEX": logo = NetworkColorLogoData.amex
        case "UNIONPAY": logo = NetworkColorLogoData.unionPay
        case "JCB": logo = NetworkColorLogoData.jcb
        case "DISCOVER": logo = NetworkColorLogoData.discover
        case "DINERS": logo = NetworkColorLogoData.diners
        case "MAESTRO": logo = NetworkColorLogoData.maestro
        default: return nil
        }
        let shapes = ColorLogoShapes(logo)
        cache[name] = shapes
        return shapes
    }
}

/// Mark heights shared by the monochrome and the colour logos.
enum NetworkLogoShapesHeight {
    static func mark(_ network: CardNetwork) -> CGFloat {
        switch network.rawValue {
        case "VISA": return 12
        case "UNIONPAY", "JCB": return 22
        case "DISCOVER": return 8
        default: return 20
        }
    }
}
