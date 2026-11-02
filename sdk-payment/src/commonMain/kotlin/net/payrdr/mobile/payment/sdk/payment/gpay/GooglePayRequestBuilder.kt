package net.payrdr.mobile.payment.sdk.payment.gpay

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import net.payrdr.mobile.payment.sdk.payment.config.GooglePayConfig

/**
 * Pure helper that serializes a [GooglePayConfig] + runtime amount into the JSON blob Google
 * Pay's `PaymentsClient.loadPaymentData(PaymentDataRequest.fromJson(...))` expects.
 *
 * This code has **no** platform dependencies — it's callable from commonTest and produces
 * byte-for-byte identical output on JVM and K/N.
 */
public object GooglePayRequestBuilder {
    private val TOTAL_PRICE_PATTERN = Regex("""^\d+(\.\d+)?$""")

    /**
     * @param config complete Google Pay configuration: merchant id, gateway and gateway merchant id set.
     * @param totalPriceMajor human-readable major-unit price, e.g. `"10.55"`. Caller is responsible
     *   for formatting (Google Pay requires a dot-separated decimal string).
     * @param currencyCode ISO 4217 alpha-3 currency.
     * @param countryCode ISO 3166-1 alpha-2 country.
     * @return JSON string ready to pass to `PaymentDataRequest.fromJson(...)`.
     */
    public fun buildPaymentDataRequest(
        config: GooglePayConfig,
        totalPriceMajor: String,
        currencyCode: String,
        countryCode: String,
    ): String {
        require(config.allowedCardNetworks.isNotEmpty()) {
            "GooglePayConfig.allowedCardNetworks must contain at least one network"
        }
        require(config.allowedAuthMethods.isNotEmpty()) {
            "GooglePayConfig.allowedAuthMethods must contain at least one method"
        }
        require(TOTAL_PRICE_PATTERN.matches(totalPriceMajor)) {
            "totalPriceMajor must match ^\\d+(\\.\\d+)?$ (dot decimal, no thousands separators, " +
                "no currency symbol). Got: '$totalPriceMajor'"
        }
        require(currencyCode.length == 3) {
            "currencyCode must be ISO 4217 alpha-3 (3 chars). Got: '$currencyCode'"
        }
        require(countryCode.length == 2) {
            "countryCode must be ISO 3166-1 alpha-2 (2 chars). Got: '$countryCode'"
        }
        requireNotNull(config.merchantId) { "GooglePayConfig.merchantId is not known" }
        requireNotNull(config.gateway) { "GooglePayConfig.gateway is not known" }
        requireNotNull(config.gatewayMerchantId) { "GooglePayConfig.gatewayMerchantId is not known" }
        val root =
            buildJsonObject {
                put("apiVersion", JsonPrimitive(2))
                put("apiVersionMinor", JsonPrimitive(0))
                put("allowedPaymentMethods", allowedPaymentMethods(config))
                put("transactionInfo", transactionInfo(totalPriceMajor, currencyCode, countryCode))
                put("merchantInfo", merchantInfo(config))
            }
        return root.toString()
    }

    /**
     * Build the minimal `isReadyToPay` JSON — used by [GooglePayTokenProvider] to check GPay
     * availability before rendering the button.
     */
    public fun buildIsReadyToPayRequest(config: GooglePayConfig): String =
        buildJsonObject {
            put("apiVersion", JsonPrimitive(2))
            put("apiVersionMinor", JsonPrimitive(0))
            put("allowedPaymentMethods", readyToPayMethods(config))
        }.toString()

    /**
     * The `allowedPaymentMethods` array alone, as `ButtonOptions.setAllowedPaymentMethods` takes it: given the
     * whole `isReadyToPay` envelope, the button fails to build its view and stays blank.
     */
    public fun buildButtonPaymentMethods(config: GooglePayConfig): String = readyToPayMethods(config).toString()

    private fun readyToPayMethods(config: GooglePayConfig): JsonArray {
        require(config.allowedCardNetworks.isNotEmpty()) {
            "GooglePayConfig.allowedCardNetworks must contain at least one network"
        }
        require(config.allowedAuthMethods.isNotEmpty()) {
            "GooglePayConfig.allowedAuthMethods must contain at least one method"
        }
        return buildJsonArray { add(baseCardPaymentMethod(config)) }
    }

    private fun allowedPaymentMethods(config: GooglePayConfig): JsonArray =
        buildJsonArray {
            add(
                JsonObject(
                    baseCardPaymentMethod(config).toMutableMap().also {
                        it["tokenizationSpecification"] = tokenizationSpecification(config)
                    },
                ),
            )
        }

    private fun baseCardPaymentMethod(config: GooglePayConfig): JsonObject =
        buildJsonObject {
            put("type", JsonPrimitive("CARD"))
            put(
                "parameters",
                buildJsonObject {
                    put(
                        "allowedAuthMethods",
                        buildJsonArray {
                            config.allowedAuthMethods.forEach { add(JsonPrimitive(it.value)) }
                        },
                    )
                    put(
                        "allowedCardNetworks",
                        buildJsonArray {
                            config.allowedCardNetworks.forEach { add(JsonPrimitive(it.value)) }
                        },
                    )
                    put("billingAddressRequired", JsonPrimitive(config.billingAddressRequired))
                    if (config.billingAddressRequired) {
                        put(
                            "billingAddressParameters",
                            buildJsonObject { put("format", JsonPrimitive("FULL")) },
                        )
                    }
                },
            )
        }

    private fun tokenizationSpecification(config: GooglePayConfig): JsonObject =
        buildJsonObject {
            put("type", JsonPrimitive("PAYMENT_GATEWAY"))
            put(
                "parameters",
                buildJsonObject {
                    put("gateway", JsonPrimitive(config.gateway))
                    put("gatewayMerchantId", JsonPrimitive(config.gatewayMerchantId))
                },
            )
        }

    private fun transactionInfo(
        totalPriceMajor: String,
        currencyCode: String,
        countryCode: String,
    ): JsonObject =
        buildJsonObject {
            put("totalPrice", JsonPrimitive(totalPriceMajor))
            put("totalPriceStatus", JsonPrimitive("FINAL"))
            put("currencyCode", JsonPrimitive(currencyCode))
            put("countryCode", JsonPrimitive(countryCode))
            put("checkoutOption", JsonPrimitive("COMPLETE_IMMEDIATE_PURCHASE"))
        }

    private fun merchantInfo(config: GooglePayConfig): JsonObject =
        buildJsonObject {
            put("merchantId", JsonPrimitive(config.merchantId))
            // Optional for Google Pay: without it the sheet shows the app's name.
            config.merchantFullName?.let { put("merchantName", JsonPrimitive(it)) }
        }
}
