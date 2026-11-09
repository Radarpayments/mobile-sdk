package net.payrdr.mobile.payment.samples.android.store.invest

import net.payrdr.mobile.payment.samples.android.merchant.TransferPrefill

/**
 * The demo investor. The merchant knows its customer, so it registers the transfer with the sender's and the
 * recipient's details (the same person: money goes to their own account) and the sheet asks only for the card.
 */
object NorthwindProfile {
    const val NAME = "Alex Kim"
    const val INITIALS = "AK"
    const val ACCOUNT_LAST4 = "7781"
    private const val ACCOUNT = "NW0000$ACCOUNT_LAST4"

    val prefill =
        TransferPrefill(
            email = "alex.kim@example.com",
            billingPayerData =
                mapOf(
                    "payerFirstName" to "Alex",
                    "payerLastName" to "Kim",
                    "payerBirthday" to "19900115",
                    "payerAccount" to ACCOUNT,
                    "billingCountry" to "CY",
                    "billingCity" to "Nicosia",
                    "billingAddressLine1" to "1 Makariou Ave",
                ),
            billingRecipientData =
                mapOf(
                    "recipientFirstName" to "Alex",
                    "recipientLastName" to "Kim",
                    "recipientAccount" to ACCOUNT,
                    // "Other": a brokerage account is none of the gateway's named account types.
                    "recipientAccountNumberType" to "100",
                    "recipientCountry" to "CY",
                ),
        )
}
