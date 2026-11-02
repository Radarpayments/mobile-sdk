import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:payrdr_sdk/payrdr_sdk.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const channel = MethodChannel('net.payrdr.sdk');
  final messenger = TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;

  late List<MethodCall> calls;

  /// The native side answers every call with [reply].
  void nativeReplies(Object reply) {
    calls = [];
    messenger.setMockMethodCallHandler(channel, (call) async {
      calls.add(call);
      return jsonEncode(reply);
    });
  }

  Map<String, Object?> sentJson() => jsonDecode(calls.single.arguments as String) as Map<String, Object?>;

  tearDown(() => messenger.setMockMethodCallHandler(channel, null));

  group('checkout request', () {
    test('a minimal config sends the gateway, the order and the defaults', () async {
      nativeReplies({'status': 'canceled', 'orderId': 'md-1'});

      await const PayrdrCheckout(PayrdrConfig(baseUrl: 'https://pay.example.com')).present('md-1');

      expect(calls.single.method, 'present');
      expect(sentJson(), {
        'config': {
          'baseUrl': 'https://pay.example.com',
          'theme': {'mode': 'system'},
          'cardScanner': false,
          'showResultScreen': false,
          'debugLogging': false,
        },
        'mdOrder': 'md-1',
      });
    });

    test('a session is sent as sessionId', () async {
      nativeReplies({'status': 'canceled', 'orderId': ''});

      await const PayrdrCheckout(PayrdrConfig(baseUrl: 'u')).presentSession('ps_1');

      expect(sentJson()['sessionId'], 'ps_1');
      expect(sentJson().containsKey('mdOrder'), isFalse);
    });

    test('a custom theme sends ARGB hex colours and only what is set', () async {
      nativeReplies({'status': 'canceled', 'orderId': 'md-1'});

      await const PayrdrCheckout(
        PayrdrConfig(
          baseUrl: 'u',
          theme: PayrdrTheme.custom(
            base: Brightness.dark,
            colors: PayrdrColors(primary: Color(0xFFF3D9B1), scrim: Color(0x80000000)),
            cornerRadius: 18,
            buttonCornerRadius: 999,
          ),
        ),
      ).present('md-1');

      expect((sentJson()['config']! as Map)['theme'], {
        'mode': 'custom',
        'base': 'dark',
        'colors': {'primary': '#FFF3D9B1', 'scrim': '#80000000'},
        'cornerRadius': 18.0,
        'buttonCornerRadius': 999.0,
      });
    });

    test('wallet options use the native names', () async {
      nativeReplies({'status': 'canceled', 'orderId': 'md-1'});

      await const PayrdrCheckout(
        PayrdrConfig(
          baseUrl: 'u',
          googlePay: GooglePayOptions(
            merchantName: 'Hearth & Co',
            environment: GooglePayEnvironment.test,
            cardNetworks: {GooglePayCardNetwork.visa},
            authMethods: {GooglePayAuthMethod.panOnly},
          ),
          applePay: ApplePayOptions(
            merchantId: 'merchant.shop',
            capabilities: {ApplePayCapability.threeDSecure},
          ),
        ),
      ).present('md-1');

      final config = sentJson()['config']! as Map;
      expect(config['googlePay'], {
        'merchantName': 'Hearth & Co',
        'environment': 'TEST',
        'cardNetworks': ['VISA'],
        'authMethods': ['PAN_ONLY'],
        'billingAddressRequired': false,
      });
      expect(config['applePay'], {
        'merchantId': 'merchant.shop',
        'capabilities': ['THREE_D_SECURE'],
      });
    });
  });

  group('checkout result', () {
    Future<CheckoutResult> resultOf(Map<String, Object?> reply) {
      nativeReplies(reply);
      return const PayrdrCheckout(PayrdrConfig(baseUrl: 'u')).present('md-1');
    }

    test('each status becomes its result', () async {
      expect(await resultOf({'status': 'completed', 'orderId': 'md-1'}), const CheckoutCompleted('md-1'));
      expect(await resultOf({'status': 'canceled', 'orderId': 'md-1'}), const CheckoutCanceled('md-1'));
      expect(await resultOf({'status': 'unknown', 'orderId': 'md-1'}), const CheckoutUnknown('md-1'));
    });

    test('a decline carries the code, the text and the bank code', () async {
      final result = await resultOf({
        'status': 'failed',
        'orderId': 'md-1',
        'error': {'code': 'declined', 'message': 'Insufficient funds', 'declineCode': '116'},
      });

      expect(
        result,
        const CheckoutFailed(
          'md-1',
          CheckoutError(CheckoutErrorCode.declined, message: 'Insufficient funds', declineCode: '116'),
        ),
      );
    });

    test('a gateway error keeps its http status', () async {
      final result = await resultOf({
        'status': 'failed',
        'orderId': 'md-1',
        'error': {'code': 'api', 'httpStatus': 500, 'gatewayErrorCode': '5'},
      });

      expect((result as CheckoutFailed).error.httpStatus, 500);
      expect(result.error.gatewayErrorCode, '5');
    });

    test('an expired session has its own code', () async {
      final result = await resultOf({
        'status': 'failed',
        'orderId': 'md-1',
        'error': {'code': 'session_expired', 'message': 'The session of order md-1 expired'},
      });

      expect((result as CheckoutFailed).error.code, CheckoutErrorCode.sessionExpired);
    });

    test('a code this version does not know is unknown', () async {
      final result = await resultOf({
        'status': 'failed',
        'orderId': 'md-1',
        'error': {'code': 'something_new'},
      });

      expect((result as CheckoutFailed).error.code, CheckoutErrorCode.unknown);
    });
  });

  group('SDK Core', () {
    test('a new card for an order sends its fields and the type', () async {
      nativeReplies({'token': 'tok'});

      final result = await PayrdrCore.generateToken(
        const CoreTokenRequest.card(mdOrder: 'md-1', pan: '4111', expiry: '12/28', cvc: '123', pubKey: 'PEM'),
      );

      expect(calls.single.method, 'coreToken');
      expect(sentJson(), {
        'type': 'card',
        'pubKey': 'PEM',
        'mdOrder': 'md-1',
        'pan': '4111',
        'expiry': '12/28',
        'cvc': '123',
      });
      expect(result.token, 'tok');
      expect(result.isSuccessful, isTrue);
    });

    test('the other kinds of token send their types', () async {
      nativeReplies({'token': 'tok'});
      await PayrdrCore.generateToken(
        const CoreTokenRequest.cardWithoutOrder(pan: '4111', expiry: '12/28', cvc: '123', pubKey: 'PEM'),
      );
      expect(sentJson()['type'], 'cardInstant');

      nativeReplies({'token': 'tok'});
      await PayrdrCore.generateToken(const CoreTokenRequest.savedCard(mdOrder: 'md-1', bindingId: 'b-1', pubKey: 'PEM'));
      expect(sentJson(), {'type': 'binding', 'pubKey': 'PEM', 'mdOrder': 'md-1', 'bindingId': 'b-1'});

      nativeReplies({'token': 'tok'});
      await PayrdrCore.generateToken(const CoreTokenRequest.savedCardWithoutOrder(bindingId: 'b-1', pubKey: 'PEM'));
      expect(sentJson()['type'], 'bindingInstant');
    });

    test('field errors come back by field', () async {
      nativeReplies({
        'errors': {'pan': 'required', 'mdOrder': 'invalid'},
      });

      final result = await PayrdrCore.generateToken(
        const CoreTokenRequest.card(mdOrder: '', pan: '', expiry: '12/28', cvc: '123', pubKey: 'PEM'),
      );

      expect(result.isSuccessful, isFalse);
      expect(result.errors, {CoreField.pan: 'required', CoreField.mdOrder: 'invalid'});
    });

    test('a request the native side cannot read throws', () async {
      nativeReplies({
        'error': {'code': 'invalid_request', 'message': 'bad'},
      });

      expect(
        PayrdrCore.generateToken(const CoreTokenRequest.savedCardWithoutOrder(bindingId: 'b', pubKey: 'PEM')),
        throwsArgumentError,
      );
    });
  });
}
