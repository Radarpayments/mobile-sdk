package net.payrdr.mobile.payment.sdk.payment.ui.util

/**
 * Card network / payment system detected from a card number's leading digits.
 */
internal enum class CardNetwork {
    VISA,
    MASTERCARD,
    AMEX,
    UNIONPAY,
    JCB,
    DISCOVER,
    DINERS,
    MAESTRO,
    UNKNOWN,
}

/** Human-readable network name, e.g. `Mastercard`; empty for [CardNetwork.UNKNOWN]. */
internal val CardNetwork.displayName: String
    get() =
        when (this) {
            CardNetwork.VISA -> "Visa"
            CardNetwork.MASTERCARD -> "Mastercard"
            CardNetwork.AMEX -> "American Express"
            CardNetwork.UNIONPAY -> "UnionPay"
            CardNetwork.JCB -> "JCB"
            CardNetwork.DISCOVER -> "Discover"
            CardNetwork.DINERS -> "Diners Club"
            CardNetwork.MAESTRO -> "Maestro"
            CardNetwork.UNKNOWN -> ""
        }

/** Names the gateway uses for this network in `payerDataParamsNeedToBeFilled`; empty for [CardNetwork.UNKNOWN]. */
internal val CardNetwork.gatewayPaymentSystems: List<String>
    get() =
        when (this) {
            CardNetwork.VISA -> listOf("VISA")
            CardNetwork.MASTERCARD -> listOf("MASTERCARD", "MAESTRO")
            CardNetwork.AMEX -> listOf("AMEX")
            CardNetwork.UNIONPAY -> listOf("CUP", "UNIONPAY")
            CardNetwork.JCB -> listOf("JCB")
            CardNetwork.DISCOVER -> listOf("DISCOVER")
            CardNetwork.DINERS -> listOf("DINERS", "DINERS_CLUB")
            // Gateways may list Maestro under its own name or under Mastercard.
            CardNetwork.MAESTRO -> listOf("MAESTRO", "MASTERCARD")
            CardNetwork.UNKNOWN -> emptyList()
        }
