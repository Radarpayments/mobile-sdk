package net.payrdr.mobile.payment.sdk.payment.api.dto.response

import kotlinx.serialization.Serializable

/**
 * Response from `getSessionStatus.do`.
 */
@Serializable
internal data class SessionStatusResponse(
    val redirect: String? = null,
    val remainingSecs: Long? = null,
    val orderNumber: String? = null,
    val amount: String? = null,
    val bindingItems: List<BindingItem> = emptyList(),
    val cvcNotRequired: Boolean = false,
    val otherWayEnabled: Boolean = false,
    val bindingEnabled: Boolean = false,
    val feeEnabled: Boolean = false,
    val backUrl: String? = null,
    val orderExpired: Boolean = false,
    val expirationDateCustomValidation: Boolean = false,
    val bindingDeactivationEnabled: Boolean = false,
    val currencyAlphaCode: String? = null,
    val merchantInfo: MerchantInfo = MerchantInfo(),
    val merchantOptions: List<String> = emptyList(),
    val orderPayerData: OrderPayerData? = null,
    val customerDetails: CustomerDetails? = null,
    val billingPayerData: BillingPayerData? = null,
    /** Payment system (`VISA`, `MASTERCARD`, …) → payer fields to collect for a card of that system. */
    val payerDataParamsNeedToBeFilled: Map<String, List<PayerParam>>? = null,
    /** `P2P` for a transfer order (`registerP2P.do`). */
    val orderPaymentWay: String? = null,
    /** Order features, e.g. `WITHOUT_TO_CARD` (a one-leg AFT transfer), `FORCE_CREATE_BINDING`, `VERIFY`. */
    val orderFeatures: List<String> = emptyList(),
    /** A transfer from a saved card needs no CVC (the e-commerce counterpart is [cvcNotRequired]). */
    val cvcBindingNotRequired: Boolean = false,
    /** Recipient of a transfer, as the merchant passed it when registering the order. */
    val billingRecipientData: BillingRecipientData? = null,
)
