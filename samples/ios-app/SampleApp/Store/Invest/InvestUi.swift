import SwiftUI

/// Northwind Invest's tokens: dark OLED navy with green for growth (ui-ux-pro-max "fintech investing").
enum InvestColors {
    static let background: UInt32 = 0xFF02_0617
    static let surface: UInt32 = 0xFF0B_1222
    static let surface2: UInt32 = 0xFF11_1A2E
    static let divider: UInt32 = 0xFF1E_293B
    static let fieldBorder: UInt32 = 0xFF33_4155
    static let text: UInt32 = 0xFFF8_FAFC
    static let muted: UInt32 = 0xFF94_A3B8
    static let accent: UInt32 = 0xFF22_C55E
    static let onAccent: UInt32 = 0xFF05_2E16
    static let error: UInt32 = 0xFFF8_7171
}

/// The tokens as SwiftUI colours.
enum NW {
    static let background = Color(argb: InvestColors.background)
    static let surface = Color(argb: InvestColors.surface)
    static let surface2 = Color(argb: InvestColors.surface2)
    static let divider = Color(argb: InvestColors.divider)
    static let fieldBorder = Color(argb: InvestColors.fieldBorder)
    static let text = Color(argb: InvestColors.text)
    static let muted = Color(argb: InvestColors.muted)
    static let accent = Color(argb: InvestColors.accent)
    static let onAccent = Color(argb: InvestColors.onAccent)
    static let accentSoft = accent.opacity(0.12)
    static let error = Color(argb: InvestColors.error)
    static let glb = Color(argb: 0xFF1E_3A8A)
    static let onGlb = Color(argb: 0xFFBF_DBFE)
    static let tch = Color(argb: 0xFF4C_1D95)
    static let onTch = Color(argb: 0xFFDD_D6FE)

    /// IBM Plex Sans; amounts with tabular figures so they do not jump while counting.
    static func font(_ size: CGFloat, _ weight: Font.Weight = .regular) -> Font { .custom("IBM Plex Sans", size: size).weight(weight) }
}

/// Euro `minor` units in the app's language, e.g. "€1,578.00".
func eur(_ minor: Int64) -> String { formatEur(minor) }

/// Whole euros without the cents, for the preset chips ("€250").
func eurWhole(_ minor: Int64) -> String {
    let format = NumberFormatter()
    format.numberStyle = .currency
    format.currencyCode = "EUR"
    format.locale = AppLanguage.locale
    format.maximumFractionDigits = 0
    return format.string(from: NSNumber(value: Double(minor) / 100)) ?? "€\(minor / 100)"
}

/// "29 Oct 2026, 13:42" in the app's language.
func operationTime(_ at: Int64) -> String {
    let format = DateFormatter()
    format.locale = AppLanguage.locale
    format.dateStyle = .medium
    format.timeStyle = .short
    return format.string(from: Date(timeIntervalSince1970: Double(at) / 1000))
}

/// A Northwind card: surface, hairline border, optionally outlined in green.
struct NWCard<Content: View>: View {
    var highlight = false
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 0) { content }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(NW.surface)
            .clipShape(RoundedRectangle(cornerRadius: 18))
            .overlay(RoundedRectangle(cornerRadius: 18).strokeBorder(highlight ? NW.accent.opacity(0.7) : NW.divider, lineWidth: 1))
    }
}

/// The green main button; shows progress while `busy`.
struct NWPrimaryButton: View {
    let label: String
    var icon: String?
    var enabled = true
    var busy = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if busy { ProgressView().tint(NW.onAccent) } else if let icon { Image(systemName: icon) }
                Text(label).font(NW.font(16, .bold)).lineLimit(1)
            }
            .foregroundColor(NW.onAccent)
            .frame(maxWidth: .infinity, minHeight: 52)
            .background(NW.accent.opacity(enabled || busy ? 1 : 0.35))
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
        .disabled(!enabled || busy)
    }
}

/// The outlined second button.
struct NWOutlineButton: View {
    let label: String
    var icon: String?
    var enabled = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if let icon { Image(systemName: icon) }
                Text(label).font(NW.font(16, .bold)).lineLimit(1).minimumScaleFactor(0.8)
            }
            .foregroundColor(enabled ? NW.text : NW.muted)
            .frame(maxWidth: .infinity, minHeight: 52)
            .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(NW.fieldBorder.opacity(enabled ? 1 : 0.5), lineWidth: 1))
            .contentShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

