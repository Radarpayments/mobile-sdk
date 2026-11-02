import 'dart:convert';

import 'checkout.dart' show payrdrChannel;

/// SDK Core: for a merchant with PCI DSS who keeps its own card form. The card
/// data are encrypted on the device into a token; your server passes it to the
/// gateway as `seToken`.
abstract final class PayrdrCore {
  /// The token for [request], or the errors of its fields.
  static Future<CoreTokenResult> generateToken(CoreTokenRequest request) async {
    final result = await payrdrChannel.invokeMethod<String>('coreToken', jsonEncode(request.toJson()));
    final json = jsonDecode(result!) as Map<String, Object?>;
    final error = json['error'] as Map<String, Object?>?;
    if (error != null) throw ArgumentError(error['message']);
    return CoreTokenResult(
      token: json['token'] as String?,
      errors: {
        for (final MapEntry(:key, :value) in ((json['errors'] as Map<String, Object?>?) ?? const {}).entries)
          CoreField.fromWire(key): value! as String,
      },
    );
  }
}

/// What to encrypt. [pubKey] is the gateway's public key in PEM.
class CoreTokenRequest {
  /// A new card for an order.
  const CoreTokenRequest.card({
    required String this.mdOrder,
    required String this.pan,
    required String this.expiry,
    required String this.cvc,
    this.cardholder,
    required this.pubKey,
  })  : _type = 'card',
        bindingId = null;

  /// A new card without an order (e.g. to save it).
  const CoreTokenRequest.cardWithoutOrder({
    required String this.pan,
    required String this.expiry,
    required String this.cvc,
    this.cardholder,
    required this.pubKey,
  })  : _type = 'cardInstant',
        mdOrder = null,
        bindingId = null;

  /// A saved card (its bindingId) for an order.
  const CoreTokenRequest.savedCard({
    required String this.mdOrder,
    required String this.bindingId,
    this.cvc,
    required this.pubKey,
  })  : _type = 'binding',
        pan = null,
        expiry = null,
        cardholder = null;

  /// A saved card without an order.
  const CoreTokenRequest.savedCardWithoutOrder({
    required String this.bindingId,
    this.cvc,
    required this.pubKey,
  })  : _type = 'bindingInstant',
        mdOrder = null,
        pan = null,
        expiry = null,
        cardholder = null;

  final String _type;
  final String? mdOrder;
  final String? pan;

  /// `MM/YY`.
  final String? expiry;
  final String? cvc;
  final String? cardholder;
  final String? bindingId;
  final String pubKey;

  Map<String, Object?> toJson() => {
        'type': _type,
        'pubKey': pubKey,
        if (mdOrder != null) 'mdOrder': mdOrder,
        if (pan != null) 'pan': pan,
        if (expiry != null) 'expiry': expiry,
        if (cvc != null) 'cvc': cvc,
        if (cardholder != null) 'cardholder': cardholder,
        if (bindingId != null) 'bindingId': bindingId,
      };
}

/// A token, or sdk-core's error code (`required`, `invalid`,
/// `invalid-format`) for each field that is wrong.
class CoreTokenResult {
  const CoreTokenResult({this.token, this.errors = const {}});

  final String? token;
  final Map<CoreField, String> errors;

  bool get isSuccessful => token != null && errors.isEmpty;
}

enum CoreField {
  pan,
  expiry,
  cvc,
  cardholder,
  bindingId,
  mdOrder,
  pubKey,
  storedPaymentId,
  unknown;

  static CoreField fromWire(String wire) => values.firstWhere((f) => f.name == wire, orElse: () => CoreField.unknown);
}
