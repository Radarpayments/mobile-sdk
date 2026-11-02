import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/testing.dart';
import 'package:payrdr_flutter_sample/app.dart';
import 'package:payrdr_flutter_sample/app_scope.dart';
import 'package:payrdr_flutter_sample/checkout/checkout.dart';
import 'package:payrdr_flutter_sample/merchant/order_registrar.dart';
import 'package:payrdr_flutter_sample/prefs/app_prefs.dart';
import 'package:payrdr_flutter_sample/store/model/scenario.dart';
import 'package:payrdr_flutter_sample/store/store_model.dart';
import 'package:payrdr_sdk/payrdr_sdk.dart';
import 'package:shared_preferences/shared_preferences.dart';

class _Registrar implements OrderRegistrar {
  final amounts = <int>[];

  @override
  Future<OrderRegistration> register(int amountMinor, String? clientId) async {
    amounts.add(amountMinor);
    return const Registered('md-paid');
  }
}

Future<AppPrefs> _pumpApp(WidgetTester tester, {OrderRegistrar? registrar, PresentSheet? present}) async {
  SharedPreferences.setMockInitialValues({});
  final prefs = await AppPrefs.load();
  tester.view.physicalSize = const Size(1170, 2532);
  tester.view.devicePixelRatio = 3;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(
    AppScope(
      prefs: prefs,
      store: StoreModel(),
      registrarFactory: (_) => registrar ?? _Registrar(),
      presentSheet: present,
      child: const SampleApp(),
    ),
  );
  await tester.pumpAndSettle();
  return prefs;
}

