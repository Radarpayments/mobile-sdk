import SwiftUI

let fieldHeight: CGFloat = 54
let groupStroke: CGFloat = 1.5

/// Where Compose puts a child it centres: on whole pixels, half a pixel going down.
func pixelCentredTop(_ height: CGFloat, in container: CGFloat) -> CGFloat {
    let scale = UIScreen.main.scale
    return ((container - height) * scale / 2).rounded(.toNearestOrAwayFromZero) / scale
}

/// The height of a cell's content: the row without the cell's own padding above and below.
let groupContentHeight: CGFloat = fieldHeight - 4

/// Where a picker's value line starts under its risen label. Measured on the Compose form: the half pixel goes up here.
func pickerValueTop(_ height: CGFloat) -> CGFloat {
    let scale = UIScreen.main.scale
    return ((groupContentHeight - labeledInputTop - height) * scale / 2).rounded(.down) / scale
}

extension View {
    /// An icon beside a picker's value. Measured on the Compose form: it sits a pixel above the line's centre.
    func pickerValueIcon() -> some View {
        offset(y: -1 / UIScreen.main.scale)
    }
}

/// Where the value starts under a risen label, from the top of the cell's content.
let labeledInputTop: CGFloat = 20

/// The sides of a cell that lie on the group's frame.
struct GroupCellEdges {
    var top = true
    var bottom = true
    var start = true
    var end = true

    static func of(row: Int, rows: Int, column: Int = 0, columns: Int = 1) -> GroupCellEdges {
        GroupCellEdges(top: row == 0, bottom: row == rows - 1, start: column == 0, end: column == columns - 1)
    }
}

struct GroupFieldStatus {
    var isError = false
    var isValid = false
    var message: String?
}

/// The line over a group or a field: its name, and what can be done with it at the trailing edge.
struct FieldGroupHeader<Trailing: View>: View {
    let title: String
    @ViewBuilder let trailing: () -> Trailing

    @Environment(\.checkoutTheme) private var theme

    var body: some View {
        HStack(spacing: 0) {
            Text(title)
                .line(theme.label, color: theme.colors.onSurfaceVariant)
                .truncationMode(.tail)
                .padding(.top, pixelCentredTop(theme.label.singleLineHeight, in: 20))
                .frame(maxWidth: .infinity, minHeight: 20, alignment: .topLeading)
                .accessibilityAddTraits(.isHeader)
            // The actions keep their touch height without making the label's line taller.
            trailing().frame(height: 20)
        }
        .frame(minHeight: 20)
    }
}

extension FieldGroupHeader where Trailing == EmptyView {
    init(title: String) {
        self.init(title: title) { EmptyView() }
    }
}

/// A rectangle outlined inside its bounds, round only at the chosen corners. The radius is the one of the line's middle,
/// as Compose strokes a shape made for the smaller size.
struct FrameOutline: Shape {
    var radius: CGFloat
    var edges = GroupCellEdges()
    var rightToLeft = false
    /// How far the outline reaches over the hairlines on the cell's inner sides.
    var reach: CGFloat = 0
    var width = groupStroke

