import RadarpaymentsSDKPayment
import SwiftUI

/// The journal of the current run: a gateway request and its answer as one item that opens to their bodies, every
/// other entry as a line. `qa_journal_text` still reads one line per entry of what is shown, so pairing on screen does
/// not change what automation reads or what Copy puts on the clipboard.
struct JournalScreen: View {
    @ObservedObject var journal: Journal
    let toaster: Toaster
    let back: () -> Void
    @State private var filter = JournalFilter.all
    @State private var query = ""

    var body: some View {
        let visible = shown(journalItems(journal.entries), filter: filter, query: query)
        let text = visible.flatMap(\.entries).map { $0.line() }.joined(separator: "\n")
        QaScreen(title: "Journal", back: back, scrolls: false, actions: {
            BarAction(text: "Copy", tag: Tags.journalCopy) { copyText("Journal", text, toaster: toaster) }
            BarAction(text: "Clear", tag: Tags.journalClear) { journal.clear() }
        }) {
            VStack(alignment: .leading, spacing: 12) {
                TextInput(text: query, tag: Tags.journalSearch, look: InputLook(placeholder: "Search", style: QaType.inputSmall)) {
                    query = $0
                }
                Segmented(
                    segments: JournalFilter.allCases.map { Segment(value: $0.rawValue, label: $0.label, tag: Tags.journalFilter($0.rawValue)) },
                    selected: filter.rawValue,
                    tag: Tags.journalLevel
                ) { value in filter = JournalFilter.allCases.first { sameText($0.rawValue, value) } ?? .all }
            }
            .padding(.vertical, 12)
            .overlay(alignment: .bottom) { HLine() }
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 0) {
                    if visible.isEmpty {
                        Note(journal.entries.isEmpty ? "The journal is empty" : "Nothing matches")
                    }
                    ForEach(Array(visible.enumerated()), id: \.offset) { _, item in
                        switch item {
                        case let .exchange(exchange): ExchangeItem(exchange: exchange)
                        case let .line(entry): LineItem(entry: entry)
                        }
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .accessibilityElement(children: .contain)
            .accessibilityIdentifier(Tags.journalText)
            .accessibilityValue(text)
        }
    }
}

/// A gateway request and its answer: method and path, then the time, the host and the outcome; tapped, the bodies.
private struct ExchangeItem: View {
    let exchange: JournalExchange
    @State private var open = false

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            (Text(exchange.method).fontWeight(.semibold) + Text(" ") + Text(exchange.path))
                .qaStyle(QaType.journalHead)
            HStack(alignment: .top, spacing: 16) {
                Text("\(timeOfDay(exchange.request.time, precision: .seconds)) · \(exchange.host)")
                    .qaStyle(QaType.journal)
                    .foregroundStyle(QaColor.muted)
                    .lineLimit(1)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text(exchange.outcome)
                    .qaStyle(QaType.journal)
                    .foregroundStyle(exchange.failed ? QaColor.bad : QaColor.ink)
                    .lineLimit(1)
            }
            .padding(.top, 4)
            if open {
                WrappedText(text: detail(of: exchange), style: QaType.journal)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(12)
                    .background(QaColor.fill)
                    .padding(.top, 12)
            }
        }
        .padding(.horizontal, QaSize.screen)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(QaColor.bg)
        .overlay(alignment: .bottom) { HLine() }
        .contentShape(Rectangle())
        .onTapGesture { open.toggle() }
        .accessibilityAddTraits(.isButton)
    }

    private func detail(of exchange: JournalExchange) -> String {
        // Keeps the arrow on the line of a long unbroken body, which then wraps by character as in the mockup.
        let request = "→\u{00A0}\(trimmed(exchange.requestDetail))"
        guard let response = exchange.responseDetail else { return request }
        return "\(request)\n\n←\u{00A0}\(response)"
    }
}

/// One entry: the time, the source, the message (red from a warning up).
private struct LineItem: View {
    let entry: JournalEntry

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Text(timeOfDay(entry.time, precision: .seconds))
                .qaStyle(QaType.journal)
                .foregroundStyle(QaColor.muted)
                .frame(width: 60, alignment: .leading)
            Text(entry.source)
                .qaStyle(QaType.journal)
                .foregroundStyle(QaColor.muted)
                .frame(width: 28, alignment: .leading)
            Text(entry.message)
                .qaStyle(QaType.journal)
                .foregroundStyle(levelRank(entry.level) >= levelRank(.warning) ? QaColor.bad : QaColor.ink)
                .textSelection(.enabled)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.horizontal, QaSize.screen)
        .padding(.vertical, 8)
        .overlay(alignment: .bottom) { HLine() }
    }
}
