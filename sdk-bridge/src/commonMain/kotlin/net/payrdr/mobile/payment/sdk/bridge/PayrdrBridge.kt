package net.payrdr.mobile.payment.sdk.bridge

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import net.payrdr.mobile.payment.sdk.core.SdkCore
import net.payrdr.mobile.payment.sdk.core.model.BindingInstantParams
import net.payrdr.mobile.payment.sdk.core.model.BindingParams
import net.payrdr.mobile.payment.sdk.core.model.CardInstantParams
import net.payrdr.mobile.payment.sdk.core.model.CardParams
import net.payrdr.mobile.payment.sdk.core.model.ParamField
import net.payrdr.mobile.payment.sdk.core.model.PaymentCardParams
import net.payrdr.mobile.payment.sdk.core.model.SdkCoreConfig
import kotlin.time.Clock

/**
 * Entry points of the SDK for the Flutter and React Native plugins, which pass JSON instead of Kotlin types. The
 * checkout opens through `PayrdrBridgeCheckout` of each platform; SDK Core works the same everywhere.
 */
public object PayrdrBridge {
    /**
     * Encrypts the card data of a merchant's own card form into a token for the gateway's `seToken` (sdk-core).
     *
     * Request: `{"type": "card" | "cardInstant" | "binding" | "bindingInstant", "pubKey": "PEM", ...}` — a new card sends
     * `pan`, `expiry` (`MM/YY`), `cvc` and optionally `cardholder`; a saved one `bindingId` and optionally `cvc`; the
     * types without `Instant` also send the order's `mdOrder`.
     *
     * Result: `{"token": "..."}`, `{"errors": {"pan": "required", ...}}` with sdk-core's codes by field, or
     * `{"error": {"code": "invalid_request", "message": "..."}}` for a request that breaks the contract.
     */
    public fun coreToken(requestJson: String): String =
        try {
            val params = BridgeJson.decode<CoreTokenDto>(requestJson).toParams()
            val result = SdkCore().generate(SdkCoreConfig(cardParams = params, timestamp = Clock.System.now().toEpochMilliseconds()))
            buildJsonObject {
                val token = result.token
                if (result.isSuccessful && token != null) {
                    put("token", token)
                } else {
                    putJsonObject("errors") { result.errors.forEach { (field, code) -> put(field.key, code) } }
                }
            }.toString()
        } catch (e: InvalidBridgeRequest) {
            buildJsonObject {
                putJsonObject("error") {
                    put("code", CheckoutResultJson.INVALID_REQUEST)
                    put("message", e.message)
                }
            }.toString()
        }
}

@Serializable
private class CoreTokenDto(
    val type: String,
    val pubKey: String = "",
    val mdOrder: String = "",
    val pan: String = "",
    val expiry: String = "",
    val cvc: String? = null,
    val cardholder: String? = null,
    val bindingId: String = "",
) {
    fun toParams(): PaymentCardParams =
        when (type) {
            "card" ->
                CardParams(
                    mdOrder = mdOrder,
                    pan = pan,
                    cvc = cvc.orEmpty(),
                    expiryMMYY = expiry,
                    cardHolder = cardholder,
                    pubKey = pubKey,
                )
            "cardInstant" ->
                CardInstantParams(
                    pan = pan,
                    cvc = cvc.orEmpty(),
                    expiryMMYY = expiry,
                    cardHolder = cardholder,
                    pubKey = pubKey,
                )
            "binding" -> BindingParams(mdOrder = mdOrder, bindingID = bindingId, cvc = cvc, pubKey = pubKey)
            "bindingInstant" -> BindingInstantParams(bindingID = bindingId, cvc = cvc, pubKey = pubKey)
            else -> throw InvalidBridgeRequest("type must be card, cardInstant, binding or bindingInstant, not $type")
        }
}

/** The JSON key of a field in `errors`. */
private val ParamField.key: String
    get() =
        when (this) {
            ParamField.UNKNOWN -> "unknown"
            ParamField.PAN -> "pan"
            ParamField.CVC -> "cvc"
            ParamField.EXPIRY -> "expiry"
            ParamField.CARDHOLDER -> "cardholder"
            ParamField.BINDING_ID -> "bindingId"
            ParamField.MD_ORDER -> "mdOrder"
            ParamField.PUB_KEY -> "pubKey"
            ParamField.STORED_PAYMENT_ID -> "storedPaymentId"
        }