    func path(in rect: CGRect) -> Path {
        let left = rightToLeft ? edges.end : edges.start
        let right = rightToLeft ? edges.start : edges.end
        let half = width / 2
        let box = CGRect(
            x: rect.minX - (left ? 0 : reach) + half,
            y: rect.minY - (edges.top ? 0 : reach) + half,
            width: rect.width + (left ? 0 : reach) + (right ? 0 : reach) - width,
            height: rect.height + (edges.top ? 0 : reach) + (edges.bottom ? 0 : reach) - width
        )
        let limit = min(box.width, box.height) / 2
        let topLeft = edges.top && left ? min(radius, limit) : 0
        let topRight = edges.top && right ? min(radius, limit) : 0
        let bottomRight = edges.bottom && right ? min(radius, limit) : 0
        let bottomLeft = edges.bottom && left ? min(radius, limit) : 0
        var path = Path()
        path.move(to: CGPoint(x: box.minX + topLeft, y: box.minY))
        path.addLine(to: CGPoint(x: box.maxX - topRight, y: box.minY))
        path.addArc(
            center: CGPoint(x: box.maxX - topRight, y: box.minY + topRight), radius: topRight,
            startAngle: .degrees(-90), endAngle: .degrees(0), clockwise: false
        )
        path.addLine(to: CGPoint(x: box.maxX, y: box.maxY - bottomRight))
        path.addArc(
            center: CGPoint(x: box.maxX - bottomRight, y: box.maxY - bottomRight), radius: bottomRight,
            startAngle: .degrees(0), endAngle: .degrees(90), clockwise: false
        )
        path.addLine(to: CGPoint(x: box.minX + bottomLeft, y: box.maxY))
        path.addArc(
            center: CGPoint(x: box.minX + bottomLeft, y: box.maxY - bottomLeft), radius: bottomLeft,
            startAngle: .degrees(90), endAngle: .degrees(180), clockwise: false
        )
        path.addLine(to: CGPoint(x: box.minX, y: box.minY + topLeft))
        path.addArc(
            center: CGPoint(x: box.minX + topLeft, y: box.minY + topLeft), radius: topLeft,
            startAngle: .degrees(180), endAngle: .degrees(270), clockwise: false
        )
        path.closeSubpath()
        return path
    }
}

/// The frame of a group of fields: rows of cells divided by hairlines.
struct FieldGroupFrame<Content: View>: View {
    @ViewBuilder let content: () -> Content

    @Environment(\.checkoutTheme) private var theme

    var body: some View {
        VStack(spacing: 0) { content() }
            .frame(maxWidth: .infinity)
            // Behind the cells: a highlighted cell draws its own frame over this one.
            .background(FrameOutline(radius: theme.fieldCorner).stroke(theme.colors.outline, lineWidth: groupStroke))
            .background(theme.colors.background)
            .clipShape(RoundedRectangle(cornerRadius: theme.fieldCorner, style: .circular))
    }
}

struct GroupRow<Content: View>: View {
    var highlighted = false
    @ViewBuilder let content: () -> Content

    var body: some View {
        HStack(spacing: 0) { content() }
            .frame(maxWidth: .infinity)
            .frame(height: fieldHeight)
            .zIndex(highlighted ? 1 : 0)
    }
}

/// A row of two cells with a hairline between them, each as wide as a Compose row makes a weighted child: whole pixels,
/// the pixel that does not divide taken from the first cell.
struct GroupSplitRow<Leading: View, Trailing: View>: View {
    var highlighted = false
    var weights: (CGFloat, CGFloat) = (1, 1)
    @ViewBuilder let leading: () -> Leading
    @ViewBuilder let trailing: () -> Trailing

    var body: some View {
        GeometryReader { proxy in
            HStack(spacing: 0) {
                leading().frame(width: Self.leadingWidth(in: proxy.size.width, weights: weights))
                GroupCellDivider()
                trailing()
            }
        }
        .frame(height: fieldHeight)
        .zIndex(highlighted ? 1 : 0)
    }

    private static func leadingWidth(in width: CGFloat, weights: (CGFloat, CGFloat)) -> CGFloat {
        let scale = UIScreen.main.scale
        let remaining = ((width - borderWidth(groupStroke)) * scale).rounded()
        let unit = remaining / (weights.0 + weights.1)
        let first = (unit * weights.0).rounded(.toNearestOrAwayFromZero)
        let second = (unit * weights.1).rounded(.toNearestOrAwayFromZero)
        let spare = remaining - first - second
        return max(first + (spare == 0 ? 0 : spare / abs(spare)), 0) / scale
    }
}

struct GroupRowDivider: View {
    @Environment(\.checkoutTheme) private var theme

    var body: some View {
        // A layout size lands on whole pixels in Compose, unlike the stroke of the frame.
        theme.colors.outline.frame(height: borderWidth(groupStroke))
    }
}

