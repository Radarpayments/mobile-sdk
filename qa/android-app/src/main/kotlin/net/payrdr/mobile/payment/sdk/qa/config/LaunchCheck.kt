package net.payrdr.mobile.payment.sdk.qa.config

/**
 * The checks that run when Launch is pressed. They are not document validation: a half-edited form and a link with two
 * fields stay valid documents. [profile] is the stored profile named by `config.profile`, null when there is none.
 */
internal fun launchProblem(
    config: QaConfig,
    profile: StandProfile?,
): Problem? = launchProblems(config, profile).firstOrNull()

/** Every check [launchProblem] makes that fails, in its order, so each screen can show the one it fixes. */
internal fun launchProblems(
    config: QaConfig,
    profile: StandProfile?,
): List<Problem> {
    val order = config.order
    val registers = order.source == OrderSource.REGISTER
    return listOfNotNull(
        launchProblemIf(config.profile == null || profile == null, "profile", "select a stored profile"),
        launchProblemIf(
            order.type == OrderType.AFT && registers && profile?.api == ApiVersion.V2,
            "order.type",
            "a transfer is registered with API v1 only",
        ),
        launchProblemIf(
            registers && profile?.api == ApiVersion.V1 && (profile.login.isBlank() || profile.password.isBlank()),
            "profile.login",
            "registering an order needs a login and a password",
        ),
        launchProblemIf(
            registers && profile?.api == ApiVersion.V2 && profile.apiKey.isBlank(),
            "profile.apiKey",
            "registering an order needs an API key",
        ),
        launchProblemIf(order.source == OrderSource.MD_ORDER && order.mdOrder == null, "order.mdOrder", "is required"),
        launchProblemIf(order.source == OrderSource.SESSION_ID && order.sessionId == null, "order.sessionId", "is required"),
        launchProblemIf(
            config.sdk.sslPinning.enabled &&
                config.sdk.sslPinning.pins
                    .isEmpty(),
            "sdk.sslPinning.pins",
            "pinning is enabled without a pin",
        ),
    )
}

private fun launchProblemIf(
    failed: Boolean,
    field: String,
    message: String,
): Problem? = if (failed) Problem(field, message) else null
