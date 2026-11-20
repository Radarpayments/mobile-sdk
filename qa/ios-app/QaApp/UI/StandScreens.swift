import SwiftUI

/// The stored stands, one selected; and the ways to add one.
struct StandsScreen: View {
    @ObservedObject var session: QaSession
    @ObservedObject var form: ConfigForm
    @ObservedObject var store: QaStore
    let scannerShown: Bool
    let back: () -> Void
    let onEdit: (StandProfile?) -> Void
    let onPasteText: () -> Void
    let onScan: () -> Void

    var body: some View {
        let selected = form.config.profile
        QaScreen(title: "Stands", back: back, actions: {
            BarAction(text: "Add", tag: Tags.profileAdd) { onEdit(nil) }
        }) {
            Caption("Saved")
            if store.profiles.isEmpty {
                Note("No stands yet")
            } else {
                ListGroup {
                    ForEach(store.profiles, id: \.name) { profile in
                        StandRow(
                            profile: profile,
                            selected: selected.map { sameText($0, profile.name) } ?? false,
                            onSelect: { session.select(profile.name) },
                            onEdit: { onEdit(profile) }
                        )
                    }
                }
            }
            Caption("Add a stand")
            ListGroup {
                NavRow(label: "Enter by hand", value: "", tag: Tags.profileEnter) { onEdit(nil) }
                NavRow(label: "Paste from clipboard", value: "", tag: Tags.profilePaste) {
                    session.accept(intake(fromText: UIPasteboard.general.string ?? ""))
                }
                NavRow(label: "Paste as text", value: "", tag: Tags.profilePasteText, action: onPasteText)
                if scannerShown {
                    NavRow(label: "Scan a QR code", value: "", tag: Tags.profileScan, action: onScan)
                }
            }
        }
    }
}

/// A stored stand: the mark, its name over its host and API, Edit on the right. Tapping the row selects it.
private struct StandRow: View {
    let profile: StandProfile
    let selected: Bool
    let onSelect: () -> Void
    let onEdit: () -> Void

    var body: some View {
        ZStack(alignment: .trailing) {
            Button(action: onSelect) {
                TwoLineRow(
                    leading: { Mark(selected: selected) },
                    first: {
                        Text(profile.name).qaStyle(selected ? QaType.bodyStrong : QaType.body).lineLimit(1)
                    },
                    second: standSummary(profile),
                    trailing: { Color.clear.frame(width: QaSize.control) }
                )
            }
            .buttonStyle(RowButtonStyle())
            .accessibilityElement(children: .combine)
            .accessibilityAddTraits(selected ? [.isButton, .isSelected] : .isButton)
            .accessibilityIdentifier(Tags.profileSelect(profile.name))
            QaLink(text: "Edit", tag: Tags.profileEdit(profile.name), action: onEdit)
                .padding(.trailing, QaSize.screen)
        }
    }
}

/// One stand: its name, gateway and API, and the login and password of v1 or the key of v2.
struct StandEditorScreen: View {
    @ObservedObject var session: QaSession
    @ObservedObject var editor: ProfileEditor
    let toaster: Toaster
    let back: () -> Void

    var body: some View {
        QaScreen(title: editor.original == nil ? "New stand" : "Stand", back: back, footer: {
            QaFooter {
                FooterWhy(problem: editor.problem, tag: Tags.validationError)
            } buttons: {
                if let original = editor.original {
                    QaButton(text: "Delete", tag: Tags.profileDelete, kind: .secondary) {
                        if session.deleteProfile(named: original) { back() }
                    }
                }
                QaButton(text: "Save", tag: Tags.profileSave) {
                    if let profile = editor.valid(), session.saveProfile(profile) {
                        toaster.say("Stand \(profile.name) saved")
                        back()
                    }
                }
            }
        }) {
            Caption("Name")
            TextInput(text: editor.name, tag: Tags.profileName) { editor.name = $0 }
            Caption("Gateway URL")
            TextInput(text: editor.gatewayUrl, tag: Tags.profileGatewayUrl, look: InputLook(keyboard: .URL, style: QaType.inputSmall)) {
                editor.gatewayUrl = $0
            }
            Caption("API")
            Segmented(
                segments: ApiVersion.allCases.map { Segment(value: $0.rawValue, label: $0.rawValue, tag: Tags.option(Tags.profileApi, $0.rawValue)) },
                selected: editor.api.rawValue,
                tag: Tags.profileApi
            ) { value in editor.api = ApiVersion.allCases.first { sameText($0.rawValue, value) } ?? .v1 }
            switch editor.api {
            case .v1:
                Caption("Login")
                TextInput(text: editor.login, tag: Tags.profileLogin) { editor.login = $0 }
                Caption("Password")
                Secret(text: editor.password, tag: Tags.profilePassword, showTag: Tags.profilePasswordShow) { editor.password = $0 }
            case .v2:
                Caption("API key")
                Secret(text: editor.apiKey, tag: Tags.profileApiKey, showTag: Tags.profileApiKeyShow) { editor.apiKey = $0 }
            }
        }
    }
}

/// A secret field, hidden until Show; it is shown on this screen only and hidden again when the screen is left.
private struct Secret: View {
    let text: String
    let tag: String
    let showTag: String
    let onChange: (String) -> Void
    @State private var shown = false

    var body: some View {
        TextInput(text: text, tag: tag, look: InputLook(hidden: !shown), onChange: onChange) {
            QaLink(text: shown ? "Hide" : "Show", tag: showTag) { shown.toggle() }
        }
    }
}
