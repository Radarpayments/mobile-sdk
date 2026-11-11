import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/painting.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:payrdr_flutter_sample/checkout/checkout.dart';
import 'package:payrdr_flutter_sample/merchant/order_registrar.dart';
import 'package:payrdr_flutter_sample/store/model/catalog.dart';
import 'package:payrdr_flutter_sample/store/model/scenario.dart';
import 'package:payrdr_flutter_sample/store/theme/cover_style.dart';
import 'package:payrdr_sdk/payrdr_sdk.dart';

class _Registrar implements OrderRegistrar {
  _Registrar(this.answer);

  final Future<OrderRegistration> Function() answer;
  final calls = <(int, String?)>[];

  @override
  Future<OrderRegistration> register(int amountMinor, String? clientId) {
    calls.add((amountMinor, clientId));
    return answer();
  }
}

PayrdrConfig _config({
  Scenario scenario = const Scenario(),
  SheetThemeOverride theme = SheetThemeOverride.cover,
  CoverId cover = CoverId.coffee,
  String applePayMerchantId = 'merchant.test',
}) => buildSdkConfig(
  baseUrl: 'https://gw.example.com/payment',
  scenario: scenario,
  cover: coverStyle(cover),
  themeOverride: theme,
  locale: 'de',
  storeName: 'Roast Lane',
  googlePayMerchantId: ' ',
  googlePayGateway: 'example',
  applePayMerchantId: applePayMerchantId,
);

void main() {
  group('toOutcome', () {
    test('maps every SDK result to what the store shows', () {
      expect(toOutcome(const CheckoutCompleted('md-1')), const Success('md-1'));
      expect(toOutcome(const CheckoutCanceled('md-1')), const Cancelled());
      expect(
        toOutcome(
          const CheckoutFailed('md-1', CheckoutError(CheckoutErrorCode.declined, message: 'Insufficient funds')),
        ),
        const Declined('Insufficient funds'),
      );
      expect(toOutcome(const CheckoutFailed('md-1', CheckoutError(CheckoutErrorCode.busy))), const Declined(null));
      expect(toOutcome(const CheckoutUnknown('md-1')), const Unconfirmed('md-1'));
    });
  });

  group('buildSdkConfig', () {
    test('passes the demo panel, the language and the wallets', () {
      final json = _config(scenario: const Scenario(scanner: false, sdkResultScreen: true)).toJson();

      expect(json['baseUrl'], 'https://gw.example.com/payment');
      expect(json['locale'], 'de');
      expect(json['cardScanner'], isFalse);
      expect(json['showResultScreen'], isTrue);
      // A blank setting is left to the gateway; the Google Pay sheet names the store.
      expect(json['googlePay'], {'merchantName': 'Roast Lane', 'gateway': 'example', 'billingAddressRequired': false});
      expect(json['applePay'], {'merchantId': 'merchant.test'});
    });

    test('hides the wallets when the panel turns them off or Apple Pay has no merchant ID', () {
      final off = _config(scenario: const Scenario(wallet: false)).toJson();
      expect(off.containsKey('googlePay'), isFalse);
      expect(off.containsKey('applePay'), isFalse);

      expect(_config(applePayMerchantId: '').toJson().containsKey('applePay'), isFalse);
    });

    test('the developer settings override the store theme', () {
      expect(_config(theme: SheetThemeOverride.light).theme.mode, 'light');
      expect(_config(theme: SheetThemeOverride.dark).theme.mode, 'dark');
      expect(_config(theme: SheetThemeOverride.system).theme.mode, 'system');
      expect(_config(theme: SheetThemeOverride.cover).theme.mode, 'custom');
    });
  });

  test('the sheet takes the brand colours and corners; the coffee shop has pill buttons', () {
    final coffee = paymentTheme(coverStyle(CoverId.coffee));
    expect(coffee.base, Brightness.dark);
    expect(coffee.colors.primary, const Color(0xFFF3D9B1));
    expect(coffee.colors.surface, const Color(0xFF2E231E));
    expect(coffee.colors.onBackground, const Color(0xFFFEF3C7));
    expect(coffee.colors.outlineStrong, const Color(0xFF5B4A40), reason: 'the grabber takes the cover border');
    expect(coffee.colors.placeholder, const Color(0xFFFEF3C7).withValues(alpha: 0.5));
    expect(coffee.colors.error, isNull, reason: 'error colours come from the base palette');
    expect(coffee.cornerRadius, 18);
    expect(coffee.buttonCornerRadius, 999);

    final furniture = paymentTheme(coverStyle(CoverId.furniture));
    expect(furniture.base, Brightness.light);
    expect(furniture.buttonCornerRadius, 12);
  });

  group('StoreCheckout', () {
    test('registers the order, opens the sheet and gives the outcome', () async {
      final registrar = _Registrar(() async => const Registered('md-42'));
      final mdOrders = <String>[];
      final opened = <Registered>[];
      final checkout = StoreCheckout(
        onMdOrder: mdOrders.add,
        present: (config, order) async {
          opened.add(order);
          return CheckoutCompleted(order.mdOrder);
        },
      );

      final outcome = await checkout.pay(registrar: registrar, config: _config(), amountMinor: 780, clientId: 'demo-1');

      expect(outcome, const Success('md-42'));
      expect(registrar.calls, [(780, 'demo-1')]);
      expect(mdOrders, ['md-42']);
      expect(opened, [const Registered('md-42')]);
      expect(checkout.state, isA<CheckoutIdle>());
    });

    test('an API V2 session opens as a session and is reported by its mdOrder', () async {
      const session = Registered('md-1', sessionId: 'ps_1');
      final mdOrders = <String>[];
      final opened = <Registered>[];
      final checkout = StoreCheckout(
        onMdOrder: mdOrders.add,
        present: (config, order) async {
          opened.add(order);
          return CheckoutCompleted(order.mdOrder);
        },
      );

      await checkout.pay(
        registrar: _Registrar(() async => session),
        config: _config(),
        amountMinor: 100,
        clientId: null,
      );

      expect(opened, [session]);
      expect(mdOrders, ['md-1']);
    });

    test('does not open the sheet when the order was not registered', () async {
      var opened = false;
      final checkout = StoreCheckout(
        onMdOrder: (_) {},
        present: (_, order) async {
          opened = true;
          return CheckoutCompleted(order.mdOrder);
        },
      );

      final outcome = await checkout.pay(
        registrar: _Registrar(() async => const MissingCredentials()),
        config: _config(),
        amountMinor: 780,
        clientId: null,
      );

      expect(outcome, isNull);
      expect(opened, isFalse);
      expect(checkout.state, isA<CheckoutRegistrationFailed>());
      expect((checkout.state as CheckoutRegistrationFailed).reason, const MissingCredentials());
    });

    test('a second tap while busy does nothing', () async {
      final registration = Completer<OrderRegistration>();
      final registrar = _Registrar(() => registration.future);
      final checkout = StoreCheckout(onMdOrder: (_) {}, present: (_, order) async => CheckoutCanceled(order.mdOrder));

      final first = checkout.pay(registrar: registrar, config: _config(), amountMinor: 100, clientId: null);
      expect(checkout.busy, isTrue);
      expect(await checkout.pay(registrar: registrar, config: _config(), amountMinor: 100, clientId: null), isNull);

      registration.complete(const Registered('md-1'));
      expect(await first, const Cancelled());
      expect(registrar.calls, hasLength(1));
      expect(checkout.busy, isFalse);
    });
  });
}
