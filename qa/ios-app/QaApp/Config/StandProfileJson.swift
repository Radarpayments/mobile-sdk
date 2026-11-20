/// Reads and writes the stand profile document of `qa/README.md`, credentials included: it is stored on the device only.
nonisolated enum StandProfileJson {
    private static let fields = ["version", "name", "gatewayUrl", "api", "login", "password", "apiKey"]

    /// [maxBytes] is raised only for the app's own stored profiles, which credentials of any length can make large.
    static func decode(_ text: String, maxBytes: Int = DocumentLimits.maxBytes) -> Decoded<StandProfile> {
        if let message = limitProblem(text, maxBytes: maxBytes) { return .invalid(Problem(field: "$", message: message)) }
        guard case let .object(root) = JSONValue.parse(text) else { return .invalid(.notAnObject) }
        let reader = JsonReader()
        if let version = reader.integral(root, "version", "version"), version != 1 { reader.fail("version", "must be 1") }
        let name = reader.text(root, "name", "name")
        if name == nil { reader.fail("name", "is required") }
        let gatewayUrl = reader.text(root, "gatewayUrl", "gatewayUrl")
        if gatewayUrl == nil { reader.fail("gatewayUrl", "is required") }
        let profile = StandProfile(
            name: name ?? "",
            gatewayUrl: gatewayUrl ?? "",
            api: reader.choice(root, "api", "api", ApiVersion.v1),
            login: reader.text(root, "login", "login") ?? "",
            password: reader.text(root, "password", "password") ?? "",
            apiKey: reader.text(root, "apiKey", "apiKey") ?? ""
        )
        let problem = firstInOrder(reader.problems + [problem(of: profile)].compactMap { $0 }, fields)
        return problem.map { .invalid($0) } ?? .ok(profile)
    }

    /// The first rule a profile breaks. It names a field and never quotes a credential.
    static func problem(of profile: StandProfile) -> Problem? {
        if !isProfileName(profile.name) { return profileNameProblem("name") }
        if !isHttpUrl(profile.gatewayUrl) {
            return Problem(field: "gatewayUrl", message: "must be an absolute http or https URL")
        }
        return nil
    }

    static func encode(_ profile: StandProfile) -> String {
        let document: JSONObject = [
            "version": .number("1"),
            "name": .string(profile.name),
            "gatewayUrl": .string(profile.gatewayUrl),
            "api": .string(profile.api.rawValue),
            "login": .string(profile.login),
            "password": .string(profile.password),
            "apiKey": .string(profile.apiKey),
        ]
        return JSONValue.object(document).prettyText
    }
}
