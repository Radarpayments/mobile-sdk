package net.payrdr.mobile.payment.sdk.payment.api

/** HTTP endpoint paths used by [PaymentApiImpl]. Paths are joined against the configured `baseUrl`. */
internal object Endpoints {
    const val GET_SESSION_STATUS = "/rest/getSessionStatus.do"
    const val PROCESS_FORM = "/rest/processform.do"
    const val PROCESS_BINDING_FORM = "/rest/processBindingForm.do"
    const val GOOGLE_PAY_PROCESS_FORM = "/google/paymentOrder.do"
    const val APPLE_PAY_PROCESS_FORM = "/applepay/paymentOrder.do"
    const val GET_FINISHED_PAYMENT_INFO = "/rest/getFinishedPaymentInfo.do"
    const val GET_PAYMENT_SETTINGS = "/rest/getPaymentSettings.do"
    const val UNBIND_CARD_ANON = "/rest/unbindcardanon.do"
    const val P2P_PERFORM = "/rest/p2p/perform.do"
    const val P2P_PERFORM_BY_BINDING = "/rest/p2p/performByBinding.do"
}
