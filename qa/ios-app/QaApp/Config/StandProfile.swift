nonisolated enum ApiVersion: String, CaseIterable {
    case v1
    case v2
}

/// A gateway stand and the credentials the app registers orders with. Stored on the device only.
nonisolated struct StandProfile: Equatable {
    var name: String
    var gatewayUrl: String
    var api: ApiVersion = .v1
    var login = ""
    var password = ""
    var apiKey = ""
}

// The credentials stay out of every log line, failure message and debugger dump that prints a profile.
nonisolated extension StandProfile: CustomStringConvertible, CustomDebugStringConvertible, CustomReflectable {
    var description: String {
        "StandProfile(name=\(name), gatewayUrl=\(gatewayUrl), api=\(api.rawValue.uppercased()))"
    }

    var debugDescription: String {
        description
    }

    var customMirror: Mirror {
        Mirror(self, children: ["name": name, "gatewayUrl": gatewayUrl, "api": api], displayStyle: .struct)
    }
}
