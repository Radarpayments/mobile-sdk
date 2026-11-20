import SwiftUI

/// The bottom of a screen under a line: an optional reason in red, then the screen's buttons.
struct QaFooter<Why: View, Buttons: View>: View {
    @ViewBuilder let why: () -> Why
    @ViewBuilder let buttons: () -> Buttons

    var body: some View {
        VStack(alignment: .leading, spacing: QaSize.gap) {
            why()
            HStack(spacing: QaSize.gap, content: buttons)
        }
        .padding(.horizontal, QaSize.screen)
        .padding(.top, 12)
        .padding(.bottom, QaSize.screen)
        .frame(maxWidth: .infinity)
        .background(QaColor.bg)
        .overlay(alignment: .top) { HLine() }
    }
}

extension QaFooter where Why == EmptyView {
    init(@ViewBuilder buttons: @escaping () -> Buttons) {
        self.init(why: { EmptyView() }, buttons: buttons)
    }
}

/// The problem a footer explains: `<field>: <message>`, the text of `qa_validation_error`.
struct FooterWhy: View {
    let problem: Problem?
    let tag: String

    var body: some View {
        if let problem {
            let text = problemText(problem)
            Text(text)
                .qaStyle(QaType.help)
                .foregroundStyle(QaColor.bad)
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityIdentifier(tag)
                .accessibilityValue(text)
        }
    }
}

/// A footer button, 52 tall: ink on the primary one, an ink frame on the secondary one, grey while off.
struct QaButton: View {
    enum Kind {
        case primary
        case secondary
    }

    let text: String
    let tag: String
    var kind = Kind.primary
    var enabled = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(text)
                .qaStyle(QaType.button)
                .lineLimit(1)
                .foregroundStyle(content)
                .frame(maxWidth: .infinity, minHeight: QaSize.button, maxHeight: QaSize.button)
                .background(background)
                .overlay { if enabled && kind == .secondary { Rectangle().strokeBorder(QaColor.ink, lineWidth: QaSize.line) } }
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .accessibilityIdentifier(tag)
    }

    private var background: Color {
        if !enabled { return QaColor.line }
        return kind == .primary ? QaColor.ink : .clear
    }

    private var content: Color {
        if !enabled { return QaColor.muted }
        return kind == .primary ? QaColor.bg : QaColor.ink
    }
}

/// `<field>: <message>`, the text of `qa_validation_error`.
func problemText(_ problem: Problem) -> String {
    "\(problem.field): \(problem.message)"
}
