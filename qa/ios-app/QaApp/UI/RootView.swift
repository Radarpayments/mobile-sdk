import SwiftUI

private enum Route: Hashable {
    case profiles
    case journal
    case result
}

/// The screens over the one session. A finished run opens its result once; a profile from outside waits for its answer
/// over whatever screen is shown.
struct RootView: View {
    @ObservedObject var session: QaSession
    @State private var path: [Route] = []
    @State private var scanning = false
    // A scanned text is applied once the scanner has closed, so that a launch in it finds the window free for the sheet.
    @State private var scanned: [Intake]?

    var body: some View {
        NavigationStack(path: $path) {
            ConfigScreen(
                session: session,
                form: session.form,
                store: session.store,
                run: session.run,
                scannerShown: scannerShown,
                onProfiles: { path.append(.profiles) },
                onJournal: { openJournal() },
                onScan: { scanning = true }
            )
            .navigationDestination(for: Route.self) { route in
                switch route {
                case .profiles:
                    ProfilesScreen(session: session, store: session.store, form: session.form, editor: session.editor)
                        .toolbar { journalButton }
                case .journal:
                    JournalScreen(journal: session.journal)
                case .result:
                    if let finished = session.shown {
                        ResultScreen(finished: finished, journal: session.journal, onRunAgain: runAgain)
                            .toolbar { journalButton }
                    }
                }
            }
        }
        .overlay {
            if let pending = session.pending {
                ConfirmProfile(pending: pending, onSave: session.confirmPending, onReject: session.rejectPending)
            }
        }
        .onChange(of: session.shown) { shown in
            if shown != nil {
                path = [.result]
            } else if path.contains(.result) {
                path = []
            }
        }
        .sheet(isPresented: $scanning, onDismiss: applyScan) {
            QrScannerView { scan in
                switch scan {
                case let .text(text): scanned = intake(fromText: text)
                case .cancelled: break
                case .unavailable: session.scannerUnavailable()
                }
                scanning = false
            }
            .ignoresSafeArea()
        }
    }

    private var scannerShown: Bool {
        #if targetEnvironment(simulator)
            false
        #else
            QrScannerView.isAvailable
        #endif
    }

    private var journalButton: some ToolbarContent {
        ToolbarItem(placement: .navigationBarTrailing) {
            Button("Journal") { openJournal() }.accessibilityIdentifier(Tags.journalOpen)
        }
    }

    private func openJournal() {
        if path.last != .journal { path.append(.journal) }
    }

    private func runAgain() {
        session.runAgain()
        path = []
    }

    private func applyScan() {
        guard let steps = scanned else { return }
        scanned = nil
        session.accept(steps)
    }
}

/// A profile from outside: saved only on Save, never selected.
private struct ConfirmProfile: View {
    let pending: PendingProfile
    let onSave: () -> Void
    let onReject: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.4).ignoresSafeArea()
            VStack(alignment: .leading, spacing: 12) {
                Text("Save the profile \(pending.profile.name)?").font(.headline)
                Text(pending.profile.gatewayUrl).font(.callout)
                if pending.replaces {
                    Text("It replaces the existing profile \(pending.profile.name).").font(.callout).foregroundStyle(.secondary)
                }
                HStack {
                    Spacer()
                    Button("Reject", action: onReject).accessibilityIdentifier(Tags.profileReject)
                    Button("Save", action: onSave)
                        .buttonStyle(.borderedProminent)
                        .accessibilityIdentifier(Tags.profileConfirm)
                }
            }
            .padding(20)
            .background(RoundedRectangle(cornerRadius: 14).fill(Color(.systemBackground)))
            .padding(32)
        }
        .accessibilityAddTraits(.isModal)
    }
}
