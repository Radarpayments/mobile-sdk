import SwiftUI

/// The stored stand profiles and the editor of one. Tapping a profile selects it for the launch and loads it here.
struct ProfilesScreen: View {
    @ObservedObject var session: QaSession
    @ObservedObject var store: QaStore
    @ObservedObject var form: ConfigForm
    @ObservedObject var editor: ProfileEditor

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if store.profiles.isEmpty {
                    Text("No profiles yet").foregroundStyle(.secondary)
                }
                ForEach(store.profiles, id: \.name) { profile in
                    row(profile)
                }
                Divider()
                fields
                if let problem = editor.problem {
                    Text(problemText(problem))
                        .font(.footnote)
                        .foregroundStyle(.red)
                        .accessibilityIdentifier(Tags.validationError)
                }
                HStack {
                    Button("Add") { editor.load(nil) }.accessibilityIdentifier(Tags.profileAdd)
                    Spacer()
                    Button("Delete", role: .destructive) {
                        if session.deleteProfile(named: trimmed(editor.name)) { editor.load(nil) }
                    }
                    .disabled(!exists)
                    .accessibilityIdentifier(Tags.profileDelete)
                    Button("Save") {
                        if let profile = editor.valid() { _ = session.saveProfile(profile) }
                    }
                    .buttonStyle(.borderedProminent)
                    .accessibilityIdentifier(Tags.profileSave)
                }
            }
            .padding()
        }
        .scrollDismissesKeyboard(.interactively)
        .navigationTitle("Profiles")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var exists: Bool {
        store.profiles.contains { sameText($0.name, trimmed(editor.name)) }
    }

    private func row(_ profile: StandProfile) -> some View {
        let selected = form.config.profile.map { sameText($0, profile.name) } ?? false
        return Button {
            session.select(profile.name)
            editor.load(profile)
        } label: {
            VStack(alignment: .leading, spacing: 2) {
                Text((selected ? "✓ " : "") + profile.name).foregroundStyle(.primary)
                Text("\(profile.gatewayUrl) · \(profile.api.rawValue)").font(.caption).foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier(Tags.profileSelect(profile.name))
    }

    @ViewBuilder
    private var fields: some View {
        editorField("Name", Tags.profileName, $editor.name)
        editorField("Gateway URL", Tags.profileGatewayUrl, $editor.gatewayUrl, keyboard: .URL)
        Choice(tag: Tags.profileApi, label: "API", value: editor.api.rawValue, options: ApiVersion.allCases.map(\.rawValue)) {
            editor.api = ApiVersion(rawValue: $0) ?? .v1
        }
        switch editor.api {
        case .v1:
            editorField("Login", Tags.profileLogin, $editor.login)
            secretField("Password", Tags.profilePassword, $editor.password)
        case .v2:
            secretField("API key", Tags.profileApiKey, $editor.apiKey)
        }
    }

    private func editorField(
        _ label: String,
        _ tag: String,
        _ text: Binding<String>,
        keyboard: UIKeyboardType = .default
    ) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.caption).foregroundStyle(.secondary)
            TextField(label, text: text)
                .textFieldStyle(.roundedBorder)
                .keyboardType(keyboard)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .accessibilityIdentifier(tag)
        }
    }

    private func secretField(_ label: String, _ tag: String, _ text: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.caption).foregroundStyle(.secondary)
            SecureField(label, text: text)
                .textFieldStyle(.roundedBorder)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .accessibilityIdentifier(tag)
        }
    }
}
