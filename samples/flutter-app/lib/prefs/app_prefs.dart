import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../merchant/order_registrar.dart';
import '../random_hex.dart';
import '../store/invest/invest_account.dart';
import '../store/model/catalog.dart';
import '../store/model/scenario.dart';

/// The demo's settings, stored only on the device. Listeners hear every change, so the screens and
/// the next payment always see the current values.
class AppPrefs extends ChangeNotifier {
  AppPrefs._(this._prefs);

  /// Reads the stored settings and makes sure there is a demo buyer.
  static Future<AppPrefs> load() async {
    final prefs = AppPrefs._(await SharedPreferences.getInstance());
    if (prefs.clientId.isEmpty) await prefs.resetClientId();
    return prefs;
  }

  final SharedPreferences _prefs;

  static const defaultBaseUrl = 'https://dev.bpcbt.com/payment';
  static const defaultMdOrder = 'c400b41a-aa3d-43db-8727-ac4ca9e8f701';
  static const defaultBindingId = '513b17f4-e32e-414f-8c74-936fd7027baa';

  /// A placeholder: Apple Pay needs the merchant ID of a real Apple developer account.
  static const defaultApplePayMerchantId = 'merchant.net.payrdr.samples.test';
  static const defaultPubKeyPem = '''
-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAij/G3JVV3TqYCFZTPmwi
4JduQMsZ2HcFLBBA9fYAvApv3FtA+zKdUGgKh/OPbtpsxe1C57gIaRclbzMoafTb
0eOdj+jqSEJMlVJYSiZ8Hn6g67evhu9wXh5ZKBQ1RUpqL36LbhYnIrP+TEGR/Vyj
bC6QTfaktcRfa8zRqJczHFsyWxnlfwKLfqKz5wSqXkShcrwcfRJCyDRjZX6OFUEC
HsWVK3WMcOV3WZREwbCkh/o5R5Vl6xoyLvSqVEKQiHupJcZu9UEOJiP3yNCn9YPg
yFs2vrCeg6qxDPFnCfetcDCLjjLenGF7VyZzBJ9G2NP3k/mNVtD8Kl7lpiurwY7E
ZwIDAQAB
-----END PUBLIC KEY-----''';

  /// Gateway base URL for the SDK and order registration.
  String get baseUrl => _prefs.getString(_Keys.baseUrl) ?? defaultBaseUrl;

  /// No merchant login or password ships with the app: they are typed in the developer settings.
  String get merchantLogin => _prefs.getString(_Keys.login) ?? '';
  String get merchantPassword => _prefs.getString(_Keys.password) ?? '';

  /// The merchant API demo orders are registered through; API V1 until the developer settings say otherwise.
  ApiVersion get apiVersion => ApiVersion.values.asNameMap()[_prefs.getString(_Keys.apiVersion)] ?? ApiVersion.v1;

  /// What API V1 demo orders are: payments until the developer settings say otherwise.
  OrderType get orderType => OrderType.values.asNameMap()[_prefs.getString(_Keys.orderType)] ?? OrderType.purchase;

  /// No API V2 key ships with the app either.
  String get apiKey => _prefs.getString(_Keys.apiKey) ?? '';

  /// Empty: Google Pay takes its gateway and merchant id from the gateway's settings for the merchant.
  String get googlePayGateway => _prefs.getString(_Keys.googlePayGateway) ?? '';
  String get googlePayMerchantId => _prefs.getString(_Keys.googlePayMerchantId) ?? '';
  String get applePayMerchantId => _prefs.getString(_Keys.applePayMerchantId) ?? defaultApplePayMerchantId;

  /// The demo buyer whose cards are saved; the order carries it when the scenario says "With cards".
  String get clientId => _prefs.getString(_Keys.clientId) ?? '';

  Scenario get scenario => Scenario(
    buyerWithCards: _prefs.getBool(_Keys.buyerWithCards) ?? false,
    wallet: _prefs.getBool(_Keys.wallet) ?? true,
    scanner: _prefs.getBool(_Keys.scanner) ?? true,
    sdkResultScreen: _prefs.getBool(_Keys.sdkResult) ?? false,
  );

  /// The business to open on the next launch; null opens the start screen.
  CoverId? get lastCover => CoverId.values.asNameMap()[_prefs.getString(_Keys.lastCover)];

  /// mdOrder of the last registered order, shown in the developer settings.
  String? get lastMdOrder => _prefs.getString(_Keys.lastMdOrder);

  SheetThemeOverride get sheetTheme =>
      SheetThemeOverride.values.asNameMap()[_prefs.getString(_Keys.sheetTheme)] ?? SheetThemeOverride.cover;

  /// Language of the demo and the SDK sheet; null follows the system.
  String? get localeOverride => _prefs.getString(_Keys.locale);

  /// The Northwind Invest demo account; the seed until a transfer or an investment changes it.
  InvestAccount get investAccount => InvestAccount.fromJson(_prefs.getString(_Keys.investAccount));

