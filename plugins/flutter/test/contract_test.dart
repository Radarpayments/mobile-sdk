import 'dart:convert';
import 'dart:io';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:payrdr_sdk/payrdr_sdk.dart';

/// The sample messages in sdk-bridge/contract, which the bridge's own tests check too: what this plugin sends is what
/// the bridge reads, and what the bridge sends back is read here. A case added there fails here until it is handled.
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const channel = MethodChannel('net.payrdr.sdk');
  final messenger = TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;

  Object? fixture(String name) => jsonDecode(File('../../sdk-bridge/contract/$name.json').readAsStringSync());
  Map<String, Object?> cases(String name) => fixture(name)! as Map<String, Object?>;

  /// What the plugin sends for [call] while the native side answers [reply].
  Future<Map<String, Object?>> sentBy(Future<Object?> Function() call, {Object reply = const {}}) async {
    String? sent;
    messenger.setMockMethodCallHandler(channel, (message) async {
      sent = message.arguments as String;
      return jsonEncode(reply);
    });
    await call();
    return jsonDecode(sent!) as Map<String, Object?>;
  }

  /// What the plugin makes of [reply] from the native side.
  Future<T> readFrom<T>(Object? reply, Future<T> Function() call) {
    messenger.setMockMethodCallHandler(channel, (_) async => jsonEncode(reply));
    return call();
  }

  const canceled = {'status': 'canceled', 'orderId': 'md-1'};

  tearDown(() => messenger.setMockMethodCallHandler(channel, null));

  test('every checkout request is sent as the bridge reads it', () async {
    const gateway = 'https://pay.example.com';
    final sends = <String, Future<Object?> Function()>{
      'minimal': () => const PayrdrCheckout(PayrdrConfig(baseUrl: gateway)).present('md-1'),
      'session': () => const PayrdrCheckout(PayrdrConfig(baseUrl: gateway)).presentSession('ps_1'),
      'allOptions': () => const PayrdrCheckout(
            PayrdrConfig(
              baseUrl: gateway,
              locale: 'de',
              theme: PayrdrTheme.dark(),
              cardScanner: true,
              showResultScreen: true,
              keyProviderUrl: 'https://keys.example.com',
              sslPins: [SslPin(host: 'pay.example.com', sha256: 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=')],
              debugLogging: true,
            ),
          ).present('md-1'),
      'customTheme': () => const PayrdrCheckout(
            PayrdrConfig(
              baseUrl: gateway,
              theme: PayrdrTheme.custom(
                base: Brightness.dark,
                colors: PayrdrColors(
                  primary: Color(0xFFF3D9B1),
                  onPrimary: Color(0xFFFFFFFF),
                  scrim: Color(0x80000000),
                ),
                cornerRadius: 18,
                buttonCornerRadius: 999,
              ),
            ),
          ).present('md-1'),
      'wallets': () => const PayrdrCheckout(
            PayrdrConfig(
              baseUrl: gateway,
              googlePay: GooglePayOptions(
                merchantId: 'BCR2',
                merchantName: 'Hearth & Co',
                gateway: 'bpcpay',
                gatewayMerchantId: 'shop',
                environment: GooglePayEnvironment.test,
                countryCode: 'DE',
                cardNetworks: {GooglePayCardNetwork.visa, GooglePayCardNetwork.mastercard},
                authMethods: {GooglePayAuthMethod.panOnly, GooglePayAuthMethod.cryptogram3ds},
                billingAddressRequired: true,
              ),
              applePay: ApplePayOptions(
                merchantId: 'merchant.shop',
                countryCode: 'DE',
                networks: {ApplePayNetwork.visa, ApplePayNetwork.amex},
                capabilities: {ApplePayCapability.threeDSecure, ApplePayCapability.debit},
              ),
            ),
          ).present('md-1'),
    };
    final expected = cases('checkout-requests');

    expect(sends.keys.toSet(), expected.keys.toSet());
    for (final MapEntry(key: name, value: send) in sends.entries) {
      expect(await sentBy(send, reply: canceled), expected[name], reason: name);
    }
  });

  test('every checkout result of the bridge is read as meant', () async {
    final results = <String, CheckoutResult>{
      'completed': const CheckoutCompleted('md-1'),
      'canceled': const CheckoutCanceled('md-1'),
      'unknown': const CheckoutUnknown('md-1'),
      'unknownAfterError': const CheckoutUnknown('md-1', CheckoutError(CheckoutErrorCode.network, message: 'offline')),
      'declined': const CheckoutFailed(
        'md-1',
        CheckoutError(CheckoutErrorCode.declined, message: 'Insufficient funds', declineCode: '116'),
      ),
      'gatewayError': const CheckoutFailed(
        'md-1',
        CheckoutError(CheckoutErrorCode.api, message: 'System error', httpStatus: 500, gatewayErrorCode: '5'),
      ),
      'sessionExpired': const CheckoutFailed(
        'md-1',
        CheckoutError(CheckoutErrorCode.sessionExpired, message: 'The session of order md-1 expired'),
      ),
      'invalidRequest': const CheckoutFailed(
        '',
        CheckoutError(
          CheckoutErrorCode.invalidRequest,
          message: 'theme.colors.onPrimary must be #RRGGBB or #AARRGGBB, not white',
        ),
      ),
      'busy': const CheckoutFailed('', CheckoutError(CheckoutErrorCode.busy, message: 'A checkout is already open')),
    };
    final sent = cases('checkout-results');

    expect(results.keys.toSet(), sent.keys.toSet());
    for (final MapEntry(key: name, value: reply) in sent.entries) {
      final result = await readFrom(reply, () => const PayrdrCheckout(PayrdrConfig(baseUrl: 'u')).present('md-1'));
      expect(result, results[name], reason: name);
    }
  });

  test('every error code of the bridge has its own value', () {
    final codes = (fixture('error-codes')! as List).cast<String>();

    expect(codes.map(CheckoutErrorCode.fromWire).map((code) => code.wire), codes);
  });

  test('every SDK Core request is sent as the bridge reads it', () async {
    final sends = <String, Future<Object?> Function()>{
      'card': () => PayrdrCore.generateToken(
            const CoreTokenRequest.card(
              mdOrder: 'md-1',
              pan: '4532896701439077',
              expiry: '12/28',
              cvc: '444',
              cardholder: 'JOHN DOE',
              pubKey: 'PEM',
            ),
          ),
      'cardWithoutOrder': () => PayrdrCore.generateToken(
            const CoreTokenRequest.cardWithoutOrder(pan: '4532896701439077', expiry: '12/28', cvc: '444', pubKey: 'PEM'),
          ),
      'savedCard': () => PayrdrCore.generateToken(
            const CoreTokenRequest.savedCard(mdOrder: 'md-1', bindingId: 'b-1', cvc: '444', pubKey: 'PEM'),
          ),
      'savedCardWithoutOrder': () =>
          PayrdrCore.generateToken(const CoreTokenRequest.savedCardWithoutOrder(bindingId: 'b-1', pubKey: 'PEM')),
    };
    final expected = cases('core-token-requests');

    expect(sends.keys.toSet(), expected.keys.toSet());
    for (final MapEntry(key: name, value: send) in sends.entries) {
      expect(await sentBy(send, reply: {'token': 'tok'}), expected[name], reason: name);
    }
  });

  test('every SDK Core answer of the bridge is read as meant', () async {
    final sent = cases('core-token-results');
    Future<CoreTokenResult> read(String name) => readFrom(
          sent[name],
          () => PayrdrCore.generateToken(const CoreTokenRequest.savedCardWithoutOrder(bindingId: 'b-1', pubKey: 'PEM')),
        );

    expect(sent.keys.toSet(), {'token', 'fieldErrors', 'invalidRequest'});
    expect((await read('token')).token, 'tok');
    expect((await read('fieldErrors')).errors, {CoreField.pan: 'required'});
    await expectLater(read('invalidRequest'), throwsArgumentError);
  });
}