struct GroupCellDivider: View {
    @Environment(\.checkoutTheme) private var theme

    var body: some View {
        theme.colors.outline.frame(width: borderWidth(groupStroke)).frame(maxHeight: .infinity)
    }
}

/// A cell of a group: its content, then the mark of a valid value.
struct GroupCell<Content: View>: View {
    var highlight: Color?
    var edges = GroupCellEdges()
    var valid = false
    @ViewBuilder let content: () -> Content

    @Environment(\.checkoutTheme) private var theme
    @Environment(\.layoutDirection) private var direction

    var body: some View {
        HStack(spacing: 4) {
            content()
            if valid {
                IconView(icon: .check, size: 18, color: theme.colors.success)
            }
        }
        .padding(EdgeInsets(top: 2, leading: 14, bottom: 2, trailing: 10))
        .frame(maxWidth: .infinity)
        .frame(height: fieldHeight)
        .overlay(outline)
        .zIndex(highlight == nil ? 0 : 1)
    }

    @ViewBuilder private var outline: some View {
        if let highlight = highlight {
            FrameOutline(radius: theme.fieldCorner, edges: edges, rightToLeft: direction == .rightToLeft, reach: groupStroke)
                .stroke(highlight, lineWidth: groupStroke)
                .allowsHitTesting(false)
        }
    }
}

/// A cell that is a button: a picker's value, or an action among the fields.
struct GroupButtonCell<Content: View>: View {
    var highlight: Color?
    var edges = GroupCellEdges()
    var enabled = true
    var identifier: String?
    let action: () -> Void
    @ViewBuilder let content: () -> Content

    var body: some View {
        Button(action: action) {
            GroupCell(highlight: highlight, edges: edges, content: content).contentShape(Rectangle())
        }
        .buttonStyle(PlainPress())
        .disabled(!enabled)
        .accessibility(identifier: identifier ?? "")
        .zIndex(highlight == nil ? 0 : 1)
    }
}

/// The name of a field inside its cell: the hint while the field is empty, a small line over the value after.
struct GroupFieldLabel: View {
    enum Tone { case hint, name, focus, error }

    let text: String
    let raised: Bool
    let tone: Tone

    @Environment(\.checkoutTheme) private var theme
    @Environment(\.layoutDirection) private var direction
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    static func tone(raised: Bool, focused: Bool, isError: Bool) -> Tone {
        if isError { return .error }
        if !raised { return .hint }
        return focused ? .focus : .name
    }

    var body: some View {
        let colors = theme.colors
        let color: Color = tone == .error ? colors.error : tone == .hint ? colors.placeholder
            : tone == .focus ? colors.primary : colors.onSurfaceVariant
        Text(text)
            .line(theme.input, color: color)
            .truncationMode(.tail)
            .scaleEffect(raised ? 0.75 : 1, anchor: .leading)
            .offset(y: raised ? -14 : 0)
            .padding(.top, pixelCentredTop(theme.input.singleLineHeight, in: groupContentHeight))
            .frame(height: groupContentHeight, alignment: .top)
            .frame(maxWidth: .infinity, alignment: .leading)
            .animation(reduceMotion ? nil : .linear(duration: 0.15), value: raised)
            .animation(.linear(duration: 0.15), value: tone)
            .allowsHitTesting(false)
            .accessibilityHidden(true)
    }
}

/// A field of a group. With `floating` the placeholder is the field's label and rises over the value.
struct GroupTextCell<Trailing: View>: View {
    let text: String
    let placeholder: String
    var hint = ""
    var floating = false
    var content: FieldContent = .none
    var numberPad = false
    var status = GroupFieldStatus()
    var highlight: Color?
    var edges = GroupCellEdges()
    var enabled = true
    var autoFocus = false
    var label: String?
    var identifier: String?
    let onChange: (String) -> Void
    var kept: (() -> String)?
    var onFocus: (Bool) -> Void = { _ in }
    @ViewBuilder let trailing: () -> Trailing

    @State private var focused = false

