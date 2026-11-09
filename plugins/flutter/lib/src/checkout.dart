import 'dart:convert';

import 'package:flutter/services.dart';

import 'config.dart';
import 'result.dart';

/// The one channel to the native SDK. JSON strings go both ways; the contract
/// is in the SDK's `sdk-bridge/README.md`.
const MethodChannel payrdrChannel = MethodChannel('net.payrdr.sdk');

/// Opens the native payment sheet over the app and gives back how it ended.
///
/// ```dart
/// final checkout = PayrdrCheckout(const PayrdrConfig(baseUrl: 'https://pay.example.com/payment'));
/// final result = await checkout.present(mdOrder);
/// switch (result) {
///   case CheckoutCompleted(): showPaid();
///   case CheckoutCanceled(): break;
///   case CheckoutFailed(:final error): showError(error.message);
///   case CheckoutUnknown(): checkOnServer(result.orderId);
/// }
/// ```
class PayrdrCheckout {
  const PayrdrCheckout(this.config);

  final PayrdrConfig config;

  /// Pays an order your server registered at the gateway (its mdOrder).
  Future<CheckoutResult> present(String mdOrder) => _open({'mdOrder': mdOrder});

  /// Pays an API v2 payment session (`ps_…`).
  Future<CheckoutResult> presentSession(String sessionId) => _open({'sessionId': sessionId});

  Future<CheckoutResult> _open(Map<String, String> order) async {
    final request = jsonEncode({'config': config.toJson(), ...order});
    final result = await payrdrChannel.invokeMethod<String>('present', request);
    return CheckoutResult.fromJson(jsonDecode(result!) as Map<String, Object?>);
  }
}
