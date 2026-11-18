import SwiftUI

@main
struct QaApp: App {
    @StateObject private var session: QaSession
    // Read once per process: the arguments of a launch, applied when the first screen is up, so that a launch in them
    // finds a window to open the sheet over.
    @State private var launchSteps: [Intake]?

    init() {
        let journal = Journal()
        let checkouts = Checkouts(journal: journal)
        let session = QaSession(
            store: QaStore(defaults: .standard, secrets: KeychainSecretStore()),
            journal: journal,
            merchant: Merchant(session: makeMerchantSession(), journal: journal),
            open: checkouts.open
        )
        _session = StateObject(wrappedValue: session)
        _launchSteps = State(initialValue: intake(fromArguments: ProcessInfo.processInfo.arguments))
    }

    var body: some Scene {
        WindowGroup {
            RootView(session: session)
                .onAppear {
                    guard let steps = launchSteps else { return }
                    launchSteps = nil
                    session.accept(steps)
                }
                .onOpenURL { url in session.accept(intake(fromLink: url.absoluteString)) }
        }
    }
}
