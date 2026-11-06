import 'dart:async';
import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;

import '../random_hex.dart';

/// "Your server": registers an order at the gateway and hands back its mdOrder (API V1) or its session (API V2).
///
/// In a real integration this code lives on the merchant's backend, which keeps the merchant
/// login and password or API key; the app only receives the order. The demo registers orders from the app
/// so that it works without a backend — never ship merchant credentials inside an app.
abstract interface class OrderRegistrar {
  /// Registers an order for [amountMinor] euro cents; [clientId] links it to a buyer's saved cards, null = guest.
  Future<OrderRegistration> register(int amountMinor, String? clientId);
}

/// Outcome of [OrderRegistrar.register].
sealed class OrderRegistration {
  const OrderRegistration();
}

/// The gateway accepted the order: pass [sessionId] to the SDK when there is one (`presentSession`, API V2),
/// [mdOrder] otherwise (`present`, API V1). An API V2 session has an mdOrder too, the one its id encodes.
final class Registered extends OrderRegistration {
  const Registered(this.mdOrder, {this.sessionId});

  final String mdOrder;
  final String? sessionId;

  @override
  bool operator ==(Object other) => other is Registered && other.mdOrder == mdOrder && other.sessionId == sessionId;

  @override
  int get hashCode => Object.hash(mdOrder, sessionId);
}

/// Login or password is not set in the developer settings; nothing was sent.
final class MissingCredentials extends OrderRegistration {
  const MissingCredentials();

  @override
  bool operator ==(Object other) => other is MissingCredentials;

  @override
  int get hashCode => (MissingCredentials).hashCode;
}

/// The API V2 key is not set in the developer settings; nothing was sent.
final class MissingApiKey extends OrderRegistration {
  const MissingApiKey();

  @override
  bool operator ==(Object other) => other is MissingApiKey;

  @override
  int get hashCode => (MissingApiKey).hashCode;
}

/// The gateway refused the order or could not be reached.
final class RegistrationFailed extends OrderRegistration {
  const RegistrationFailed(this.message);

  final String message;

  @override
  bool operator ==(Object other) => other is RegistrationFailed && other.message == message;

  @override
  int get hashCode => message.hashCode;
}

/// Registers orders through API V1 of the gateway at [baseUrl], in euros: a payment with `register.do`, an AFT
/// transfer with `registerP2P.do` ([orderType]); gives up after [timeout].
class GatewayOrderRegistrar implements OrderRegistrar {
  GatewayOrderRegistrar({
    required this.baseUrl,
    required this._login,
    required this._password,
    required this._http,
    this.timeout = const Duration(seconds: 20),
    this.orderType = OrderType.purchase,
    this.prefill,
  });

  final String baseUrl;
  final String _login;
  final String _password;
  final http.Client _http;
  final Duration timeout;
  final OrderType orderType;

  /// Details a transfer is registered with; ignored for a payment.
  final TransferPrefill? prefill;

  static const _eur = '978';
  static const _withoutToCard = 'WITHOUT_TO_CARD';
  static const _verify = 'VERIFY';
  static const _forceCreateBinding = 'FORCE_CREATE_BINDING';

  @override
  Future<OrderRegistration> register(int amountMinor, String? clientId) async {
    if (_login.trim().isEmpty || _password.trim().isEmpty) return const MissingCredentials();
    if (orderType == OrderType.aft) return _registerTransfer(amountMinor, clientId);
    return _callGateway(
      timeout,
      parseRegisterResponse,
      () => _http.post(
        Uri.parse('$_restUrl/register.do'),
        body: {
          'userName': _login,
          'password': _password,
          'amount': '$amountMinor',
          'currency': _eur,
          'orderNumber': _newOrderNumber(),
          'returnUrl': _returnUrl,
          'clientId': ?clientId,
        },
      ),
    );
  }

