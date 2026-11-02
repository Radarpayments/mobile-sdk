package net.payrdr.mobile.payment.sdk.bridge

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import net.payrdr.mobile.payment.sdk.payment.applepay.ApplePayMerchantCapability
import net.payrdr.mobile.payment.sdk.payment.applepay.ApplePayNetwork
import net.payrdr.mobile.payment.sdk.payment.config.ApplePayConfig
import net.payrdr.mobile.payment.sdk.payment.config.CheckoutConfig
import net.payrdr.mobile.payment.sdk.payment.config.GooglePayConfig
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayEnvironment
import net.payrdr.mobile.payment.sdk.payment.logger.ConsoleLogTransport
import net.payrdr.mobile.payment.sdk.payment.scanner.provider.MlKitVisionCardScannerProvider
import net.payrdr.mobile.payment.sdk.payment.ssl.SslPinningConfig
import net.payrdr.mobile.payment.sdk.payment.ui.theme.DefaultDarkColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.DefaultLightColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentShapes
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentTheme

/** A request of the bridge that does not follow the contract; its message says what is wrong. */
internal class InvalidBridgeRequest(
    message: String,
) : IllegalArgumentException(message)

/** One checkout to open: what it looks like ([config]) and what it pays ([checkout]). */
internal class CheckoutRequest(
    val config: SdkPaymentConfig,
    val checkout: CheckoutConfig,
) {
    /** The `mdOrder` a result without one is given; empty for a session. */
    val orderId: String get() = (checkout as? CheckoutConfig.MdOrder)?.value.orEmpty()

    companion object {
        /** Reads `{"config": {...}, "mdOrder": "..."}` (or `"sessionId"`); throws [InvalidBridgeRequest]. */
        fun parse(json: String): CheckoutRequest {
            val dto = BridgeJson.decode<CheckoutRequestDto>(json)
            val checkout =
                dto.mdOrder?.let { CheckoutConfig.MdOrder(it) }
                    ?: dto.sessionId?.let { CheckoutConfig.SessionId(it) }
                    ?: throw InvalidBridgeRequest("The request names neither mdOrder nor sessionId")
            return CheckoutRequest(dto.config.toSdkConfig(), checkout)
        }
    }
}