  /// Saves the Northwind Invest demo account; null puts it back as a fresh install has it.
  Future<void> setInvestAccount(InvestAccount? value) => _setString(_Keys.investAccount, value?.toJson());

  /// The SDK Core form: what was pasted last time. Card data are never stored.
  String get mdOrder => _prefs.getString(_Keys.mdOrder) ?? defaultMdOrder;
  String get pubKeyPem => _prefs.getString(_Keys.pubKeyPem) ?? defaultPubKeyPem;
  String get bindingId => _prefs.getString(_Keys.bindingId) ?? defaultBindingId;
  String get sdkCoreVariant => _prefs.getString(_Keys.sdkCoreVariant) ?? 'new';

  Future<void> setBaseUrl(String value) => _setString(_Keys.baseUrl, value);

  /// Stores the merchant login and/or password typed in the developer settings; they never leave the device.
  Future<void> setCredentials({String? login, String? password}) async {
    if (login != null) await _prefs.setString(_Keys.login, login);
    if (password != null) await _prefs.setString(_Keys.password, password);
    notifyListeners();
  }

  /// Chooses the merchant API demo orders are registered through.
  Future<void> setApiVersion(ApiVersion value) => _setString(_Keys.apiVersion, value.name);

  /// Chooses whether API V1 demo orders are payments or AFT transfers.
  Future<void> setOrderType(OrderType value) => _setString(_Keys.orderType, value.name);

  /// Stores the API V2 key typed in the developer settings; it never leaves the device.
  Future<void> setApiKey(String value) => _setString(_Keys.apiKey, value);

  /// Google Pay gateway name and/or merchant id; null leaves a value as it is.
  Future<void> setGooglePay({String? gateway, String? merchantId}) async {
    if (gateway != null) await _prefs.setString(_Keys.googlePayGateway, gateway);
    if (merchantId != null) await _prefs.setString(_Keys.googlePayMerchantId, merchantId);
    notifyListeners();
  }

  Future<void> setApplePayMerchantId(String value) => _setString(_Keys.applePayMerchantId, value);

  Future<void> setScenario(Scenario value) async {
    await _prefs.setBool(_Keys.buyerWithCards, value.buyerWithCards);
    await _prefs.setBool(_Keys.wallet, value.wallet);
    await _prefs.setBool(_Keys.scanner, value.scanner);
    await _prefs.setBool(_Keys.sdkResult, value.sdkResultScreen);
    notifyListeners();
  }

  /// Replaces the demo buyer with a new one, without saved cards.
  Future<void> resetClientId() => _setString(_Keys.clientId, _newClientId());

  Future<void> setLastCover(CoverId? value) => _setString(_Keys.lastCover, value?.name);

  Future<void> setLastMdOrder(String value) => _setString(_Keys.lastMdOrder, value);

  Future<void> setSheetTheme(SheetThemeOverride value) => _setString(_Keys.sheetTheme, value.name);

  Future<void> setLocaleOverride(String? code) => _setString(_Keys.locale, code);

  Future<void> setMdOrder(String value) => _setString(_Keys.mdOrder, value);

  Future<void> setPubKeyPem(String value) => _setString(_Keys.pubKeyPem, value);

  Future<void> setBindingId(String value) => _setString(_Keys.bindingId, value);

  Future<void> setSdkCoreVariant(String value) => _setString(_Keys.sdkCoreVariant, value);

  /// Saves [value]; null removes it.
  Future<void> _setString(String key, String? value) async {
    await (value == null ? _prefs.remove(key) : _prefs.setString(key, value));
    notifyListeners();
  }

  @override
  String toString() =>
      'AppPrefs(baseUrl: $baseUrl, apiVersion: ${apiVersion.name}, orderType: ${orderType.name}, '
      'merchantLogin: $merchantLogin, clientId: $clientId)';
}

// Short on purpose: with a 41-character id the test gateway registered the order but declined the
// payment that saves the card (71015). The shape is the start of a UUID, as in the native samples.
String _newClientId() => 'demo-${randomHex(8)}-${randomHex(4)}';

abstract final class _Keys {
  static const baseUrl = 'base_url';
  static const mdOrder = 'md_order';
  static const pubKeyPem = 'pub_key_pem';
  static const bindingId = 'core_bid';
  static const sdkCoreVariant = 'core_var';
  static const googlePayGateway = 'gpay_gw';
  static const googlePayMerchantId = 'gpay_mid';
  static const applePayMerchantId = 'apay_mid';
  static const login = 'store_login';
  static const password = 'store_password';
  static const apiVersion = 'store_api_version';
  static const orderType = 'store_order_type';
  static const apiKey = 'store_api_key';
  static const clientId = 'store_client_id';
  static const buyerWithCards = 'store_buyer_cards';
  static const wallet = 'store_wallet';
  static const scanner = 'store_scanner';
  static const sdkResult = 'store_sdk_result';
  static const lastCover = 'store_last_cover';
  static const lastMdOrder = 'store_last_md_order';
  static const sheetTheme = 'store_sheet_theme';
  static const locale = 'store_locale';
  static const investAccount = 'invest_account';
}