  /// A one-leg AFT transfer (`WITHOUT_TO_CARD`, transaction type `A`): the buyer's card is debited, the money goes
  /// to the recipient; the sheet asks for the sender and recipient details the gateway needs.
  ///
  /// A transfer of 0 only saves the buyer's card for [clientId]: the gateway checks the card and reverses (`VERIFY`),
  /// and refuses `VERIFY` without `FORCE_CREATE_BINDING`.
  Future<OrderRegistration> _registerTransfer(int amountMinor, String? clientId) {
    final features = amountMinor == 0 ? const [_withoutToCard, _verify, _forceCreateBinding] : const [_withoutToCard];
    final order = {
      'username': _login,
      'password': _password,
      'amount': amountMinor,
      'currency': _eur,
      'orderNumber': _newOrderNumber(),
      'returnUrl': _returnUrl,
      'transactionTypeIndicator': 'A',
      'features': {'feature': features},
      'clientId': ?clientId,
      'email': ?prefill?.email,
      if (prefill?.billingPayerData case final payer? when payer.isNotEmpty) 'billingPayerData': payer,
      if (prefill?.billingRecipientData case final recipient? when recipient.isNotEmpty)
        'billingRecipientData': recipient,
    };
    // The answer is shaped like that of register.do: orderId, or errorMessage.
    return _callGateway(
      timeout,
      parseRegisterResponse,
      () => _http.post(
        Uri.parse('$_restUrl/api/p2p/registerP2P.do'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode(order),
      ),
    );
  }

  String get _restUrl => '${baseUrl.replaceAll(RegExp(r'/+$'), '')}/rest';

  @override
  String toString() => 'GatewayOrderRegistrar(baseUrl: $baseUrl, orderType: ${orderType.name})';

  // Gateways limit orderNumber to 32 characters.
  String _newOrderNumber() => 'demo-${randomHex(20)}';
}

/// Creates API V2 sessions (`POST /api2/sessions`) at the gateway of [baseUrl] with the merchant's API key, in
/// euros; gives up after [timeout]. The buyer comes back from the bank page to `sdk://done`, which the SDK expects
/// (the merchant's own pages work too).
class ApiV2SessionRegistrar implements OrderRegistrar {
  ApiV2SessionRegistrar({
    required this.baseUrl,
    required this._apiKey,
    required this._http,
    this.timeout = const Duration(seconds: 20),
  });

  final String baseUrl;
  final String _apiKey;
  final http.Client _http;
  final Duration timeout;

  @override
  Future<OrderRegistration> register(int amountMinor, String? clientId) async {
    if (_apiKey.trim().isEmpty) return const MissingApiKey();
    final session = {
      'amount': amountMinor,
      'currency': 'EUR',
      'successUrl': _returnUrl,
      'failureUrl': _returnUrl,
      // API V2's name for the buyer whose cards the gateway keeps (API V1's clientId).
      'merchantCustomerId': ?clientId,
    };
    return _callGateway(
      timeout,
      parseSessionResponse,
      () => _http.post(
        Uri.parse('${api2Url(baseUrl)}/sessions'),
        headers: {'X-API-KEY': _apiKey, 'Content-Type': 'application/json'},
        body: jsonEncode(session),
      ),
    );
  }

  @override
  String toString() => 'ApiV2SessionRegistrar(baseUrl: $baseUrl)';
}

/// The merchant API a demo order is registered through.
enum ApiVersion { v1, v2 }

/// What a demo order is: a payment, or a one-leg AFT transfer (API V1 `registerP2P.do`).
enum OrderType { purchase, aft }

/// Details a transfer is registered with so that the sheet does not ask for them: the buyer's [email], and
/// `billingPayerData` / `billingRecipientData` as `registerP2P.do` takes them (gateway field name → value).
@immutable
class TransferPrefill {
  const TransferPrefill({this.email, this.billingPayerData = const {}, this.billingRecipientData = const {}});

