import RadarpaymentsSDKPayment
import SwiftUI

/// What a finished run came to: the outcome, the ids to copy, what it was launched with and the journal. Rows with
/// nothing to show are left out.
struct ResultScreen: View {
    let finished: FinishedRun
    @ObservedObject var journal: Journal
    let toaster: Toaster
    let back: () -> Void
    let onRunAgain: () -> Void
    let openJournal: () -> Void

    var body: some View {
        let outcome = finished.outcome
        QaScreen(title: "Result", back: back, footer: {
            QaFooter {
                QaButton(text: "Copy report", tag: Tags.report, kind: .secondary) {
                    copyText("Report", runReport(app: currentAppInfo(), finished: finished, entries: journal.snapshot()), toaster: toaster)
                }
                QaButton(text: "Run again", tag: Tags.runAgain, action: onRunAgain)
            }
        }) {
            Banner(outcome: outcome)
            if outcome.mdOrder != nil || outcome.sessionId != nil || outcome.error != nil {
                Caption("Order")
                ListGroup {
                    if let mdOrder = outcome.mdOrder {
                        CopyRow(label: "mdOrder", value: mdOrder, tag: Tags.resultMdOrder, copyTag: Tags.resultMdOrderCopy, toaster: toaster)
                    }
                    if let sessionId = outcome.sessionId {
                        CopyRow(
                            label: "Session id", value: sessionId, tag: Tags.resultSessionId, copyTag: Tags.resultSessionIdCopy, toaster: toaster
                        )
                    }
                    if let error = outcome.error {
                        ErrorRow(error: error, httpStatus: outcome.httpStatus)
                    }
                }
            }
            Caption("Launched with")
            ListGroup {
                ValueRow(label: "Stand", value: "\(finished.profile.name) · API \(finished.profile.api.rawValue)", tag: Tags.resultStand)
                ValueRow(label: "Order", value: launchedOrderSummary(finished.config.order), tag: Tags.resultOrder)
                ValueRow(label: "Theme", value: finished.config.sdk.theme.rawValue, tag: Tags.resultTheme)
            }
            Caption("Evidence")
            ListGroup {
                NavRow(label: "Journal", value: entriesSummary(journal.entries), tag: Tags.journalOpen, action: openJournal)
            }
        }
    }
}

/// The outcome on its colour: the title and the order's status under it.
private struct Banner: View {
    let outcome: RunOutcome

    var body: some View {
        let detail = statusDetail(outcome.status).joined(separator: "\n")
        VStack(alignment: .leading, spacing: QaSize.gap) {
            Text(outcomeTitle(outcome.type))
                .qaStyle(QaType.outcome)
                .accessibilityIdentifier(Tags.resultType)
                .accessibilityValue(outcome.type.rawValue)
            Text(detail)
                .qaStyle(QaType.value)
                .accessibilityIdentifier(Tags.resultOrderStatus)
                .accessibilityValue(outcome.status.text)
        }
        .foregroundStyle(QaColor.onOutcome)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, QaSize.screen)
        .padding(.vertical, 24)
        .background(toneColor(outcomeTone(outcome.type)))
    }
}

/// An id over its label's line, with Copy on the right.
private struct CopyRow: View {
    let label: String
    let value: String
    let tag: String
    let copyTag: String
    let toaster: Toaster

    var body: some View {
        TwoLineRow(
            leading: { EmptyView() },
            first: { Text(label).qaStyle(QaType.body).lineLimit(1) },
            second: value,
            trailing: { QaLink(text: "Copy", tag: copyTag) { copyText(label, value, toaster: toaster) } }
        )
        .overlay { AccessibleText(text: value, tag: tag) }
    }
}

/// The run's error in red, with the HTTP status in the label when there was one.
private struct ErrorRow: View {
    let error: String
    let httpStatus: Int?

    var body: some View {
        TwoLineRow(
            leading: { EmptyView() },
            first: { Text(httpStatus.map { "Error · HTTP \($0)" } ?? "Error").qaStyle(QaType.body).lineLimit(1) },
            second: error,
            secondColor: QaColor.bad,
            trailing: { EmptyView() }
        )
        .overlay { AccessibleText(text: error, tag: Tags.resultError) }
    }
}

/// A label with its value on the right; the value is the identifier's text.
private struct ValueRow: View {
    let label: String
    let value: String
    let tag: String

    var body: some View {
        ListRow {
            Text(label).qaStyle(QaType.body).lineLimit(1).layoutPriority(1)
            RowValue(text: value)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label)
        .accessibilityValue(value)
        .accessibilityIdentifier(tag)
    }
}

// An invisible element over a row's value that carries its identifier and text for automation, leaving the row's
// own Copy link a separate element.
private struct AccessibleText: View {
    let text: String
    let tag: String

    var body: some View {
        Color.clear
            .allowsHitTesting(false)
            .accessibilityElement()
            .accessibilityLabel(text)
            .accessibilityValue(text)
            .accessibilityIdentifier(tag)
    }
}

/// The app, the SDK, the device and the OS, as the report's first two sections print them.
func currentAppInfo() -> AppInfo {
    let info = Bundle.main.infoDictionary ?? [:]
    let version = info["CFBundleShortVersionString"] as? String ?? "-"
    let build = info["CFBundleVersion"] as? String ?? "-"
    return AppInfo(
        appVersion: "\(version) (\(build))",
        sdkVersion: SdkPayment.companion.VERSION,
        device: "Apple \(modelIdentifier())",
        os: "\(UIDevice.current.systemName) \(UIDevice.current.systemVersion)"
    )
}

// The model identifier (iPhone16,1), which names the hardware exactly; a simulator reports the device it simulates.
private func modelIdentifier() -> String {
    if let simulated = ProcessInfo.processInfo.environment["SIMULATOR_MODEL_IDENTIFIER"] { return simulated }
    var system = utsname()
    uname(&system)
    return withUnsafeBytes(of: &system.machine) { bytes in
        String(decoding: bytes.prefix { $0 != 0 }, as: UTF8.self)
    }
}
