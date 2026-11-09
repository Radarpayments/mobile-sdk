import 'package:flutter/foundation.dart';
import 'package:payrdr_sdk/payrdr_sdk.dart';

import '../merchant/order_registrar.dart';
import '../store/model/scenario.dart';
import '../store/theme/cover_style.dart';

/*
 * Everything this demo does with the payment SDK lives in this file. To take payments in your app:
 *
 *   1. Describe the checkout:         PayrdrConfig(baseUrl: ..., theme: ..., googlePay: ..., applePay: ...)
 *   2. Get the order.                 Your backend registers it at the gateway — an mdOrder through API V1
 *      (register.do), a session id through API V2 (POST /api2/sessions); this demo does it in-app through
 *      OrderRegistrar only so that it runs without a backend.
 *   3. Open the payment sheet:        await PayrdrCheckout(config).present(mdOrder)
 *                                     or .presentSession(sessionId)
 *   4. Handle the result:             completed, failed, canceled or unknown — see toOutcome().
 *
 * The app needs no native code and no manifest entries for it: MainActivity stays a plain
 * FlutterActivity, AppDelegate is the one `flutter create` made, and the SDK's Android libraries bring
 * the INTERNET permission and the Google Pay entry. iOS needs only what the features themselves need:
 * the camera usage text for the scanner and the Apple Pay entitlement.
 */

/// What the store does after the payment sheet closes.
sealed class CheckoutOutcome {
  const CheckoutOutcome();
}

/// Paid; [paymentId] identifies the payment at the gateway.
final class Success extends CheckoutOutcome {
  const Success(this.paymentId);

  final String paymentId;

  @override
  bool operator ==(Object other) => other is Success && other.paymentId == paymentId;

  @override
  int get hashCode => paymentId.hashCode;
}

/// The bank or the gateway refused the payment.
final class Declined extends CheckoutOutcome {
  const Declined(this.message);

  final String? message;

  @override
  bool operator ==(Object other) => other is Declined && other.message == message;

  @override
  int get hashCode => message.hashCode;
}

/// The buyer closed the sheet.
final class Cancelled extends CheckoutOutcome {
  const Cancelled();

  @override
  bool operator ==(Object other) => other is Cancelled;

  @override
  int get hashCode => (Cancelled).hashCode;
}

/// The SDK could not learn how the payment ended; the store must not offer to pay again until it knows.
final class Unconfirmed extends CheckoutOutcome {
  const Unconfirmed(this.paymentId);

  final String paymentId;

  @override
  bool operator ==(Object other) => other is Unconfirmed && other.paymentId == paymentId;

  @override
  int get hashCode => paymentId.hashCode;
}

/// Where the store's "Pay" button is in the payment path.
sealed class CheckoutState {
  const CheckoutState();
}

final class CheckoutIdle extends CheckoutState {
  const CheckoutIdle();
}

/// Step 2: the order is being registered.
final class CheckoutRegistering extends CheckoutState {
  const CheckoutRegistering();
}

/// Step 3: the payment sheet is open.
final class CheckoutInSheet extends CheckoutState {
  const CheckoutInSheet();
}

/// Step 2 failed; the sheet was not opened.
final class CheckoutRegistrationFailed extends CheckoutState {
  const CheckoutRegistrationFailed(this.reason);

  final OrderRegistration reason;
}

/// Step 1: the SDK config for the current demo panel, cover theme and developer settings.
///
/// Each platform reads its own wallet: Google Pay on Android, Apple Pay on iOS. `GooglePayOptions()` is
/// enough: the SDK takes the rest from the gateway and the order. Here the developer settings may override
/// two values (blank = from the gateway), and the Google Pay sheet names the store the buyer is in
/// ([storeName]) instead of the gateway's merchant name: all the demo stores share one merchant. Apple Pay
/// needs the merchant ID of the app's entitlement; country and currency come from the order.
PayrdrConfig buildSdkConfig({
  required String baseUrl,
  required Scenario scenario,
  required CoverStyle cover,
  required SheetThemeOverride themeOverride,
  required String? locale,
  required String storeName,
  required String googlePayMerchantId,
  required String googlePayGateway,
  required String applePayMerchantId,
}) => PayrdrConfig(
  baseUrl: baseUrl,
  locale: locale,
  theme: switch (themeOverride) {
    SheetThemeOverride.cover => paymentTheme(cover),
    SheetThemeOverride.light => const PayrdrTheme.light(),
    SheetThemeOverride.dark => const PayrdrTheme.dark(),
    SheetThemeOverride.system => const PayrdrTheme.system(),
  },
  googlePay: scenario.wallet
      ? GooglePayOptions(
          merchantId: _blankToNull(googlePayMerchantId),
          merchantName: storeName,
          gateway: _blankToNull(googlePayGateway),
        )
      : null,
  applePay: scenario.wallet && applePayMerchantId.trim().isNotEmpty
      ? ApplePayOptions(merchantId: applePayMerchantId.trim())
      : null,
  cardScanner: scenario.scanner,
  showResultScreen: scenario.sdkResultScreen,
);

