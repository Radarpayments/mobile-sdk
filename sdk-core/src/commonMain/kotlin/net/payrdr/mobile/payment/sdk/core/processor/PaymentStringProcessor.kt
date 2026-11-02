package net.payrdr.mobile.payment.sdk.core.processor

import net.payrdr.mobile.payment.sdk.core.model.CardInfo
import net.payrdr.mobile.payment.sdk.core.model.MsdkRegisteredFrom

/**
 * Generates the slash-delimited payment-string that gets RSA-encrypted into the token.
 *
 * Format positions (1-based): `timestamp/uuid/pan/cvv/expdate/mdOrder/bindingId/cardholder/registeredFrom`.
 */
public interface PaymentStringProcessor {
    public fun createPaymentString(
        order: String = "",
        timestamp: Long,
        uuid: String,
        cardInfo: CardInfo,
        registeredFrom: MsdkRegisteredFrom = MsdkRegisteredFrom.MSDK_CORE,
    ): String
}
