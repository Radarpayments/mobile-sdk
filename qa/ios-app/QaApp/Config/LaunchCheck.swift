/// The checks that run when Launch is pressed. They are not document validation: a half-edited form and a link with two
/// fields stay valid documents. [profile] is the stored profile named by `config.profile`, nil when there is none.
nonisolated func launchProblem(config: QaConfig, profile: StandProfile?) -> Problem? {
    let order = config.order
    let registers = order.source == .register
    let checks: [(Bool, String, String)] = [
        (config.profile == nil || profile == nil, "profile", "select a stored profile"),
        (
            order.type == .aft && registers && profile?.api == .v2,
            "order.type",
            "a transfer is registered with API v1 only"
        ),
        (
            registers && profile?.api == .v1 && (isBlank(profile?.login ?? "") || isBlank(profile?.password ?? "")),
            "profile.login",
            "registering an order needs a login and a password"
        ),
        (
            registers && profile?.api == .v2 && isBlank(profile?.apiKey ?? ""),
            "profile.apiKey",
            "registering an order needs an API key"
        ),
        (order.source == .mdOrder && order.mdOrder == nil, "order.mdOrder", "is required"),
        (order.source == .sessionId && order.sessionId == nil, "order.sessionId", "is required"),
        (
            config.sdk.sslPinning.enabled && config.sdk.sslPinning.pins.isEmpty,
            "sdk.sslPinning.pins",
            "pinning is enabled without a pin"
        ),
    ]
    return checks.first { $0.0 }.map { Problem(field: $0.1, message: $0.2) }
}
