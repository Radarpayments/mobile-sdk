package net.payrdr.mobile.payment.sdk.qa.config

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val MAX_AMOUNT = 999_999_999_999L
private const val PINS = "sdk.sslPinning.pins"
private const val EXTRA = "order.extra"
private const val GP = "sdk.googlePay"
private const val AP = "sdk.applePay"

private val LOCALES = listOf("en", "ru", "ro", "el", "bg", "de", "fr", "es", "ar")
private val GP_ENVIRONMENTS = listOf("test", "production")
private val GP_NETWORKS = listOf("AMEX", "DISCOVER", "INTERAC", "JCB", "MASTERCARD", "VISA")
private val GP_AUTH_METHODS = listOf("PAN_ONLY", "CRYPTOGRAM_3DS")
private val AP_NETWORKS = listOf("AMEX", "DISCOVER", "JCB", "MASTERCARD", "VISA")
private val AP_CAPABILITIES = listOf("CREDIT", "DEBIT", "EMV", "THREE_D_SECURE")
private val FORBIDDEN_EXTRA_KEYS = listOf("username", "password", "token")
private val PIN_PATTERN = Regex("[A-Za-z0-9+/]{43}=")

// What OkHttp's CertificatePinner accepts and the iOS delegate matches: no scheme, port or path, labels of at most 63
// characters (OkHttp rejects longer ones), an optional leading `*.` (one label) or `**.` (any depth).
private val PIN_HOST_PATTERN = Regex("""(\*\*?\.)?[A-Za-z0-9-]{1,63}(\.[A-Za-z0-9-]{1,63})*""")

// The table order of the contract; a group path ranks before its members.
private val FIELDS =
    listOf(
        "version",
        "profile",
        "order",
        "order.source",
        "order.type",
        "order.amount",
        "order.currency",
        "order.clientId",
        "order.mdOrder",
        "order.sessionId",
        "order.features",
        EXTRA,
        "sdk",
        "sdk.locale",
        "sdk.theme",
        "sdk.showResultScreen",
        "sdk.scanner",
        "sdk.keyProviderUrl",
        "sdk.sslPinning",
        "sdk.sslPinning.enabled",
        PINS,
        GP,
        "$GP.enabled",
        "$GP.billingAddressRequired",
        "$GP.merchantId",
        "$GP.merchantFullName",
        "$GP.gateway",
        "$GP.gatewayMerchantId",
        "$GP.countryCode",
        "$GP.environment",
        "$GP.allowedCardNetworks",
        "$GP.allowedAuthMethods",
        AP,
        "$AP.enabled",
        "$AP.merchantId",
        "$AP.supportedNetworks",
        "$AP.merchantCapabilities",
        "$AP.countryCode",
    )

internal object QaConfigJson {
    private val writer = Json { prettyPrint = true }

    fun decode(text: String): Decoded<QaConfig> {
        limitProblem(text)?.let { return Decoded.Invalid(Problem("$", it)) }
        val root = parseObject(text) ?: return Decoded.Invalid(NOT_AN_OBJECT)
        val reader = JsonReader()
        val version = reader.integral(root, "version", "version")
        if (version != null && version != 1L) reader.fail("version", "must be 1")
        val config =
            QaConfig(
                profile = reader.nullableText(root, "profile", "profile"),
                order = readOrder(reader, reader.group(root, "order", "order")),
                sdk = readSdk(reader, reader.group(root, "sdk", "sdk")),
            )
        val problem = firstInOrder(reader.problems + listOfNotNull(problemOf(config)), FIELDS)
        return if (problem == null) Decoded.Ok(config) else Decoded.Invalid(problem)
    }

    fun problemOf(config: QaConfig): Problem? {
        val order = config.order
        val sdk = config.sdk
        val googlePay = sdk.googlePay
        val applePay = sdk.applePay
        return listOfNotNull(
            config.profile
                ?.takeUnless { NAME_PATTERN.matches(it) }
                ?.let { Problem("profile", "must be 1 to 32 characters of a-z, 0-9, _ and -") },
            problemIf(order.amount !in 0..MAX_AMOUNT, "order.amount", "must be between 0 and $MAX_AMOUNT"),
            problemIf(order.currency !in Currencies.codes, "order.currency", "must be one of ${Currencies.codes.joinToString(", ")}"),
            problemIf(
                order.features.any { feature -> feature.isBlank() || feature.any { it == ',' || it.isWhitespace() } },
                "order.features",
                "each feature must be non-blank, without commas and whitespace",
            ),
            extraProblem(order.extra),
            problemIf(sdk.locale != null && sdk.locale !in LOCALES, "sdk.locale", "must be one of ${LOCALES.joinToString(", ")}"),
            problemIf(
                sdk.keyProviderUrl != null && !isHttpUrl(sdk.keyProviderUrl),
                "sdk.keyProviderUrl",
                "must be an absolute http or https URL",
            ),
            pinsProblem(sdk.sslPinning.pins),
            problemIf(
                googlePay.environment != null && googlePay.environment !in GP_ENVIRONMENTS,
                "$GP.environment",
                "must be test or production",
            ),
            subsetProblem("$GP.allowedCardNetworks", googlePay.allowedCardNetworks, GP_NETWORKS),
            subsetProblem("$GP.allowedAuthMethods", googlePay.allowedAuthMethods, GP_AUTH_METHODS),
            problemIf(applePay.merchantId.isBlank(), "$AP.merchantId", "must not be blank"),
            subsetProblem("$AP.supportedNetworks", applePay.supportedNetworks, AP_NETWORKS),
            subsetProblem("$AP.merchantCapabilities", applePay.merchantCapabilities, AP_CAPABILITIES),
        ).firstOrNull()
    }