/// Round back button of the Northwind screens.
struct NWBackButton: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: "chevron.left")
                .font(.system(size: 17, weight: .semibold))
                .foregroundColor(NW.text)
                .frame(width: 44, height: 44)
                .background(NW.surface)
                .clipShape(Circle())
                .overlay(Circle().strokeBorder(NW.divider, lineWidth: 1))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text("Back"))
    }
}

/// The square badge of a fund or an operation.
struct NWBadge<Content: View>: View {
    let background: Color
    @ViewBuilder let content: Content

    var body: some View {
        content
            .frame(width: 40, height: 40)
            .background(background)
            .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

struct TickerBadge: View {
    let ticker: String

    var body: some View {
        let glb = ticker == InvestAccount.globalEtf
        NWBadge(background: glb ? NW.glb : NW.tch) {
            Text(verbatim: ticker).font(NW.font(12, .bold)).foregroundColor(glb ? NW.onGlb : NW.onTch)
        }
    }
}

func assetName(_ ticker: String) -> String {
    ticker == InvestAccount.globalEtf
        ? String(localized: "Global ETF", bundle: AppLanguage.bundle)
        : String(localized: "Tech Leaders", bundle: AppLanguage.bundle)
}

/// A label–value line of a details card.
struct DetailRow<Value: View>: View {
    let label: String
    var divider = true
    @ViewBuilder let value: Value

    var body: some View {
        VStack(spacing: 0) {
            if divider { NW.divider.frame(height: 1) }
            HStack {
                Text(label).font(NW.font(14)).foregroundColor(NW.muted)
                Spacer(minLength: 12)
                value
            }
            .padding(.vertical, 10)
        }
    }
}

/// The portfolio's line with a fading area under it, ending in a dot.
struct PortfolioChart: View {
    let period: ChartPeriod

    var body: some View {
        GeometryReader { geo in
            let points = period.points
            let step = geo.size.width / CGFloat(points.count - 1)
            let pad: CGFloat = 6
            let at = { (i: Int) in CGPoint(x: CGFloat(i) * step, y: pad + (1 - points[i]) * (geo.size.height - 2 * pad)) }
            let line = Path { p in
                for i in points.indices { i == 0 ? p.move(to: at(i)) : p.addLine(to: at(i)) }
            }
            ZStack {
                Path { p in
                    p.addPath(line)
                    p.addLine(to: CGPoint(x: geo.size.width, y: geo.size.height))
                    p.addLine(to: CGPoint(x: 0, y: geo.size.height))
                    p.closeSubpath()
                }
                .fill(LinearGradient(colors: [NW.accent.opacity(0.28), NW.accent.opacity(0)], startPoint: .top, endPoint: .bottom))
                line.stroke(NW.accent, style: StrokeStyle(lineWidth: 2.2, lineJoin: .round))
                Circle().fill(NW.accent).frame(width: 8, height: 8).position(at(points.count - 1))
            }
        }
        // Time runs left to right in every language.
        .environment(\.layoutDirection, .leftToRight)
        .accessibilityHidden(true)
    }
}

/// Periods of the portfolio chart; each shows its own fixed curve (the demo has no price history).
enum ChartPeriod: CaseIterable {
    case week, month, quarter, year, all

    var label: String {
        switch self {
        case .week: return String(localized: "1W", bundle: AppLanguage.bundle)
        case .month: return String(localized: "1M", bundle: AppLanguage.bundle)
        case .quarter: return String(localized: "3M", bundle: AppLanguage.bundle)
        case .year: return String(localized: "1Y", bundle: AppLanguage.bundle)
        case .all: return String(localized: "All", bundle: AppLanguage.bundle)
        }
    }

    var points: [CGFloat] {
        switch self {
        case .week: return [0.55, 0.5, 0.58, 0.52, 0.6, 0.57, 0.66]
        case .month: return [0.18, 0.24, 0.21, 0.35, 0.31, 0.44, 0.41, 0.57, 0.52, 0.68, 0.64, 0.8, 0.85]
        case .quarter: return [0.3, 0.22, 0.35, 0.28, 0.45, 0.4, 0.52, 0.6, 0.55, 0.72]
        case .year: return [0.1, 0.2, 0.16, 0.3, 0.42, 0.38, 0.5, 0.47, 0.62, 0.7, 0.66, 0.82]
        case .all: return [0.05, 0.12, 0.1, 0.22, 0.3, 0.28, 0.45, 0.55, 0.5, 0.7, 0.78, 0.9]
        }
    }
}
