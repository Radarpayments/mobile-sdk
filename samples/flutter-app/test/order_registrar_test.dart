import 'dart:async';
import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:payrdr_flutter_sample/merchant/order_registrar.dart';
import 'package:payrdr_flutter_sample/store/invest/northwind_profile.dart';

void main() {
  GatewayOrderRegistrar registrar(
    MockClient client, {
    String login = 'merchant-api',
    String password = 'secret',
    OrderType orderType = OrderType.purchase,
  }) => GatewayOrderRegistrar(
    baseUrl: 'https://gw.example.com/payment/',
    login: login,
    password: password,
    http: client,
    timeout: const Duration(milliseconds: 50),
    orderType: orderType,
  );

  test('posts the order to register.do as a form and returns the orderId', () async {
    late http.Request sent;
    final client = MockClient((request) async {
      sent = request;
      return http.Response(
        '{"orderId":"70906e55-7114-41d6-8332-4609dc6590f4","formUrl":"https://gw.example.com/x"}',
        200,
      );
    });

    final result = await registrar(client).register(1578, 'demo-1234abcd-ef01');

    expect(result, const Registered('70906e55-7114-41d6-8332-4609dc6590f4'));
    expect(sent.method, 'POST');
    expect(sent.url.toString(), 'https://gw.example.com/payment/rest/register.do');
    expect(sent.headers['content-type'], startsWith('application/x-www-form-urlencoded'));
    final form = sent.bodyFields;
    expect(form, containsPair('userName', 'merchant-api'));
    expect(form, containsPair('password', 'secret'));
    expect(form, containsPair('amount', '1578'));
    expect(form, containsPair('currency', '978'));
    expect(form, containsPair('returnUrl', 'sdk://done'));
    expect(form, containsPair('clientId', 'demo-1234abcd-ef01'));
    // Gateways limit orderNumber to 32 characters.
    expect(form['orderNumber'], matches(RegExp(r'^demo-[0-9a-f]{20}$')));
  });

  test('a guest order carries no clientId and every order a new number', () async {
    final forms = <Map<String, String>>[];
    final client = MockClient((request) async {
      forms.add(request.bodyFields);
      return http.Response('{"orderId":"1"}', 200);
    });

    await registrar(client).register(100, null);
    await registrar(client).register(100, null);

    expect(forms.first.containsKey('clientId'), isFalse);
    expect(forms.first['orderNumber'], isNot(forms.last['orderNumber']));
  });

  test('sends nothing without login or password', () async {
    var calls = 0;
    final client = MockClient((_) async {
      calls++;
      return http.Response('{}', 200);
    });

    expect(await registrar(client, login: ' ').register(100, null), const MissingCredentials());
    expect(await registrar(client, password: '').register(100, null), const MissingCredentials());
    expect(calls, 0);
  });

  test('gives up when the gateway does not answer in time', () async {
    final client = MockClient((_) => Completer<http.Response>().future);

    expect(await registrar(client).register(100, null), const RegistrationFailed('The gateway did not answer in 0 s'));
  });

  test('reports a connection failure', () async {
    final client = MockClient((_) async => throw http.ClientException('Failed host lookup'));

    expect(await registrar(client).register(100, null), const RegistrationFailed('Failed host lookup'));
  });

  test('reads the answer as UTF-8 whatever the headers say', () async {
    final client = MockClient((_) async => http.Response.bytes(utf8.encode('{"errorMessage":"Неверный логин"}'), 200));

    expect(await registrar(client).register(100, null), const RegistrationFailed('Неверный логин'));
  });

  group('AFT transfers (registerP2P)', () {
    test('an AFT order is registered as a one-leg transfer that returns to the SDK, as JSON', () async {
      late http.Request sent;
      final client = MockClient((request) async {
        sent = request;
        return http.Response(
          '{"errorCode":0,"errorMessage":"Successful","orderId":"md-aft","formUrl":"https://gw.example.com/x"}',
          200,
        );
      });

      final result = await registrar(client, orderType: OrderType.aft).register(1578, 'demo-1234abcd-ef01');

      expect(result, const Registered('md-aft'));
      expect(sent.method, 'POST');
      expect(sent.url.toString(), 'https://gw.example.com/payment/rest/api/p2p/registerP2P.do');
      expect(sent.headers['content-type'], startsWith('application/json'));
      final body = jsonDecode(sent.body) as Map<String, Object?>;
      expect(body, containsPair('username', 'merchant-api'));
      expect(body, containsPair('password', 'secret'));
      expect(body, containsPair('amount', 1578));
      expect(body, containsPair('currency', '978'));
      expect(body, containsPair('returnUrl', 'sdk://done'));
      expect(body, containsPair('transactionTypeIndicator', 'A'));
      // An object with a list, not a flat list: the gateway rejects that as "Features are invalid".
      expect(body['features'], {
        'feature': ['WITHOUT_TO_CARD'],
      });
      expect(body, containsPair('clientId', 'demo-1234abcd-ef01'));
      expect(body['orderNumber'], matches(RegExp(r'^demo-[0-9a-f]{20}$')));
    });

    test('a guest transfer carries no clientId', () async {
      late http.Request sent;
      final client = MockClient((request) async {
        sent = request;
        return http.Response('{"errorCode":0,"orderId":"md-aft"}', 200);
      });

      await registrar(client, orderType: OrderType.aft).register(100, null);

      expect((jsonDecode(sent.body) as Map<String, Object?>).containsKey('clientId'), isFalse);
    });

    test('a refused transfer gives the gateway message', () async {
      final client = MockClient(
        (_) async => http.Response('{"errorCode":14,"errorMessage":"Features are invalid"}', 200),
      );

      expect(
        await registrar(client, orderType: OrderType.aft).register(100, null),
        const RegistrationFailed('Features are invalid'),
      );
    });

    test('sends nothing without login or password', () async {
      var calls = 0;
      final client = MockClient((_) async {
        calls++;
        return http.Response('{}', 200);
      });

      expect(
        await registrar(client, login: '', orderType: OrderType.aft).register(100, null),
        const MissingCredentials(),
      );
      expect(calls, 0);
    });

    test('gives up when the gateway does not answer in time', () async {
      final client = MockClient((_) => Completer<http.Response>().future);

      expect(
        await registrar(client, orderType: OrderType.aft).register(100, null),
        const RegistrationFailed('The gateway did not answer in 0 s'),
      );
    });

    test('a payment order stays on register.do', () async {
      late http.Request sent;
      final client = MockClient((request) async {
        sent = request;
        return http.Response('{"orderId":"1"}', 200);
      });

      await registrar(client).register(100, null);

      expect(sent.url.toString(), 'https://gw.example.com/payment/rest/register.do');
    });

    test('an AFT order carries the prefill the app passes', () async {
      late http.Request sent;
      final client = MockClient((request) async {
        sent = request;
        return http.Response('{"orderId":"md-aft"}', 200);
      });

      await GatewayOrderRegistrar(
        baseUrl: 'https://gw.example.com/payment/',
        login: 'merchant-api',
        password: 'secret',
        http: client,
        orderType: OrderType.aft,
        prefill: NorthwindProfile.prefill,
      ).register(10000, null);

      final body = jsonDecode(sent.body) as Map<String, dynamic>;
      expect(body['email'], 'alex.kim@example.com');
      expect(body['billingPayerData'], containsPair('payerFirstName', 'Alex'));
      expect(body['billingPayerData'], containsPair('payerBirthday', '19900115'));
      expect(body['billingPayerData'], containsPair('billingCountry', 'CY'));
      expect(body['billingRecipientData'], containsPair('recipientAccount', 'NW00007781'));
      expect(body['billingRecipientData'], containsPair('recipientAccountNumberType', '100'));
      expect(body['transactionTypeIndicator'], 'A');
    });

    test('an AFT order without prefill sends no payer data', () async {
      late http.Request sent;
      final client = MockClient((request) async {
        sent = request;
        return http.Response('{"orderId":"md-aft"}', 200);
      });

      await registrar(client, orderType: OrderType.aft).register(100, null);

      final body = jsonDecode(sent.body) as Map<String, dynamic>;
      expect(body.containsKey('email'), isFalse);
      expect(body.containsKey('billingPayerData'), isFalse);
      expect(body.containsKey('billingRecipientData'), isFalse);
    });

    test('a payment order ignores the prefill', () async {
      late http.Request sent;
      final client = MockClient((request) async {
        sent = request;
        return http.Response('{"orderId":"1"}', 200);
      });

      await GatewayOrderRegistrar(
        baseUrl: 'https://gw.example.com/payment/',
        login: 'merchant-api',
        password: 'secret',
        http: client,
        prefill: NorthwindProfile.prefill,
      ).register(100, null);

      expect(sent.bodyFields.containsKey('email'), isFalse);
    });

    test('the factory passes the prefill to API V1', () {
      final v1 = merchantRegistrar(
        apiVersion: ApiVersion.v1,
        baseUrl: 'https://gw.example.com/payment/',
        login: 'l',
        password: 'p',
        apiKey: '',
        http: MockClient((_) async => http.Response('{}', 200)),
        orderType: OrderType.aft,
        prefill: NorthwindProfile.prefill,
      );
      expect((v1 as GatewayOrderRegistrar).prefill, NorthwindProfile.prefill);
    });
  });

  group('API V2', () {
    // A session the dev gateway created, and the mdOrder of its paymentUrl.
    const sessionId = 'ps_2GRSigK2sUQKFSAZWbg2z7UuQxxjz7wqCjZqYyWRiq22aF8xxCDPua';
    const sessionMdOrder = '0180a01a-a8cb-75b8-8f9d-1a5f00ab8a11';
    const sessionBody =
        '{"id":"$sessionId","amount":1000,"status":"open","paymentUrl":"https://x?mdOrder=$sessionMdOrder"}';

    ApiV2SessionRegistrar sessions(MockClient client, {String apiKey = 'key-1'}) => ApiV2SessionRegistrar(
      baseUrl: 'https://gw.example.com/payment/',
      apiKey: apiKey,
      http: client,
      timeout: const Duration(milliseconds: 50),
    );

    test('api2 lives next to the payment page of the base URL', () {
      expect(api2Url('https://gw.example.com/payment'), 'https://gw.example.com/api2');
      expect(api2Url(' https://gw.example.com/payment/ '), 'https://gw.example.com/api2');
      expect(api2Url('https://gw.example.com:443/payment'), 'https://gw.example.com/api2');
      expect(api2Url('https://gw.example.com:8443/payment'), 'https://gw.example.com:8443/api2');
    });

    test('a session id is the Base58 of "ps:" and the mdOrder', () {
      expect(sessionIdToMdOrder(sessionId), sessionMdOrder);
      expect(sessionIdToMdOrder('not-a-session'), isNull);
      expect(sessionIdToMdOrder('ps_'), isNull);
      expect(sessionIdToMdOrder('ps_0OIl'), isNull, reason: 'not Base58');
      // Base58 of "md:1": decodes, but not to a session.
      expect(sessionIdToMdOrder('ps_3oBKJY'), isNull);
    });

    test('creates a guest session that returns to the SDK, as JSON with the API key', () async {
      late http.Request sent;
      final client = MockClient((request) async {
        sent = request;
        return http.Response(sessionBody, 200);
      });

      final result = await sessions(client).register(157800, null);

      expect(result, const Registered(sessionMdOrder, sessionId: sessionId));
      expect(sent.method, 'POST');
      expect(sent.url.toString(), 'https://gw.example.com/api2/sessions');
      expect(sent.headers['X-API-KEY'], 'key-1');
      expect(sent.headers['content-type'], startsWith('application/json'));
      expect(jsonDecode(sent.body), {
        'amount': 157800,
        'currency': 'EUR',
        'successUrl': 'sdk://done',
        'failureUrl': 'sdk://done',
      });
    });

    test('the buyer with cards goes as merchantCustomerId', () async {
      late http.Request sent;
      final client = MockClient((request) async {
        sent = request;
        return http.Response(sessionBody, 200);
      });

      await sessions(client).register(100, 'demo-1234abcd-ef01');

      expect(jsonDecode(sent.body), containsPair('merchantCustomerId', 'demo-1234abcd-ef01'));
    });

    test('sends nothing without an API key', () async {
      var calls = 0;
      final client = MockClient((_) async {
        calls++;
        return http.Response(sessionBody, 200);
      });

      expect(await sessions(client, apiKey: ' ').register(100, null), const MissingApiKey());
      expect(calls, 0);
    });

    test('gives up when the gateway does not answer in time', () async {
      final client = MockClient((_) => Completer<http.Response>().future);

      expect(await sessions(client).register(100, null), const RegistrationFailed('The gateway did not answer in 0 s'));
    });

    test('reports a connection failure', () async {
      final client = MockClient((_) async => throw http.ClientException('Failed host lookup'));

      expect(await sessions(client).register(100, null), const RegistrationFailed('Failed host lookup'));
    });

    test('the API key is not in toString', () {
      expect(sessions(MockClient((_) async => http.Response('', 200))).toString(), isNot(contains('key-1')));
    });

    group('parseSessionResponse', () {
      test('takes the session and the mdOrder its id encodes', () {
        expect(parseSessionResponse(sessionBody), const Registered(sessionMdOrder, sessionId: sessionId));
      });

      test('takes the gateway error message', () {
        expect(
          parseSessionResponse('{"error":{"errorCode":"unauthorized","errorMessage":"Unknown user"}}'),
          const RegistrationFailed('Unknown user'),
        );
      });

      test('names the error code when there is no message', () {
        expect(
          parseSessionResponse('{"error":{"errorCode":"unauthorized"}}'),
          const RegistrationFailed('Gateway error unauthorized'),
        );
      });

      test('rejects what is not a JSON object or not a session', () {
        expect(parseSessionResponse('<html>502</html>'), const RegistrationFailed('Unexpected gateway response'));
        expect(parseSessionResponse('[]'), const RegistrationFailed('Unexpected gateway response'));
        expect(
          parseSessionResponse('{"id":"not-a-session"}'),
          const RegistrationFailed('Unexpected session id not-a-session'),
        );
      });
    });
  });

  test('the developer settings choose the API and, for API V1, the order type', () {
    final client = MockClient((_) async => http.Response('', 200));
    OrderRegistrar forVersion(ApiVersion apiVersion, {OrderType orderType = OrderType.purchase}) => merchantRegistrar(
      apiVersion: apiVersion,
      orderType: orderType,
      baseUrl: 'https://gw.example.com/payment',
      login: 'merchant-api',
      password: 'secret',
      apiKey: 'key-1',
      http: client,
    );

    expect(
      forVersion(ApiVersion.v1),
      isA<GatewayOrderRegistrar>().having((r) => r.orderType, 'orderType', OrderType.purchase),
    );
    expect(
      forVersion(ApiVersion.v1, orderType: OrderType.aft),
      isA<GatewayOrderRegistrar>().having((r) => r.orderType, 'orderType', OrderType.aft),
    );
    expect(forVersion(ApiVersion.v2), isA<ApiV2SessionRegistrar>());
    expect(forVersion(ApiVersion.v2, orderType: OrderType.aft), isA<ApiV2SessionRegistrar>());
  });

  group('parseRegisterResponse', () {
    test('takes the orderId', () {
      expect(parseRegisterResponse('{"orderId":"abc"}'), const Registered('abc'));
    });

    test('takes the gateway error message', () {
      expect(
        parseRegisterResponse('{"errorCode":"1","errorMessage":"Order number is duplicated"}'),
        const RegistrationFailed('Order number is duplicated'),
      );
    });

    test('names the error code when there is no message', () {
      expect(parseRegisterResponse('{"errorCode":"5"}'), const RegistrationFailed('Gateway error 5'));
    });

    test('rejects what is not a JSON object', () {
      expect(parseRegisterResponse('<html>502</html>'), const RegistrationFailed('Unexpected gateway response'));
      expect(parseRegisterResponse('[]'), const RegistrationFailed('Unexpected gateway response'));
    });
  });
}
