package net.payrdr.mobile.payment.sdk.payment.config

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.payrdr.mobile.payment.sdk.payment.applepay.ApplePayMerchantCapability
import net.payrdr.mobile.payment.sdk.payment.applepay.ApplePayNetwork
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayAuthMethod
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayCardNetwork
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayEnvironment
import net.payrdr.mobile.payment.sdk.payment.ssl.SslPinningConfig
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentTheme

/**
 * A checkout's config and order written down as a string, so the checkout can be rebuilt after Android ends the
 * process while it is open. What cannot be written down comes back as the default: the fonts and shapes of a custom
 * theme, the log transport and the card scanner — the payment still completes, only its look may get simpler.
 */
internal object RestorableCheckout {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(
        config: SdkPaymentConfig,
        checkout: CheckoutConfig,
    ): String =
        json.encodeToString(
            Saved.serializer(),
            Saved(
                baseUrl = config.baseUrl,
                keyProviderUrl = config.keyProviderUrl,
                locale = config.locale,
                showResultScreen = config.showResultScreen,
                theme =
                    when (val theme = config.theme) {
                        PaymentTheme.System -> SavedTheme("system")
                        PaymentTheme.Light -> SavedTheme("light")
                        PaymentTheme.Dark -> SavedTheme("dark")
                        is PaymentTheme.Custom -> SavedTheme("custom", theme.colors.toArgbList())
                    },
                googlePay =
                    config.googlePay?.let {
                        SavedGooglePay(
                            it.merchantId,
                            it.merchantFullName,
                            it.gateway,
                            it.gatewayMerchantId,
                            it.environment?.name,
                            it.allowedCardNetworks.map { n -> n.name },
                            it.allowedAuthMethods.map { m -> m.name },
                            it.countryCode,
                            it.billingAddressRequired,
                        )
                    },
                applePay =
                    config.applePay?.let {
                        SavedApplePay(
                            it.merchantId,
                            it.supportedNetworks.map { n -> n.name },
                            it.merchantCapabilities.map { c -> c.name },
                            it.countryCode,
                        )
                    },
                pins = config.sslPinning?.entries?.map { SavedPin(it.hostname, it.pinString.removePrefix("sha256/")) },
                mdOrder = (checkout as? CheckoutConfig.MdOrder)?.value,
                sessionId = (checkout as? CheckoutConfig.SessionId)?.value,
            ),
        )

    fun decode(value: String): Pair<SdkPaymentConfig, CheckoutConfig> {
        val saved = json.decodeFromString(Saved.serializer(), value)
        val config =
            SdkPaymentConfig(
                baseUrl = saved.baseUrl,
                keyProviderUrl = saved.keyProviderUrl,
                locale = saved.locale,
                showResultScreen = saved.showResultScreen,
                theme =
                    when (saved.theme.kind) {
                        "light" -> PaymentTheme.Light
                        "dark" -> PaymentTheme.Dark
                        "custom" ->
                            saved.theme.colors
                                ?.toPaymentColors()
                                ?.let { PaymentTheme.Custom(it) } ?: PaymentTheme.System
                        else -> PaymentTheme.System
                    },
                googlePay =
                    saved.googlePay?.let {
                        GooglePayConfig(
                            merchantId = it.merchantId,
                            merchantFullName = it.merchantFullName,
                            gateway = it.gateway,
                            gatewayMerchantId = it.gatewayMerchantId,
                            environment = it.environment?.let(GooglePayEnvironment::valueOf),
                            allowedCardNetworks = it.cardNetworks.map(GooglePayCardNetwork::valueOf).toSet(),
                            allowedAuthMethods = it.authMethods.map(GooglePayAuthMethod::valueOf).toSet(),
                            countryCode = it.countryCode,
                            billingAddressRequired = it.billingAddressRequired,
                        )
                    },
                applePay =
                    saved.applePay?.let {
                        ApplePayConfig(
                            merchantId = it.merchantId,
                            supportedNetworks = it.networks.map(ApplePayNetwork::valueOf).toSet(),
                            merchantCapabilities = it.capabilities.map(ApplePayMerchantCapability::valueOf).toSet(),
                            countryCode = it.countryCode,
                        )
                    },
                sslPinning =
                    saved.pins?.takeIf { it.isNotEmpty() }?.let { pins ->
                        pins.fold(SslPinningConfig.Builder()) { builder, pin -> builder.addPin(pin.host, pin.pin) }.build()
                    },
            )
        val checkout = saved.sessionId?.let { CheckoutConfig.SessionId(it) } ?: CheckoutConfig.MdOrder(saved.mdOrder.orEmpty())
        return config to checkout
    }

    private fun PaymentColors.toArgbList(): List<Long> =
        listOf(
            primary,
            onPrimary,
            background,
            onBackground,
            surface,
            onSurface,
            error,
            onError,
            outline,
            success,
            onSurfaceVariant,
            placeholder,
            outlineStrong,
            scrim,
        ).map { it.value.toLong() }

    private fun List<Long>.toPaymentColors(): PaymentColors? {
        if (size != COLOR_COUNT) return null
        val c = map { Color(it.toULong()) }
        return PaymentColors(
            primary = c[0],
            onPrimary = c[1],
            background = c[2],
            onBackground = c[3],
            surface = c[4],
            onSurface = c[5],
            error = c[6],
            onError = c[7],
            outline = c[8],
            success = c[9],
            onSurfaceVariant = c[10],
            placeholder = c[11],
            outlineStrong = c[12],
            scrim = c[13],
        )
    }

    private const val COLOR_COUNT = 14

    @Serializable
    private data class Saved(
        val baseUrl: String,
        val keyProviderUrl: String? = null,
        val locale: String? = null,
        val showResultScreen: Boolean = false,
        val theme: SavedTheme = SavedTheme("system"),
        val googlePay: SavedGooglePay? = null,
        val applePay: SavedApplePay? = null,
        val pins: List<SavedPin>? = null,
        val mdOrder: String? = null,
        val sessionId: String? = null,
    )

    @Serializable
    private data class SavedTheme(
        val kind: String,
        val colors: List<Long>? = null,
    )

    @Serializable
    private data class SavedGooglePay(
        val merchantId: String?,
        val merchantFullName: String?,
        val gateway: String?,
        val gatewayMerchantId: String?,
        val environment: String?,
        val cardNetworks: List<String>,
        val authMethods: List<String>,
        val countryCode: String?,
        val billingAddressRequired: Boolean,
    )

    @Serializable
    private data class SavedApplePay(
        val merchantId: String,
        val networks: List<String>,
        val capabilities: List<String>,
        val countryCode: String?,
    )

    @Serializable
    private data class SavedPin(
        val host: String,
        val pin: String,
    )
}
