package net.payrdr.mobile.payment.sdk.qa.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.payrdr.mobile.payment.sdk.qa.config.Decoded
import net.payrdr.mobile.payment.sdk.qa.config.GooglePayOptions
import net.payrdr.mobile.payment.sdk.qa.config.OrderConfig
import net.payrdr.mobile.payment.sdk.qa.config.OrderSource
import net.payrdr.mobile.payment.sdk.qa.config.OrderType
import net.payrdr.mobile.payment.sdk.qa.config.Problem
import net.payrdr.mobile.payment.sdk.qa.config.QaConfig
import net.payrdr.mobile.payment.sdk.qa.config.QaConfigJson
import net.payrdr.mobile.payment.sdk.qa.config.SdkOptions
import net.payrdr.mobile.payment.sdk.qa.config.ThemePreset
import net.payrdr.mobile.payment.sdk.qa.config.extraFromText
import net.payrdr.mobile.payment.sdk.qa.config.extraToText
import net.payrdr.mobile.payment.sdk.qa.config.namesFromText
import net.payrdr.mobile.payment.sdk.qa.config.namesToText
import net.payrdr.mobile.payment.sdk.qa.config.pinsFromText
import net.payrdr.mobile.payment.sdk.qa.config.pinsToText

/**
 * The configuration form's state: one text per field tag of [Tags.fields]. An edit that parses becomes [config] at once;
 * one that does not stays a draft with its [Problem], and [config] keeps the last value in which every field parsed, so
 * that the store and a launch never see a half-typed field. The texts are snapshot state, so a screen recomposes on them.
 */
internal class ConfigForm(
    initial: QaConfig,
) {
    var config: QaConfig by mutableStateOf(initial)
        private set

    // Every edited field keeps its text until the next set(), even a valid one: "VERIFY," parses to the same features
    // as "VERIFY", and showing the latter would eat the comma being typed.
    private val drafts = mutableStateMapOf<String, String>()
    private val problems = mutableStateMapOf<String, Problem>()

    /** The first pending problem in the order of the form. */
    val problem: Problem?
        get() = Tags.fields.firstNotNullOfOrNull { problems[it] }

    /** The pending problem of the field [tag], null when its text parsed. */
    fun problem(tag: String): Problem? = problems[tag]

    fun text(tag: String): String = drafts[tag] ?: field(tag).read(config)

    fun edit(
        tag: String,
        text: String,
    ) {
        drafts[tag] = text
        val applied =
            when (val written = field(tag).write(config, text)) {
                is Decoded.Ok -> checked(written.value)
                is Decoded.Invalid -> written
            }
        when (applied) {
            is Decoded.Ok -> {
                config = applied.value
                problems.remove(tag)
            }
            is Decoded.Invalid -> problems[tag] = applied.problem
        }
    }

    fun set(config: QaConfig) {
        drafts.clear()
        problems.clear()
        this.config = config
    }
}

private class Field(
    val read: (QaConfig) -> String,
    val write: (QaConfig, String) -> Decoded<QaConfig>,
)

// A written document read back is what an export and a restart see: the sets in their contract order, a blank optional
// text as null, and every rule of the contract checked, the size of the whole document included.
private fun checked(config: QaConfig): Decoded<QaConfig> = QaConfigJson.decode(QaConfigJson.encode(config))

private fun field(tag: String): Field = FIELDS[tag] ?: error("$tag is not a field of the configuration")

private fun order(
    read: (OrderConfig) -> String,
    write: (OrderConfig, String) -> Decoded<OrderConfig>,
) = Field({ read(it.order) }, { config, text -> write(config.order, text).map { config.copy(order = it) } })

private fun sdk(
    read: (SdkOptions) -> String,
    write: (SdkOptions, String) -> Decoded<SdkOptions>,
) = Field({ read(it.sdk) }, { config, text -> write(config.sdk, text).map { config.copy(sdk = it) } })

private fun googlePay(
    read: (GooglePayOptions) -> String,
    write: (GooglePayOptions, String) -> Decoded<GooglePayOptions>,
) = sdk({ read(it.googlePay) }, { sdk, text -> write(sdk.googlePay, text).map { sdk.copy(googlePay = it) } })

private fun <T, R> Decoded<T>.map(transform: (T) -> R): Decoded<R> =
    when (this) {
        is Decoded.Ok -> Decoded.Ok(transform(value))
        is Decoded.Invalid -> this
    }

private fun ok(value: String?): Decoded<String?> = Decoded.Ok(value?.takeIf { it.isNotBlank() })

// toLongOrNull takes a sign and the digits of every script; Swift's Int64 does not, and the apps must agree.
private fun amount(text: String): Decoded<Long> =
    text
        .takeIf { it.isNotEmpty() && it.all { char -> char in '0'..'9' } }
        ?.toLongOrNull()
        ?.let { Decoded.Ok(it) }
        ?: Decoded.Invalid(Problem("order.amount", "must be a whole number of minor units"))

private fun flag(
    path: String,
    text: String,
): Decoded<Boolean> = text.toBooleanStrictOrNull()?.let { Decoded.Ok(it) } ?: Decoded.Invalid(Problem(path, "must be true or false"))

private fun <T> choice(
    path: String,
    text: String,
    values: List<T>,
    json: (T) -> String,
): Decoded<T> =
    values.firstOrNull { json(it) == text }?.let { Decoded.Ok(it) }
        ?: Decoded.Invalid(Problem(path, "must be one of ${values.joinToString(", ") { json(it) }}"))