    var body: some View {
        GroupCell(highlight: highlight, edges: edges, valid: status.isValid && !status.isError) {
            ZStack(alignment: .leading) {
                let raised = focused || !text.isEmpty
                FormTextField(
                    text: text,
                    placeholder: floating ? (raised ? hint : "") : placeholder,
                    content: content,
                    numberPad: numberPad,
                    enabled: enabled,
                    autoFocus: autoFocus,
                    label: label ?? placeholder,
                    errorMessage: status.message,
                    identifier: identifier,
                    onChange: onChange,
                    kept: kept,
                    onFocus: {
                        focused = $0
                        onFocus($0)
                    }
                )
                .padding(.top, floating ? labeledInputTop : 0)
                if floating {
                    GroupFieldLabel(
                        text: placeholder,
                        raised: raised,
                        tone: GroupFieldLabel.tone(raised: raised, focused: focused, isError: status.isError)
                    )
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            trailing()
        }
    }
}

extension GroupTextCell where Trailing == EmptyView {
    init(
        text: String, placeholder: String, hint: String = "", floating: Bool = false, content: FieldContent = .none,
        numberPad: Bool = false, status: GroupFieldStatus = GroupFieldStatus(), highlight: Color? = nil,
        edges: GroupCellEdges = GroupCellEdges(), enabled: Bool = true, autoFocus: Bool = false, label: String? = nil,
        identifier: String? = nil, onChange: @escaping (String) -> Void, kept: (() -> String)? = nil,
        onFocus: @escaping (Bool) -> Void = { _ in }
    ) {
        self.init(
            text: text, placeholder: placeholder, hint: hint, floating: floating, content: content, numberPad: numberPad,
            status: status, highlight: highlight, edges: edges, enabled: enabled, autoFocus: autoFocus, label: label,
            identifier: identifier, onChange: onChange, kept: kept, onFocus: onFocus
        ) { EmptyView() }
    }
}

/// What a picker's cell shows: the label, risen over the chosen value.
struct GroupPickerContent<Value: View>: View {
    let label: String
    var isError = false
    let hasValue: Bool
    @ViewBuilder let value: () -> Value

    @Environment(\.checkoutTheme) private var theme

    var body: some View {
        ZStack(alignment: .leading) {
            if hasValue {
                HStack(spacing: 0) { value() }
                    .padding(.top, pickerValueTop(theme.input.singleLineHeight))
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                    .padding(.top, labeledInputTop)
            }
            GroupFieldLabel(text: label, raised: hasValue, tone: GroupFieldLabel.tone(raised: hasValue, focused: false, isError: isError))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

/// The message under a group or a field.
struct FieldMessage: View {
    let text: String
    var isError = true

    @Environment(\.checkoutTheme) private var theme

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(text.components(separatedBy: "\n").enumerated()), id: \.offset) { line in
                let color = isError ? theme.colors.error : theme.colors.onSurfaceVariant
                LeadingParagraph(text: line.element, style: theme.caption, color: color)
                    .padding(.top, line.offset == 0 ? 0 : Self.gap(above: line.offset, style: theme.caption))
            }
        }
    }

    /// The room above a line that is not the first. Skia stretches the ascent and the descent of a line to the line
    /// height, leaves the first ascent as the font has it, and rounds each baseline to a pixel — so the second line
    /// comes a pixel closer than the line height says.
    private static func gap(above line: Int, style: CheckoutTextStyle) -> CGFloat {
        let scale = UIScreen.main.scale
        let font = style.uiFont
        let ascent = font.ascender * scale
        let descent = -font.descender * scale
        let stretch = style.lineHeight * scale / (ascent + descent)
        let single = style.singleLineHeight * scale
        let pitch = (style.lineHeight * scale).rounded()
        guard line == 1 else { return max(pitch - single, 0) / scale }
        let first = (ascent + descent * stretch).rounded()
        let second = (first + ascent * stretch).rounded() - ascent.rounded()
        return max(second - single, 0) / scale
    }
}
