package net.payrdr.mobile.payment.sdk.qa.config

import kotlinx.serialization.json.JsonObject

internal enum class OrderSource(
    val json: String,
) {
    REGISTER("register"),
    MD_ORDER("mdOrder"),
    SESSION_ID("sessionId"),
}

internal enum class OrderType(
    val json: String,
) {
    PAYMENT("payment"),
    AFT("aft"),
}

internal enum class ThemePreset(
    val json: String,
) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    CONTRAST("contrast"),
    ROUNDED("rounded"),
    SQUARE("square"),
    LARGE_TEXT("largeText"),
    DEBUG("debug"),
}

internal data class Pin(
    val host: String,
    val pin: String,
)

internal data class OrderConfig(
    val source: OrderSource = OrderSource.REGISTER,
    val type: OrderType = OrderType.PAYMENT,
    val amount: Long = 10_000,
    val currency: String = "EUR",
    val clientId: String? = null,
    val features: List<String> = emptyList(),
    val extra: JsonObject = JsonObject(emptyMap()),
    val mdOrder: String? = null,
    val sessionId: String? = null,
)

internal data class SslPinningOptions(
    val enabled: Boolean = false,
    val pins: List<Pin> = emptyList(),
)

// The sets default to the canonical order of the contract, so that a decoded empty document equals the default.
internal data class GooglePayOptions(
    val enabled: Boolean = false,
    val merchantId: String? = null,
    val merchantFullName: String? = null,
    val gateway: String? = null,
    val gatewayMerchantId: String? = null,
    val environment: String? = null,
    val allowedCardNetworks: List<String> = listOf("MASTERCARD", "VISA"),
    val allowedAuthMethods: List<String> = listOf("PAN_ONLY", "CRYPTOGRAM_3DS"),
    val countryCode: String? = null,
    val billingAddressRequired: Boolean = false,
)

internal data class ApplePayOptions(
    val enabled: Boolean = false,
    val merchantId: String = "merchant.net.payrdr.samples.test",
    val supportedNetworks: List<String> = listOf("MASTERCARD", "VISA"),
    val merchantCapabilities: List<String> = listOf("CREDIT", "DEBIT", "THREE_D_SECURE"),
    val countryCode: String? = null,
)

internal data class SdkOptions(
    val locale: String? = null,
    val theme: ThemePreset = ThemePreset.SYSTEM,
    val showResultScreen: Boolean = false,
    val googlePay: GooglePayOptions = GooglePayOptions(),
    val applePay: ApplePayOptions = ApplePayOptions(),
    val scanner: Boolean = false,
    val sslPinning: SslPinningOptions = SslPinningOptions(),
    val keyProviderUrl: String? = null,
)

internal data class QaConfig(
    val profile: String? = null,
    val order: OrderConfig = OrderConfig(),
    val sdk: SdkOptions = SdkOptions(),
)
