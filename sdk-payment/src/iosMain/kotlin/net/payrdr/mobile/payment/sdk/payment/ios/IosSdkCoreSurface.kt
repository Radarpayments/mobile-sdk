package net.payrdr.mobile.payment.sdk.payment.ios

import net.payrdr.mobile.payment.sdk.core.SdkCore
import net.payrdr.mobile.payment.sdk.core.model.BindingParams
import net.payrdr.mobile.payment.sdk.core.model.CardInstantParams
import net.payrdr.mobile.payment.sdk.core.model.CardParams
import net.payrdr.mobile.payment.sdk.core.model.PaymentCardParams
import net.payrdr.mobile.payment.sdk.core.model.SdkCoreConfig

/**
 * Swift-callable factory surface for sdk-core types.
 *
 * Kotlin/Native only emits Obj-C headers for types actually referenced by the
 * compiled module's public API. sdk-payment consumes `PaymentCardParams` as an
 * abstract type but never references the `CardParams` / `CardInstantParams` /
 * `BindingParams` subclasses or `SdkCore` / `SdkCoreConfig` directly, so Swift
 * consumers embedding the XCFramework have no way to construct them.
 *
 * These factories cost nothing at runtime and keep the sdk-payment surface
 * unchanged for android consumers (who import sdk-core classes directly).
 */
public fun iosSdkCore(): SdkCore = SdkCore()

public fun iosSdkCoreConfig(
    cardParams: PaymentCardParams,
    timestamp: Long,
): SdkCoreConfig = SdkCoreConfig(cardParams = cardParams, timestamp = timestamp)

public fun iosCardParams(
    mdOrder: String,
    pan: String,
    cvc: String,
    expiryMMYY: String,
    cardHolder: String?,
    pubKey: String,
): PaymentCardParams =
    CardParams(
        mdOrder = mdOrder,
        pan = pan,
        cvc = cvc,
        expiryMMYY = expiryMMYY,
        cardHolder = cardHolder,
        pubKey = pubKey,
    )

public fun iosCardInstantParams(
    pan: String,
    cvc: String,
    expiryMMYY: String,
    cardHolder: String?,
    pubKey: String,
): PaymentCardParams =
    CardInstantParams(
        pan = pan,
        cvc = cvc,
        expiryMMYY = expiryMMYY,
        cardHolder = cardHolder,
        pubKey = pubKey,
    )

public fun iosBindingParams(
    mdOrder: String,
    bindingID: String,
    cvc: String?,
    pubKey: String,
): PaymentCardParams =
    BindingParams(
        mdOrder = mdOrder,
        bindingID = bindingID,
        cvc = cvc,
        pubKey = pubKey,
    )
