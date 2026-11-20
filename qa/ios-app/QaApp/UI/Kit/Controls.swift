import SwiftUI

/// The mark of a one-of list: a 16 square frame, filled with a 3 inset when chosen.
struct Mark: View {
    let selected: Bool

    var body: some View {
        Rectangle()
            .strokeBorder(QaColor.ink, lineWidth: QaSize.line)
            .background { if selected { QaColor.ink.padding(3) } }
            .frame(width: 16, height: 16)
            .accessibilityHidden(true)
    }
}

/// A square switch, 44 by 24: a frame and a 16 knob that moves right and inverts when on.
struct QaSwitch: View {
    let isOn: Bool

    var body: some View {
        ZStack(alignment: isOn ? .trailing : .leading) {
            Rectangle()
                .fill(isOn ? QaColor.ink : .clear)
                .overlay { Rectangle().strokeBorder(QaColor.ink, lineWidth: QaSize.line) }
            Rectangle()
                .fill(isOn ? QaColor.bg : QaColor.ink)
                .frame(width: 16, height: 16)
                .padding(4)
        }
        .frame(width: 44, height: 24)
        .accessibilityHidden(true)
    }
}

/// A 10 square in the colour of a run's outcome.
struct OutcomeSquare: View {
    let color: Color

    var body: some View {
        Rectangle().fill(color).frame(width: 10, height: 10).accessibilityHidden(true)
    }
}

/// One choice of a segmented control.
struct Segment {
    let value: String
    let label: String
    let tag: String
}

/// One of a few choices side by side in an ink frame, the chosen one inverted. The frame carries `tag` and reads as the
/// chosen value; each segment carries its own identifier.
struct Segmented: View {
    let segments: [Segment]
    let selected: String
    let tag: String
    let onSelect: (String) -> Void

    var body: some View {
        HStack(spacing: 0) {
            ForEach(Array(segments.enumerated()), id: \.offset) { index, segment in
                if index > 0 { QaColor.ink.frame(width: QaSize.line) }
                let on = sameText(segment.value, selected)
                Button { onSelect(segment.value) } label: {
                    Text(segment.label)
                        .qaStyle(QaType.label)
                        .lineLimit(1)
                        .foregroundStyle(on ? QaColor.bg : QaColor.ink)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .background(on ? QaColor.ink : .clear)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(on ? [.isButton, .isSelected] : .isButton)
                .accessibilityIdentifier(segment.tag)
            }
        }
        .frame(height: QaSize.control)
        .overlay { Rectangle().strokeBorder(QaColor.ink, lineWidth: QaSize.line) }
        .padding(.horizontal, QaSize.screen)
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier(tag)
        .accessibilityValue(selected)
    }
}

/// The line under a field: a hint in grey or the field's problem in red, 8 below the field.
struct Help: View {
    let text: String
    var isProblem = false

    var body: some View {
        Text(text)
            .qaStyle(QaType.help)
            .foregroundStyle(isProblem ? QaColor.bad : QaColor.muted)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, QaSize.screen)
            .padding(.top, QaSize.gap)
    }
}

/// A grey block that explains something about the screen.
struct Note: View {
    let text: String

    init(_ text: String) {
        self.text = text
    }

    var body: some View {
        Text(text)
            .qaStyle(QaType.help)
            .foregroundStyle(QaColor.muted)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, QaSize.screen)
            .padding(.vertical, 12)
            .background(QaColor.fill)
            .padding(.horizontal, QaSize.screen)
            .padding(.top, 24)
    }
}

/// An underlined word that acts inside a row or a field; its touch area is at least 44 square.
struct QaLink: View {
    let text: String
    let tag: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(text)
                .qaStyle(QaType.link)
                .foregroundStyle(QaColor.ink)
                .frame(minWidth: QaSize.control, minHeight: QaSize.control)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier(tag)
    }
}

/// A chip of a set: 36 tall, inverted when on, in a 44 tall touch area; `ChipFlow` stacks those areas with no gap, which
/// leaves 8 between the lines of chips.
struct Chip: View {
    let text: String
    let on: Bool
    let tag: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(text)
                .qaStyle(QaType.value)
                .lineLimit(1)
                .foregroundStyle(on ? QaColor.bg : QaColor.ink)
                .padding(.horizontal, 12)
                .frame(height: QaSize.chip)
                .background(on ? QaColor.ink : .clear)
                .overlay { Rectangle().strokeBorder(on ? QaColor.ink : QaColor.line, lineWidth: QaSize.line) }
                .frame(height: QaSize.control)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(on ? [.isButton, .isSelected] : .isButton)
        .accessibilityValue(on ? "true" : "false")
        .accessibilityIdentifier(tag)
    }
}

/// Lays its children out in lines, `spacing` between neighbours and no gap between the lines.
struct ChipFlow: Layout {
    var spacing = QaSize.gap

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache _: inout ()) -> CGSize {
        let width = proposal.width ?? .infinity
        let frames = place(subviews, width: width)
        let height = frames.map(\.maxY).max() ?? 0
        return CGSize(width: width.isFinite ? width : (frames.map(\.maxX).max() ?? 0), height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal _: ProposedViewSize, subviews: Subviews, cache _: inout ()) {
        let frames = place(subviews, width: bounds.width)
        for (index, frame) in frames.enumerated() {
            subviews[index].place(
                at: CGPoint(x: bounds.minX + frame.minX, y: bounds.minY + frame.minY),
                proposal: ProposedViewSize(frame.size)
            )
        }
    }

    private func place(_ subviews: Subviews, width: CGFloat) -> [CGRect] {
        var frames: [CGRect] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var lineHeight: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > 0, x + size.width > width {
                x = 0
                y += lineHeight
                lineHeight = 0
            }
            frames.append(CGRect(origin: CGPoint(x: x, y: y), size: size))
            x += size.width + spacing
            lineHeight = max(lineHeight, size.height)
        }
        return frames
    }
}

/// A frame for an input: 52 tall, a grey line that turns ink on focus and red on a problem.
struct FieldFrame<Content: View>: View {
    let focused: Bool
    let isProblem: Bool
    var minHeight = QaSize.field
    @ViewBuilder let content: () -> Content

    var body: some View {
        content()
            .frame(maxWidth: .infinity, minHeight: minHeight, alignment: .leading)
            .overlay { Rectangle().strokeBorder(border, lineWidth: QaSize.line) }
            .padding(.horizontal, QaSize.screen)
    }

    private var border: Color {
        if isProblem { return QaColor.bad }
        return focused ? QaColor.ink : QaColor.line
    }
}