private val FIELDS: Map<String, Field> =
    mapOf(
        Tags.PROFILE to Field({ it.profile.orEmpty() }, { config, text -> ok(text).map { config.copy(profile = it) } }),
        Tags.ORDER_SOURCE to
            order({ it.source.json }) { order, text ->
                choice("order.source", text, OrderSource.entries) { it.json }.map { order.copy(source = it) }
            },
        Tags.ORDER_TYPE to
            order({ it.type.json }) { order, text ->
                choice("order.type", text, OrderType.entries) { it.json }.map { order.copy(type = it) }
            },
        Tags.ORDER_AMOUNT to order({ it.amount.toString() }) { order, text -> amount(text).map { order.copy(amount = it) } },
        Tags.ORDER_CURRENCY to order({ it.currency }) { order, text -> Decoded.Ok(order.copy(currency = text)) },
        Tags.ORDER_CLIENT_ID to order({ it.clientId.orEmpty() }) { order, text -> ok(text).map { order.copy(clientId = it) } },
        Tags.ORDER_MD_ORDER to order({ it.mdOrder.orEmpty() }) { order, text -> ok(text).map { order.copy(mdOrder = it) } },
        Tags.ORDER_SESSION_ID to order({ it.sessionId.orEmpty() }) { order, text -> ok(text).map { order.copy(sessionId = it) } },
        Tags.ORDER_FEATURES to
            order({ namesToText(it.features) }) { order, text -> Decoded.Ok(order.copy(features = namesFromText(text).distinct())) },
        Tags.ORDER_EXTRA to order({ extraToText(it.extra) }) { order, text -> extraFromText(text).map { order.copy(extra = it) } },
        Tags.SDK_LOCALE to sdk({ it.locale.orEmpty() }) { sdk, text -> ok(text).map { sdk.copy(locale = it) } },
        Tags.SDK_THEME to
            sdk({ it.theme.json }) { sdk, text ->
                choice("sdk.theme", text, ThemePreset.entries) { it.json }.map { sdk.copy(theme = it) }
            },
        Tags.SDK_SHOW_RESULT_SCREEN to
            sdk({ it.showResultScreen.toString() }) { sdk, text ->
                flag("sdk.showResultScreen", text).map { sdk.copy(showResultScreen = it) }
            },
        Tags.SDK_SCANNER to sdk({ it.scanner.toString() }) { sdk, text -> flag("sdk.scanner", text).map { sdk.copy(scanner = it) } },
        Tags.SDK_KEY_PROVIDER_URL to sdk({ it.keyProviderUrl.orEmpty() }) { sdk, text -> ok(text).map { sdk.copy(keyProviderUrl = it) } },
        Tags.SDK_SSL_PINNING_ENABLED to
            sdk({ it.sslPinning.enabled.toString() }) { sdk, text ->
                flag("sdk.sslPinning.enabled", text).map { sdk.copy(sslPinning = sdk.sslPinning.copy(enabled = it)) }
            },
        Tags.SDK_SSL_PINNING_PINS to
            sdk({ pinsToText(it.sslPinning.pins) }) { sdk, text ->
                pinsFromText(text).map { sdk.copy(sslPinning = sdk.sslPinning.copy(pins = it)) }
            },
        Tags.SDK_GOOGLE_PAY_ENABLED to
            googlePay({ it.enabled.toString() }) { options, text ->
                flag("sdk.googlePay.enabled", text).map { options.copy(enabled = it) }
            },
        Tags.SDK_GOOGLE_PAY_MERCHANT_ID to
            googlePay({ it.merchantId.orEmpty() }) { options, text -> ok(text).map { options.copy(merchantId = it) } },
        Tags.SDK_GOOGLE_PAY_MERCHANT_FULL_NAME to
            googlePay({ it.merchantFullName.orEmpty() }) { options, text -> ok(text).map { options.copy(merchantFullName = it) } },
        Tags.SDK_GOOGLE_PAY_GATEWAY to
            googlePay({ it.gateway.orEmpty() }) { options, text -> ok(text).map { options.copy(gateway = it) } },
        Tags.SDK_GOOGLE_PAY_GATEWAY_MERCHANT_ID to
            googlePay({ it.gatewayMerchantId.orEmpty() }) { options, text -> ok(text).map { options.copy(gatewayMerchantId = it) } },
        Tags.SDK_GOOGLE_PAY_ENVIRONMENT to
            googlePay({ it.environment.orEmpty() }) { options, text -> ok(text).map { options.copy(environment = it) } },
        Tags.SDK_GOOGLE_PAY_ALLOWED_CARD_NETWORKS to
            googlePay({ namesToText(it.allowedCardNetworks) }) { options, text ->
                Decoded.Ok(options.copy(allowedCardNetworks = namesFromText(text)))
            },
        Tags.SDK_GOOGLE_PAY_ALLOWED_AUTH_METHODS to
            googlePay({ namesToText(it.allowedAuthMethods) }) { options, text ->
                Decoded.Ok(options.copy(allowedAuthMethods = namesFromText(text)))
            },
        Tags.SDK_GOOGLE_PAY_COUNTRY_CODE to
            googlePay({ it.countryCode.orEmpty() }) { options, text -> ok(text).map { options.copy(countryCode = it) } },
        Tags.SDK_GOOGLE_PAY_BILLING_ADDRESS_REQUIRED to
            googlePay({ it.billingAddressRequired.toString() }) { options, text ->
                flag("sdk.googlePay.billingAddressRequired", text).map { options.copy(billingAddressRequired = it) }
            },
    )