  final String? email;
  final Map<String, String> billingPayerData;
  final Map<String, String> billingRecipientData;
}

/// The registrar for the API version and order type chosen in the developer settings; transfers go through API V1 and
/// carry [prefill] when there is one.
OrderRegistrar merchantRegistrar({
  required ApiVersion apiVersion,
  required String baseUrl,
  required String login,
  required String password,
  required String apiKey,
  required http.Client http,
  OrderType orderType = OrderType.purchase,
  TransferPrefill? prefill,
}) => switch (apiVersion) {
  ApiVersion.v1 => GatewayOrderRegistrar(
    baseUrl: baseUrl,
    login: login,
    password: password,
    http: http,
    orderType: orderType,
    prefill: prefill,
  ),
  ApiVersion.v2 => ApiV2SessionRegistrar(baseUrl: baseUrl, apiKey: apiKey, http: http),
};

/// API V2 is served next to the payment page: `https://host/payment` → `https://host/api2`.
String api2Url(String baseUrl) {
  final url = Uri.parse(baseUrl.trim());
  // Uri drops the default port of the scheme, so only a port of its own is kept.
  return Uri(scheme: url.scheme, host: url.host, port: url.hasPort ? url.port : null, path: '/api2').toString();
}

/// Reads a `POST /api2/sessions` response: the session on success, `error.errorMessage` otherwise.
OrderRegistration parseSessionResponse(String body) {
  final Object? json;
  try {
    json = jsonDecode(body);
  } on FormatException {
    return const RegistrationFailed('Unexpected gateway response');
  }
  if (json is! Map<String, Object?>) return const RegistrationFailed('Unexpected gateway response');
  if (json['id'] case final id?) {
    final mdOrder = sessionIdToMdOrder('$id');
    return mdOrder == null ? RegistrationFailed('Unexpected session id $id') : Registered(mdOrder, sessionId: '$id');
  }
  final error = json['error'];
  final Object? message = error is Map<String, Object?> ? error['errorMessage'] : null;
  final Object? code = error is Map<String, Object?> ? error['errorCode'] : null;
  return RegistrationFailed(message != null ? '$message' : 'Gateway error ${code ?? ''}');
}

/// The mdOrder of an API V2 session, as the SDK's `SessionIdConverter` reads it: the id is `ps_` and the Base58
/// of `ps:` + mdOrder. Null for what is not such an id.
String? sessionIdToMdOrder(String sessionId) {
  if (!sessionId.startsWith(_sessionPrefix)) return null;
  final body = sessionId.substring(_sessionPrefix.length);
  if (body.isEmpty) return null;
  var value = BigInt.zero;
  for (final char in body.split('')) {
    final digit = _base58.indexOf(char);
    if (digit < 0) return null;
    value = value * _fiftyEight + BigInt.from(digit);
  }
  final bytes = <int>[];
  for (; value > BigInt.zero; value >>= 8) {
    bytes.add((value & _byteMask).toInt());
  }
  // Each leading '1' stands for a zero byte.
  final zeros = body.length - body.replaceFirst(RegExp('^1+'), '').length;
  final String decoded;
  try {
    decoded = utf8.decode([...List.filled(zeros, 0), ...bytes.reversed]);
  } on FormatException {
    return null;
  }
  return decoded.startsWith(_mdOrderPrefix) ? decoded.substring(_mdOrderPrefix.length) : null;
}

const _sessionPrefix = 'ps_';
const _mdOrderPrefix = 'ps:';
const _base58 = '123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz';
final _fiftyEight = BigInt.from(58);
final _byteMask = BigInt.from(0xff);

/// One gateway call of "your server"; a stalled DNS lookup must not keep "Pay" busy forever.
Future<OrderRegistration> _callGateway(
  Duration timeout,
  OrderRegistration Function(String body) parse,
  Future<http.Response> Function() request,
) async {
  final http.Response response;
  try {
    response = await request().timeout(timeout);
  } on TimeoutException {
    return RegistrationFailed('The gateway did not answer in ${timeout.inSeconds} s');
  } on http.ClientException catch (e) {
    return RegistrationFailed(e.message);
  } on FormatException catch (e) {
    return RegistrationFailed(e.message);
  }
  // The gateway answers in UTF-8 but does not always say so; `body` would fall back to Latin-1.
  return parse(utf8.decode(response.bodyBytes, allowMalformed: true));
}

const _returnUrl = 'sdk://done';

/// Reads a `register.do` response: `orderId` on success, `errorMessage` otherwise.
OrderRegistration parseRegisterResponse(String body) {
  final Object? json;
  try {
    json = jsonDecode(body);
  } on FormatException {
    return const RegistrationFailed('Unexpected gateway response');
  }
  if (json is! Map<String, Object?>) return const RegistrationFailed('Unexpected gateway response');
  if (json['orderId'] case final orderId?) return Registered('$orderId');
  if (json['errorMessage'] case final message?) return RegistrationFailed('$message');
  return RegistrationFailed('Gateway error ${json['errorCode'] ?? ''}');
}
