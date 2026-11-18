import RadarpaymentsSDKPayment
import SwiftUI

/// What the finished run produced. Run again goes back to the configuration, which is kept as it was.
struct ResultScreen: View {
    let finished: FinishedRun
    let journal: Journal
    let onRunAgain: () -> Void
    @State private var copied: String?

    var body: some View {
        let outcome = finished.outcome
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                value("Result", outcome.type.rawValue, Tags.resultType)
                HStack(alignment: .bottom) {
                    value("mdOrder", outcome.mdOrder, Tags.resultMdOrder)
                    Spacer()
                    Button("Copy") {
                        UIPasteboard.general.string = outcome.mdOrder ?? ""
                        copied = "mdOrder copied"
                    }
                    .disabled(outcome.mdOrder == nil)
                    .accessibilityIdentifier(Tags.resultMdOrderCopy)
                }
                value("Session id", outcome.sessionId, Tags.resultSessionId)
                value("Order status", outcome.status.text, Tags.resultOrderStatus)
                value("Error", outcome.error, Tags.resultError)
                value("HTTP status", outcome.httpStatus.map(String.init), nil)
                Button("Copy report") {
                    UIPasteboard.general.string = runReport(app: currentAppInfo(), finished: finished, entries: journal.snapshot())
                    copied = "Report copied"
                }
                .buttonStyle(.bordered)
                .accessibilityIdentifier(Tags.report)
                Button("Run again", action: onRunAgain)
                    .buttonStyle(.borderedProminent)
                    .accessibilityIdentifier(Tags.runAgain)
                if let copied {
                    Text(copied).font(.footnote).foregroundStyle(.secondary)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
        }
        .navigationTitle("Result")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden()
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button("Back", action: onRunAgain)
            }
        }
    }

    @ViewBuilder
    private func value(_ label: String, _ text: String?, _ tag: String?) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label).font(.caption).foregroundStyle(.secondary)
            if let tag {
                Text(text ?? "-").textSelection(.enabled).accessibilityIdentifier(tag)
            } else {
                Text(text ?? "-").textSelection(.enabled)
            }
        }
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
