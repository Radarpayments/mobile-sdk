package net.payrdr.mobile.payment.sdk.qa.config

internal enum class ApiVersion(
    val json: String,
) {
    V1("v1"),
    V2("v2"),
}

internal data class StandProfile(
    val name: String,
    val gatewayUrl: String,
    val api: ApiVersion = ApiVersion.V1,
    val login: String = "",
    val password: String = "",
    val apiKey: String = "",
) {
    // The credentials stay out of every log line and failure message that prints a profile.
    override fun toString(): String = "StandProfile(name=$name, gatewayUrl=$gatewayUrl, api=$api)"
}
