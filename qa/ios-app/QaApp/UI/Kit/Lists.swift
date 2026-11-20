import SwiftUI

/// A group's name over it: small capitals, 24 above and 8 below.
struct Caption: View {
    let text: String

    init(_ text: String) {
        self.text = text
    }

    var body: some View {
        Text(text.uppercased())
            .qaStyle(QaType.caption)
            .foregroundStyle(QaColor.muted)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, QaSize.screen)
            .padding(.top, QaSize.captionTop)
            .padding(.bottom, QaSize.captionBottom)
            .accessibilityAddTraits(.isHeader)
    }
}

/// Rows between two full lines. Each row draws its own separator inset by 16 at its top; the group's top line covers
/// the first one.
struct ListGroup<Content: View>: View {
    var tag: String?
    var value: String?
    @ViewBuilder let content: () -> Content

    var body: some View {
        let group = VStack(spacing: 0, content: content)
            .overlay(alignment: .top) { HLine() }
            .overlay(alignment: .bottom) { HLine() }
        if let tag {
            group
                .accessibilityElement(children: .contain)
                .accessibilityIdentifier(tag)
                .accessibilityValue(value ?? "")
        } else {
            group
        }
    }
}

/// One row of a list: 52 tall (68 for two lines), 16 at the sides, 16 between its parts.
struct ListRow<Content: View>: View {
    var minHeight = QaSize.row
    @ViewBuilder let content: () -> Content

    var body: some View {
        HStack(spacing: 16, content: content)
            .padding(.horizontal, QaSize.screen)
            .frame(maxWidth: .infinity, minHeight: minHeight, alignment: .leading)
            .contentShape(Rectangle())
            .overlay(alignment: .top) { HLine(leading: QaSize.screen) }
    }
}

/// A row that opens a screen: a label, its value on the right (red when it is a problem) and a chevron. The row is one
/// element whose value is `readsAs` when it stands for a field, the shown value otherwise.
struct NavRow: View {
    let label: String
    let value: String
    let tag: String
    var valueIsProblem = false
    var readsAs: String?
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            ListRow {
                Text(label).qaStyle(QaType.body).lineLimit(1).layoutPriority(1)
                RowValue(text: value, isProblem: valueIsProblem)
                RowChevron()
            }
        }
        .buttonStyle(RowButtonStyle())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label)
        .accessibilityValue(readsAs ?? value)
        .accessibilityAddTraits(.isButton)
        .accessibilityIdentifier(tag)
    }
}

/// A row's value on the right: monospace, muted (red for a problem), cut at the end.
struct RowValue: View {
    let text: String
    var isProblem = false

    var body: some View {
        Text(text)
            .qaStyle(QaType.value)
            .foregroundStyle(isProblem ? QaColor.bad : QaColor.muted)
            .lineLimit(1)
            .truncationMode(.tail)
            .frame(maxWidth: .infinity, alignment: .trailing)
    }
}

struct RowChevron: View {
    var body: some View {
        Chevron(direction: .right, size: 8, stroke: 1.5)
    }
}

/// A row of two lines: the first given by the caller, a monospace line under it; leading and trailing parts around.
struct TwoLineRow<Leading: View, First: View, Trailing: View>: View {
    @ViewBuilder let leading: () -> Leading
    @ViewBuilder let first: () -> First
    let second: String
    var secondColor = QaColor.muted
    @ViewBuilder let trailing: () -> Trailing

    var body: some View {
        ListRow(minHeight: QaSize.rowTwoLines) {
            leading()
            VStack(alignment: .leading, spacing: 0) {
                first()
                SecondLine(text: second, color: secondColor)
            }
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity, alignment: .leading)
            trailing()
        }
    }
}

/// The second line of a two-line row.
struct SecondLine: View {
    let text: String
    var color = QaColor.muted

    var body: some View {
        Text(text).qaStyle(QaType.value).foregroundStyle(color).lineLimit(1).truncationMode(.tail)
    }
}

/// A row that turns a setting on and off; the whole row toggles and carries the tag, its value `true` or `false`.
struct SwitchRow: View {
    let label: String
    let isOn: Bool
    let tag: String
    let onChange: (Bool) -> Void

    var body: some View {
        Button { onChange(!isOn) } label: {
            ListRow {
                Text(label).qaStyle(QaType.body).lineLimit(1).frame(maxWidth: .infinity, alignment: .leading)
                QaSwitch(isOn: isOn)
            }
        }
        .buttonStyle(RowButtonStyle())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label)
        .accessibilityValue(isOn ? "true" : "false")
        .accessibilityAddTraits(.isButton)
        .accessibilityIdentifier(tag)
    }
}

/// A row of a one-of list: the mark, the label (bold when chosen).
struct MarkRow: View {
    let label: String
    let selected: Bool
    let tag: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            ListRow {
                Mark(selected: selected)
                Text(label)
                    .qaStyle(selected ? QaType.bodyStrong : QaType.body)
                    .lineLimit(1)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .buttonStyle(RowButtonStyle())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label)
        .accessibilityAddTraits(selected ? [.isButton, .isSelected] : .isButton)
        .accessibilityIdentifier(tag)
    }
}

/// A tapped row darkens to the fill colour; nothing else moves.
struct RowButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .foregroundStyle(QaColor.ink)
            .background(configuration.isPressed ? QaColor.fill : QaColor.bg)
    }
}
