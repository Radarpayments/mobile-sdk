import 'package:flutter/widgets.dart';
import 'package:http/http.dart' as http;

import 'checkout/checkout.dart';
import 'l10n/app_localizations.dart';
import 'merchant/order_registrar.dart';
import 'prefs/app_prefs.dart';
import 'store/store_model.dart';

/// Builds the "your server" registrar from the developer settings; tests provide a fake.
typedef RegistrarFactory = OrderRegistrar Function(AppPrefs prefs);

final _http = http.Client();

OrderRegistrar gatewayRegistrar(AppPrefs prefs) => merchantRegistrar(
  apiVersion: prefs.apiVersion,
  orderType: prefs.orderType,
  baseUrl: prefs.baseUrl,
  login: prefs.merchantLogin,
  password: prefs.merchantPassword,
  apiKey: prefs.apiKey,
  http: _http,
);

/// "Your server" for Northwind Invest: the same, but a transfer is registered with the investor's details.
typedef TransferRegistrarFactory = OrderRegistrar Function(AppPrefs prefs, TransferPrefill prefill);

OrderRegistrar gatewayTransferRegistrar(AppPrefs prefs, TransferPrefill prefill) => merchantRegistrar(
  apiVersion: prefs.apiVersion,
  orderType: prefs.orderType,
  baseUrl: prefs.baseUrl,
  login: prefs.merchantLogin,
  password: prefs.merchantPassword,
  apiKey: prefs.apiKey,
  http: _http,
  prefill: prefill,
);

/// What every screen shares: the settings, the carts, "your server" and the way the sheet opens.
/// The objects stay the same for the whole app; screens listen to them for changes.
class AppScope extends InheritedWidget {
  const AppScope({
    super.key,
    required this.prefs,
    required this.store,
    this.registrarFactory = gatewayRegistrar,
    this.transferRegistrarFactory = gatewayTransferRegistrar,
    this.presentSheet,
    required super.child,
  });

  final AppPrefs prefs;
  final StoreModel store;
  final RegistrarFactory registrarFactory;
  final TransferRegistrarFactory transferRegistrarFactory;

  /// Replaces the SDK sheet in tests; null opens the real one.
  final PresentSheet? presentSheet;

  static AppScope of(BuildContext context) => context.getInheritedWidgetOfExactType<AppScope>()!;

  @override
  bool updateShouldNotify(AppScope oldWidget) => false;
}

extension AppContext on BuildContext {
  AppLocalizations get l10n => AppLocalizations.of(this);

  AppPrefs get prefs => AppScope.of(this).prefs;

  StoreModel get store => AppScope.of(this).store;
}
