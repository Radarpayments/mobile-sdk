import SwiftUI

/// The home screen: one row per section with its state in words, a section's problem in red on its row, Launch in the
/// footer.
struct HomeScreen: View {
    @ObservedObject var session: QaSession
    @ObservedObject var form: ConfigForm
    @ObservedObject var store: QaStore
    @ObservedObject var run: QaRun
    let toaster: Toaster
    let scannerShown: Bool
    let go: (Route) -> Void
    let onScan: () -> Void

    var body: some View {
        let config = form.config
        let profile = session.selectedProfile
        let launchCheck = session.launchCheck
        let problems = [form.problem].compactMap { $0 } + launchProblems(config: config, profile: profile)
        let problemOf = { (row: HomeRow) in problems.first { homeRow($0.field) == row } }
        QaScreen(title: "QA Sample App", actions: { menu }, footer: {
            QaFooter {
                FooterWhy(problem: session.shownProblem ?? launchCheck, tag: Tags.validationError)
            } buttons: {
                QaButton(
                    text: run.state.isBusy ? "Running…" : "Launch",
                    tag: Tags.launch,
                    enabled: session.canLaunch
                ) { session.launch() }
            }
        }) {
            Caption("Stand")
            ListGroup {
                StandHomeRow(name: config.profile, profile: profile, problem: problemOf(.stand)) { go(.stands) }
            }
            Caption("Order")
            ListGroup {
                summaryRow("Order", orderSummary(config.order), Tags.groupOrder, problemOf(.order))
                if config.order.source == .register {
                    summaryRow("Amount", amountSummary(config.order), Tags.homeAmount, problemOf(.amount))
                    summaryRow("Features", featuresSummary(config.order), Tags.homeFeatures, problemOf(.features))
                }
            }
            Caption("Checkout")
            ListGroup {
                summaryRow("Theme", config.sdk.theme.rawValue, Tags.groupTheme, problemOf(.theme), route: .theme)
                summaryRow("Behaviour", behaviourSummary(config.sdk), Tags.groupSdk, problemOf(.behaviour), route: .behaviour)
                summaryRow("Apple Pay", applePaySummary(config.sdk), Tags.groupWallets, problemOf(.wallet), route: .wallet)
                summaryRow("SSL pinning", sslPinningSummary(config.sdk), Tags.groupSecurity, problemOf(.sslPinning), route: .sslPinning)
            }
            if let lastRun = session.lastRun {
                Caption("Last run")
                ListGroup {
                    LastRunRow(lastRun: lastRun) { go(.result) }
                }
            }
        }
    }

    private func summaryRow(
        _ label: String,
        _ summary: String,
        _ tag: String,
        _ problem: Problem?,
        route: Route = .order,
        readsAs: String? = nil
    ) -> some View {
        NavRow(
            label: label,
            value: problem.map(rowProblem) ?? summary,
            tag: tag,
            valueIsProblem: problem != nil,
            readsAs: readsAs
        ) { go(route) }
    }

    private var menu: some View {
        BarMenu(tag: Tags.menu) {
            Button("Import from clipboard") { session.accept(intake(fromText: UIPasteboard.general.string ?? "")) }
                .accessibilityIdentifier(Tags.importClipboard)
            if scannerShown {
                Button("Import from QR code", action: onScan).accessibilityIdentifier(Tags.importQr)
            }
            Button("Export to clipboard") { copyText("Configuration", QaConfigJson.encode(form.config), toaster: toaster) }
                .accessibilityIdentifier(Tags.export)
            Button("Reset") { session.reset() }.accessibilityIdentifier(Tags.reset)
            Button("Journal") { go(.journal) }.accessibilityIdentifier(Tags.journalOpen)
        }
    }
}

/// The stand: its name over its host and API, or the problem that stops a launch. The row opens Stands; its name is
/// the `qa_profile` element that reads as the selected profile.
private struct StandHomeRow: View {
    let name: String?
    let profile: StandProfile?
    let problem: Problem?
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            TwoLineRow(
                leading: { EmptyView() },
                first: {
                    Text(name ?? "No stand")
                        .qaStyle(QaType.bodyStrong)
                        .lineLimit(1)
                        .accessibilityIdentifier(Tags.profile)
                        .accessibilityValue(name ?? "")
                },
                second: problem.map(rowProblem) ?? profile.map(standSummary) ?? "",
                secondColor: problem == nil ? QaColor.muted : QaColor.bad,
                trailing: { RowChevron() }
            )
        }
        .buttonStyle(RowButtonStyle())
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier(Tags.profilesOpen)
    }
}

/// The last finished run: its outcome's colour and title over the time and the order's status. Opens its result.
private struct LastRunRow: View {
    let lastRun: LastRun
    let action: () -> Void

    var body: some View {
        let outcome = lastRun.finished.outcome
        Button(action: action) {
            TwoLineRow(
                leading: { EmptyView() },
                first: {
                    HStack(spacing: 10) {
                        OutcomeSquare(color: toneColor(outcomeTone(outcome.type)))
                        Text(outcomeTitle(outcome.type)).qaStyle(QaType.bodyStrong).lineLimit(1)
                    }
                },
                second: lastRunSummary(lastRun.time, outcome.status),
                trailing: { RowChevron() }
            )
        }
        .buttonStyle(RowButtonStyle())
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier(Tags.homeLastRun)
    }
}

/// The colour of an outcome: green, red, or grey for a run that neither succeeded nor failed.
func toneColor(_ tone: Tone) -> Color {
    switch tone {
    case .ok: QaColor.ok
    case .bad: QaColor.bad
    case .neutral: QaColor.muted
    }
}
