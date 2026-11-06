/// How a checkout ended. Every result names the order ([orderId], the
/// gateway's mdOrder): your server, not this result, decides whether to
/// release the goods.
sealed class CheckoutResult {
  const CheckoutResult(this.orderId);

  final String orderId;

  factory CheckoutResult.fromJson(Map<String, Object?> json) {
    final orderId = json['orderId'] as String? ?? '';
    final error = json['error'] == null ? null : CheckoutError.fromJson(json['error']! as Map<String, Object?>);
    return switch (json['status']) {
      'completed' => CheckoutCompleted(orderId),
      'canceled' => CheckoutCanceled(orderId),
      'failed' => CheckoutFailed(orderId, error ?? const CheckoutError(CheckoutErrorCode.unknown)),
      _ => CheckoutUnknown(orderId, error),
    };
  }
}

/// The gateway confirmed the payment.
final class CheckoutCompleted extends CheckoutResult {
  const CheckoutCompleted(super.orderId);

  @override
  bool operator ==(Object other) => other is CheckoutCompleted && other.orderId == orderId;

  @override
  int get hashCode => Object.hash(CheckoutCompleted, orderId);

  @override
  String toString() => 'CheckoutCompleted($orderId)';
}

/// The payment did not happen: declined, expired, or the sheet could not open
/// ([CheckoutErrorCode.busy], [CheckoutErrorCode.noScreen],
/// [CheckoutErrorCode.invalidRequest]).
final class CheckoutFailed extends CheckoutResult {
  const CheckoutFailed(super.orderId, this.error);

  final CheckoutError error;

  @override
  bool operator ==(Object other) => other is CheckoutFailed && other.orderId == orderId && other.error == error;

  @override
  int get hashCode => Object.hash(CheckoutFailed, orderId, error);

  @override
  String toString() => 'CheckoutFailed($orderId, $error)';
}

/// The buyer closed the sheet.
final class CheckoutCanceled extends CheckoutResult {
  const CheckoutCanceled(super.orderId);

  @override
  bool operator ==(Object other) => other is CheckoutCanceled && other.orderId == orderId;

  @override
  int get hashCode => Object.hash(CheckoutCanceled, orderId);

  @override
  String toString() => 'CheckoutCanceled($orderId)';
}

/// The payment was sent but how it ended is not known (no network, or the
/// gateway is still deciding). Check the order on your server or wait for the
/// gateway's callback; do not offer to pay again meanwhile.
final class CheckoutUnknown extends CheckoutResult {
  const CheckoutUnknown(super.orderId, [this.error]);

  /// The last error seen while checking, if any.
  final CheckoutError? error;

  @override
  bool operator ==(Object other) => other is CheckoutUnknown && other.orderId == orderId && other.error == error;

  @override
  int get hashCode => Object.hash(CheckoutUnknown, orderId, error);

  @override
  String toString() => 'CheckoutUnknown($orderId, $error)';
}

/// What went wrong. [message] is the gateway's or the bank's text when there
/// is one.
class CheckoutError {
  const CheckoutError(
    this.code, {
    this.message,
    this.declineCode,
    this.httpStatus,
    this.gatewayErrorCode,
  });

  factory CheckoutError.fromJson(Map<String, Object?> json) => CheckoutError(
        CheckoutErrorCode.fromWire(json['code'] as String?),
        message: json['message'] as String?,
        declineCode: json['declineCode'] as String?,
        httpStatus: (json['httpStatus'] as num?)?.toInt(),
        gatewayErrorCode: json['gatewayErrorCode'] as String?,
      );

  final CheckoutErrorCode code;
  final String? message;

  /// The bank's code of a [CheckoutErrorCode.declined] payment.
  final String? declineCode;

  /// HTTP status and the gateway's own code of a [CheckoutErrorCode.api]
  /// error.
  final int? httpStatus;
  final String? gatewayErrorCode;

  @override
  bool operator ==(Object other) =>
      other is CheckoutError &&
      other.code == code &&
      other.message == message &&
      other.declineCode == declineCode &&
      other.httpStatus == httpStatus &&
      other.gatewayErrorCode == gatewayErrorCode;

  @override
  int get hashCode => Object.hash(code, message, declineCode, httpStatus, gatewayErrorCode);

  @override
  String toString() => 'CheckoutError(${code.wire}${message == null ? '' : ': $message'})';
}

enum CheckoutErrorCode {
  declined('declined'),
  api('api'),
  network('network'),
  transaction('transaction'),
  sessionNotFound('session_not_found'),
  orderNotFound('order_not_found'),

  /// The time to pay ran out: the gateway declined the order, nothing was charged. Register a new one to pay again.
  sessionExpired('session_expired'),
  invalidSessionId('invalid_session_id'),
  cryptogram('cryptogram'),
  notConfigured('not_configured'),
  threeDsFailed('three_ds_failed'),
  threeDsTimeout('three_ds_timeout'),
  googlePay('google_pay'),
  applePay('apple_pay'),

  /// The request did not follow the contract (e.g. a bad colour).
  invalidRequest('invalid_request'),

  /// Another checkout is already on screen.
  busy('busy'),

  /// The app had no screen to put the sheet over.
  noScreen('no_screen'),
  unknown('unknown');

  const CheckoutErrorCode(this.wire);

  /// The code in the native contract.
  final String wire;

  static CheckoutErrorCode fromWire(String? wire) =>
      values.firstWhere((code) => code.wire == wire, orElse: () => CheckoutErrorCode.unknown);
}
