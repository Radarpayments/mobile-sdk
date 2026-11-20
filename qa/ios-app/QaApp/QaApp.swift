import SwiftUI

@main
struct QaApp: App {
    @StateObject private var session: QaSession

    init() {
        let journal = Journal()
        let checkouts = Checkouts(journal: journal)
        // The launch arguments are read once per process and queued before any link. Input from outside waits until
        // the scene is active, so that a launch in it finds a key window to open the sheet over.
        let session = QaSession(
            store: QaStore(defaults: .standard, secrets: KeychainSecretStore()),
            journal: journal,
            merchant: Merchant(session: makeMerchantSession(), journal: journal),
            arguments: intake(fromArguments: ProcessInfo.processInfo.arguments),
            active: false,
            open: checkouts.open
        )
        _session = StateObject(wrappedValue: session)
    }

    var body: some Scene {
        WindowGroup {
            RootView(session: session)
                .modifier(SceneActivity(session: session))
                .onOpenURL { url in session.accept(intake(fromLink: url.absoluteString)) }
        }
    }
}

private struct SceneActivity: ViewModifier {
    let session: QaSession
    @Environment(\.scenePhase) private var phase

    func body(content: Content) -> some View {
        content
            .onAppear { session.setActive(phase == .active) }
            .onChange(of: phase) { phase in session.setActive(phase == .active) }
    }
}