void main() {
  testWidgets('the start screen shows the three stores and SDK Core', (tester) async {
    await _pumpApp(tester);

    expect(find.text('Payment demo'), findsOneWidget);
    expect(find.text('SDK Payment'), findsOneWidget);
    expect(find.text('Hearth & Co'), findsOneWidget);
    expect(find.text('Roast Lane'), findsOneWidget);
    expect(find.text('Stage Pass'), findsOneWidget);
    expect(find.text('SDK Core'), findsOneWidget);
    expect(find.text('Your own card form'), findsOneWidget);
    expect(find.byTooltip('Developer settings'), findsOneWidget);
  });

  testWidgets('the scenario panel changes the next payment', (tester) async {
    final prefs = await _pumpApp(tester);
    await tester.tap(find.byKey(const Key('cover_tickets')));
    await tester.pumpAndSettle();
    await tester.tap(find.textContaining('Checkout ·'));
    await tester.pumpAndSettle();

    expect(find.text('Scenario: Guest'), findsOneWidget);
    await tester.tap(find.byKey(const Key('store_scenario')));
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('scenario_with_cards')));
    await tester.tap(find.byKey(const Key('scenario_scanner')));
    await tester.tap(find.byKey(const Key('scenario_sdk_result')));
    await tester.pumpAndSettle();

    expect(prefs.scenario, const Scenario(buyerWithCards: true, wallet: true, scanner: false, sdkResultScreen: true));
    final clientId = prefs.clientId;
    await tester.tap(find.text('Reset'));
    await tester.pumpAndSettle();
    expect(prefs.clientId, isNot(clientId));

    await tester.tapAt(const Offset(20, 20));
    await tester.pumpAndSettle();
    expect(find.text('Scenario: With cards'), findsOneWidget);
  });

  testWidgets('a paid furniture order ends on the store result screen with an empty cart', (tester) async {
    final registrar = _Registrar();
    PayrdrConfig? opened;
    final prefs = await _pumpApp(
      tester,
      registrar: registrar,
      present: (config, order) async {
        opened = config;
        return CheckoutCompleted(order.mdOrder);
      },
    );
    await tester.tap(find.byKey(const Key('cover_furniture')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Birch stool'));
    await tester.pumpAndSettle();
    await tester.tap(find.textContaining('Add to cart'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Cart · 1').last);
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('store_pay')));
    await tester.pumpAndSettle();

    expect(registrar.amounts, [9900]);
    expect(prefs.lastMdOrder, 'md-paid');
    expect(opened?.theme.mode, 'custom');
    expect(find.text('Thank you for your order'), findsOneWidget);
    expect(find.text('md-paid'), findsOneWidget);

    await tester.tap(find.text('Done'));
    await tester.pumpAndSettle();
    expect(find.text('Autumn\ncollection'), findsOneWidget, reason: 'an empty cart goes back to the catalogue');
  });

  testWidgets('without credentials the store says so and offers the settings', (tester) async {
    final nothingSent = MockClient((_) async => fail('nothing is sent without credentials'));
    await _pumpApp(
      tester,
      registrar: GatewayOrderRegistrar(baseUrl: 'https://x', login: '', password: '', http: nothingSent),
    );
    await tester.tap(find.byKey(const Key('cover_tickets')));
    await tester.pumpAndSettle();
    await tester.tap(find.textContaining('Checkout ·'));
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('store_pay')));
    await tester.pumpAndSettle();

    expect(find.text('Merchant login and password are not set.'), findsOneWidget);
    await tester.tap(find.text('Open settings'));
    await tester.pumpAndSettle();
    expect(find.text('Developer settings'), findsOneWidget);
  });

  testWidgets('without an API key the store says so', (tester) async {
    final nothingSent = MockClient((_) async => fail('nothing is sent without an API key'));
    await _pumpApp(
      tester,
      registrar: ApiV2SessionRegistrar(baseUrl: 'https://x', apiKey: '', http: nothingSent),
    );
    await tester.tap(find.byKey(const Key('cover_tickets')));
    await tester.pumpAndSettle();
    await tester.tap(find.textContaining('Checkout ·'));
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('store_pay')));
    await tester.pumpAndSettle();

    expect(find.text('The API key is not set.'), findsOneWidget);
    expect(find.text('Merchant login and password are not set.'), findsNothing);
  });

  testWidgets('the developer settings switch between the login and password of API V1 and the key of API V2', (
    tester,
  ) async {
    final prefs = await _pumpApp(tester);
    await tester.tap(find.byTooltip('Developer settings'));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('dev_login')), findsOneWidget);
    expect(find.byKey(const Key('dev_api_key')), findsNothing);

    await tester.tap(find.text('API V2'));
    await tester.pumpAndSettle();
    expect(prefs.apiVersion, ApiVersion.v2);
    expect(find.text('Sessions are created with POST /api2/sessions using the API key.'), findsOneWidget);
    expect(find.byKey(const Key('dev_login')), findsNothing);
    expect(find.byKey(const Key('dev_password')), findsNothing);
    await tester.enterText(find.byKey(const Key('dev_api_key')), 'key-1');
    await tester.pumpAndSettle();
    expect(prefs.apiKey, 'key-1');
    expect(
      find.text('Stored only on this device. In your app the order is registered by your backend.'),
      findsOneWidget,
    );

    await tester.tap(find.text('API V1'));
    await tester.pumpAndSettle();
    expect(prefs.apiVersion, ApiVersion.v1);
    expect(find.byKey(const Key('dev_login')), findsOneWidget);
    expect(
      find.text('Stored only on this device. In your app the order is registered by your backend.'),
      findsOneWidget,
    );
  });

  testWidgets('the API V1 settings choose between a payment and an AFT transfer', (tester) async {
    const aftHint =
        'Transfers are registered with registerP2P.do (one-leg AFT): the sheet asks for the sender and recipient details.';
    final prefs = await _pumpApp(tester);
    await tester.tap(find.byTooltip('Developer settings'));
    await tester.pumpAndSettle();
    expect(find.text('Order'), findsOneWidget);
    expect(find.text(aftHint), findsNothing);

    await tester.tap(find.text('AFT transfer'));
    await tester.pumpAndSettle();
    expect(prefs.orderType, OrderType.aft);
    expect(find.text(aftHint), findsOneWidget);
    expect(find.byKey(const Key('dev_login')), findsOneWidget);

    await tester.tap(find.text('API V2'));
    await tester.pumpAndSettle();
    expect(find.text('Order'), findsNothing);
    expect(find.text('AFT transfer'), findsNothing);
    expect(find.text(aftHint), findsNothing);

    await tester.tap(find.text('API V1'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Payment'));
    await tester.pumpAndSettle();
    expect(prefs.orderType, OrderType.purchase);
    expect(find.text(aftHint), findsNothing);
  });

  testWidgets('the language setting switches the demo to Arabic, right to left', (tester) async {
    final prefs = await _pumpApp(tester);

    await prefs.setLocaleOverride('ar');
    await tester.pumpAndSettle();

    expect(find.text('عرض توضيحي للدفع'), findsOneWidget);
    expect(Directionality.of(tester.element(find.text('Hearth & Co'))), TextDirection.rtl);
  });

  testWidgets('"Order again" pays the last coffee order straight away', (tester) async {
    final registrar = _Registrar();
    await _pumpApp(tester, registrar: registrar, present: (_, order) async => CheckoutCompleted(order.mdOrder));
    await tester.tap(find.byKey(const Key('cover_coffee')));
    await tester.pumpAndSettle();
    await tester.tap(find.byTooltip('Add Flat white'));
    await tester.tap(find.byTooltip('Add Flat white'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Cart · 2'));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('store_pay')));
    await tester.pumpAndSettle();
    expect(find.text('42'), findsOneWidget, reason: 'the order number for the barista');
    await tester.tap(find.text('Done'));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Order again'));
    await tester.pumpAndSettle();

    expect(registrar.amounts, [780, 780]);
    expect(find.text('43'), findsOneWidget);
  });

  testWidgets('SDK Core and the developer settings open from the start screen', (tester) async {
    await _pumpApp(tester);

    await tester.tap(find.byKey(const Key('core_tile')));
    await tester.pumpAndSettle();
    expect(find.text('Card information'), findsOneWidget);
    await tester.tap(find.text('Saved card'));
    await tester.pumpAndSettle();
    expect(find.text('bindingId'), findsOneWidget);
    await tester.pageBack();
    await tester.pumpAndSettle();

    await tester.tap(find.byTooltip('Developer settings'));
    await tester.pumpAndSettle();
    expect(find.text('Merchant login'), findsOneWidget);
    await tester.scrollUntilVisible(find.text('Last order'), 200, scrollable: find.byType(Scrollable).first);
    expect(find.text('—'), findsOneWidget, reason: 'no order registered yet');
  });
}
