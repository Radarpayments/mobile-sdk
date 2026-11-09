import '../../merchant/order_registrar.dart';

/// The demo investor. The merchant knows its customer, so it registers the transfer with the sender's and the
/// recipient's details (the same person: money goes to their own account) and the sheet asks only for the card.
abstract final class NorthwindProfile {
  static const name = 'Alex Kim';
  static const initials = 'AK';
  static const accountLast4 = '7781';
  static const _account = 'NW0000$accountLast4';

  static const prefill = TransferPrefill(
    email: 'alex.kim@example.com',
    billingPayerData: {
      'payerFirstName': 'Alex',
      'payerLastName': 'Kim',
      'payerBirthday': '19900115',
      'payerAccount': _account,
      'billingCountry': 'CY',
      'billingCity': 'Nicosia',
      'billingAddressLine1': '1 Makariou Ave',
    },
    billingRecipientData: {
      'recipientFirstName': 'Alex',
      'recipientLastName': 'Kim',
      'recipientAccount': _account,
      // "Other": a brokerage account is none of the gateway's named account types.
      'recipientAccountNumberType': '100',
      'recipientCountry': 'CY',
    },
  );
}
