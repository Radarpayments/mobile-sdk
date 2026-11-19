import SwiftUI

/// The app's screens. Each section of the home screen opens one; Stands opens the editor, Order its optional fields.
enum Route: Hashable {
    case stands
    case stand
    case importText
    case order
    case field(String)
    case theme
    case behaviour
    case locale
    case wallet
    case sslPinning
    case result
    case journal
}

/// The screens over the one session. A finished run opens its result once; a profile from outside waits for its answer
/// over whatever screen is shown.
struct RootView: View {
    @ObservedObject var session: QaSession
    @StateObject private var toaster = Toaster()
    @State private var path: [Route] = []
    @State private var scanning = false
    // A scanned text is applied once the scanner has closed, so that a launch in it finds the window free for the sheet.
    @State private var scanned: [Intake]?
    @State private var scannerFailed = false

    var body: some View {
        NavigationStack(path: $path) {
            HomeScreen(
                session: session,
                form: session.form,
                store: session.store,
                run: session.run,
                toaster: toaster,
                scannerShown: scannerShown,
                go: go,
                onImportText: openTextImport,
                onScan: { scanning = true }
            )
            .navigationDestination(for: Route.self, destination: screen)
        }
        .overlay { ToastLayer(toaster: toaster) }
        .overlay {
            if let pending = session.pending {
                ConfirmProfile(pending: pending, onSave: session.confirmPending, onReject: session.rejectPending)
            }
        }
        .onChange(of: session.shown) { shown in
            if shown != nil { path = [.result] }
        }
        .sheet(isPresented: $scanning, onDismiss: applyScan) {
            QrScannerView { scan in
                switch scan {
                case let .text(text): scanned = intake(fromText: text)
                case .cancelled: break
                case .unavailable: scannerFailed = true
                }
                scanning = false
            }
            .ignoresSafeArea()
        }
        .alert("The QR scanner is not available", isPresented: $scannerFailed) {
            Button("OK", role: .cancel) {}
        } message: {
            Text("The camera is missing or access to it is denied.")
        }
    }

    @ViewBuilder
    private func screen(_ route: Route) -> some View {
        let form = session.form
        switch route {
        case .stands:
            StandsScreen(
                session: session,
                form: form,
                store: session.store,
                scannerShown: scannerShown,
                back: back,
                onEdit: { profile in
                    session.editor.load(profile)
                    go(.stand)
                },
                onPasteText: openTextImport,
                onScan: { scanning = true }
            )
        case .stand: StandEditorScreen(session: session, editor: session.editor, toaster: toaster, back: back)
        case .importText: ImportTextScreen(session: session, textImport: session.textImport, back: back)
        case .order: OrderScreen(session: session, form: form, back: back, go: go)
        case let .field(tag): FieldScreen(form: form, tag: tag, back: back)
        case .theme: ThemeScreen(form: form, back: back)
        case .behaviour: BehaviourScreen(form: form, back: back, go: go)
        case .locale: LocaleScreen(form: form, back: back)
        case .wallet: ApplePayScreen(form: form, back: back)
        case .sslPinning: SslPinningScreen(session: session, form: form, back: back)
        case .result:
            if let lastRun = session.lastRun {
                ResultScreen(
                    finished: lastRun.finished,
                    journal: session.journal,
                    toaster: toaster,
                    back: back,
                    onRunAgain: runAgain,
                    openJournal: { go(.journal) }
                )
            } else {
                Color.clear.onAppear { path = [] }
            }
        case .journal: JournalScreen(journal: session.journal, toaster: toaster, back: back)
        }
    }

    private var scannerShown: Bool {
        #if targetEnvironment(simulator)
            false
        #else
            QrScannerView.isAvailable
        #endif
    }

    private func go(_ route: Route) {
        if path.last != route { path.append(route) }
    }

    private func openTextImport() {
        session.textImport.clear()
        go(.importText)
    }

    private func back() {
        if !path.isEmpty { path.removeLast() }
    }

    // Only a finished run is reset: a run started from elsewhere meanwhile is left alone.
    private func runAgain() {
        if case .finished = session.run.state { session.runAgain() }
        path = []
    }

    private func applyScan() {
        guard let steps = scanned else { return }
        scanned = nil
        session.accept(steps)
    }
}

/// A stand from outside: saved only on Save, never selected.
private struct ConfirmProfile: View {
    let pending: PendingProfile
    let onSave: () -> Void
    let onReject: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.4).ignoresSafeArea()
            VStack(alignment: .leading, spacing: 0) {
                Text("Save the stand \(pending.profile.name)?")
                    .qaStyle(QaType.title)
                    .padding(.horizontal, QaSize.screen)
                    .padding(.top, 20)
                Text(pending.profile.gatewayUrl)
                    .qaStyle(QaType.value)
                    .foregroundStyle(QaColor.muted)
                    .padding(.horizontal, QaSize.screen)
                    .padding(.top, QaSize.gap)
                if pending.replaces {
                    Text("It replaces the stored stand \(pending.profile.name).")
                        .qaStyle(QaType.help)
                        .foregroundStyle(QaColor.muted)
                        .padding(.horizontal, QaSize.screen)
                        .padding(.top, QaSize.gap)
                }
                QaFooter {
                    QaButton(text: "Reject", tag: Tags.profileReject, kind: .secondary, action: onReject)
                    QaButton(text: "Save", tag: Tags.profileConfirm, action: onSave)
                }
                .padding(.top, 20)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .foregroundStyle(QaColor.ink)
            .background(QaColor.bg)
            .overlay { Rectangle().strokeBorder(QaColor.line, lineWidth: QaSize.line) }
            .padding(QaSize.screen)
        }
        .accessibilityAddTraits(.isModal)
    }
}