internal object BridgeJson {
    val format: Json =
        Json {
            // A plugin newer than the native SDK may send options this version does not have yet.
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    inline fun <reified T> decode(json: String): T =
        try {
            format.decodeFromString<T>(json)
        } catch (e: SerializationException) {
            throw InvalidBridgeRequest(e.message ?: "Malformed request")
        } catch (e: IllegalArgumentException) {
            throw InvalidBridgeRequest(e.message ?: "Malformed request")
        }
}

@Serializable
private class CheckoutRequestDto(
    val config: ConfigDto,
    val mdOrder: String? = null,
    val sessionId: String? = null,
)

@Serializable
private class ConfigDto(
    val baseUrl: String,
    val keyProviderUrl: String? = null,
    val locale: String? = null,
    val theme: ThemeDto? = null,
    val googlePay: GooglePayDto? = null,
    val applePay: ApplePayDto? = null,
    val cardScanner: Boolean = false,
    val showResultScreen: Boolean = false,
    val sslPins: List<PinDto> = emptyList(),
    val debugLogging: Boolean = false,
) {
    fun toSdkConfig(): SdkPaymentConfig =
        SdkPaymentConfig(
            baseUrl = baseUrl,
            keyProviderUrl = keyProviderUrl,
            sslPinning = sslPins.takeIf { it.isNotEmpty() }?.toPinning(),
            logTransport = ConsoleLogTransport.takeIf { debugLogging },
            locale = locale,
            theme = theme?.toTheme() ?: PaymentTheme.System,
            googlePay = googlePay?.toConfig(),
            applePay = applePay?.toConfig(),
            cardScannerProvider = MlKitVisionCardScannerProvider().takeIf { cardScanner },
            showResultScreen = showResultScreen,
        )
}

@Serializable
private class PinDto(
    val host: String,
    val sha256: String,
)

private fun List<PinDto>.toPinning(): SslPinningConfig =
    try {
        fold(SslPinningConfig.Builder()) { builder, pin -> builder.addPin(pin.host, pin.sha256) }.build()
    } catch (e: IllegalArgumentException) {
        throw InvalidBridgeRequest("sslPins: ${e.message}")
    }

/**
 * `mode` is `system`, `light`, `dark` or `custom`. A custom theme starts from the `base` palette (`light` or `dark`) and
 * changes only the [colors] it names (`#RRGGBB` or `#AARRGGBB`) and the corners it sets, in dp.
 */
@Serializable
private class ThemeDto(
    val mode: String = "system",
    val base: String = "light",
    val colors: Map<String, String> = emptyMap(),
    val cornerRadius: Float? = null,
    val buttonCornerRadius: Float? = null,
) {
    fun toTheme(): PaymentTheme =
        when (mode) {
            "system" -> PaymentTheme.System
            "light" -> PaymentTheme.Light
            "dark" -> PaymentTheme.Dark
            "custom" -> PaymentTheme.Custom(colors = customColors(), shapes = customShapes())
            else -> throw InvalidBridgeRequest("theme.mode must be system, light, dark or custom, not $mode")
        }

    private fun customColors(): PaymentColors {
        val stock =
            when (base) {
                "light" -> DefaultLightColors
                "dark" -> DefaultDarkColors
                else -> throw InvalidBridgeRequest("theme.base must be light or dark, not $base")
            }

        fun pick(
            name: String,
            fallback: Color,
        ): Color = colors[name]?.let { parseColor(name, it) } ?: fallback
        colors.keys.firstOrNull { it !in COLOR_NAMES }?.let { throw InvalidBridgeRequest("theme.colors has no colour $it") }
        return PaymentColors(
            primary = pick("primary", stock.primary),
            onPrimary = pick("onPrimary", stock.onPrimary),
            background = pick("background", stock.background),
            onBackground = pick("onBackground", stock.onBackground),
            surface = pick("surface", stock.surface),
            onSurface = pick("onSurface", stock.onSurface),
            error = pick("error", stock.error),
            onError = pick("onError", stock.onError),
            outline = pick("outline", stock.outline),
            success = pick("success", stock.success),
            onSurfaceVariant = pick("onSurfaceVariant", stock.onSurfaceVariant),
            placeholder = pick("placeholder", stock.placeholder),
            outlineStrong = pick("outlineStrong", stock.outlineStrong),
            scrim = pick("scrim", stock.scrim),
        )
    }

    private fun customShapes(): PaymentShapes {
        val stock = PaymentShapes.Default
        val corners = cornerRadius?.let { RoundedCornerShape(it.dp) }
        return stock.copy(
            field = corners ?: stock.field,
            button = buttonCornerRadius?.let { RoundedCornerShape(it.dp) } ?: corners ?: stock.button,
            card = corners ?: stock.card,
        )
    }

    private companion object {
        val COLOR_NAMES =
            setOf(
                "primary",
                "onPrimary",
                "background",
                "onBackground",
                "surface",
                "onSurface",
                "error",
                "onError",
                "outline",
                "success",
                "onSurfaceVariant",
                "placeholder",
                "outlineStrong",
                "scrim",
            )
    }
}

/** `#RRGGBB` (opaque) or `#AARRGGBB`. */
private fun parseColor(
    name: String,
    hex: String,
): Color {
    val digits = hex.removePrefix("#")
    val argb = digits.takeIf { it.length == 6 || it.length == 8 }?.toLongOrNull(radix = 16)
    argb ?: throw InvalidBridgeRequest("theme.colors.$name must be #RRGGBB or #AARRGGBB, not $hex")
    return Color(if (digits.length == 6) argb or 0xFF000000 else argb)
}

/** Every field may be left out: the SDK then takes it from the gateway, as in [GooglePayConfig]. */
@Serializable
private class GooglePayDto(
    val merchantId: String? = null,
    val merchantName: String? = null,
    val gateway: String? = null,
    val gatewayMerchantId: String? = null,
    val environment: String? = null,
    val countryCode: String? = null,
    val cardNetworks: List<String>? = null,
    val authMethods: List<String>? = null,
    val billingAddressRequired: Boolean = false,
) {
    fun toConfig(): GooglePayConfig {
        val stock = GooglePayConfig()
        return GooglePayConfig(
            merchantId = merchantId,
            merchantFullName = merchantName,
            gateway = gateway,
            gatewayMerchantId = gatewayMerchantId,
            environment = environment?.let { enumOf<GooglePayEnvironment>("googlePay.environment", it) },
            allowedCardNetworks = cardNetworks?.enumSet("googlePay.cardNetworks") ?: stock.allowedCardNetworks,
            allowedAuthMethods = authMethods?.enumSet("googlePay.authMethods") ?: stock.allowedAuthMethods,
            countryCode = countryCode,
            billingAddressRequired = billingAddressRequired,
        )
    }
}

@Serializable
private class ApplePayDto(
    val merchantId: String? = null,
    val countryCode: String? = null,
    val networks: List<String>? = null,
    val capabilities: List<String>? = null,
) {
    fun toConfig(): ApplePayConfig {
        val id = merchantId?.takeIf { it.isNotBlank() } ?: throw InvalidBridgeRequest("applePay.merchantId is required")
        val stock = ApplePayConfig(merchantId = id)
        return ApplePayConfig(
            merchantId = id,
            supportedNetworks = networks?.enumSet<ApplePayNetwork>("applePay.networks") ?: stock.supportedNetworks,
            merchantCapabilities =
                capabilities?.enumSet<ApplePayMerchantCapability>("applePay.capabilities") ?: stock.merchantCapabilities,
            countryCode = countryCode,
        )
    }
}

private inline fun <reified E : Enum<E>> enumOf(
    field: String,
    name: String,
): E =
    enumValues<E>().firstOrNull { it.name == name }
        ?: throw InvalidBridgeRequest("$field: $name is not one of ${enumValues<E>().joinToString { it.name }}")

private inline fun <reified E : Enum<E>> List<String>.enumSet(field: String): Set<E> = mapTo(linkedSetOf()) { enumOf<E>(field, it) }
