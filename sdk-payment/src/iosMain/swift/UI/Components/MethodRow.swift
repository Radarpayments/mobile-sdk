import SwiftUI

/// A framed row of the methods list: a leading mark, a title over an optional subtitle, a trailing mark.
/// Without an action, and in a skeleton, it takes no taps.
struct MethodRow<Leading: View, Trailing: View>: View {
    let title: String
    var subtitle: String?
    var numericTitle = false
    var selected = false
    /// A choice among the rows rather than a button.
    var isChoice = false
    var action: (() -> Void)?
    @ViewBuilder let leading: () -> Leading
    @ViewBuilder let trailing: () -> Trailing

    @Environment(\.checkoutTheme) private var theme
    @Environment(\.isPlaceholder) private var isPlaceholder

    var body: some View {
        if let action = action, !isPlaceholder {
            Button(action: action) { row }
                .buttonStyle(PlainPress())
                .accessibilityAddTraits(isChoice && selected ? .isSelected : [])
        } else {
            row
        }
    }

    private var row: some View {
        let colors = theme.colors
        let titleStyle = numericTitle ? theme.body.weight(theme.title.weight).tabular() : theme.body.weight(theme.title.weight)
        let shape = RoundedRectangle(cornerRadius: theme.cardCorner, style: .circular)
        return HStack(spacing: 12) {
            leading()
            VStack(alignment: .leading, spacing: 0) {
                Text(numericTitle ? SavedCardSpecs.shared.leftToRight(text: title) : title)
                    .line(titleStyle, color: colors.onBackground)
                    .truncationMode(.tail)
                    .placeholderText()
                if let subtitle = subtitle {
                    Text(subtitle).line(theme.caption, color: colors.onSurfaceVariant).placeholderText()
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            trailing()
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .frame(minHeight: 60)
        .background(shape.fill(selected ? colors.primarySoft : colors.background))
        .overlay(shape.strokeBorder(selected ? colors.primary : colors.outline, lineWidth: borderWidth(1.5)))
        .contentShape(shape)
        .animation(.spring(response: 0.3, dampingFraction: 1), value: selected)
    }
}

/// A border as thick as Compose draws one: its width is rounded up to whole pixels.
func borderWidth(_ width: CGFloat) -> CGFloat {
    let scale = UIScreen.main.scale
    return (width * scale).rounded(.up) / scale
}

struct RadioMark: View {
    let selected: Bool

    @Environment(\.checkoutTheme) private var theme
    @Environment(\.isPlaceholder) private var isPlaceholder

    var body: some View {
        if isPlaceholder {
            PlaceholderBlock(corner: 11).frame(width: 22, height: 22)
        } else {
            ZStack {
                Circle().strokeBorder(selected ? theme.colors.primary : theme.colors.outlineStrong, lineWidth: borderWidth(2))
                if selected {
                    Circle().fill(theme.colors.primary).frame(width: 10, height: 10)
                }
            }
            .frame(width: 22, height: 22)
            .accessibilityHidden(true)
        }
    }
}

struct ChevronMark: View {
    @Environment(\.checkoutTheme) private var theme
    @Environment(\.isPlaceholder) private var isPlaceholder

    var body: some View {
        if isPlaceholder {
            PlaceholderBlock().frame(width: 8, height: 14).frame(width: 20, height: 20)
        } else {
            IconView(icon: .chevron, size: 20, color: theme.colors.placeholder)
        }
    }
}

/// The dashed card outline with a plus that leads the "New card" row.
struct AddCardPlate: View {
    @Environment(\.checkoutTheme) private var theme
    @Environment(\.isPlaceholder) private var isPlaceholder

    var body: some View {
        if isPlaceholder {
            PlaceholderBlock().frame(width: 40, height: 26)
        } else {
            ZStack {
                DashedPlateOutline(radius: 6)
                    .stroke(theme.colors.outlineStrong, style: StrokeStyle(lineWidth: 1.5, dash: [4, 3]))
                IconView(icon: .plus, size: 16, color: theme.colors.onSurfaceVariant)
            }
            .frame(width: 40, height: 26)
        }
    }
}

/// A rounded rectangle traced clockwise from the bottom of its left edge, as the Android checkout's renderer traces
/// one: where the outline starts decides where the dashes fall.
private struct DashedPlateOutline: Shape {
    let radius: CGFloat

    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.minX, y: rect.maxY - radius))
        path.addLine(to: CGPoint(x: rect.minX, y: rect.minY + radius))
        path.addArc(
            center: CGPoint(x: rect.minX + radius, y: rect.minY + radius),
            radius: radius, startAngle: .degrees(180), endAngle: .degrees(270), clockwise: false
        )
        path.addLine(to: CGPoint(x: rect.maxX - radius, y: rect.minY))
        path.addArc(
            center: CGPoint(x: rect.maxX - radius, y: rect.minY + radius),
            radius: radius, startAngle: .degrees(270), endAngle: .degrees(0), clockwise: false
        )
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY - radius))
        path.addArc(
            center: CGPoint(x: rect.maxX - radius, y: rect.maxY - radius),
            radius: radius, startAngle: .degrees(0), endAngle: .degrees(90), clockwise: false
        )
        path.addLine(to: CGPoint(x: rect.minX + radius, y: rect.maxY))
        path.addArc(
            center: CGPoint(x: rect.minX + radius, y: rect.maxY - radius),
            radius: radius, startAngle: .degrees(90), endAngle: .degrees(180), clockwise: false
        )
        path.closeSubpath()
        return path
    }
}

/// A saved card's network mark in its 40×26 slot.
struct CardNetworkIcon: View {
    let logo: NetworkLogoSpec?

    @Environment(\.isPlaceholder) private var isPlaceholder

    var body: some View {
        if isPlaceholder {
            PlaceholderBlock().frame(width: 40, height: 26)
        } else {
            NetworkLogoView(logo: logo)
        }
    }
}

extension CheckoutTheme {
    /// Whether the network logos take their variants for a dark background, by the Compose UI's own rule.
    var logosAreDark: Bool {
        var red: CGFloat = 0
        var green: CGFloat = 0
        var blue: CGFloat = 0
        var alpha: CGFloat = 0
        UIColor(colors.background).getRed(&red, green: &green, blue: &blue, alpha: &alpha)
        func byte(_ value: CGFloat) -> Int64 { Int64((min(max(value, 0), 1) * 255).rounded()) }
        return SavedCardSpecs.shared.isDark(background: byte(alpha) << 24 | byte(red) << 16 | byte(green) << 8 | byte(blue))
    }
}
