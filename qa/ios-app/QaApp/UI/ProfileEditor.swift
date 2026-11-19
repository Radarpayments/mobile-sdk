import Combine

/// The profile being edited. Held in memory only: the password and the API key live nowhere but in the Keychain.
@MainActor
final class ProfileEditor: ObservableObject {
    @Published var name = ""
    @Published var gatewayUrl = ""
    @Published var api = ApiVersion.v1
    @Published var login = ""
    @Published var password = ""
    @Published var apiKey = ""
    @Published private(set) var problem: Problem?
    /// The name of the stored profile being edited, nil for a new one.
    @Published private(set) var original: String?

    /// A save or delete the store could not carry out.
    func failed(_ problem: Problem) {
        self.problem = problem
    }

    func load(_ profile: StandProfile?) {
        original = profile?.name
        name = profile?.name ?? ""
        gatewayUrl = profile?.gatewayUrl ?? ""
        api = profile?.api ?? .v1
        login = profile?.login ?? ""
        password = profile?.password ?? ""
        apiKey = profile?.apiKey ?? ""
        problem = nil
    }

    /// The edited profile when it is valid; otherwise nil, and `problem` says why.
    func valid() -> StandProfile? {
        let profile = StandProfile(
            name: trimmed(name), gatewayUrl: trimmed(gatewayUrl), api: api, login: login, password: password, apiKey: apiKey
        )
        problem = StandProfileJson.problem(of: profile)
        return problem == nil ? profile : nil
    }
}