    fun encode(config: QaConfig): String {
        val order = config.order
        val sdk = config.sdk
        val document: JsonElement =
            buildJsonObject {
                put("version", 1)
                put("profile", config.profile)
                put(
                    "order",
                    buildJsonObject {
                        put("source", order.source.json)
                        put("type", order.type.json)
                        put("amount", order.amount)
                        put("currency", order.currency)
                        put("clientId", order.clientId)
                        put("mdOrder", order.mdOrder)
                        put("sessionId", order.sessionId)
                        put("features", strings(order.features.distinct()))
                        put("extra", order.extra)
                    },
                )
                put(
                    "sdk",
                    buildJsonObject {
                        put("locale", sdk.locale)
                        put("theme", sdk.theme.json)
                        put("showResultScreen", sdk.showResultScreen)
                        put("scanner", sdk.scanner)
                        put("keyProviderUrl", sdk.keyProviderUrl)
                        put("sslPinning", pinningJson(sdk.sslPinning))
                        put("googlePay", googlePayJson(sdk.googlePay))
                        put("applePay", applePayJson(sdk.applePay))
                    },
                )
            }
        return writer.encodeToString(JsonElement.serializer(), document)
    }
}

private fun pinningJson(pinning: SslPinningOptions) =
    buildJsonObject {
        put("enabled", pinning.enabled)
        put(
            "pins",
            JsonArray(
                pinning.pins.map { pin ->
                    buildJsonObject {
                        put("host", pin.host)
                        put("pin", pin.pin)
                    }
                },
            ),
        )
    }

private fun googlePayJson(options: GooglePayOptions) =
    buildJsonObject {
        put("enabled", options.enabled)
        put("billingAddressRequired", options.billingAddressRequired)
        put("merchantId", options.merchantId)
        put("merchantFullName", options.merchantFullName)
        put("gateway", options.gateway)
        put("gatewayMerchantId", options.gatewayMerchantId)
        put("countryCode", options.countryCode)
        put("environment", options.environment)
        put("allowedCardNetworks", strings(canonical(options.allowedCardNetworks, GP_NETWORKS)))
        put("allowedAuthMethods", strings(canonical(options.allowedAuthMethods, GP_AUTH_METHODS)))
    }

private fun applePayJson(options: ApplePayOptions) =
    buildJsonObject {
        put("enabled", options.enabled)
        put("merchantId", options.merchantId)
        put("supportedNetworks", strings(canonical(options.supportedNetworks, AP_NETWORKS)))
        put("merchantCapabilities", strings(canonical(options.merchantCapabilities, AP_CAPABILITIES)))
        put("countryCode", options.countryCode)
    }

private fun strings(values: List<String>) = JsonArray(values.map { JsonPrimitive(it) })

private fun readOrder(
    reader: JsonReader,
    source: JsonObject,
): OrderConfig {
    val defaults = OrderConfig()
    return OrderConfig(
        source = reader.choice(source, "source", "order.source", OrderSource.entries, { it.json }, defaults.source),
        type = reader.choice(source, "type", "order.type", OrderType.entries, { it.json }, defaults.type),
        amount = reader.integral(source, "amount", "order.amount") ?: defaults.amount,
        currency = reader.text(source, "currency", "order.currency") ?: defaults.currency,
        clientId = reader.nullableText(source, "clientId", "order.clientId"),
        features = reader.names(source, "features", "order.features")?.distinct() ?: defaults.features,
        extra = reader.group(source, "extra", EXTRA),
        mdOrder = reader.nullableText(source, "mdOrder", "order.mdOrder"),
        sessionId = reader.nullableText(source, "sessionId", "order.sessionId"),
    )
}

