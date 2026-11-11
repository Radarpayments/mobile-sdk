import SwiftUI
import UIKit

/// A country's flag. Drawn once into a bitmap of the screen's pixels, as the Compose checkout draws it.
struct FlagIcon: View {
    let emoji: String
    var size: CGFloat = 20

    private static let glyphFill: CGFloat = 0.82
    private static var cache: [String: UIImage] = [:]

    var body: some View {
        Image(uiImage: Self.image(of: emoji, side: (size * UIScreen.main.scale).rounded()))
            .resizable()
            .frame(width: size, height: size)
            .accessibilityHidden(true)
    }

    private static func image(of emoji: String, side: CGFloat) -> UIImage {
        let key = "\(emoji)-\(Int(side))"
        if let cached = cache[key] { return cached }
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        let rect = CGRect(x: 0, y: 0, width: side, height: side)
        let image = UIGraphicsImageRenderer(size: rect.size, format: format).image { _ in
            (emoji as NSString).draw(in: rect, withAttributes: [.font: UIFont.systemFont(ofSize: side * glyphFill)])
        }
        cache[key] = image
        return image
    }
}
