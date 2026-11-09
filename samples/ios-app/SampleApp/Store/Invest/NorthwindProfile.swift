import Foundation

/// The demo investor. The merchant knows its customer, so it registers the transfer with the sender's and the
/// recipient's details (the same person: money goes to their own account) and the sheet asks only for the card.
enum NorthwindProfile {
    static let name = "Alex Kim"
    static let initials = "AK"
    static let accountLast4 = "7781"
    private static let account = "NW0000" + accountLast4

    static let prefill = TransferPrefill(
        email: "alex.kim@example.com",
        billingPayerData: [
            "payerFirstName": "Alex",
            "payerLastName": "Kim",
            "payerBirthday": "19900115",
            "payerAccount": account,
            "billingCountry": "CY",
            "billingCity": "Nicosia",
            "billingAddressLine1": "1 Makariou Ave",
        ],
        billingRecipientData: [
            "recipientFirstName": "Alex",
            "recipientLastName": "Kim",
            "recipientAccount": account,
            // "Other": a brokerage account is none of the gateway's named account types.
            "recipientAccountNumberType": "100",
            "recipientCountry": "CY",
        ]
    )
}