private fun readSdk(
    reader: JsonReader,
    source: JsonObject,
): SdkOptions {
    val defaults = SdkOptions()
    return SdkOptions(
        locale = reader.nullableText(source, "locale", "sdk.locale"),
        theme = reader.choice(source, "theme", "sdk.theme", ThemePreset.entries, { it.json }, defaults.theme),
        showResultScreen = reader.flag(source, "showResultScreen", "sdk.showResultScreen", defaults.showResultScreen),
        googlePay = readGooglePay(reader, reader.group(source, "googlePay", GP)),
        applePay = readApplePay(reader, reader.group(source, "applePay", AP)),
        scanner = reader.flag(source, "scanner", "sdk.scanner", defaults.scanner),
        sslPinning = readPinning(reader, reader.group(source, "sslPinning", "sdk.sslPinning")),
        keyProviderUrl = reader.nullableText(source, "keyProviderUrl", "sdk.keyProviderUrl"),
    )
}

private fun readPinning(
    reader: JsonReader,
    source: JsonObject,
): SslPinningOptions {
    val enabled = reader.flag(source, "enabled", "sdk.sslPinning.enabled", false)
    val element = source["pins"]?.takeUnless { it is JsonNull }
    val pins =
        when (element) {
            null -> emptyList()
            is JsonArray -> element.mapNotNull { readPin(reader, it) }
            else -> {
                reader.fail(PINS, "must be a list of objects")
                emptyList()
            }
        }
    return SslPinningOptions(enabled, pins)
}

private fun readPin(
    reader: JsonReader,
    element: JsonElement,
): Pin? {
    if (element !is JsonObject) {
        reader.fail(PINS, "each pin must be an object")
        return null
    }
    return Pin(reader.text(element, "host", PINS).orEmpty(), reader.text(element, "pin", PINS).orEmpty())
}

private fun readGooglePay(
    reader: JsonReader,
    source: JsonObject,
): GooglePayOptions {
    val defaults = GooglePayOptions()
    return GooglePayOptions(
        enabled = reader.flag(source, "enabled", "$GP.enabled", defaults.enabled),
        merchantId = reader.nullableText(source, "merchantId", "$GP.merchantId"),
        merchantFullName = reader.nullableText(source, "merchantFullName", "$GP.merchantFullName"),
        gateway = reader.nullableText(source, "gateway", "$GP.gateway"),
        gatewayMerchantId = reader.nullableText(source, "gatewayMerchantId", "$GP.gatewayMerchantId"),
        environment = reader.nullableText(source, "environment", "$GP.environment"),
        allowedCardNetworks =
            reader.names(source, "allowedCardNetworks", "$GP.allowedCardNetworks")?.let { canonical(it, GP_NETWORKS) }
                ?: defaults.allowedCardNetworks,
        allowedAuthMethods =
            reader.names(source, "allowedAuthMethods", "$GP.allowedAuthMethods")?.let { canonical(it, GP_AUTH_METHODS) }
                ?: defaults.allowedAuthMethods,
        countryCode = reader.nullableText(source, "countryCode", "$GP.countryCode"),
        billingAddressRequired = reader.flag(source, "billingAddressRequired", "$GP.billingAddressRequired", false),
    )
}

private fun readApplePay(
    reader: JsonReader,
    source: JsonObject,
): ApplePayOptions {
    val defaults = ApplePayOptions()
    return ApplePayOptions(
        enabled = reader.flag(source, "enabled", "$AP.enabled", defaults.enabled),
        merchantId = reader.text(source, "merchantId", "$AP.merchantId") ?: defaults.merchantId,
        supportedNetworks =
            reader.names(source, "supportedNetworks", "$AP.supportedNetworks")?.let { canonical(it, AP_NETWORKS) }
                ?: defaults.supportedNetworks,
        merchantCapabilities =
            reader.names(source, "merchantCapabilities", "$AP.merchantCapabilities")?.let { canonical(it, AP_CAPABILITIES) }
                ?: defaults.merchantCapabilities,
        countryCode = reader.nullableText(source, "countryCode", "$AP.countryCode"),
    )
}

internal fun isPinValid(pin: Pin): Boolean = PIN_HOST_PATTERN.matches(pin.host) && PIN_PATTERN.matches(pin.pin)

internal fun pinsProblem(pins: List<Pin>): Problem? =
    if (pins.all(::isPinValid)) null else Problem(PINS, "each pin needs a hostname and a 44 character base64 SHA-256 pin")

internal fun extraProblem(extra: JsonObject): Problem? =
    if (extra.keys.none { key -> FORBIDDEN_EXTRA_KEYS.any { it.equals(key, ignoreCase = true) } }) {
        null
    } else {
        Problem(EXTRA, "must not contain ${FORBIDDEN_EXTRA_KEYS.joinToString(", ")}")
    }

// Unknown members are kept at the end so that problemOf still sees and reports them.
private fun canonical(
    values: List<String>,
    allowed: List<String>,
): List<String> = allowed.filter { it in values } + values.filter { it !in allowed }.distinct()

private fun subsetProblem(
    field: String,
    values: List<String>,
    allowed: List<String>,
): Problem? = problemIf(values.any { it !in allowed }, field, "must be a subset of ${allowed.joinToString(", ")}")

private fun problemIf(
    failed: Boolean,
    field: String,
    message: String,
): Problem? = if (failed) Problem(field, message) else null
