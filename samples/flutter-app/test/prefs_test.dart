import 'package:flutter_test/flutter_test.dart';
import 'package:payrdr_flutter_sample/app_scope.dart';
import 'package:payrdr_flutter_sample/merchant/order_registrar.dart';
import 'package:payrdr_flutter_sample/prefs/app_prefs.dart';
import 'package:payrdr_flutter_sample/store/model/catalog.dart';
import 'package:payrdr_flutter_sample/store/model/scenario.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  setUp(() => SharedPreferences.setMockInitialValues({}));

  test('nothing ships with the app but the test gateway and a placeholder Apple Pay merchant', () async {
    final prefs = await AppPrefs.load();

    expect(prefs.baseUrl, 'https://dev.bpcbt.com/payment');
    expect(prefs.merchantLogin, isEmpty);
    expect(prefs.merchantPassword, isEmpty);
    expect(prefs.apiVersion, ApiVersion.v1);
    expect(prefs.orderType, OrderType.purchase);
    expect(prefs.apiKey, isEmpty);
    expect(prefs.googlePayGateway, isEmpty);
    expect(prefs.googlePayMerchantId, isEmpty);
    expect(prefs.applePayMerchantId, 'merchant.net.payrdr.samples.test');
    expect(prefs.scenario, const Scenario());
    expect(prefs.sheetTheme, SheetThemeOverride.cover);
    expect(prefs.localeOverride, isNull);
    expect(prefs.lastCover, isNull);
    expect(prefs.lastMdOrder, isNull);
  });

  test('every setting survives a restart', () async {
    final prefs = await AppPrefs.load();
    await prefs.setBaseUrl('https://gw.example.com/payment');
    await prefs.setCredentials(login: 'merchant-api', password: 'secret');
    await prefs.setApiVersion(ApiVersion.v2);
    await prefs.setOrderType(OrderType.aft);
    await prefs.setApiKey('api-key-1');
    await prefs.setGooglePay(gateway: 'example', merchantId: 'BCR2DN4T');
    await prefs.setApplePayMerchantId('merchant.com.example');
    await prefs.setScenario(const Scenario(buyerWithCards: true, wallet: false, scanner: false, sdkResultScreen: true));
    await prefs.setSheetTheme(SheetThemeOverride.dark);
    await prefs.setLocaleOverride('ar');
    await prefs.setLastCover(CoverId.tickets);
    await prefs.setLastMdOrder('md-1');
    await prefs.setMdOrder('md-core');
    await prefs.setBindingId('binding-1');
    await prefs.setPubKeyPem('-----BEGIN PUBLIC KEY-----');
    await prefs.setSdkCoreVariant('stored');

    final again = await AppPrefs.load();

    expect(again.baseUrl, 'https://gw.example.com/payment');
    expect((again.merchantLogin, again.merchantPassword), ('merchant-api', 'secret'));
    expect((again.apiVersion, again.apiKey), (ApiVersion.v2, 'api-key-1'));
    expect(again.orderType, OrderType.aft);
    expect((again.googlePayGateway, again.googlePayMerchantId), ('example', 'BCR2DN4T'));
    expect(again.applePayMerchantId, 'merchant.com.example');
    expect(again.scenario, const Scenario(buyerWithCards: true, wallet: false, scanner: false, sdkResultScreen: true));
    expect(again.sheetTheme, SheetThemeOverride.dark);
    expect(again.localeOverride, 'ar');
    expect(again.lastCover, CoverId.tickets);
    expect(again.lastMdOrder, 'md-1');
    expect((again.mdOrder, again.bindingId, again.pubKeyPem), ('md-core', 'binding-1', '-----BEGIN PUBLIC KEY-----'));
    expect(again.sdkCoreVariant, 'stored');
    expect(again.toString(), isNot(contains('secret')));
    expect(again.toString(), isNot(contains('api-key-1')));
  });

  test('null clears the language and the business to reopen', () async {
    final prefs = await AppPrefs.load();
    await prefs.setLocaleOverride('de');
    await prefs.setLastCover(CoverId.coffee);

    await prefs.setLocaleOverride(null);
    await prefs.setLastCover(null);

    expect(prefs.localeOverride, isNull);
    expect(prefs.lastCover, isNull);
  });

  test('a demo buyer exists from the start; Reset makes a new one', () async {
    final prefs = await AppPrefs.load();
    final first = prefs.clientId;

    expect(first, matches(RegExp(r'^demo-[0-9a-f]{8}-[0-9a-f]{4}$')));
    expect((await AppPrefs.load()).clientId, first);

    await prefs.resetClientId();
    expect(prefs.clientId, isNot(first));
  });

  test('orders go through the API chosen in the settings', () async {
    final prefs = await AppPrefs.load();
    expect(gatewayRegistrar(prefs), isA<GatewayOrderRegistrar>());

    await prefs.setApiVersion(ApiVersion.v2);
    expect(gatewayRegistrar(prefs), isA<ApiV2SessionRegistrar>());
  });

  test('API V1 orders are of the type chosen in the settings', () async {
    final prefs = await AppPrefs.load();
    expect(
      gatewayRegistrar(prefs),
      isA<GatewayOrderRegistrar>().having((r) => r.orderType, 'orderType', OrderType.purchase),
    );

    await prefs.setOrderType(OrderType.aft);
    expect(
      gatewayRegistrar(prefs),
      isA<GatewayOrderRegistrar>().having((r) => r.orderType, 'orderType', OrderType.aft),
    );
  });

  test('listeners hear every change', () async {
    final prefs = await AppPrefs.load();
    var changes = 0;
    prefs.addListener(() => changes++);

    await prefs.setScenario(const Scenario(wallet: false));
    await prefs.setCredentials(login: 'a');

    expect(changes, 2);
  });
}