String? _blankToNull(String value) => value.trim().isEmpty ? null : value.trim();

/// The SDK sheet in the brand of [style]: colours and corners; typography stays the SDK's own.
/// Error and success colours come from the light or dark base palette.
PayrdrTheme paymentTheme(CoverStyle style) => PayrdrTheme.custom(
  base: style.dark ? Brightness.dark : Brightness.light,
  colors: PayrdrColors(
    primary: style.primary,
    onPrimary: style.onPrimary,
    background: style.background,
    onBackground: style.text,
    surface: style.fieldSurface,
    onSurface: style.text,
    outline: style.border,
    onSurfaceVariant: style.muted,
  ),
  cornerRadius: style.radius,
  buttonCornerRadius: style.pillButtons ? 999 : style.radius,
);

/// Step 4: turns the SDK result into what the store shows next.
CheckoutOutcome toOutcome(CheckoutResult result) => switch (result) {
  CheckoutCompleted(:final orderId) => Success(orderId),
  CheckoutCanceled() => const Cancelled(),
  CheckoutFailed(:final error) => Declined(error.message),
  // How the payment ended is not known yet: a real store checks the order on its server (or waits for the
  // gateway's callback) before telling the buyer anything, and never offers to pay again meanwhile.
  CheckoutUnknown(:final orderId) => Unconfirmed(orderId),
};

/// Opens the sheet for [order]; tests replace it.
typedef PresentSheet = Future<CheckoutResult> Function(PayrdrConfig config, Registered order);

/// Step 3: an API V2 session opens by its id, an API V1 order by its mdOrder.
Future<CheckoutResult> _presentSheet(PayrdrConfig config, Registered order) => switch (order.sessionId) {
  final sessionId? => PayrdrCheckout(config).presentSession(sessionId),
  null => PayrdrCheckout(config).present(order.mdOrder),
};

/// Steps 2 and 3 for the store's "Pay" button: registers the order, then opens the sheet. A second tap
/// while the order is being registered or the sheet is open does nothing. [onMdOrder] sees every
/// registered order (the demo shows the last one in the developer settings).
class StoreCheckout extends ChangeNotifier {
  StoreCheckout({required this.onMdOrder, PresentSheet? present}) : present = present ?? _presentSheet;

  final ValueChanged<String> onMdOrder;
  final PresentSheet present;

  CheckoutState _state = const CheckoutIdle();
  bool _disposed = false;

  CheckoutState get state => _state;

  bool get busy => _state is CheckoutRegistering || _state is CheckoutInSheet;

  /// Pays [amountMinor] euro cents; [clientId] = the demo buyer with saved cards, null = guest. Gives the
  /// outcome once the sheet closes, null when it did not open.
  Future<CheckoutOutcome?> pay({
    required OrderRegistrar registrar,
    required PayrdrConfig config,
    required int amountMinor,
    required String? clientId,
  }) async {
    if (busy) return null;
    _set(const CheckoutRegistering());
    final registration = await registrar.register(amountMinor, clientId);
    if (registration is! Registered) {
      _set(CheckoutRegistrationFailed(registration));
      return null;
    }
    onMdOrder(registration.mdOrder);
    _set(const CheckoutInSheet());
    try {
      return toOutcome(await present(config, registration));
    } finally {
      _set(const CheckoutIdle());
    }
  }

  void _set(CheckoutState state) {
    _state = state;
    if (!_disposed) notifyListeners();
  }

  @override
  void dispose() {
    _disposed = true;
    super.dispose();
  }
}
