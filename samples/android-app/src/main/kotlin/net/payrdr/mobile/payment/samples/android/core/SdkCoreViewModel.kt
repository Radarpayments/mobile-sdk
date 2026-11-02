package net.payrdr.mobile.payment.samples.android.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.payrdr.mobile.payment.sdk.core.SdkCore
import net.payrdr.mobile.payment.sdk.core.TokenResult
import net.payrdr.mobile.payment.sdk.core.model.BindingParams
import net.payrdr.mobile.payment.sdk.core.model.CardInstantParams
import net.payrdr.mobile.payment.sdk.core.model.CardParams
import net.payrdr.mobile.payment.sdk.core.model.ParamField
import net.payrdr.mobile.payment.sdk.core.model.PaymentCardParams
import net.payrdr.mobile.payment.sdk.core.model.SdkCoreConfig

enum class SdkCoreVariant(
    val storageKey: String,
) {
    NEW_CARD("new"),
    INSTANT("instant"),
    STORED("stored"),
    ;

    companion object {
        fun fromStorageKey(key: String): SdkCoreVariant = entries.firstOrNull { it.storageKey == key } ?: NEW_CARD
    }
}

/**
 * Simple view-model: holds form state in-memory and produces `PaymentCardParams` according to
 * the active variant. Token generation is synchronous via [SdkCore.generate].
 *
 * Not an Android `ViewModel` — sample uses this with `remember { }` in the screen composable.
 */
class SdkCoreViewModel {
    private val _variant = mutableStateOf(SdkCoreVariant.NEW_CARD)

    @get:JvmName("getVariant")
    @set:JvmName("setVariantPrivate")
    var variant: SdkCoreVariant by _variant

    private val _pan = mutableStateOf("")

    @get:JvmName("getPan")
    @set:JvmName("setPanPrivate")
    var pan: String by _pan

    private val _expiry = mutableStateOf("")

    @get:JvmName("getExpiry")
    @set:JvmName("setExpiryPrivate")
    var expiry: String by _expiry

    private val _cvc = mutableStateOf("")

    @get:JvmName("getCvc")
    @set:JvmName("setCvcPrivate")
    var cvc: String by _cvc

    private val _holder = mutableStateOf("")

    @get:JvmName("getHolder")
    @set:JvmName("setHolderPrivate")
    var holder: String by _holder

    private val _mdOrder = mutableStateOf("")

    @get:JvmName("getMdOrder")
    @set:JvmName("setMdOrderPrivate")
    var mdOrder: String by _mdOrder

    private val _pubKeyPem = mutableStateOf("")

    @get:JvmName("getPubKeyPem")
    @set:JvmName("setPubKeyPemPrivate")
    var pubKeyPem: String by _pubKeyPem

    private val _bindingId = mutableStateOf("")

    @get:JvmName("getBindingId")
    @set:JvmName("setBindingIdPrivate")
    var bindingId: String by _bindingId

    fun setVariant(v: SdkCoreVariant) {
        variant = v
    }

    fun setPan(v: String) {
        pan = v
    }

    fun setExpiry(v: String) {
        expiry = v
    }

    fun setCvc(v: String) {
        cvc = v
    }

    fun setHolder(v: String) {
        holder = v
    }

    fun setMdOrder(v: String) {
        mdOrder = v
    }

    fun setPubKeyPem(v: String) {
        pubKeyPem = v
    }

    fun setBindingId(v: String) {
        bindingId = v
    }

    /** Builds `PaymentCardParams` matching the current variant. Caller is responsible for field validation. */
    fun buildParams(): PaymentCardParams =
        when (variant) {
            SdkCoreVariant.NEW_CARD ->
                CardParams(
                    mdOrder = mdOrder,
                    pan = pan,
                    cvc = cvc,
                    expiryMMYY = formatExpiryInput(expiry),
                    cardHolder = holder.ifBlank { null },
                    pubKey = pubKeyPem,
                )
            SdkCoreVariant.INSTANT ->
                CardInstantParams(
                    pan = pan,
                    cvc = cvc,
                    expiryMMYY = formatExpiryInput(expiry),
                    cardHolder = holder.ifBlank { null },
                    pubKey = pubKeyPem,
                )
            SdkCoreVariant.STORED ->
                BindingParams(
                    mdOrder = mdOrder,
                    bindingID = bindingId,
                    cvc = cvc.ifBlank { null },
                    pubKey = pubKeyPem,
                )
        }

    fun generate(): TokenResult {
        val config = SdkCoreConfig(cardParams = buildParams(), timestamp = System.currentTimeMillis())
        return SdkCore().generate(config)
    }
}

/** Inputs of the SDK Core form; each sdk-core [ParamField] error is shown next to one of them. */
enum class CoreField {
    PAN,
    EXPIRY,
    CVC,
    HOLDER,
    BINDING_ID,
    MD_ORDER,
    PUB_KEY,
}

/** The form field an sdk-core validation error belongs to; null for errors no field explains. */
fun ParamField.toCoreField(): CoreField? =
    when (this) {
        ParamField.PAN -> CoreField.PAN
        ParamField.EXPIRY -> CoreField.EXPIRY
        ParamField.CVC -> CoreField.CVC
        ParamField.CARDHOLDER -> CoreField.HOLDER
        ParamField.BINDING_ID -> CoreField.BINDING_ID
        ParamField.MD_ORDER -> CoreField.MD_ORDER
        ParamField.PUB_KEY -> CoreField.PUB_KEY
        ParamField.UNKNOWN, ParamField.STORED_PAYMENT_ID -> null
    }

/** Typed expiry as `MM/YY`: digits only, the slash added after the month. */
fun formatExpiryInput(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(4)
    return if (digits.length > 2) digits.take(2) + "/" + digits.drop(2) else digits
}
