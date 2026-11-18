import RadarpaymentsSDKPayment
import SwiftUI

/// The lines of the current run, from a level up and containing the search text, as one text that can be selected.
struct JournalScreen: View {
    @ObservedObject var journal: Journal
    @State private var level = LogLevel.debug
    @State private var query = ""

    private static let levels: [LogLevel] = [.debug, .info, .warning, .error]

    var body: some View {
        let text = filtered(journal.entries, minLevel: level, query: query).map { $0.line() }.joined(separator: "\n")
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Choice(
                    tag: Tags.journalLevel,
                    label: "Level",
                    value: levelName(level),
                    options: Self.levels.map(levelName)
                ) { name in
                    level = Self.levels.first { sameText(levelName($0), name) } ?? .debug
                }
                Button("Clear") { journal.clear() }.accessibilityIdentifier(Tags.journalClear)
            }
            TextField("Search", text: $query)
                .textFieldStyle(.roundedBorder)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .accessibilityIdentifier(Tags.journalSearch)
            ScrollView {
                Text(text)
                    .font(.system(.caption, design: .monospaced))
                    .textSelection(.enabled)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .accessibilityIdentifier(Tags.journalText)
            }
        }
        .padding()
        .navigationTitle("Journal")
        .navigationBarTitleDisplayMode(.inline)
    }
}
