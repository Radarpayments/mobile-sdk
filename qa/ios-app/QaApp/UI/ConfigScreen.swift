import SwiftUI

/// The configuration: the stand profile, the five groups and the Launch button pinned under them.
struct ConfigScreen: View {
    @ObservedObject var session: QaSession
    @ObservedObject var form: ConfigForm
    @ObservedObject var store: QaStore
    @ObservedObject var run: QaRun
    let scannerShown: Bool
    let onProfiles: () -> Void
    let onJournal: () -> Void
    let onScan: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 4) {
                    Choice(
                        tag: Tags.profile,
                        label: "Stand profile",
                        value: form.text(Tags.profile),
                        options: store.profiles.map(\.name)
                    ) { session.select($0) }
                    if let problem = form.problem(Tags.profile) ?? session.profileProblem {
                        FieldProblem(problem: problem)
                    }
                }
                ConfigGroups(form: form)
            }
            .padding()
        }
        .scrollDismissesKeyboard(.interactively)
        .safeAreaInset(edge: .bottom) { launchBar }
        .navigationTitle("PAYRDR QA")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItemGroup(placement: .navigationBarTrailing) {
                Button("Profiles", action: onProfiles).accessibilityIdentifier(Tags.profilesOpen)
                Button("Journal", action: onJournal).accessibilityIdentifier(Tags.journalOpen)
                menu
            }
        }
    }

    private var menu: some View {
        Menu {
            Button("Import from clipboard") {
                session.accept(intake(fromText: UIPasteboard.general.string ?? ""))
            }
            .accessibilityIdentifier(Tags.importClipboard)
            if scannerShown {
                Button("Import from QR code", action: onScan).accessibilityIdentifier(Tags.importQr)
            }
            Button("Export to clipboard") {
                UIPasteboard.general.string = QaConfigJson.encode(form.config)
            }
            .accessibilityIdentifier(Tags.export)
            Button("Reset", role: .destructive) { session.reset() }.accessibilityIdentifier(Tags.reset)
        } label: {
            Image(systemName: "ellipsis.circle").accessibilityLabel("Menu")
        }
        .accessibilityIdentifier(Tags.menu)
    }

    private var launchBar: some View {
        VStack(alignment: .leading, spacing: 8) {
            if let problem = session.shownProblem {
                Text(problemText(problem))
                    .font(.footnote)
                    .foregroundStyle(.red)
                    .accessibilityIdentifier(Tags.validationError)
            }
            Button {
                session.launch()
            } label: {
                Text(run.state.isBusy ? "Running…" : "Launch").frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .disabled(!session.canLaunch)
            .accessibilityIdentifier(Tags.launch)
        }
        .padding()
        .background(.bar)
    }
}
